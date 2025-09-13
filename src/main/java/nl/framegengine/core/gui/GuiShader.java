package nl.framegengine.core.gui;

import nl.framegengine.core.shaders.Shader;
import nl.framegengine.core.utils.Transformation;
import org.joml.Matrix4f;

public class GuiShader extends Shader {

    public GuiShader() throws Exception {
        super();
    }

    public Shader init() throws Exception {
        loadVertexShaderFromFile("/shaders/GUI/guiGeneric.vs");
        loadFragmentShaderFromFile("/shaders/GUI/guiGeneric.fs");
        link();
        super.init();
        return this;
    }

    @Override
    public void createRequiredUniforms() throws Exception {
        createUniform("modelMatrix");
        createUniform("uiColor");
        createUniform("hasTexture");
    }

    public void prepare(GuiObject guiObject) {
        Matrix4f modelMatrix = Transformation.getModelMatrix(guiObject);
        setUniform("modelMatrix", modelMatrix);
        setUniform("uiColor", guiObject.getColor());
        setUniform("hasTexture", guiObject.getTexture() == -1 ? 0 : 1);
    }
}
