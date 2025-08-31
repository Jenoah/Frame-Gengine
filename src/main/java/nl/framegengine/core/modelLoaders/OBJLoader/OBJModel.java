package nl.framegengine.core.modelLoaders.OBJLoader;

import nl.framegengine.core.visual.Material;
import nl.framegengine.core.visual.Face;
import nl.framegengine.core.utils.Conversion;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OBJModel {
    private ArrayList<Face> faces = new ArrayList<>();
    private int[] indices = new int[0];
    private Material material;

    private Vector3f[] vertices;
    private Vector3f[] normals;
    private Vector2f[] uvs;

    public OBJModel(){}

    public OBJModel(ArrayList<Face> faces) {
        this.faces = faces;
        ArrayList<Integer> faceIndices = new ArrayList<>();
        ArrayList<Integer> faceNormalIndices = new ArrayList<>();
        ArrayList<Integer> faceTextureIndices = new ArrayList<>();

        faces.forEach(face -> {
            faceIndices.addAll(Arrays.stream(face.getVertexIndices()).boxed().toList());
            faceNormalIndices.addAll(Arrays.stream(face.getNormals()).boxed().toList());
            faceTextureIndices.addAll(Arrays.stream(face.getTextureCoords()).boxed().toList());
        });

        indices = Conversion.toIntArray(faceIndices);
    }

    public OBJModel(ArrayList<Face> faces, Material material) {
        this.faces = faces;
        ArrayList<Integer> faceIndices = new ArrayList<>();
        faces.forEach(face -> {
            faceIndices.addAll(Arrays.stream(face.getVertexIndices()).boxed().toList());
        });
        indices = Conversion.toIntArray(faceIndices);
        this.material = material;
    }

    public OBJModel(Material material) {
        this.material = material;
    }

    public ArrayList<Face> getFaces() {
        return faces;
    }

    public void addFace(Face face){
        this.faces.add(face);
    }

    public int[] getIndices() {
                return indices;
    }

    public void setIndices(List<Integer> indices){
        this.indices = Conversion.toIntArray(indices);
    }

    public void setIndices(int[] indices) {
        this.indices = indices;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public Vector3f[] getVertices(){
        return vertices;
    }

    public Vector3f[] getNormals(){
        return normals;
    }

    public Vector2f[] getTextures(){
        return uvs;
    }

    public void setVertices(List<Vector3f> vertices){
        this.vertices = vertices.toArray(new Vector3f[0]);
    }

    public void setVertices(Vector3f[] vertices) {
        this.vertices = vertices;
    }

    public void setNormals(List<Vector3f> normals){
        this.normals = normals.toArray(new Vector3f[0]);
    }

    public void setNormals(Vector3f[] normals){
        this.normals = normals;
    }

    public void setUvs(List<Vector2f> uvs){
        this.uvs = uvs.toArray(new Vector2f[0]);
    }

    public void setUvs(Vector2f[] uvs){
        this.uvs = uvs;
    }

    public void cleanUp(){

    }
}
