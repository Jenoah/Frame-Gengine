package nl.framegengine.core.visual;

import nl.framegengine.core.utils.IJsonSerializable;
import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.utils.JsonHelper;
import nl.framegengine.editor.EngineSettings;
import nl.framegengine.editor.ManifestHelper;

import javax.json.*;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Paths;

public class Texture implements IJsonSerializable {

    private int id;
    private String guid;
    private boolean pointFilter = false;
    private boolean flipped = false;
    private boolean repeat = true;
    private boolean isNormalMap = false;
    private boolean isDataTexture = false;
    private String texturePath = "";

    public int getId() {
        return id;
    }

    public Texture(){}

    public Texture(int id) {
        this.id = id;
        this.guid = TextureLoader.getGuidById(this.id);
        this.texturePath = ManifestHelper.getPathByGuid(ManifestHelper.manifestFileType.TEXTURE, this.guid);
    }

    public Texture(String texturePath){
        this.id = TextureLoader.loadTexture(texturePath);
        this.texturePath = texturePath;
        this.guid = TextureLoader.getGuidById(this.id);
    }

    public Texture(String texturePath, boolean pointFilter){
        this.id = TextureLoader.loadTexture(texturePath, pointFilter);
        this.pointFilter = pointFilter;
        this.texturePath = texturePath;
        this.guid = TextureLoader.getGuidById(this.id);
    }

    public Texture(String texturePath, boolean pointFilter, boolean flipped){
        this.id = TextureLoader.loadTexture(texturePath, pointFilter, flipped);
        this.pointFilter = pointFilter;
        this.flipped = flipped;
        this.texturePath = texturePath;
        this.guid = TextureLoader.getGuidById(this.id);
    }

    public Texture(String texturePath, boolean pointFilter, boolean flipped, boolean repeat, boolean isNormalMap){
        this.id = TextureLoader.loadTexture(texturePath, pointFilter, flipped, repeat, isNormalMap);
        this.pointFilter = pointFilter;
        this.flipped = flipped;
        this.repeat = repeat;
        this.isNormalMap = isNormalMap;
        this.texturePath = texturePath;
        this.guid = TextureLoader.getGuidById(this.id);
    }

    public Texture(String texturePath, boolean pointFilter, boolean flipped, boolean repeat, boolean isNormalMap, boolean isDataTexture){
        this.id = TextureLoader.loadTexture(texturePath, pointFilter, flipped, repeat, isNormalMap, isDataTexture);
        this.pointFilter = pointFilter;
        this.flipped = flipped;
        this.repeat = repeat;
        this.isNormalMap = isNormalMap;
        this.isDataTexture = isDataTexture;
        this.texturePath = texturePath;
        this.guid = TextureLoader.getGuidById(this.id);
    }

    public String getTexturePath(){
        return !texturePath.isBlank() ? texturePath : ManifestHelper.getPathByGuid(ManifestHelper.manifestFileType.TEXTURE, guid);
    }

    @Override
    public JsonObject serializeToJson() {
        return JsonHelper.objectToJson(this, new String[]{"id", "texturePath"});
    }

    @Override
    public IJsonSerializable deserializeFromJson(String json) {
        JsonReader jsonReader = Json.createReader(new StringReader(json));
        JsonObject jsonInfo = jsonReader.readObject();
        try {
            JsonHelper.loadVariableIntoObject(this, jsonInfo);
        } catch (Exception e) {
            Debug.logError("Error loading in data: " + e.getMessage());
        }

        if(guid == null || guid.isBlank()) return null;

        texturePath = ManifestHelper.getPathByGuid(ManifestHelper.manifestFileType.TEXTURE, guid);

        if(texturePath != null && !texturePath.isBlank()){
            String textureLocalPath = texturePath;
            String absoluteTexturePath = Paths.get(EngineSettings.currentProjectDirectory, textureLocalPath).toAbsolutePath().toString();
            File textureFile = new File(absoluteTexturePath);
            InputStream is = getClass().getResourceAsStream(absoluteTexturePath);
            if(textureFile.exists() || is != null) textureLocalPath = absoluteTexturePath;

            this.id = TextureLoader.loadTexture(textureLocalPath,
                    pointFilter,
                    flipped,
                    repeat,
                    isNormalMap,
                    isDataTexture);
        }
        this.guid = TextureLoader.getGuidById(this.id);
        return this;
    }

    public final String getGuid(){
        return guid;
    }

    @Override
    public IJsonSerializable setGuid(String guid) {
        this.guid = guid;
        return this;
    }
}
