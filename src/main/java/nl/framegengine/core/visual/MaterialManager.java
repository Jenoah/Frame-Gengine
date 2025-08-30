package nl.framegengine.core.visual;

import nl.framegengine.core.debugging.Debug;
import nl.framegengine.editor.ManifestHelper;

import java.util.HashMap;
import java.util.Map;

public class MaterialManager {
    private static final Map<String, Material> materials = new HashMap<>();

    public static Material loadMaterial(String fileName){
        String materialGuid = ManifestHelper.getGuidbyPath(ManifestHelper.manifestFileType.MATERIAL, fileName);

        if(materials.containsKey(materialGuid)) return materials.get(materialGuid);
        Debug.logError("Function not implemented yet...");

        return new Material();
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
