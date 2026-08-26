package nl.framegengine.editor.panels;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.*;
import imgui.type.ImBoolean;
import imgui.type.ImFloat;
import imgui.type.ImInt;
import imgui.type.ImString;
import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.entity.GameObject;
import nl.framegengine.core.lighting.Light;
import nl.framegengine.core.utils.ClassHelper;
import nl.framegengine.core.utils.IJsonSerializable;
import nl.framegengine.core.utils.JsonHelper;
import nl.framegengine.core.utils.ObjectPool;
import nl.framegengine.core.visual.Material;
import nl.framegengine.core.visual.Texture;
import nl.framegengine.core.visual.TextureLoader;
import nl.framegengine.editor.EditorPanel;
import nl.framegengine.editor.ImGuiHelper;
import nl.framegengine.editor.ManifestHelper;
import nl.framegengine.editor.editorComponents.Icons;
import nl.framegengine.editor.editorComponents.InputField;
import nl.framegengine.editor.editorComponents.Panel;
import nl.framegengine.editor.editorComponents.Text;
import nl.framegengine.editor.editorRenderers.MaterialRenderer;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class InfoPanel extends EditorPanel {

    private IJsonSerializable currentlySelectedObject = null;
    private final List<Field> hierarchyObjects = new ArrayList<>();
    private String[] textureNames = new String[0];
    private MaterialRenderer materialRenderer;
    private int materialPreviewFBOID = -1;
    private ImString nameBuffer = new ImString();


    public InfoPanel(int posX, int posY, int sizeX, int sizeY) {
        super(posX, posY, sizeX, sizeY);
        ManifestHelper.addEventCallback(this::updateTextureList);
        updateTextureList();
        windowName = Icons.BOX + " Info";
        materialRenderer = new MaterialRenderer();
    }

    public void postStartInit(){
        try {
            materialRenderer.postStartInit();
            materialPreviewFBOID = materialRenderer.getPreviewFBOID();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void renderFrame() {
        if(currentlySelectedObject == null) return;
        drawTitlePanel(currentlySelectedObject);

        if(currentlySelectedObject instanceof GameObject gameObject){
            drawTransform(gameObject);

            gameObject.getComponents().forEach(comp -> {
                Panel.startPanel();
                ImGui.text(Icons.GetIcon(comp) + " " + comp.getClass().getSimpleName());
                ImGui.spacing();

                boolean hasFields = false;

                Class<?> clazz = comp.getClass();

                while (clazz != null && clazz != Object.class) {
                    for (Field field : clazz.getDeclaredFields()) {
                        if (Modifier.isPrivate(field.getModifiers()) || comp.getFieldsToIgnore().contains(field.getName())) continue;

                        try {
                            field.setAccessible(true);
                            Object value = field.get(comp);

                            if (value == null) continue;

                            drawOption(field, comp);
                            hasFields = true;
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    clazz = clazz.getSuperclass();
                }

                ImGui.setCursorPosX(ImGui.getCursorPosX() + Panel.getPaddingX());
                if(!hasFields) ImGui.text(Icons.GetIcon(comp) + " No fields found");

                Panel.endPanel();
            });
        }

        if(currentlySelectedObject instanceof Light light){
            drawLightSettingsPanel(light);
        }

        if(currentlySelectedObject instanceof Material material){
            Panel.startPanel();
            ImGui.beginGroup();
            drawMaterialPanel(material);
            ImGui.endGroup();
            Panel.endPanel();
        }
    }

    public void setCurrentlySelectedObject(IJsonSerializable selectedObject){
        currentlySelectedObject = selectedObject;
        hierarchyObjects.clear();

        if(currentlySelectedObject == null) return;
        try {
            if(currentlySelectedObject instanceof GameObject) {
                hierarchyObjects.add(ClassHelper.getFieldFromObject("isEnabled", currentlySelectedObject.getClass()));
                hierarchyObjects.add(ClassHelper.getFieldFromObject("localPosition", currentlySelectedObject.getClass()));
                hierarchyObjects.add(ClassHelper.getFieldFromObject("localRotation", currentlySelectedObject.getClass()));
                hierarchyObjects.add(ClassHelper.getFieldFromObject("scale", currentlySelectedObject.getClass()));
                nameBuffer.set(((GameObject) currentlySelectedObject).getName());
            }
        } catch (NoSuchFieldException ignored) {}
        ClassHelper.getAllPublicAndProtectedProperties(hierarchyObjects, currentlySelectedObject.getClass());
    }

    private void drawOption(Field field, Object drawingObject) throws IllegalAccessException {
        Object objectValue = field.get(drawingObject);
        String fieldName = field.getName() + "##" + field.hashCode();

        if(drawingObject instanceof GameObject && GameObject.fieldsToIgnore.contains(field.getName())) return;

        ImGui.setCursorPosX(ImGui.getCursorPosX() + Panel.getPaddingX());

        // Special handling for Texture fields (even if null)
        if (field.getType() == Texture.class) {
            ImGui.setWindowFontScale(1.1f);
            ImGui.text(field.getName());
            if (objectValue != null) {
                drawObject(objectValue);
            }
            drawManifestType(ManifestHelper.manifestFileType.TEXTURE, objectValue, field, drawingObject);
            ImGui.setWindowFontScale(0.4f);
            ImGui.newLine();
            ImGui.setWindowFontScale(1f);
            return;
        }

        if(objectValue == null) {
            ImGui.setWindowFontScale(1.1f);
            ImGui.text(field.getName());
            ImGui.text("null");
            ImGui.setWindowFontScale(0.4f);
            ImGui.newLine();
            ImGui.setWindowFontScale(1f);
            return;
        }

        ImGui.setWindowFontScale(1.1f);
        switch (objectValue) {
            case Float f -> {
                ImFloat ImFl = new ImFloat(f);
                if (ImGui.inputFloat(fieldName, ImFl)) {
                    field.setAccessible(true);
                    field.set(drawingObject, ImFl.floatValue());
                    if(drawingObject instanceof GameObject go) go.callUpdate();
                }
            }
            case String str -> {
                ImString imStr = new ImString(str);
                if (ImGui.inputText(fieldName, imStr)) {
                    field.setAccessible(true);
                    field.set(drawingObject, imStr.get());
                    if(drawingObject instanceof GameObject go) go.callUpdate();
                }
            }
            case Integer integer -> {
                ImInt imInteger = new ImInt(integer);
                if (ImGui.inputInt(fieldName, imInteger)) {
                    field.setAccessible(true);
                    field.set(drawingObject, imInteger.get());
                    if(drawingObject instanceof GameObject go) go.callUpdate();
                }
            }
            case Boolean bool -> {
                ImBoolean imBool = new ImBoolean(bool);
                if (ImGui.checkbox(fieldName, imBool)) {
                    field.setAccessible(true);
                    field.set(drawingObject, imBool.get());
                    if(drawingObject instanceof GameObject go) go.callUpdate();
                }
            }
            case Vector3f vector -> {
                float[] vec3Array = new float[]{vector.x, vector.y, vector.z};
                if (ImGui.inputFloat3(fieldName, vec3Array)) {
                    vector.set(vec3Array[0], vec3Array[1], vec3Array[2]);
                    if (drawingObject instanceof GameObject go) {
                        field.setAccessible(true);
                        field.set(drawingObject, vector);
                        go.callUpdate();
                    } else {
                        field.setAccessible(true);
                        field.set(drawingObject, vector);
                    }
                }
            }
            case Vector4f vector -> {
                float[] vec4Array = new float[]{vector.x, vector.y, vector.z, vector.w};
                if (ImGui.inputFloat4(fieldName, vec4Array)) {
                    vector.set(vec4Array[0], vec4Array[1], vec4Array[2], vec4Array[3]);
                    field.setAccessible(true);
                    field.set(drawingObject, vector);
                    if(drawingObject instanceof GameObject go) go.callUpdate();
                }
            }
            case Quaternionf quaternion -> {
                float[] quaternionArray = new float[]{quaternion.x, quaternion.y, quaternion.z, quaternion.w};
                if (ImGui.inputFloat4(fieldName, quaternionArray)) {
                    quaternion.set(quaternionArray[0], quaternionArray[1], quaternionArray[2], quaternionArray[3]);
                    field.setAccessible(true);
                    field.set(drawingObject, quaternion);
                    if(drawingObject instanceof GameObject go) go.callUpdate();
                }
            }
            case Material material -> {
                drawMaterialPanel(material);
            }
            case Set<?> set -> {
                if(!set.isEmpty()) {
                    if(ImGui.collapsingHeader(field.getName())) {
                        set.forEach(setItem -> {
                            if(ImGui.collapsingHeader(setItem.getClass().getSimpleName() + "##" + setItem.hashCode() + currentlySelectedObject.getGuid())) {
                                drawObject(setItem);
                            }
                        });
                    }
                }
            }
            default -> {
                ImGui.text(field.getName());
                ImGui.text(objectValue.toString());
            }
        }
        ImGui.setWindowFontScale(0.4f);
        ImGui.newLine();
        ImGui.setWindowFontScale(1f);
    }

    private void drawObject(Object object){
        //ImGui.text(object.getClass().getSimpleName());
        ImGui.indent(10);
        List<Field> objectFields = new ArrayList<>();
        ClassHelper.getAllPublicAndProtectedProperties(objectFields, object.getClass());
        objectFields.forEach(objectField -> {
            try {
                objectField.setAccessible(true);
                drawOption(objectField, object);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
        ImGui.unindent(10);
    }

    private void drawManifestType(ManifestHelper.manifestFileType fileType, Object object, Field field, Object drawingObject){
        if(fileType == ManifestHelper.manifestFileType.TEXTURE){
            Texture texture = (Texture)object;
            final String NO_TEXTURE_LABEL = "<No Texture>";

            // Show preview if texture exists
            if(texture != null) {
                ImGui.image(texture.getId(), new ImVec2(32, 32));
            }

            int selectedIndex = 0; // Default to no texture
            if(texture != null && texture.getGuid() != null) {
                String textureGuid = texture.getGuid();
                for(int i = 0; i < textureNames.length; i++) {
                    String itemName = textureNames[i];
                    if(!itemName.equals(NO_TEXTURE_LABEL)) {
                        String itemGuid = ImGuiHelper.guidFromName(itemName);
                        if(textureGuid.equals(itemGuid)) {
                            selectedIndex = i;
                            break;
                        }
                    }
                }
            }

            ImInt currentSelectedItem = new ImInt(selectedIndex);

            if(ImGui.combo("##" + fileType.name().toLowerCase() + field.hashCode(), currentSelectedItem, textureNames)){
                String selectedName = textureNames[currentSelectedItem.get()];

                if(selectedName.equals(NO_TEXTURE_LABEL)) {
                    try {
                        field.setAccessible(true);
                        field.set(drawingObject, null);
                        if(drawingObject instanceof GameObject go) go.callUpdate();
                        Debug.log("Texture set to null");
                    } catch (IllegalAccessException e) {
                        Debug.logError("Failed to set texture to null: " + e.getMessage());
                    }
                } else {
                    String textureGUID = ImGuiHelper.guidFromName(selectedName);
                    String texturePath = ManifestHelper.getPathByGuid(ManifestHelper.manifestFileType.TEXTURE, textureGUID);
                    if(texturePath == null || texturePath.isEmpty()) {
                        Debug.logError("Could not find path for texture GUID: " + textureGUID);
                        return;
                    }

                    Texture selectedTexture = buildTextureForField(field.getName(), texturePath);
                    if(selectedTexture.getId() != -1 && selectedTexture.getId() != TextureLoader.getDefaultTextureId()){
                        Debug.log("Selected " + selectedName + " with ID: " + selectedTexture.getId() + " and GUID: " + selectedTexture.getGuid());
                        try {
                            field.setAccessible(true);
                            field.set(drawingObject, selectedTexture);
                            if(drawingObject instanceof GameObject go) go.callUpdate();
                        } catch (IllegalAccessException e) {
                            Debug.logError("Failed to update texture field: " + e.getMessage());
                        }
                    } else {
                        Debug.logError("Failed to load texture for GUID: " + textureGUID);
                    }
                }
            }
        }else{
            ImGui.text("Manifest dropdown not implement for type "+ fileType.name());
        }
    }

    private String getRawFieldName(String fieldNameRaw){
        if(!fieldNameRaw.isEmpty()) return fieldNameRaw.split("##")[0];
        return fieldNameRaw;
    }

    private Texture buildTextureForField(String fieldName, String texturePath) {
        return switch (fieldName) {
            case "normalMap"    -> new Texture(texturePath, false, false, true, true,  false);
            case "roughnessMap",
                 "metallicMap",
                 "aoMap"        -> new Texture(texturePath, false, false, true, false, true);
            default             -> new Texture(texturePath);
        };
    }

    private void updateTextureList(){
        List<String> manifestItems = new ArrayList<>();
        manifestItems.add("<No Texture>"); // Add empty option for null texture - using angle brackets to avoid conflicts
        ManifestHelper.getTextures().forEach(manifestItem -> manifestItems.add(manifestItem.get("filename")+"##"+manifestItem.get("guid")));
        textureNames = manifestItems.toArray(new String[0]);
    }

    // Individual items
    private void drawTitlePanel(IJsonSerializable jsonObject) {
        boolean isGameObject = jsonObject instanceof GameObject;

        Panel.startPanel();

        String objectType = JsonHelper.getIJsonSerializableType(jsonObject);
        ImGui.text(Icons.GetIcon(jsonObject) + " " + objectType);

        if (isGameObject) {
            GameObject gameObject = (GameObject) jsonObject;

            float checkboxWidth = ImGui.getFrameHeight() + ImGui.getStyle().getItemInnerSpacingX() + ImGui.calcTextSize("Enabled").x;

            ImGui.sameLine();

            float rightEdge = ImGui.getCursorPosX() + ImGui.getContentRegionAvailX();

            ImGui.newLine();

            ImGui.setCursorPosX(ImGui.getCursorPosX() + Panel.getPaddingX());
            ImGui.setNextItemWidth(ImGui.getContentRegionAvailX() - checkboxWidth * 2);
            if(ImGui.inputText("Name##" + gameObject.getGuid(), nameBuffer)){
                gameObject.setName(nameBuffer.get());
            }

            ImGui.sameLine();
            ImGui.setCursorPosX(rightEdge - checkboxWidth - Panel.getPaddingX());

            if (ImGui.checkbox("Enabled##" + gameObject.getGuid(), gameObject.isEnabled())) {
                gameObject.setEnabled(!gameObject.isEnabled());
                gameObject.callUpdate();
            }



        }

        Panel.endPanel();
    }

    private void drawTransform(GameObject gameObject){
        Panel.startPanel();
        ImGui.text(Icons.TRANSFORM + " Transform");

        float tableWidth = Panel.getPanelWidth() - Panel.getPaddingX() * 2.0f;

        ImGui.setCursorPosX(ImGui.getCursorPosX() + Panel.getPaddingX());
        if (ImGui.beginTable("transform##" + gameObject.getGuid(), 2, ImGuiTableFlags.SizingStretchProp, new ImVec2(tableWidth, 0))) {

            ImGui.tableSetupColumn("Label", ImGuiTableColumnFlags.WidthStretch, 1.0f);
            ImGui.tableSetupColumn("Value", ImGuiTableColumnFlags.WidthStretch, 3.0f);

            ImGui.tableNextRow();
            ImGui.tableNextColumn();
            ImGui.tableNextColumn();

            if (ImGui.beginTable(
                    "transformXYZHeader##" + gameObject.getGuid(), 3, ImGuiTableFlags.SizingStretchSame)) {

                ImGui.tableNextColumn();
                Text.ColoredAndCentered("X", 0.85f, 0.25f, 0.25f);

                ImGui.tableNextColumn();
                Text.ColoredAndCentered("Y", 0.30f, 0.85f, 0.30f);

                ImGui.tableNextColumn();
                Text.ColoredAndCentered("Z", 0.35f, 0.50f, 1.00f);

                ImGui.endTable();
            }

            ImGui.tableNextRow();
            ImGui.tableNextColumn();
            ImGui.text("Position");

            ImGui.tableNextColumn();
            Vector3f transformVector = ObjectPool.VECTOR3F_POOL.obtain()
                    .set(gameObject.getLocalPosition());

            float[] position = {
                    transformVector.x,
                    transformVector.y,
                    transformVector.z
            };


            ImGui.setNextItemWidth(-1);
            if (ImGui.inputFloat3("##position", position)) {
                gameObject.setPosition(position[0], position[1], position[2]);
            }

            ImGui.tableNextRow();
            ImGui.tableNextColumn();
            ImGui.text("Rotation");

            ImGui.tableNextColumn();
            transformVector = ObjectPool.VECTOR3F_POOL.obtain()
                    .set(gameObject.getLocalEulerAngles());

            float[] rotation = {
                    transformVector.x,
                    transformVector.y,
                    transformVector.z
            };

            ImGui.setNextItemWidth(-1);
            if (ImGui.inputFloat3("##rotation", rotation)) {
                gameObject.setRotation(rotation[0], rotation[1], rotation[2]);
            }

            ImGui.tableNextRow();
            ImGui.tableNextColumn();
            ImGui.text("Scale");

            ImGui.tableNextColumn();
            transformVector = ObjectPool.VECTOR3F_POOL.obtain()
                    .set(gameObject.getLocalScale());

            float[] scale = {
                    transformVector.x,
                    transformVector.y,
                    transformVector.z
            };

            ImGui.setNextItemWidth(-1);
            if (ImGui.inputFloat3("##scale", scale)) {
                gameObject.setScale(scale[0], scale[1], scale[2]);
            }

            ImGui.endTable();

            ObjectPool.VECTOR3F_POOL.free(transformVector);
        }

        Panel.endPanel();
    }

    private void drawLightSettingsPanel(Light light) {
        Panel.startPanel();

        ImGui.text(Icons.LIGHT + " " + light.getClass().getSimpleName());

        float tableWidth = Panel.getPanelWidth() - Panel.getPaddingX() * 2.0f;
        ImGui.setCursorPosX(ImGui.getCursorPosX() + Panel.getPaddingY());

        if (ImGui.beginTable("lightSettings##" + light.getGuid(), 2, ImGuiTableFlags.SizingStretchProp, new ImVec2(tableWidth, 0))) {

            ImGui.tableSetupColumn("Label", ImGuiTableColumnFlags.WidthStretch, 1.0f);
            ImGui.tableSetupColumn("Value", ImGuiTableColumnFlags.WidthStretch, 3.0f);

            ImGui.tableNextRow();
            ImGui.tableNextColumn();
            ImGui.tableNextColumn();

            if (ImGui.beginTable(
                    "lightSettingsRGBHeader##" + light.getGuid(), 3, ImGuiTableFlags.SizingStretchSame)) {

                ImGui.tableNextColumn();
                Text.ColoredAndCentered("R", 0.85f, 0.25f, 0.25f);

                ImGui.tableNextColumn();
                Text.ColoredAndCentered("G", 0.30f, 0.85f, 0.30f);

                ImGui.tableNextColumn();
                Text.ColoredAndCentered("B", 0.35f, 0.50f, 1.00f);

                ImGui.endTable();
            }

            ImGui.tableNextRow();
            ImGui.tableNextColumn();

            ImGui.text("Color");

            ImGui.tableNextColumn();
            Vector3f colorVector = ObjectPool.VECTOR3F_POOL.obtain()
                    .set(light.getColor());

            float[] lightColor = {
                    colorVector.x,
                    colorVector.y,
                    colorVector.z
            };


            ImGui.setNextItemWidth(-1);
            if (ImGui.inputFloat3("##lightColor", lightColor)) {
                light.setColor(lightColor[0], lightColor[1], lightColor[2]);
            }

            ImGui.endTable();

            ImGui.setCursorPosX(ImGui.getCursorPosX() + Panel.getPaddingY());
            ImFloat intensity = new ImFloat(light.getIntensity());
            if (ImGui.inputFloat("intensity##" + light.getGuid(), intensity)) {
                light.setIntensity(intensity.get());
            }

            ImGui.setCursorPosX(ImGui.getCursorPosX() + Panel.getPaddingY());
            ImFloat linear = new ImFloat(light.getIntensity());
            if (ImGui.inputFloat("linear##" + light.getGuid(), linear)) {
                light.setLinear(linear.get());
            }

            ImGui.setCursorPosX(ImGui.getCursorPosX() + Panel.getPaddingY());
            ImFloat exponent = new ImFloat(light.getExponent());
            if (ImGui.inputFloat("exponent##" + light.getGuid(), exponent)) {
                light.setExponent(exponent.get());
            }

            ImGui.setCursorPosX(ImGui.getCursorPosX() + Panel.getPaddingY());
            ImFloat constant = new ImFloat(light.getConstant());
            if (ImGui.inputFloat("constant##" + light.getGuid(), constant)) {
                light.setConstant(constant.get());
            }

            Panel.endPanel();
        }
    }

    private void drawMaterialPanel(Material material) {
        ImGui.text(Icons.HIGHLIGHT + " Material");
        ImGui.text("Shader: " + material.getShader().getClass().getSimpleName());
        ImGui.spacing();

        float tableWidth = Panel.getPanelWidth() - Panel.getPaddingX() * 2.0f;

        if (ImGui.beginTable("materialSettings##" + material.getGuid(), 2, ImGuiTableFlags.SizingStretchProp, new ImVec2(tableWidth, 0))) {

            ImGui.tableSetupColumn("Label", ImGuiTableColumnFlags.WidthStretch, 1.0f);
            ImGui.tableSetupColumn("Value", ImGuiTableColumnFlags.WidthStretch, 4.0f);

            ImGui.tableNextRow();
            ImGui.tableNextColumn();
            ImGui.tableNextColumn();

            if (ImGui.beginTable(
                    "materialRGBAHeader##" + material.getGuid(), 4, ImGuiTableFlags.SizingStretchSame)) {

                ImGui.tableNextColumn();
                Text.ColoredAndCentered("R", 0.85f, 0.25f, 0.25f);

                ImGui.tableNextColumn();
                Text.ColoredAndCentered("G", 0.30f, 0.85f, 0.30f);

                ImGui.tableNextColumn();
                Text.ColoredAndCentered("B", 0.35f, 0.50f, 1.00f);

                ImGui.tableNextColumn();
                Text.ColoredAndCentered("A", 1.00f, 1.00f, 1.00f);

                ImGui.endTable();
            }

            ImGui.tableNextRow();
            ImGui.tableNextColumn();
            ImGui.text("Diffuse");
            ImGui.tableNextColumn();
            ImGui.setNextItemWidth(-1);
            InputField.vector4("##materialDiffuse" + material.getGuid(), material.getDiffuseColor());

            ImGui.tableNextRow();
            ImGui.tableNextColumn();
            ImGui.text("Ambient");
            ImGui.tableNextColumn();
            ImGui.setNextItemWidth(-1);
            InputField.vector4("##materialAmbient" + material.getGuid(), material.getAmbientColor());

            ImGui.tableNextRow();
            ImGui.tableNextColumn();
            ImGui.text("Specular");
            ImGui.tableNextColumn();
            ImGui.setNextItemWidth(-1);
            InputField.vector4("##materialSpecular" + material.getGuid(), material.getSpecularColor());

            ImGui.endTable();
        }

        ImGui.spacing();

        //ImGui.setCursorPosX(ImGui.getCursorPosX() + Panel.getPaddingX());
        ImFloat reflectance = new ImFloat(material.getReflectance());
        if (ImGui.inputFloat("reflectance##reflectance" + material.getGuid(), reflectance)) {
            material.setReflectance(reflectance.get());
        }

        //ImGui.setCursorPosX(ImGui.getCursorPosX() + Panel.getPaddingX());
        ImFloat metallic = new ImFloat(material.getMetallic());
        if (ImGui.inputFloat("metallic##metallic" + material.getGuid(), metallic)) {
            material.setMetallic(metallic.get());
        }

        //ImGui.setCursorPosX(ImGui.getCursorPosX() + Panel.getPaddingX());
        ImFloat roughness = new ImFloat(material.getRoughness());
        if (ImGui.inputFloat("roughness##roughness" + material.getGuid(), roughness)) {
            material.setRoughness(roughness.get());
        }

        //ImGui.setCursorPosX(ImGui.getCursorPosX() + Panel.getPaddingX());
        ImFloat tiling = new ImFloat(material.getTilingScale());
        if (ImGui.inputFloat("tiling##tiling" + material.getGuid(), tiling)) {
            material.setTilingScale(tiling.get());
        }

        ImGui.spacing();

        //ImGui.setCursorPosX(ImGui.getCursorPosX() + Panel.getPaddingX());
        if (ImGui.beginTable("materialTextures##" + material.getGuid(), 2, ImGuiTableFlags.SizingStretchProp, new ImVec2(tableWidth, 0))) {

            ImGui.tableSetupColumn("Label", ImGuiTableColumnFlags.WidthStretch, 1.0f);
            ImGui.tableSetupColumn("Value", ImGuiTableColumnFlags.WidthStretch, 2.0f);

            ImGui.tableNextRow();
            ImGui.tableNextColumn();

            ImGui.text("Albedo");
            ImGui.tableNextColumn();
            try {
                Field albedoField = material.getClass().getDeclaredField("albedoTexture");
                albedoField.setAccessible(true);
                drawManifestType(ManifestHelper.manifestFileType.TEXTURE, material.getAlbedoTexture(), albedoField, material);
            } catch (Exception e) {
                Debug.logError("Error loading texture: " + e);
            }

            ImGui.tableNextRow();
            ImGui.tableNextColumn();

            ImGui.text("Normal");
            ImGui.tableNextColumn();

            try {
                Field normalField = material.getClass().getDeclaredField("normalMap");
                normalField.setAccessible(true);
                drawManifestType(ManifestHelper.manifestFileType.TEXTURE, material.getNormalMap(), normalField, material);
            } catch (Exception e) {
                Debug.logError("Error loading texture: " + e);
            }

            ImGui.tableNextRow();
            ImGui.tableNextColumn();

            ImGui.text("Roughness");
            ImGui.tableNextColumn();

            try {
                Field roughnessField = material.getClass().getDeclaredField("roughnessMap");
                roughnessField.setAccessible(true);
                drawManifestType(ManifestHelper.manifestFileType.TEXTURE, material.getRoughnessMap(), roughnessField, material);
            } catch (Exception e) {
                Debug.logError("Error loading texture: " + e);
            }

            ImGui.tableNextRow();
            ImGui.tableNextColumn();

            ImGui.text("Metallic");
            ImGui.tableNextColumn();

            try {
                Field metallicField = material.getClass().getDeclaredField("metallicMap");
                metallicField.setAccessible(true);
                drawManifestType(ManifestHelper.manifestFileType.TEXTURE, material.getMetallicMap(), metallicField, material);
            } catch (Exception e) {
                Debug.logError("Error loading texture: " + e);
            }

            ImGui.tableNextRow();
            ImGui.tableNextColumn();

            ImGui.text("Ambient Occlusion");
            ImGui.tableNextColumn();

            try {
                Field aoField = material.getClass().getDeclaredField("aoMap");
                aoField.setAccessible(true);
                drawManifestType(ManifestHelper.manifestFileType.TEXTURE, material.getAoMap(), aoField, material);
            } catch (Exception e) {
                Debug.logError("Error loading texture: " + e);
            }

            ImGui.endTable();
        }

        ImGui.spacing();

        //ImGui.setCursorPosX(ImGui.getCursorPosX() + Panel.getPaddingX());
        ImBoolean doubleSided = new ImBoolean(material.isDoubleSided());
        if (ImGui.checkbox("doubleSided##doubleSided" + material.getGuid(), doubleSided)) {
            material.setDoubleSided(doubleSided.get());
        }

        //ImGui.setCursorPosX(ImGui.getCursorPosX() + Panel.getPaddingX());
        ImBoolean castShadow = new ImBoolean(material.castShadow());
        if (ImGui.checkbox("castShadow##castShadow" + material.getGuid(), castShadow)) {
            material.castShadow(castShadow.get());
        }

        //ImGui.setCursorPosX(ImGui.getCursorPosX() + Panel.getPaddingX());
        ImBoolean receiveShadow = new ImBoolean(material.receiveShadows());
        if (ImGui.checkbox("receiveShadow##receiveShadow" + material.getGuid(), receiveShadow)) {
            material.receiveShadows(receiveShadow.get());
        }

        //ImGui.setCursorPosX(ImGui.getCursorPosX() + Panel.getPaddingX());
        ImBoolean transparent = new ImBoolean(material.isTransparent());
        if (ImGui.checkbox("transparent##transparent" + material.getGuid(), transparent)) {
            material.setTransparent(transparent.get());
        }

        ImGui.spacing();

        materialRenderer.renderPreview(material);
        ImGui.image(materialPreviewFBOID, new ImVec2(tableWidth, tableWidth));
    }
}