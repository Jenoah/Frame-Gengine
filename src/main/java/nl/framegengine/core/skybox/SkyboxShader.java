package nl.framegengine.core.skybox;

import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.entity.SceneManager;
import nl.framegengine.core.shaders.Shader;
import nl.framegengine.core.utils.Constants;
import org.joml.Matrix4f;

public class SkyboxShader extends Shader {

    public SkyboxShader() throws Exception {
        super();
    }

    public Shader init() throws Exception {
        loadVertexShaderFromFile("/shaders/skybox/skyboxGeneric.vs");
        loadFragmentShaderFromFile("/shaders/skybox/skyboxGeneric.fs");
        link();
        super.init();
        return this;
    }

    @Override
    public void createRequiredUniforms() throws Exception {
        createUniform("textureSampler");
        createUniform("projectionMatrix");
        createUniform("viewMatrix");
        createUniform("fogColor");
    }

    public void prepare(Camera camera) {
        Matrix4f viewMatrix = new Matrix4f(camera.getViewMatrix());
        viewMatrix.m30(0);
        viewMatrix.m31(0);
        viewMatrix.m32(0);
        Matrix4f projectionMatrix = window.getProjectionMatrix();

        setUniform("viewMatrix", viewMatrix);
        setUniform("projectionMatrix", projectionMatrix);
        setUniform("fogColor", SceneManager.currentScene != null ? SceneManager.currentScene.getFogColor() : Constants.AMBIENT_COLOR);
    }
}
