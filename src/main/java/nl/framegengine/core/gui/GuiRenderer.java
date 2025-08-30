package nl.framegengine.core.gui;

import nl.framegengine.core.visual.ModelManager;
import nl.framegengine.core.debugging.RenderMetrics;
import nl.framegengine.core.visual.Model;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.util.List;

public class GuiRenderer {

    private final Model quad;
    private final GuiShader shader;

    private RenderMetrics metrics;
    private boolean recordMetrics = false;

    public GuiRenderer(){
        Vector2f[] positions = {
                new Vector2f(-1f, -1f),
                new Vector2f(1f, -1f),
                new Vector2f(-1f, 1f),
                new Vector2f(1f, 1f)
        };

        quad = ModelManager.loadModel(positions);
        try {
            shader = new GuiShader();
            shader.init();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void render(List<GuiObject> guiObjects){
        if (recordMetrics) metrics.recordShaderBind();
        prepare();

        if (recordMetrics) {
            metrics.recordVaoBind();
        }
        GL30.glBindVertexArray(quad.getId());
        GL20.glEnableVertexAttribArray(0);

        for (GuiObject gui: guiObjects) {
            shader.prepare(gui);

            if (recordMetrics) {
                metrics.recordStateChange();
            }

            if(gui.getTexture() != -1) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, gui.getTexture());
            }

            if (recordMetrics) metrics.recordDrawCall();
            GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
        }

        GL20.glDisableVertexAttribArray(0);
        GL30.glBindVertexArray(0);
        endRendering();

    }

    public void cleanUp(){
        shader.cleanUp();
    }

    private void prepare(){
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        shader.bind();
    }

    private void endRendering(){
        shader.unbind();
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    public void setMetrics(RenderMetrics metrics){
        this.metrics = metrics;
        recordMetrics = true;
    }

    public void recordMetrics(boolean recordMetrics) {
        this.recordMetrics = recordMetrics;
    }
}
