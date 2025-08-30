package nl.framegengine.core.shaders;

import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.entity.SceneManager;
import nl.framegengine.core.visual.MeshMaterialSet;
import nl.framegengine.core.utils.Transformation;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import static org.lwjgl.opengl.GL11.glDepthMask;

public class UnlitShader extends Shader{
    public UnlitShader() throws Exception {
        super();
        loadVertexShaderFromFile("/shaders/unlit/unlit.vs");
        loadFragmentShaderFromFile("/shaders/unlit/unlit.fs");
        link();
    }

    @Override
    public void createRequiredUniforms() throws Exception {
        createMaterialUniform("material");
        createUniform("albedoMap");
        createUniform("modelMatrix");
        createUniform("viewMatrix");
        createUniform("projectionMatrix");
        createUniform("fogColor");
        createUniform("fogDensity");
        createUniform("fogGradient");
    }

    @Override
    public void prepare(MeshMaterialSet meshMaterialSet, Camera camera) {
        glDepthMask(true);

        Matrix4f modelMatrix = Transformation.getModelMatrix(meshMaterialSet.getRoot());

        this.setUniform("material", meshMaterialSet.material);
        this.setUniform("modelMatrix", modelMatrix);
        this.setUniform("viewMatrix", camera.getViewMatrix());

        if(meshMaterialSet.material.hasAlbedoTexture()){
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, meshMaterialSet.material.getAlbedoTexture().getId());
            this.setTexture("albedoMap", 0);
        }
    }

    public void updateGenericUniforms(){
        bind();

        //Fog
        this.setUniform("fogColor", SceneManager.fogColor);
        this.setUniform("fogDensity", SceneManager.fogDensity);
        this.setUniform("fogGradient", SceneManager.fogGradient);

        //Camera
        setUniform("projectionMatrix", window.getProjectionMatrix());

        unbind();
    }
}
