package nl.framegengine.core.shaders;
import nl.framegengine.core.visual.MeshMaterialSet;
import nl.framegengine.core.utils.Transformation;
import org.joml.Matrix4f;

public class ShadowShader extends Shader{
    public ShadowShader() throws Exception {
        super();
        loadVertexShaderFromFile("/shaders/shadow/vertex.vs");
        loadFragmentShaderFromFile("/shaders/shadow/fragment.fs");
        link();
    }

    @Override
    public void createRequiredUniforms() throws Exception {
        //createUniform("modelTexture");
        createUniform("modelMatrix");
        createUniform("projectionViewMatrix");
        createUniform("useInstancing");
    }

    public void prepare(MeshMaterialSet meshMaterialSet, Matrix4f projectionViewMatrix) {
        Matrix4f modelMatrix = Transformation.getModelMatrix(meshMaterialSet.getRoot());

        this.setUniform("projectionViewMatrix", projectionViewMatrix);

        if(meshMaterialSet.getMesh().isInstanced()){
            this.setUniform("useInstancing", true);
        }else{
            this.setUniform("useInstancing", false);
            this.setUniform("modelMatrix", modelMatrix);
        }
    }
}
