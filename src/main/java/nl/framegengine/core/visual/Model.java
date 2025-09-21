package nl.framegengine.core.visual;

import nl.framegengine.core.shaders.ShaderManager;

import java.util.ArrayList;
import java.util.HashMap;

public class Model {
    private final ArrayList<Material> material = new ArrayList<>();
    private final Mesh mesh;
    private HashMap<int[], Integer> vertexMaterialRange = new HashMap<>();

    public Model(Mesh mesh){
        this.mesh = mesh;
        this.material.add(new Material(ShaderManager.getDefaultShader()));
        this.vertexMaterialRange.put(mesh.getTriangles(), 0);
        //this.material = new Material(ShaderManager.litShader);
    }

    public Model(Model model, Texture texture){
        this.mesh = model.getMesh();
        this.material.add(new Material(model.getMaterial()).setAlbedoTexture(texture));
        this.vertexMaterialRange = model.getVertexMaterialRange();
    }

    public Model(Model model, String texturePath){
        this.mesh = model.getMesh();
        Material modelMaterial = new Material(model.getMaterial());
        if(!texturePath.isEmpty()) modelMaterial.setAlbedoTexture(new Texture(TextureLoader.loadTexture(texturePath)));
        this.material.add(modelMaterial);
        this.vertexMaterialRange = model.getVertexMaterialRange();
    }

    public int getId() {
        return mesh.getVaoID();
    }

    public Material getMaterial() {
        return material.getFirst();
    }

    public Material getMaterial(int materialIndex) {
        return material.get(materialIndex);
    }

    public ArrayList<Material> getMaterials(){
        return material;
    }

    public void setMaterial(Material material, int index){
        this.material.set(index, material);
    }

    public void setMaterial(Material material) {
        this.material.set(0, material);
    }

    public Mesh getMesh(){
        return mesh;
    }

    public HashMap<int[], Integer> getVertexMaterialRange() {
        return vertexMaterialRange;
    }

    public void addMaterial(int[] indices, Material material){
        this.material.add(material);
        this.vertexMaterialRange.put(indices, this.material.size() - 1);
    }
}
