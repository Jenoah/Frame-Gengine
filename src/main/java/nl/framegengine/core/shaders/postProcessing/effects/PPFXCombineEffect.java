package nl.framegengine.core.shaders.postProcessing.effects;

import nl.framegengine.core.rendering.renderers.ImageRenderer;
import nl.framegengine.core.shaders.postProcessing.PPFXCombineShader;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public class PPFXCombineEffect {
    private final ImageRenderer renderer;
    private final PPFXCombineShader shader;

    public PPFXCombineEffect(){
        try {
            shader = new PPFXCombineShader();
            shader.init();
            shader.setIntensity(1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        renderer = new ImageRenderer();
    }

    public PPFXCombineEffect(int targetFBOWidth, int targetFBOHeight){
        try {
            shader = new PPFXCombineShader();
            shader.init();
            shader.setIntensity(1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        renderer = new ImageRenderer(targetFBOWidth, targetFBOHeight);
    }

    public void render(int primaryTextureID, int secondaryTextureID){
        shader.bind();
        shader.prepare();

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, primaryTextureID);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, secondaryTextureID);

        shader.setTexture("primaryTexture", 0);
        shader.setTexture("secondaryTexture", 1);

        renderer.renderQuad();

        shader.unbind();
    }

    public void setIntensity(float intensity){
        shader.setIntensity(intensity);
    }

    public int getOutputTexture(){
        return renderer.getOutputTexture();
    }

    public void cleanUp(){
        renderer.cleanUp();
        shader.cleanUp();
    }
}
