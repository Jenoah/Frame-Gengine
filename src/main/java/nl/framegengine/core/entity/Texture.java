package nl.framegengine.core.entity;

import nl.framegengine.core.IJsonSerializable;
import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.loaders.TextureLoader;
import nl.framegengine.core.utils.JsonHelper;
import nl.framegengine.editor.EngineSettings;

import javax.json.*;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Paths;

public class Texture implements IJsonSerializable {

    private int id;
    private String guid;
    protected String texturePath;
    private boolean pointFilter = false;
    private boolean flipped = false;
    private boolean repeat = true;
    private boolean isNormalMap = false;

    public int getId() {
        return id;
    }

    public Texture(){}

    public Texture(int id) {
        this.id = id;
        this.texturePath = "";
        this.guid = TextureLoader.getGuidById(this.id);
    }

    public Texture(String texturePath){
        this.id = TextureLoader.loadTexture(texturePath);
        this.texturePath = texturePath;
        this.guid = TextureLoader.getGuidById(this.id);
    }

    public Texture(String texturePath, boolean pointFilter){
        this.id = TextureLoader.loadTexture(texturePath, pointFilter);
        this.texturePath = texturePath;
        this.pointFilter = pointFilter;
        this.guid = TextureLoader.getGuidById(this.id);
    }

    public Texture(String texturePath, boolean pointFilter, boolean flipped){
        this.id = TextureLoader.loadTexture(texturePath, pointFilter, flipped);
        this.texturePath = texturePath;
        this.pointFilter = pointFilter;
        this.flipped = flipped;
        this.guid = TextureLoader.getGuidById(this.id);
    }

    public Texture(String texturePath, boolean pointFilter, boolean flipped, boolean repeat, boolean isNormalMap){
        this.id = TextureLoader.loadTexture(texturePath, pointFilter, flipped, repeat, isNormalMap);
        this.texturePath = texturePath;
        this.pointFilter = pointFilter;
        this.flipped = flipped;
        this.repeat = repeat;
        this.isNormalMap = isNormalMap;
        this.guid = TextureLoader.getGuidById(this.id);
    }

    public final String getTexturePath(){
        return texturePath;
    }

    @Override
    public JsonObject serializeToJson() {
        return JsonHelper.objectToJson(this, new String[]{"id"});
    }

    @Override
    public IJsonSerializable deserializeFromJson(String json) {
        JsonReader jsonReader = Json.createReader(new StringReader(json));
        JsonObject jsonInfo = jsonReader.readObject();
        try {
            JsonHelper.loadVariableIntoObject(this, jsonInfo);
        } catch (Exception e) {
            Debug.LogError("Error loading in data: " + e.getMessage());
        }

        if(texturePath != null && !texturePath.isEmpty()){

            String absoluteTexturePath = Paths.get(EngineSettings.currentProjectDirectory, texturePath).toAbsolutePath().toString();
            File textureFile = new File(absoluteTexturePath);
            InputStream is = getClass().getResourceAsStream(absoluteTexturePath);
            if(textureFile.exists() || is != null) texturePath = absoluteTexturePath;

            this.id = TextureLoader.loadTexture(texturePath,
                    pointFilter,
                    flipped,
                    repeat,
                    isNormalMap);
        }
        this.guid = TextureLoader.getGuidById(this.id);
        return this;
    }

    public final String getGuid(){
        return guid;
    }
}
