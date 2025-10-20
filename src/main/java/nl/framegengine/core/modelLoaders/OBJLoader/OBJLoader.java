package nl.framegengine.core.modelLoaders.OBJLoader;

import nl.framegengine.core.visual.ModelManager;
import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.visual.Material;
import nl.framegengine.core.visual.Texture;
import nl.framegengine.core.visual.TextureLoader;
import nl.framegengine.core.visual.Face;
import nl.framegengine.core.visual.MeshMaterialSet;
import nl.framegengine.core.shaders.ShaderManager;
import nl.framegengine.core.utils.FileHelper;

import nl.framegengine.editor.EngineSettings;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class OBJLoader {

    public static Set<MeshMaterialSet> loadOBJModel(String fileName) {
        Path filePath = Path.of(EngineSettings.currentProjectDirectory, fileName);
        if(filePath.toFile().exists()) fileName = filePath.toString();
        List<String> lines = FileHelper.readAllLines(fileName);
        OBJObject objObject = new OBJObject();
        objObject.setModelPath(fileName);

        List<Vector3f> vertices = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        List<Vector2f> textures = new ArrayList<>();

        OBJModel currentModel = null;
        HashMap<String, Material> mtlInfo = new HashMap<>();
        String mtlFolder = Paths.get(fileName).getParent().toString() + "/";

        for (String line : lines) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] tokens = line.trim().split("\\s+");
            if (tokens.length == 0) continue;

            switch (tokens[0]) {
                case "o" -> {
                    currentModel = new OBJModel();
                    currentModel.setMaterial(new Material(ShaderManager.getDefaultShader()));
                    objObject.addObjModel(currentModel);
                }
                case "mtllib" -> mtlInfo = loadMTL(mtlFolder + tokens[1]);
                case "usemtl" -> {
                    currentModel = new OBJModel();
                    if(mtlInfo.containsKey(tokens[1])){
                        currentModel.setMaterial(mtlInfo.get(tokens[1]));
                    }else{
                        currentModel.setMaterial(new Material(ShaderManager.getDefaultShader()));
                    }
                    objObject.addObjModel(currentModel);
                }
                case "v" -> vertices.add(new Vector3f(
                        Float.parseFloat(tokens[1]),
                        Float.parseFloat(tokens[2]),
                        Float.parseFloat(tokens[3])
                ));
                case "vn" -> normals.add(new Vector3f(
                        Float.parseFloat(tokens[1]),
                        Float.parseFloat(tokens[2]),
                        Float.parseFloat(tokens[3])
                ));
                case "vt" -> textures.add(new Vector2f(
                        Float.parseFloat(tokens[1]),
                        Float.parseFloat(tokens[2])
                ));
                case "f" -> {
                    if (currentModel == null) {
                        currentModel = new OBJModel();
                        currentModel.setMaterial(new Material(ShaderManager.getDefaultShader()));
                        objObject.addObjModel(currentModel);
                    }

                    int faceCount = tokens.length - 1;
                    int[] vertexIndices = new int[faceCount];
                    int[] textureIndices = new int[faceCount];
                    int[] normalIndices = new int[faceCount];

                    for (int i = 1; i <= faceCount; i++) {
                        String[] vertexData = tokens[i].split("/");
                        int vIdx = Integer.parseInt(vertexData[0]);
                        vIdx = vIdx < 0 ? vertices.size() + vIdx : vIdx - 1;
                        vertexIndices[i - 1] = vIdx;

                        int tIdx = (vertexData.length > 1 && !vertexData[1].isEmpty())
                                ? Integer.parseInt(vertexData[1]) : 0;
                        tIdx = tIdx < 0 ? textures.size() + tIdx : (tIdx > 0 ? tIdx - 1 : -1);
                        textureIndices[i - 1] = tIdx;

                        int nIdx = (vertexData.length > 2 && !vertexData[2].isEmpty())
                                ? Integer.parseInt(vertexData[2]) : 0;
                        nIdx = nIdx < 0 ? normals.size() + nIdx : (nIdx > 0 ? nIdx - 1 : -1);
                        normalIndices[i - 1] = nIdx;
                    }

                    // Triangulate any polygon (3 or more vertices)
                    for (int i = 1; i < faceCount - 1; i++) {
                        Face face = new Face(
                                new int[]{vertexIndices[0], vertexIndices[i], vertexIndices[i + 1]},
                                new int[]{textureIndices[0], textureIndices[i], textureIndices[i + 1]},
                                new int[]{normalIndices[0], normalIndices[i], normalIndices[i + 1]}
                        );
                        currentModel.addFace(face);
                    }
                }
            }
        }

        // Ensure at least one model exists
        if (objObject.getObjModels().isEmpty()) {
            currentModel = new OBJModel();
            currentModel.setMaterial(new Material(ShaderManager.getDefaultShader()));
            objObject.addObjModel(currentModel);
        }

        // Assemble final model data
        for (OBJModel model : objObject.getObjModels()) {
            List<Vector3f> finalVertices = new ArrayList<>();
            List<Vector2f> finalTextures = new ArrayList<>();
            List<Vector3f> finalNormals = new ArrayList<>();
            List<Integer> finalIndices = new ArrayList<>();
            Map<String, Integer> uniqueVertexMap = new HashMap<>();

            for (Face face : model.getFaces()) {
                for (int i = 0; i < face.getVertexIndices().length; i++) {
                    int vIdx = face.getVertexIndices()[i];
                    int tIdx = face.getTextureCoords()[i];
                    int nIdx = face.getNormals()[i];

                    String key = vIdx + "/" + tIdx + "/" + nIdx;
                    Integer index = uniqueVertexMap.get(key);
                    if (index == null) {
                        Vector3f v = vertices.get(vIdx);
                        Vector2f t = (tIdx != -1 && tIdx < textures.size())
                                ? textures.get(tIdx) : new Vector2f(0, 0);
                        Vector3f n = (nIdx != -1 && nIdx < normals.size())
                                ? normals.get(nIdx) : new Vector3f(0, 0, 0);

                        finalVertices.add(v);
                        finalTextures.add(t);
                        finalNormals.add(n);

                        index = finalVertices.size() - 1;
                        uniqueVertexMap.put(key, index);
                    }
                    finalIndices.add(index);
                }
            }

            model.setVertices(finalVertices);
            model.setUvs(finalTextures);
            model.setNormals(finalNormals);
            model.setIndices(finalIndices);
        }

        return ModelManager.loadModel(objObject);
    }

    public static Set<MeshMaterialSet> loadOBJModel(String fileName, Texture texturePath) {
        Set<MeshMaterialSet> meshMaterialSets = loadOBJModel(fileName);
        meshMaterialSets.forEach((meshMaterialSet -> {
            meshMaterialSet.material.setAlbedoTexture(texturePath);
        }));

        return meshMaterialSets;
    }

    public static HashMap<String, Material> loadMTL(String fileName) {
        HashMap<String, Material> mtlMaterials = new HashMap<>();
        Material currentMaterial = null;

        List<String> lines = FileHelper.readAllLines(fileName);
        for (String line : lines) {
            String[] tokens = line.split("\\s+");
            switch (tokens[0]) {
                case "newmtl":
                    currentMaterial = new Material(ShaderManager.getDefaultShader());
                    mtlMaterials.put(tokens[1], currentMaterial);
                    break;
                case "Kd":
                    currentMaterial.setDiffuseColor(new Vector4f(Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]), Float.parseFloat(tokens[3]), 1));
                    break;
                case "Ks":
                    currentMaterial.setSpecularColor(new Vector4f(Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]), Float.parseFloat(tokens[3]), 1));
                    break;
                case "Ka":
                    currentMaterial.setAmbientColor(new Vector4f(Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]), Float.parseFloat(tokens[3]), 1));
                    break;
                case "map_Kd":
                    StringBuilder texturePath = new StringBuilder();
                    for(int i = 1; i < tokens.length; i++){
                        texturePath.append(tokens[i]);
                    }
                    texturePath = new StringBuilder("textures/" + Paths.get(texturePath.toString()).getFileName().toString());
                    try {
                        Texture texture = new Texture(TextureLoader.loadTexture(texturePath.toString()));
                        currentMaterial.setAlbedoTexture(texture);
                    } catch (Exception e) {
                        Debug.logError("Texture is not placed in resource texture folder: " + texturePath);
                    }
                    break;
            }
        }
        return mtlMaterials;
    }

}
