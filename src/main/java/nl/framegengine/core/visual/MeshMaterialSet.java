package nl.framegengine.core.visual;

import nl.framegengine.core.utils.IJsonSerializable;
import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.entity.GameObject;
import nl.framegengine.core.shaders.ShaderManager;
import nl.framegengine.core.utils.JsonHelper;
import nl.framegengine.editor.ManifestHelper;

import javax.json.*;
import java.io.StringReader;

public class MeshMaterialSet implements IJsonSerializable {
    private Mesh mesh;
    public Material material;
    private GameObject root;

    public MeshMaterialSet() {}

    public MeshMaterialSet(Mesh mesh, Material material) {
        this.mesh = mesh;
        this.material = material;
    }

    public MeshMaterialSet(Mesh mesh) {
        this.mesh = mesh;
        this.material = new Material(ShaderManager.getDefaultShader());
    }

    public Mesh getMesh(){
        return this.mesh;
    }

    public GameObject getRoot() {
        return this.root;
    }

    public MeshMaterialSet setRoot(GameObject root) {
        this.root = root;
        return this;
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
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        JsonObject jsonObject = JsonHelper.objectToJson(this, new String[]{"root", "material"});
        jsonObject.forEach(jsonObjectBuilder::add);
        if(getRoot() != null) jsonObjectBuilder.add("root", root.getGuid());
        if(material != null &&
                material.getGuid() != null &&
                ManifestHelper.hasGuid(ManifestHelper.manifestFileType.MATERIAL, material.getGuid())) {
            jsonObjectBuilder.add("material", material.getGuid());
        }else{
            jsonObjectBuilder.add("material", "");
        }
        return jsonObjectBuilder.build();
    }

    @Override
    public IJsonSerializable deserializeFromJson(String json) {
        JsonReader jsonReader = Json.createReader(new StringReader(json));
        JsonObject jsonInfo = jsonReader.readObject();
        try{
            JsonHelper.loadVariableIntoObject(this, jsonInfo, new String[]{"material"});
            if(JsonHelper.hasJsonKey(jsonInfo, "material") &&
                    jsonInfo.get("material").getValueType() == JsonValue.ValueType.STRING){
                this.material = MaterialManager.loadMaterialByGuid(jsonInfo.getString("material"));
            }
        } catch (Exception e) {
            Debug.logError("Error loading in data: " + e.getMessage());
        }
        if(this.material == null) this.material = MaterialManager.defaultMaterial;
        if(this.material.shader == null) this.material.shader = ShaderManager.getDefaultShader();
        return this;
    }
}
