package nl.framegengine.core.skybox;

import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.ModelManager;
import nl.framegengine.core.debugging.RenderMetrics;
import nl.framegengine.core.entity.Model;
import nl.framegengine.core.loaders.TextureLoader;
import org.joml.Vector3f;
import org.lwjgl.opengl.*;

public class SkyboxRenderer {
    private static final float SIZE = 500f;
    private static final Vector3f[] VERTICES = {
            new Vector3f(-SIZE,  SIZE, -SIZE),
            new Vector3f(-SIZE, -SIZE, -SIZE),
            new Vector3f(SIZE, -SIZE, -SIZE),
            new Vector3f(SIZE, -SIZE, -SIZE),
            new Vector3f(SIZE,  SIZE, -SIZE),
            new Vector3f(-SIZE,  SIZE, -SIZE),

            new Vector3f(-SIZE, -SIZE,  SIZE),
            new Vector3f( -SIZE, -SIZE, -SIZE),
            new Vector3f(-SIZE,  SIZE, -SIZE),
            new Vector3f(-SIZE,  SIZE, -SIZE),
            new Vector3f(-SIZE,  SIZE,  SIZE),
            new Vector3f(-SIZE, -SIZE,  SIZE),

            new Vector3f(SIZE, -SIZE, -SIZE),
            new Vector3f(SIZE, -SIZE,  SIZE),
            new Vector3f(SIZE,  SIZE,  SIZE),
            new Vector3f(SIZE,  SIZE,  SIZE),
            new Vector3f(SIZE,  SIZE, -SIZE),
            new Vector3f(SIZE, -SIZE, -SIZE),

            new Vector3f(-SIZE, -SIZE,  SIZE),
            new Vector3f(-SIZE,  SIZE,  SIZE),
            new Vector3f(SIZE,  SIZE,  SIZE),
            new Vector3f(SIZE,  SIZE,  SIZE),
            new Vector3f(SIZE, -SIZE,  SIZE),
            new Vector3f(-SIZE, -SIZE,  SIZE),

            new Vector3f(-SIZE,  SIZE, -SIZE),
            new Vector3f(SIZE,  SIZE, -SIZE),
            new Vector3f(SIZE,  SIZE,  SIZE),
            new Vector3f(SIZE,  SIZE,  SIZE),
            new Vector3f(-SIZE,  SIZE,  SIZE),
            new Vector3f(-SIZE,  SIZE, -SIZE),

            new Vector3f(-SIZE, -SIZE, -SIZE),
            new Vector3f(-SIZE, -SIZE,  SIZE),
            new Vector3f(SIZE, -SIZE, -SIZE),
            new Vector3f(SIZE, -SIZE, -SIZE),
            new Vector3f(-SIZE, -SIZE,  SIZE),
            new Vector3f(SIZE, -SIZE,  SIZE)
    };

    private final Model cube;
    private final int textureID;
    private final SkyboxShader shader;
    private Camera mainCamera;

    private RenderMetrics metrics;
    private boolean recordMetrics = false;

    public SkyboxRenderer(String[] textureFiles){
        cube = ModelManager.loadModel(VERTICES);
        textureID = TextureLoader.loadCubeMapTexture(textureFiles);

        try {
            shader = new SkyboxShader();
            shader.init();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void render(){
        if(mainCamera == null) return;
        prepare();
        if (recordMetrics) metrics.recordStateChange();
        shader.prepare(mainCamera);
        if (recordMetrics) metrics.recordVaoBind();
        GL30.glBindVertexArray(cube.getId());
        GL20.glEnableVertexAttribArray(0);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, textureID);
        if (recordMetrics) metrics.recordDrawCall();
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, cube.getMesh().getVertexCount());
        GL20.glDisableVertexAttribArray(0);
        GL30.glBindVertexArray(0);
        endRendering();
    }

    public void cleanUp(){
        shader.cleanUp();
    }

    private void prepare(){
        GL11.glDepthMask(false);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        if (recordMetrics) metrics.recordShaderBind();
        shader.bind();
    }

    private void endRendering(){
        GL11.glDepthMask(true);
        GL11.glDepthFunc(GL11.GL_LESS);
        shader.unbind();
    }

    public void setMetrics(RenderMetrics metrics){
        this.metrics = metrics;
        recordMetrics = true;
    }

    public void recordMetrics(boolean recordMetrics) {
        this.recordMetrics = recordMetrics;
    }

    public void setMainCamera(Camera camera){
        this.mainCamera = camera;
    }
}
