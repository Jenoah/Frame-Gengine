package nl.framegengine.core.modelLoaders;

import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.utils.Conversion;
import nl.framegengine.core.visual.*;
import nl.framegengine.editor.EngineSettings;
import org.joml.Vector4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;

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

    public static Set<MeshMaterialSet> load(String resourcePath, String texturesDir) {
        return load(resourcePath, texturesDir, aiProcess_JoinIdenticalVertices | aiProcess_Triangulate | aiProcess_FixInfacingNormals);
    }

    public static Set<MeshMaterialSet> load(String resourcePath, String texturesDir, int flags) {
        Path filePath = Path.of(EngineSettings.currentProjectDirectory, resourcePath);
        if(filePath.toFile().exists()){
            resourcePath = filePath.toString();
        }else{
            try {
                URL resource = StaticMeshLoader.class.getResource(resourcePath);
                if(resource != null) resourcePath = Paths.get(resource.toURI()).toAbsolutePath().toString();
            } catch (URISyntaxException e) {
                Debug.logError("Error loading model at " + resourcePath + ". " + e.getMessage());
            }
        }
        AIScene aiScene = aiImportFile(resourcePath, flags);
        if (aiScene == null) {
            Debug.logError("Error loading model at " + resourcePath + ". " + aiGetErrorString());
            return new HashSet<>();
        }

        int numMaterials = aiScene.mNumMaterials();
        PointerBuffer aiMaterials = aiScene.mMaterials();
        List<Material> materials = new ArrayList<>();
        for (int i = 0; i < numMaterials; i++) {
            AIMaterial aiMaterial = AIMaterial.create(aiMaterials.get(i));
            processMaterial(aiMaterial, materials, texturesDir);
        }

        int numMeshes = aiScene.mNumMeshes();
        PointerBuffer aiMeshes = aiScene.mMeshes();
        Set<MeshMaterialSet> meshMaterialSets = new HashSet<>();
        for (int i = 0; i < numMeshes; i++) {
            AIMesh aiMesh = AIMesh.create(aiMeshes.get(i));
            MeshMaterialSet mms = processMesh(aiMesh, materials);
            meshMaterialSets.add(mms);
        }

        return meshMaterialSets;
    }

    private static void processMaterial(AIMaterial aiMaterial, List<Material> materials, String texturesDir){
        Material material = new Material();

        AIColor4D colour = AIColor4D.create();
        AIString path = AIString.calloc();
        Assimp.aiGetMaterialTexture(aiMaterial, aiTextureType_DIFFUSE, 0, path, (IntBuffer) null, null, null, null, null, null);
        String textPath = path.dataString();
        Texture texture = null;

        if (!textPath.isEmpty()) material.setAlbedoTexture(new Texture(TextureLoader.loadTexture(texturesDir + "/" + textPath)));

        int result = aiGetMaterialColor(aiMaterial, AI_MATKEY_COLOR_AMBIENT, aiTextureType_NONE, 0, colour);
        if (result == 0) material.setAmbientColor(new Vector4f(colour.r(), colour.g(), colour.b(), colour.a()));

        result = aiGetMaterialColor(aiMaterial, AI_MATKEY_COLOR_DIFFUSE, aiTextureType_NONE, 0, colour);
        if (result == 0) material.setDiffuseColor(new Vector4f(colour.r(), colour.g(), colour.b(), colour.a()));

        result = aiGetMaterialColor(aiMaterial, AI_MATKEY_COLOR_SPECULAR, aiTextureType_NONE, 0, colour);
        if (result == 0) material.setSpecularColor(new Vector4f(colour.r(), colour.g(), colour.b(), colour.a()));

        materials.add(material);
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
