package nl.framegengine.core.shaders;

import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.visual.MeshMaterialSet;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public class TriplanarShader extends SimpleLitShader {

    private int topTextureID, sideTextureID;
    private float blendFactor = 0.5f;

    public TriplanarShader(int topTextureID, int sideTextureID) throws Exception {
        super();
        setTextureIDs(topTextureID, sideTextureID);
    }

    public TriplanarShader() throws Exception {
        super();
        loadVertexShaderFromFile("/shaders/lit/triplanar/triplanar.vs");
        loadFragmentShaderFromFile("/shaders/lit/triplanar/triplanar.fs");
        link();
        init();
    }

    @Override
    public void createRequiredUniforms() throws Exception {
        super.createRequiredUniforms();

        createUniform("topTexture");
        //createUniform("textureSampler");
        createUniform("sideTexture");
        createUniform("blendFactor");
    }

    @Override
    public void prepare(MeshMaterialSet meshMaterialSet, Camera camera) {
        super.prepare(meshMaterialSet, camera);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, topTextureID);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, sideTextureID);

        setTexture("topTexture", 0);
        setTexture("sideTexture", 1);

        setUniform("blendFactor", blendFactor);
    }

    public void setTextureIDs(int topTextureID, int sideTextureID){
        this.topTextureID = topTextureID;
        this.sideTextureID = sideTextureID;
    }

    public void setBlendFactor(float blendFactor){
        this.blendFactor = blendFactor;
    }

    @Override
    public void cleanUp() {
        super.cleanUp();
        //UNBIND TEXTURES?
    }
}
