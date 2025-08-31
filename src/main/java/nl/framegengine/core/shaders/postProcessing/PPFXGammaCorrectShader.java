package nl.framegengine.core.shaders.postProcessing;

import nl.framegengine.core.shaders.Shader;
import nl.framegengine.core.utils.Constants;

public class PPFXGammaCorrectShader extends Shader {

    public PPFXGammaCorrectShader() throws Exception {
        super();
    }

    public void init() throws Exception {
        loadVertexShaderFromFile("/shaders/postProcessing/ppfxGeneric.vs");
        loadFragmentShaderFromFile("/shaders/postProcessing/ppfxGammaCorrect.fs");
        link();
        super.init();
    }

    @Override
    public void createRequiredUniforms() throws Exception {
        createUniform("gamma");
    }

    @Override
    public void prepare() {
        setUniform("gamma", Constants.GAMMA);
    }
}
