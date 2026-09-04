package nl.framegengine.core.rendering.shadow;

import nl.framegengine.core.engine.WindowManager;
import nl.framegengine.editor.EditorWindow;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;

public class ShadowFrameBuffer {
    private final int width;
    private final int height;

    private int frameBuffer;
    private int shadowMapTextureID;

    public ShadowFrameBuffer(int width, int height) {
        this.width = width;
        this.height = height;
        initialiseFrameBuffer();
    }

    public void cleanUp() {
        GL30.glDeleteFramebuffers(frameBuffer);
        GL11.glDeleteTextures(shadowMapTextureID);
    }

    public void bindFrameBuffer() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, frameBuffer);
        GL11.glViewport(0, 0, width, height);
    }

    public void unbindFrameBuffer() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glViewport(0, 0, WindowManager.getInstance() != null ? WindowManager.getInstance().getWidth() : EditorWindow.windowWidth, WindowManager.getInstance() != null ? WindowManager.getInstance().getHeight() : EditorWindow.windowHeight);
    }

    public int getShadowMap() {
        return shadowMapTextureID;
    }

    private void initialiseFrameBuffer() {
        createFrameBuffer();
        createDepthBufferAttachment();
        unbindFrameBuffer();
    }

    private void createFrameBuffer() {
        frameBuffer = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, frameBuffer);
        GL11.glDrawBuffer(GL30.GL_NONE);
        GL11.glReadBuffer(GL30.GL_NONE);
    }

    private void createDepthBufferAttachment() {
        shadowMapTextureID = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, shadowMapTextureID);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT16, width, height, 0,
                GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL32.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL15.GL_TEXTURE_2D, shadowMapTextureID, 0);

    }
}
