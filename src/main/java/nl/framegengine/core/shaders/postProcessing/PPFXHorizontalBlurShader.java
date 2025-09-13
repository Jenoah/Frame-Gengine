package nl.framegengine.core.shaders.postProcessing;

import nl.framegengine.core.shaders.Shader;

public class PPFXHorizontalBlurShader extends Shader {
    private int targetWidth = 1280;

    public PPFXHorizontalBlurShader() throws Exception {
        super();
    }

    public Shader init() throws Exception {
        loadVertexShaderFromFile("/shaders/postProcessing/ppfxHorizontalBlur.vs");
        loadFragmentShaderFromFile("/shaders/postProcessing/ppfxBlur.fs");
        link();
        super.init();
        return this;
    }

    @Override
    public void createRequiredUniforms() throws Exception {
        createUniform("targetWidth");
    }

    public void setTargetWidth(int targetWidth){
        this.targetWidth = targetWidth;
    }

    @Override
    public void prepare() {
        this.setUniform("targetWidth", targetWidth);
    }
}
