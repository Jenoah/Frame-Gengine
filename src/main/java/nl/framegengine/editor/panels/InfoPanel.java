package nl.framegengine.editor.panels;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.*;
import imgui.type.ImBoolean;
import imgui.type.ImFloat;
import imgui.type.ImInt;
import imgui.type.ImString;
import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.entity.GameObject;
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
import nl.framegengine.editor.editorComponents.Text;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.lang.reflect.Field;
import java.util.*;

public class InfoPanel extends EditorPanel {

    private IJsonSerializable currentlySelectedObject = null;
    private final List<Field> hierarchyObjects = new ArrayList<>();

    private String[] textureNames = new String[0];
    private final float paddingX = ImGui.getStyle().getWindowPaddingX();
    private final float paddingY = ImGui.getStyle().getWindowPaddingY();
    private final Set<String> fieldsToIgnore = Set.of("scale", "localPosition", "isEnabled", "localRotation");

    public InfoPanel(int posX, int posY, int sizeX, int sizeY) {
        super(posX, posY, sizeX, sizeY);
        ManifestHelper.addEventCallback(this::updateTextureList);
        updateTextureList();
        windowName = Icons.BOX + " Info";
    }

    @Override
    public void renderFrame() {
        if(currentlySelectedObject == null) return;
        DrawTitlePanel(currentlySelectedObject);

        if(currentlySelectedObject instanceof GameObject){
            DrawTransform((GameObject) currentlySelectedObject);
        }

        for (Field field : hierarchyObjects) {
            try {
                field.setAccessible(true);
                Object value = field.get(currentlySelectedObject);
                if(value == null) continue;
                drawOption(field, currentlySelectedObject);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
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
            }
        } catch (NoSuchFieldException ignored) {}
        ClassHelper.getAllPublicAndProtectedProperties(hierarchyObjects, currentlySelectedObject.getClass());
    }

    private void drawOption(Field field, Object drawingObject) throws IllegalAccessException {
        Object objectValue = field.get(drawingObject);
        String fieldName = field.getName() + "##" + field.hashCode();

        if(fieldsToIgnore.contains(field.getName())) return;

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
                        String rawFieldName = getRawFieldName(fieldName);
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
                ImGui.text(field.getName());
                drawObject(material);
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

            if(ImGui.combo(fileType.name().toLowerCase() + "##" + field.hashCode(), currentSelectedItem, textureNames)){
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
    private void DrawTitlePanel(IJsonSerializable jsonObject) {
        boolean isGameObject = jsonObject instanceof GameObject;

        startPanel();

        String objectType = JsonHelper.getIJsonSerializableType(jsonObject);
        ImGui.text(Icons.GetIcon(jsonObject) + " " + objectType);

        if (isGameObject) {
            GameObject gameObject = (GameObject) jsonObject;

            float checkboxWidth = ImGui.getFrameHeight() + ImGui.getStyle().getItemInnerSpacingX() + ImGui.calcTextSize("Enabled").x;

            ImGui.sameLine();

            float rightEdge = ImGui.getCursorPosX() + ImGui.getContentRegionAvailX();

            ImGui.setCursorPosX(rightEdge - checkboxWidth - paddingX);

            if (ImGui.checkbox("Enabled##" + gameObject.getGuid(), gameObject.isEnabled())) {
                gameObject.setEnabled(!gameObject.isEnabled());
                gameObject.callUpdate();
            }
        }

        endPanel();
    }

    private void DrawTransform(GameObject gameObject){
        startPanel();
        ImGui.text(Icons.TRANSFORM + " Transform");

        float tableWidth = panelWidth - paddingX * 2.0f;

        ImGui.setCursorPosX(ImGui.getCursorPosX() + paddingX);
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

        endPanel();
        ImGui.spacing();
    }

    private float panelWidth;

    private void startPanel(){
        ImDrawList drawList = ImGui.getWindowDrawList();

        panelWidth = ImGui.getContentRegionAvailX();

        drawList.channelsSplit(2);
        drawList.channelsSetCurrent(1);

        ImGui.beginGroup();

        ImGui.setCursorPosX(ImGui.getCursorPosX() + paddingX);
        ImGui.setCursorPosY(ImGui.getCursorPosY() + paddingY);
    }

    private void endPanel() {
        ImDrawList drawList = ImGui.getWindowDrawList();

        // Bottom padding
        ImGui.dummy(0, paddingY);

        ImGui.endGroup();

        ImVec2 min = ImGui.getItemRectMin();
        ImVec2 max = ImGui.getItemRectMax();

        // Force the panel to span the entire available width.
        max.x = min.x + panelWidth;

        drawList.channelsSetCurrent(0);

        drawList.addRectFilled(
                min.x,
                min.y,
                max.x,
                max.y,
                ImGui.getColorU32(ImGuiCol.ChildBg),
                6.0f
        );

        drawList.addRect(
                min.x,
                min.y,
                max.x,
                max.y,
                ImGui.getColorU32(ImGuiCol.Border),
                6.0f
        );

        drawList.channelsMerge();
        ImGui.spacing();
    }
}