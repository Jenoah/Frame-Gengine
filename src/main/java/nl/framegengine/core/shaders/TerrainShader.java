package nl.framegengine.core.shaders;

import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.visual.MeshMaterialSet;
import org.joml.Vector2f;

public class TerrainShader extends Shader {
    private Vector2f terrainHeight = new Vector2f(-1, 1);

    public TerrainShader() throws Exception {
        super();
        loadVertexShaderFromFile("/shaders/terrain/terrain.vs");
        loadFragmentShaderFromFile("/shaders/terrain/terrain.fs");
        link();
    }

    @Override
    public void createRequiredUniforms() throws Exception {
        super.createRequiredUniforms();

        createUniform("terrainHeight");
    }

    @Override
    public void prepare(MeshMaterialSet meshMaterialSet, Camera camera) {
        super.prepare(meshMaterialSet, camera);
        setUniform("terrainHeight", terrainHeight);
    }

    public void setTerrainHeight(Vector2f terrainHeight){
        this.terrainHeight = terrainHeight;
    }
}
