package nl.framegengine.core.shaders.postProcessing.effects;

import nl.framegengine.core.rendering.renderers.ImageRenderer;
import nl.framegengine.core.shaders.Shader;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public class PPFXGenericEffect {

    private final ImageRenderer renderer;
    protected Shader shader;

    public PPFXGenericEffect(int targetFBOWidth, int targetFBOHeight, Shader shader){
        try {
            this.shader = shader;
            shader.init();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        renderer = new ImageRenderer(targetFBOWidth, targetFBOHeight);
    }

    public PPFXGenericEffect(Shader shader){
        try {
            this.shader = shader;
            shader.init();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        renderer = new ImageRenderer();
    }

    public void render(int textureID){
        shader.bind();

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);

        shader.prepare();
        renderer.renderQuad();

        shader.unbind();
    }

    public int getOutputTexture(){
        return renderer.getOutputTexture();
    }

    public void cleanUp(){
        renderer.cleanUp();
        shader.cleanUp();
    }

}
