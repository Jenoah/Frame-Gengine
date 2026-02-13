package nl.framegengine.core.modelLoaders;

import nl.framegengine.core.components.visual.RenderComponent;
import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.entity.GameObject;
import nl.framegengine.core.utils.Conversion;
import nl.framegengine.core.utils.FileHelper;
import nl.framegengine.core.visual.*;
import nl.framegengine.editor.EngineSettings;
import nl.framegengine.editor.ManifestHelper;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.lwjgl.assimp.Assimp.*;

public class StaticMeshLoader {

    private static final int defaultFlags = aiProcess_JoinIdenticalVertices | aiProcess_Triangulate |
            aiProcess_FixInfacingNormals | aiProcess_GenBoundingBoxes | aiProcess_ImproveCacheLocality;

    public static Set<MeshMaterialSet> load(String resourcePath) {
        return load(resourcePath, "");
    }

    public static Set<MeshMaterialSet> load(String resourcePath, int subMeshId) {
        return load(resourcePath, "");
    }

    public static Set<MeshMaterialSet> load(String resourcePath, String texturesDir) {
        return load(resourcePath, -1, texturesDir);
    }

    public static Set<MeshMaterialSet> load(String resourcePath, int subMeshId, String texturesDir) {
        return load(resourcePath, subMeshId, texturesDir, defaultFlags);
    }

    public static Set<MeshMaterialSet> load(String resourcePath, String texturesDir, int flags) {
        return load(resourcePath, -1, texturesDir, flags);
    }

    public static Set<MeshMaterialSet> load(String resourcePath, int subMeshId, String texturesDir, int flags) {
        AIScene aiScene = pathToAIScene(resourcePath, flags);
        if(aiScene == null) return new HashSet<>();

        PointerBuffer aiMeshes = aiScene.mMeshes();
        Set<MeshMaterialSet> meshMaterialSets = new HashSet<>();
        List<Material> materials = aiSceneToMaterialList(aiScene, texturesDir);
            int numMeshes = aiScene.mNumMeshes();

        if(subMeshId == -1) {
            for (int i = 0; i < numMeshes; i++) {
                AIMesh aiMesh = AIMesh.create(aiMeshes.get(i));
                MeshMaterialSet mms = processMesh(aiMesh, materials);
                mms.getMesh().setMeshPath(resourcePath);
                meshMaterialSets.add(mms);
            }
        }else if(numMeshes > subMeshId){
            AIMesh aiMesh = AIMesh.create(aiScene.mMeshes().get(subMeshId));
            MeshMaterialSet mms = processMesh(aiMesh, materials);
            mms.getMesh().setMeshPath(resourcePath);
            mms.getMesh().setMeshId(subMeshId);
            meshMaterialSets.add(mms);
        }

        return meshMaterialSets;
    }

    public static GameObject loadIntoGameObject(String resourcePath) {
        return loadIntoGameObject(resourcePath, "");
    }

    public static GameObject loadIntoGameObject(String resourcePath, String texturesDir) {
        return loadIntoGameObject(resourcePath, texturesDir, defaultFlags);
    }

    public static GameObject loadIntoGameObject(String resourcePath, String texturesDir, int flags){
        AIScene aiScene = pathToAIScene(resourcePath, flags);
        if(aiScene == null) return null;

        List<Material> materials = aiSceneToMaterialList(aiScene, texturesDir);

        if(aiScene.mRootNode() == null) return null;

        GameObject rootGameObject = aiSceneToHierarchicalGameObject(aiScene, aiScene.mRootNode(), materials, resourcePath);
        processTransform(rootGameObject, new Matrix4f());

        if(rootGameObject.getName().equals("RootNode")) rootGameObject.setName(FileHelper.getFileName(resourcePath));
        if(rootGameObject.getChildren().size() == 1){
            return rootGameObject.getChildren().getFirst().setParent(null);
        }

        return rootGameObject;
    }

    private static AIScene pathToAIScene(String resourcePath, int flags){
        // Handle builtin: prefix — skip project directory, go straight to classpath
        if (resourcePath.startsWith(Mesh.BUILTIN_PREFIX)) {
            resourcePath = resourcePath.substring(Mesh.BUILTIN_PREFIX.length());
            try {
                URL resource = StaticMeshLoader.class.getResource(resourcePath);
                if (resource != null) resourcePath = Paths.get(resource.toURI()).toAbsolutePath().toString();
            } catch (URISyntaxException e) {
                Debug.logError("Error loading built-in model at " + resourcePath + ". " + e.getMessage());
                return null;
            }
        } else {
            // Try resolving as a manifest GUID first
            String guidResolved = ManifestHelper.getPathByGuid(ManifestHelper.manifestFileType.MODEL, resourcePath);
            if (guidResolved != null) {
                resourcePath = guidResolved;
            }

            // Try project directory, then classpath
            Path filePath = Path.of(EngineSettings.currentProjectDirectory, resourcePath);
            if (filePath.toFile().exists()) {
                resourcePath = filePath.toString();
            } else {
                try {
                    URL resource = StaticMeshLoader.class.getResource(resourcePath);
                    if (resource != null)
                        resourcePath = Paths.get(resource.toURI()).toAbsolutePath().toString();
                } catch (URISyntaxException e) {
                    Debug.logError("Error loading model at " + resourcePath + ". " + e.getMessage());
                    return null;
                }
            }
        }

        AIScene aiScene = aiImportFile(resourcePath, flags);
        if (aiScene == null) {
            Debug.logError("Error loading model at " + resourcePath + ". " + aiGetErrorString());
            return null;
        }

        return aiScene;
    }

    private static List<Material> aiSceneToMaterialList(AIScene aiScene, String texturesDir){
        int numMaterials = aiScene.mNumMaterials();
        PointerBuffer aiMaterials = aiScene.mMaterials();
        List<Material> materials = new ArrayList<>();
        for (int i = 0; i < numMaterials; i++) {
            AIMaterial aiMaterial = AIMaterial.create(aiMaterials.get(i));
            materials.add(processMaterial(aiMaterial, texturesDir));
        }

        return materials;
    }

    private static GameObject aiSceneToHierarchicalGameObject(AIScene aiScene, AINode aiNode, List<Material> materials, String resourcePath){
        String objectName = aiNode.mName().dataString();
        if(!objectName.isEmpty()) objectName = objectName.split("_\\$")[0];
        GameObject rootObject = new GameObject(objectName);

        rootObject.setMatrix(Conversion.toMatrix4F(aiNode.mTransformation()));

        int childNodeCount = aiNode.mNumMeshes();
        if(childNodeCount > 0){
            IntBuffer nodeMeshes = aiNode.mMeshes();
            for(int i = 0; i < childNodeCount; i++){
                int meshIndex = nodeMeshes.get(i);
                AIMesh aiMesh = AIMesh.create(aiScene.mMeshes().get(meshIndex));
                MeshMaterialSet mms = processMesh(aiMesh, materials);
                mms.getMesh().setMeshPath(resourcePath);
                mms.getMesh().setMeshId(meshIndex);
                RenderComponent renderComponent = rootObject.getComponent(RenderComponent.class);
                if(renderComponent != null){
                    renderComponent.addMesh(mms);
                }else{
                    renderComponent = new RenderComponent(mms);
                    rootObject.addComponent(renderComponent);
                }

                renderComponent.calculateAABB();
            }
        }

        int numChildren = aiNode.mNumChildren();
        PointerBuffer children = aiNode.mChildren();
        for (int i = 0; i < numChildren; i++) {
            AINode childNode = AINode.create(children.get(i));
            GameObject childObject = aiSceneToHierarchicalGameObject(aiScene, childNode, materials, resourcePath);
            rootObject.addChild(childObject);
        }

        return rootObject;
    }

    private static void processTransform(GameObject gameObject, Matrix4f parentTransform){
        Matrix4f worldTransform = new Matrix4f(parentTransform).mul(gameObject.getMatrix());
        gameObject.setMatrix(worldTransform);

        for (GameObject child : gameObject.getChildren()) {
            processTransform(child, worldTransform);
        }
    }

    private static Material processMaterial(AIMaterial aiMaterial, String texturesDir){
        Material material = new Material();

        AIColor4D colour = AIColor4D.create();
        AIString path = AIString.calloc();
        
        // Load albedo/diffuse texture
        Assimp.aiGetMaterialTexture(aiMaterial, aiTextureType_DIFFUSE, 0, path, (IntBuffer) null, null, null, null, null, null);
        String diffuseTexturePath = path.dataString();

        if(!texturesDir.isEmpty() && !texturesDir.endsWith(File.separator)) texturesDir += File.separator;
        if (!diffuseTexturePath.isEmpty()) material.setAlbedoTexture(new Texture(texturesDir + diffuseTexturePath));
        
        // Load normal map
        path.clear();
        Assimp.aiGetMaterialTexture(aiMaterial, aiTextureType_NORMALS, 0, path, (IntBuffer) null, null, null, null, null, null);
        String normalMapPath = path.dataString();
        if (!normalMapPath.isEmpty()) {
            material.setNormalMap(new Texture(texturesDir + normalMapPath, false, false, true, true));
        }
        
        // Load roughness map
        path.clear();
        Assimp.aiGetMaterialTexture(aiMaterial, aiTextureType_DIFFUSE_ROUGHNESS, 0, path, (IntBuffer) null, null, null, null, null, null);
        String roughnessMapPath = path.dataString();
        if (roughnessMapPath.isEmpty()) {
            // Try shininess as fallback
            path.clear();
            Assimp.aiGetMaterialTexture(aiMaterial, aiTextureType_SHININESS, 0, path, (IntBuffer) null, null, null, null, null, null);
            roughnessMapPath = path.dataString();
        }
        if (!roughnessMapPath.isEmpty()) {
            material.setRoughnessMap(new Texture(texturesDir + roughnessMapPath, false, false, true, false, true));
        }
        
        // Load metallic map
        path.clear();
        Assimp.aiGetMaterialTexture(aiMaterial, aiTextureType_METALNESS, 0, path, (IntBuffer) null, null, null, null, null, null);
        String metallicMapPath = path.dataString();
        if (!metallicMapPath.isEmpty()) {
            material.setMetallicMap(new Texture(texturesDir + metallicMapPath, false, false, true, false, true));
        }
        
        // Load AO map
        path.clear();
        Assimp.aiGetMaterialTexture(aiMaterial, aiTextureType_AMBIENT_OCCLUSION, 0, path, (IntBuffer) null, null, null, null, null, null);
        String aoMapPath = path.dataString();
        if (aoMapPath.isEmpty()) {
            // Try lightmap as fallback
            path.clear();
            Assimp.aiGetMaterialTexture(aiMaterial, aiTextureType_LIGHTMAP, 0, path, (IntBuffer) null, null, null, null, null, null);
            aoMapPath = path.dataString();
        }
        if (!aoMapPath.isEmpty()) {
            material.setAOMap(new Texture(texturesDir + aoMapPath, false, false, true, false, true));
        }

        int result = aiGetMaterialColor(aiMaterial, AI_MATKEY_COLOR_AMBIENT, aiTextureType_NONE, 0, colour);
        if (result == 0) material.setAmbientColor(new Vector4f(colour.r(), colour.g(), colour.b(), colour.a()));

        result = aiGetMaterialColor(aiMaterial, AI_MATKEY_COLOR_DIFFUSE, aiTextureType_NONE, 0, colour);
        if (result == 0) material.setDiffuseColor(new Vector4f(colour.r(), colour.g(), colour.b(), 1));

        result = aiGetMaterialColor(aiMaterial, AI_MATKEY_COLOR_SPECULAR, aiTextureType_NONE, 0, colour);
        if (result == 0) material.setSpecularColor(new Vector4f(colour.r(), colour.g(), colour.b(), colour.a()));

        float[] value = new float[1]; // or float value[1] in C
        result = aiGetMaterialFloatArray(aiMaterial, AI_MATKEY_REFLECTIVITY, aiTextureType_NONE, 0, value, null);
        if (result == 0) material.setReflectance(value[0]);

        result = aiGetMaterialFloatArray(aiMaterial, AI_MATKEY_ROUGHNESS_FACTOR, aiTextureType_NONE, 0, value, null);
        if (result == 0) material.setRoughness(value[0]);

        result = aiGetMaterialFloatArray(aiMaterial, AI_MATKEY_METALLIC_FACTOR, aiTextureType_NONE, 0, value, null);
        if (result == 0) material.setMetallic(value[0]);

        result = aiGetMaterialFloatArray(aiMaterial, AI_MATKEY_OPACITY, aiTextureType_NONE, 0, value, null);
        if (result == 0){
            material.getDiffuseColor().w = value[0];
            //TODO: Fix importing of transparency
            //material.setTransparent(true);
        }

        result = aiGetMaterialFloatArray(aiMaterial, AI_MATKEY_TWOSIDED, aiTextureType_NONE, 0, value, null);
        if (result == 0) material.setDoubleSided(value[0] == 0);

        result = aiGetMaterialFloatArray(aiMaterial, AI_MATKEY_COLOR_TRANSPARENT, aiTextureType_NONE, 0, value, null);
        if (result == 0) material.getDiffuseColor().w = 1f - value[0];

        return material;
    }

    private static MeshMaterialSet processMesh(AIMesh aiMesh, List<Material> materials) {
        List<Float> vertices = new ArrayList<>();
        List<Float> textures = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        processVertices(aiMesh, vertices);
        processNormals(aiMesh, normals);
        processTextureCoords(aiMesh, textures);
        processIndices(aiMesh, indices);

        Mesh mesh = new Mesh(
                Conversion.toFloatArray(vertices),
                Conversion.toFloatArray(textures),
                Conversion.toIntArray(indices),
                Conversion.toFloatArray(normals));

        Material material = MaterialManager.defaultMaterial;
        int materialIdx = aiMesh.mMaterialIndex();
        if (materialIdx >= 0 && materialIdx < materials.size()) {
            material = materials.get(materialIdx);
        }

        return new MeshMaterialSet(mesh, material);
    }

    private static void processVertices(AIMesh aiMesh, List<Float> vertices) {
        AIVector3D.Buffer aiVertices = aiMesh.mVertices();
        while (aiVertices.remaining() > 0) {
            AIVector3D aiVertex = aiVertices.get();
            vertices.add(aiVertex.x());
            vertices.add(aiVertex.y());
            vertices.add(aiVertex.z());
        }
    }

    private static void processNormals(AIMesh aiMesh, List<Float> normals) {
        AIVector3D.Buffer aiNormals = aiMesh.mNormals();
        if(aiNormals == null) return;

        while (aiNormals.remaining() > 0) {
            AIVector3D aiNormal = aiNormals.get();
            normals.add(aiNormal.x());
            normals.add(aiNormal.y());
            normals.add(aiNormal.z());
        }
    }

    private static void processTextureCoords(AIMesh aiMesh, List<Float> textureCoords) {
        AIVector3D.Buffer aiTextureCoords = aiMesh.mTextureCoords(0);
        if(aiTextureCoords == null) return;

        while (aiTextureCoords.remaining() > 0) {
            AIVector3D aiTextureCoord = aiTextureCoords.get();
            textureCoords.add(aiTextureCoord.x());
            textureCoords.add(1 - aiTextureCoord.y());
        }
    }

    private static void processIndices(AIMesh aiMesh, List<Integer> indices) {
        int faceCount = aiMesh.mNumFaces();
        AIFace.Buffer aiFaces = aiMesh.mFaces();
        for (int i = 0; i < faceCount; i++) {
            AIFace face = aiFaces.get(i);
            IntBuffer indexBuffer = face.mIndices();
            while (indexBuffer.remaining() > 0) {
                indices.add(indexBuffer.get());
            }
        }
    }
}
