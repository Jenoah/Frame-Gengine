package nl.framegengine.core.components;

import imgui.ImGui;
import nl.framegengine.core.utils.IJsonSerializable;
import nl.framegengine.core.entity.GameObject;
import nl.framegengine.core.utils.JsonHelper;
import nl.framegengine.editor.EngineSettings;
import nl.framegengine.editor.editorComponents.Panel;
import nl.framegengine.editor.panels.ICustomEditorPanel;
import nl.framegengine.editor.panels.InfoPanel;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Set;

public class Component implements IJsonSerializable, ICustomEditorPanel {
    protected GameObject root = null;
    protected boolean hasInitiated = false;
    public boolean runInEditor = false;
    protected boolean hasCleanedUp = false;
    protected boolean addedDuringPlaymode = false;

    protected Set<String> fieldsToIgnore = Set.of("fieldsToIgnore", "runInEditor", "guid", "root", "hasCleanedUp", "addedDuringPlaymode", "hasInitiated");

    private boolean isEnabled = true;

    public void initiate(){
        if(hasInitiated) return;
        hasInitiated = true;
        if(EngineSettings.isInGame) addedDuringPlaymode = true;
    }

    public void update(){}

    public GameObject getRoot(){
        return root;
    }

    public Component setRoot(GameObject root){
        this.root = root;
        return this;
    }

    public void enable(){
        isEnabled = true;
    }

    public void disable(){
        isEnabled = false;
    }

    public final boolean getEnabled(){ return root.isEnabled() && isEnabled; }

    public final boolean isAddedDuringPlaymode(){ return addedDuringPlaymode; }

    public void cleanUp(){
        if(hasCleanedUp) return;
        hasCleanedUp =  true;
        if(addedDuringPlaymode) root.removeComponent(this);
    }

    @Override
    public String getGuid() {
        return "NoGuid";
    }

    @Override
    public IJsonSerializable setGuid(String guid) {
        return null;
    }

    @Override
    public JsonObject serializeToJson() {
        return JsonHelper.objectToJson(this, new String[]{"hasInitiated", "fieldsToIgnore"});
    }

    @Override
    public IJsonSerializable deserializeFromJson(String json) {
        JsonReader jsonReader = Json.createReader(new StringReader(json));
        JsonObject jsonInfo = jsonReader.readObject();
        try {
            JsonHelper.loadVariableIntoObject(this, jsonInfo, new String[]{"class"});
        } catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return this;
    }


    @Override
    public void renderPanel() {
        boolean hasFields = false;
        for (Field field : getClass().getDeclaredFields()) {
            if (Modifier.isPrivate(field.getModifiers()) || fieldsToIgnore.contains(field.getName()) || Modifier.isStatic(field.getModifiers())) continue;

            hasFields = true;

            try {
                field.setAccessible(true);
                Object value = field.get(this);

                if (value == null) continue;

                InfoPanel.drawOption(field, this);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        ImGui.setCursorPosX(ImGui.getCursorPosX() + Panel.getPaddingX());
        if(!hasFields) ImGui.text("No fields found");
    }
}
