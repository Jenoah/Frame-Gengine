package nl.framegengine.core.visual;

import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.entity.SceneManager;
import nl.framegengine.core.modelLoaders.OBJLoader.OBJObject;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ModelManager {
    private static final HashMap<Integer, Mesh> meshes = new HashMap<>();

    public static Model loadModel(Vector3f[] vertices, Vector2f[] uvs, int[] indices, Vector3f[] normals){
        Mesh mesh = new Mesh(vertices, uvs, indices, normals);
        meshes.put(mesh.getVaoID(), mesh);
        return new Model(mesh);
    }

    public static Model loadModel(Mesh mesh){
        meshes.put(mesh.getVaoID(), mesh);
        return new Model(mesh);
    }

    public static Set<MeshMaterialSet> loadModel(OBJObject objObject){
        Set<MeshMaterialSet> meshMaterialSets = new HashSet<>();

        objObject.getObjModels().forEach((objModel -> {
            if(objModel.getVertices().length == 0) return;
            Vector3f[] modelVertices = objModel.getVertices();
            Vector2f[] modelUVs = objModel.getTextures();
            int[] modelIndices = objModel.getIndices();
            Vector3f[] modelNormals = objModel.getNormals();

            Mesh modelMesh = new Mesh(modelVertices, modelUVs, modelIndices, modelNormals);
            modelMesh.setMeshPath(objObject.getModelPath());
            meshes.put(modelMesh.getVaoID(), modelMesh);
            meshMaterialSets.add(new MeshMaterialSet(modelMesh, objModel.getMaterial()));
        }));

        return meshMaterialSets;
    }
    public static Model loadModel(Vector2f[] vertices){
        Mesh mesh = new Mesh(vertices);
        meshes.put(mesh.getVaoID(), mesh);
        return new Model(mesh);
    }
    public static Model loadModel(Vector3f[] vertices){
        Mesh mesh = new Mesh(vertices);
        meshes.put(mesh.getVaoID(), mesh);
        return new Model(mesh);
    }
    public static Model loadModel(float[] vertices){
        Mesh mesh = new Mesh(vertices, null, 3);
        meshes.put(mesh.getVaoID(), mesh);
        return new Model(mesh);
    }
    public static Model loadModel(float[] vertices, float[] uvs, int[] triangles, float[] normals){
        Mesh mesh = new Mesh(vertices, uvs, triangles, normals);
        meshes.put(mesh.getVaoID(), mesh);
        return new Model(mesh);
    }

    public static int loadModelID(float[] vertices, float[] uvs, int dimensions){
        Mesh mesh = new Mesh(vertices, uvs, dimensions);
        meshes.put(mesh.getVaoID(), mesh);
        return mesh.getVaoID();
    }

    public static Mesh addMesh(Mesh mesh){
        if(!meshes.containsKey(mesh.getVaoID())) meshes.put(mesh.getVaoID(), mesh);
        return mesh;
    }

    public static void unloadModel(int modelID){
        if(meshes.containsKey(modelID)) meshes.get(modelID).cleanUp();
        meshes.remove(modelID);
    }

    public static void unloadMeshId(int meshId){
        if(SceneManager.currentScene != null) SceneManager.currentScene.removeVaoId(meshId);
        meshes.remove(meshId);
    }

    public static void cleanUp(){
        for(Mesh mesh: meshes.values().stream().toList()){
            meshes.remove(mesh.getVaoID());
            mesh.cleanUp();
        }

        //TODO: Unload textures not used ATM
        //TextureLoader.cleanUp();
    }

    public static void cleanUp(Set<Integer> vaoIDs){
        Set<Integer> cleanedVaoIds = new HashSet<>(vaoIDs.size());

        for (Integer vaoId : vaoIDs.stream().toList()) {
            Debug.log("Cleaning Vao ID of " + vaoId);
            if(cleanedVaoIds.contains(vaoId)) continue;
            Mesh mesh = meshes.get(vaoId);
            if(mesh != null) mesh.cleanUp();
            cleanedVaoIds.add(vaoId);
            meshes.remove(vaoId);
        };
    }
}
