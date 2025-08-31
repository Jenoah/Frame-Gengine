package nl.framegengine.core.visual;

import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.utils.FileHelper;
import nl.framegengine.editor.EngineSettings;
import nl.framegengine.editor.ManifestHelper;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class MaterialManager {
    private static final Map<String, Material> materials = new HashMap<>();
    public static final Material defaultMaterial = new Material().setGuid("default_mat");

    public static Material loadMaterialByGuid(String guid) throws FileNotFoundException {
        if(guid == null || guid.isBlank()) {
            //Debug.logError("Material not found with guid " + guid);
            return defaultMaterial;
        }
        if(materials.containsKey(guid)) return materials.get(guid);
        String filePath = ManifestHelper.getPathByGuid(ManifestHelper.manifestFileType.MATERIAL, guid);
        return loadMaterial(filePath);
    }

    public static Material loadMaterial(String filePath) throws FileNotFoundException {

        if(filePath == null || filePath.isBlank()) return defaultMaterial;

        File materialFile = new File(filePath);
        if(!materialFile.exists()) materialFile = new File(EngineSettings.currentProjectDirectory + File.separator + filePath);
        if(!materialFile.exists()) return defaultMaterial;

        String materialGuid = ManifestHelper.getGuidbyPath(ManifestHelper.manifestFileType.MATERIAL, materialFile.getPath());
        if(materials.containsKey(materialGuid)) return materials.get(materialGuid);

        InputStream is = EngineSettings.isCompiled ?
                MaterialManager.class.getResourceAsStream(filePath) :
                new FileInputStream(materialFile.getPath());

        JsonReader reader = Json.createReader(is);
        JsonObject materialInfo = reader.readObject();

        Material material = new Material();
        material.deserializeFromJson(materialInfo.toString());

        materials.put(materialGuid, material);

        return material;
    }

    public static void saveMaterials(){
        materials.forEach((guid, material) -> {
            if(material == MaterialManager.defaultMaterial) return;
            String materialPath = ManifestHelper.getPathByGuid(ManifestHelper.manifestFileType.MATERIAL, guid);
            if(materialPath == null || materialPath.isBlank()){
                Debug.logError("Material path not found by Guid " + guid);
                return;
            }
            File materialFile = new File(EngineSettings.currentProjectDirectory + File.separator + materialPath);
            if(!materialFile.exists()){
                Debug.logError("Material file not found at " + materialPath);
                return;
            }

            Map<String, Boolean> config = new HashMap<>();
            config.put(JsonGenerator.PRETTY_PRINTING, true);
            JsonWriterFactory jsonWriterFactory = Json.createWriterFactory(config);

            StringWriter stringWriter = new StringWriter();
            JsonWriter jsonWriter = jsonWriterFactory.createWriter(stringWriter);
            jsonWriter.write(material.serializeToJson());

            FileHelper.writeToFile(stringWriter.toString(), materialFile.getAbsolutePath());
        });
    }

    public static void addMaterial(Material material){
        if(materials.containsKey(material.guid)) return;
        if(materials.containsValue(material)){
            Debug.logError("Material already exists, but under different guid");
            return;
        }
        materials.put(material.guid, material);
    }
}
