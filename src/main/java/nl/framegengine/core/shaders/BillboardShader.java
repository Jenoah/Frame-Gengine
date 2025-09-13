package nl.framegengine.core.shaders;

import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.entity.SceneManager;
import nl.framegengine.core.utils.Constants;
import nl.framegengine.core.visual.MeshMaterialSet;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public class BillboardShader extends Shader{
    public BillboardShader() throws Exception {
        super();
        loadVertexShaderFromFile("/shaders/billboard/billboard.vs");
        loadFragmentShaderFromFile("/shaders/billboard/billboard.fs");
        link();
    }

    @Override
    public void createRequiredUniforms() throws Exception {
        createMaterialUniform("material");
        createUniform("textureSampler");
        createUniform("viewMatrix");
        createUniform("textureSampler");
        createUniform("objectPosition");
        createUniform("projectionMatrix");
        createUniform("modelScale");
        createUniform("fogColor");
        createUniform("fogDensity");
        createUniform("fogGradient");
    }

    @Override
    public void prepare(MeshMaterialSet meshMaterialSet, Camera camera) {
        this.setUniform("material", meshMaterialSet.material);
        this.setUniform("objectPosition", meshMaterialSet.getRoot().getPosition());
        this.setUniform("modelScale", meshMaterialSet.getRoot().getScale());
        this.setUniform("viewMatrix", camera.getViewMatrix());

        if(meshMaterialSet.material.getAlbedoTexture() != null){
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, meshMaterialSet.material.getAlbedoTexture().getId());
            this.setTexture("textureSampler", 0);
        }
    }

    public void updateGenericUniforms(){
        bind();

        //Fog
        this.setUniform("fogColor", SceneManager.currentScene != null ? SceneManager.currentScene.getFogColor() : Constants.AMBIENT_COLOR);
        this.setUniform("fogDensity", SceneManager.currentScene != null ? SceneManager.currentScene.getFogDensity() : 0.025f);
        this.setUniform("fogGradient", SceneManager.currentScene != null ? SceneManager.currentScene.getFogGradient() : 10f);

        //Camera
        setUniform("projectionMatrix", window.getProjectionMatrix());

        unbind();
    }
}
