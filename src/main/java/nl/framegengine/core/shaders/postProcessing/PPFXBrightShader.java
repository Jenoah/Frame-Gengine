package nl.framegengine.core.shaders.postProcessing;

import nl.framegengine.core.shaders.Shader;

public class PPFXBrightShader extends Shader {
    private float threshold = 0.6f;

    public PPFXBrightShader() throws Exception {
        super();
    }

    public Shader init() throws Exception {
        loadVertexShaderFromFile("/shaders/postProcessing/ppfxGeneric.vs");
        loadFragmentShaderFromFile("/shaders/postProcessing/ppfxBrightFilter.fs");
        link();
        super.init();
        return this;
    }

    @Override
    public void createRequiredUniforms() throws Exception {
        createUniform("threshold");
    }

    public void setThreshold(float threshold){
        this.threshold = threshold;
    }

    @Override
    public void prepare() {
        setUniform("threshold", threshold);
    }
}
