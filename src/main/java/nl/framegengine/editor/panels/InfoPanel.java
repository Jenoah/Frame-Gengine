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
import nl.framegengine.core.utils.ClassHelper;
import nl.framegengine.core.utils.IJsonSerializable;
import nl.framegengine.core.utils.JsonHelper;
import nl.framegengine.core.visual.Texture;
import nl.framegengine.core.visual.TextureLoader;
import nl.framegengine.editor.EditorPanel;
import nl.framegengine.editor.ImGuiHelper;
import nl.framegengine.editor.ManifestHelper;
import nl.framegengine.editor.editorComponents.Collapse;
import nl.framegengine.editor.editorComponents.Icons;
import nl.framegengine.editor.editorComponents.Panel;
import nl.framegengine.editor.editorRenderers.MaterialRenderer;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.lang.reflect.Field;
import java.util.*;

public class InfoPanel extends EditorPanel {

    private static IJsonSerializable currentlySelectedObject = null;
    private final List<Field> hierarchyObjects = new ArrayList<>();
    private static String[] textureNames = new String[0];
    public static MaterialRenderer materialRenderer;
    public static int materialPreviewFBOID = -1;
    private static ImString nameBuffer = new ImString();


    public InfoPanel(int posX, int posY, int sizeX, int sizeY) {
        super(posX, posY, sizeX, sizeY);
        ManifestHelper.addEventCallback(this::updateTextureList);
        updateTextureList();
        windowName = Icons.BOX + " Info";
        materialRenderer = new MaterialRenderer();
    }

    public void postStartInit() {
        try {
            materialRenderer.postStartInit();
            materialPreviewFBOID = materialRenderer.getPreviewFBOID();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void renderFrame() {
        if (currentlySelectedObject == null) return;
        drawTitlePanel(currentlySelectedObject);
        if (currentlySelectedObject instanceof ICustomEditorPanel customEditorPanel) {
            customEditorPanel.renderPanel();
        }else{
            drawObject(currentlySelectedObject);
        }
    }

    public void setCurrentlySelectedObject(IJsonSerializable selectedObject) {
        currentlySelectedObject = selectedObject;
        hierarchyObjects.clear();

        if (currentlySelectedObject == null) return;
        try {
            if (currentlySelectedObject instanceof GameObject) {
                hierarchyObjects.add(ClassHelper.getFieldFromObject("isEnabled", currentlySelectedObject.getClass()));
                hierarchyObjects.add(ClassHelper.getFieldFromObject("localPosition", currentlySelectedObject.getClass()));
                hierarchyObjects.add(ClassHelper.getFieldFromObject("localRotation", currentlySelectedObject.getClass()));
                hierarchyObjects.add(ClassHelper.getFieldFromObject("scale", currentlySelectedObject.getClass()));
                nameBuffer.set(((GameObject) currentlySelectedObject).getName());
            }
        } catch (NoSuchFieldException ignored) {}
        ClassHelper.getAllPublicAndProtectedProperties(hierarchyObjects, currentlySelectedObject.getClass());
    }

    public static void drawOption(Field field, Object drawingObject) throws IllegalAccessException {
        Object objectValue = field.get(drawingObject);
        String fieldName = field.getName() + "##" + field.hashCode();

        if (drawingObject instanceof GameObject && GameObject.fieldsToIgnore.contains(field.getName())) return;

        // Special handling for Texture fields (even if null)
        if (field.getType() == Texture.class) {
            ImGui.text(field.getName());
            if (objectValue != null) {
                drawObject(objectValue);
            }
            drawManifestType(ManifestHelper.manifestFileType.TEXTURE, objectValue, field, drawingObject);
            ImGui.spacing();
            return;
        }

        if (objectValue == null) {
            ImGui.text(field.getName());
            ImGui.text("null");
            ImGui.spacing();
            return;
        }

        switch (objectValue) {
            case Float f -> {
                ImFloat ImFl = new ImFloat(f);
                if (ImGui.inputFloat(fieldName, ImFl)) {
                    field.setAccessible(true);
                    field.set(drawingObject, ImFl.floatValue());
                    if (drawingObject instanceof GameObject go) go.callUpdate();
                }
            }
            case String str -> {
                ImString imStr = new ImString(str);
                if (ImGui.inputText(fieldName, imStr)) {
                    field.setAccessible(true);
                    field.set(drawingObject, imStr.get());
                    if (drawingObject instanceof GameObject go) go.callUpdate();
                }
            }
            case Integer integer -> {
                ImInt imInteger = new ImInt(integer);
                if (ImGui.inputInt(fieldName, imInteger)) {
                    field.setAccessible(true);
                    field.set(drawingObject, imInteger.get());
                    if (drawingObject instanceof GameObject go) go.callUpdate();
                }
            }
            case Boolean bool -> {
                ImBoolean imBool = new ImBoolean(bool);
                if (ImGui.checkbox(fieldName, imBool)) {
                    field.setAccessible(true);
                    field.set(drawingObject, imBool.get());
                    if (drawingObject instanceof GameObject go) go.callUpdate();
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
                    if (drawingObject instanceof GameObject go) go.callUpdate();
                }
            }
            case Quaternionf quaternion -> {
                float[] quaternionArray = new float[]{quaternion.x, quaternion.y, quaternion.z, quaternion.w};
                if (ImGui.inputFloat4(fieldName, quaternionArray)) {
                    quaternion.set(quaternionArray[0], quaternionArray[1], quaternionArray[2], quaternionArray[3]);
                    field.setAccessible(true);
                    field.set(drawingObject, quaternion);
                    if (drawingObject instanceof GameObject go) go.callUpdate();
                }
            }
            case ICustomEditorPanel customEditor when !(objectValue instanceof GameObject) -> {
                customEditor.renderPanel();
            }
            case Set<?> set -> {
                if (!set.isEmpty()) {
                    if (Collapse.Regular(field.getName())) {
                        set.forEach(setItem -> {
                            if (Collapse.Regular(setItem.getClass().getSimpleName() + "##" + setItem.hashCode() + currentlySelectedObject.getGuid())) {
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
        ImGui.spacing();
    }

    public static void drawObject(Object object) {
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

    public static void drawManifestType(ManifestHelper.manifestFileType fileType, Object object, Field field, Object drawingObject) {
        if (fileType == ManifestHelper.manifestFileType.TEXTURE) {
            Texture texture = (Texture) object;
            final String NO_TEXTURE_LABEL = "<No Texture>";

            // Show preview if texture exists
            if (texture != null) {
                ImGui.image(texture.getId(), new ImVec2(32, 32));
            }

            int selectedIndex = 0; // Default to no texture
            if (texture != null && texture.getGuid() != null) {
                String textureGuid = texture.getGuid();
                for (int i = 0; i < textureNames.length; i++) {
                    String itemName = textureNames[i];
                    if (!itemName.equals(NO_TEXTURE_LABEL)) {
                        String itemGuid = ImGuiHelper.guidFromName(itemName);
                        if (textureGuid.equals(itemGuid)) {
                            selectedIndex = i;
                            break;
                        }
                    }
                }
            }

            ImInt currentSelectedItem = new ImInt(selectedIndex);

            if (ImGui.combo("##" + fileType.name().toLowerCase() + field.hashCode(), currentSelectedItem, textureNames)) {
                String selectedName = textureNames[currentSelectedItem.get()];

                if (selectedName.equals(NO_TEXTURE_LABEL)) {
                    try {
                        field.setAccessible(true);
                        field.set(drawingObject, null);
                        if (drawingObject instanceof GameObject go) go.callUpdate();
                        Debug.log("Texture set to null");
                    } catch (IllegalAccessException e) {
                        Debug.logError("Failed to set texture to null: " + e.getMessage());
                    }
                } else {
                    String textureGUID = ImGuiHelper.guidFromName(selectedName);
                    String texturePath = ManifestHelper.getPathByGuid(ManifestHelper.manifestFileType.TEXTURE, textureGUID);
                    if (texturePath == null || texturePath.isEmpty()) {
                        Debug.logError("Could not find path for texture GUID: " + textureGUID);
                        return;
                    }

                    Texture selectedTexture = buildTextureForField(field.getName(), texturePath);
                    if (selectedTexture.getId() != -1 && selectedTexture.getId() != TextureLoader.getDefaultTextureId()) {
                        Debug.log("Selected " + selectedName + " with ID: " + selectedTexture.getId() + " and GUID: " + selectedTexture.getGuid());
                        try {
                            field.setAccessible(true);
                            field.set(drawingObject, selectedTexture);
                            if (drawingObject instanceof GameObject go) go.callUpdate();
                        } catch (IllegalAccessException e) {
                            Debug.logError("Failed to update texture field: " + e.getMessage());
                        }
                    } else {
                        Debug.logError("Failed to load texture for GUID: " + textureGUID);
                    }
                }
            }
        } else {
            ImGui.text("Manifest dropdown not implement for type " + fileType.name());
        }
    }

    private static String getRawFieldName(String fieldNameRaw) {
        if (!fieldNameRaw.isEmpty()) return fieldNameRaw.split("##")[0];
        return fieldNameRaw;
    }

    private static Texture buildTextureForField(String fieldName, String texturePath) {
        return switch (fieldName) {
            case "normalMap" -> new Texture(texturePath, false, false, true, true, false);
            case "roughnessMap",
                 "metallicMap",
                 "aoMap" -> new Texture(texturePath, false, false, true, false, true);
            default -> new Texture(texturePath);
        };
    }

    private void updateTextureList() {
        List<String> manifestItems = new ArrayList<>();
        manifestItems.add("<No Texture>"); // Add empty option for null texture - using angle brackets to avoid conflicts
        ManifestHelper.getTextures().forEach(manifestItem -> manifestItems.add(manifestItem.get("filename") + "##" + manifestItem.get("guid")));
        textureNames = manifestItems.toArray(new String[0]);
    }

    // Individual items
    private static void drawTitlePanel(IJsonSerializable jsonObject) {
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
            if (ImGui.inputText("Name##" + gameObject.getGuid(), nameBuffer)) {
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
}
