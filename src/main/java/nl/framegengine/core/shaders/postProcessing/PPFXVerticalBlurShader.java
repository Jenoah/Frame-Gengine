package nl.framegengine.core.shaders.postProcessing;

import nl.framegengine.core.shaders.Shader;

public class PPFXVerticalBlurShader extends Shader {
    private int targetHeight = 720;

    public PPFXVerticalBlurShader() throws Exception {
        super();
    }

    public Shader init() throws Exception {
        loadVertexShaderFromFile("/shaders/postProcessing/ppfxVerticalBlur.vs");
        loadFragmentShaderFromFile("/shaders/postProcessing/ppfxBlur.fs");
        link();
        super.init();
        return this;
    }

    @Override
    public void createRequiredUniforms() throws Exception {
        createUniform("targetHeight");
    }

    public void setTargetHeight(int targetHeight){
        this.targetHeight = targetHeight;
    }

    @Override
    public void prepare() {
        setUniform("targetHeight", targetHeight);
    }
}
