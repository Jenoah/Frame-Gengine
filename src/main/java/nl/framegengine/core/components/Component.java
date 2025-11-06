package nl.framegengine.core.components;

import nl.framegengine.core.utils.IJsonSerializable;
import nl.framegengine.core.entity.GameObject;
import nl.framegengine.core.utils.JsonHelper;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;

public class Component implements IJsonSerializable {
    protected GameObject root = null;
    protected boolean hasInitiated = false;
    public boolean runInEditor = false;

    private boolean isEnabled = true;

    public void initiate(){
        if(hasInitiated) return;
        hasInitiated = true;
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

    public void cleanUp(){ }

    @Override
    public JsonObject serializeToJson() {
        return JsonHelper.objectToJson(this, new String[]{"hasInitiated"});
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
}
