package nl.framegengine.core.rendering;

import nl.framegengine.core.rendering.renderers.ComponentRenderer;
import nl.framegengine.core.rendering.renderers.DebugRenderer;
import nl.framegengine.core.rendering.renderers.PostProcessing;
import nl.framegengine.core.rendering.shadow.ShadowRenderer;
import nl.framegengine.core.rendering.utils.FrameBuffer;
import nl.framegengine.editor.EditorWindow;
import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.components.RenderComponent;
import nl.framegengine.core.debugging.RenderMetrics;
import nl.framegengine.core.fonts.fontRendering.FontRenderer;
import nl.framegengine.core.gui.GuiRenderer;
import nl.framegengine.core.engine.WindowManager;
import nl.framegengine.core.entity.Scene;
import nl.framegengine.core.skybox.SkyboxRenderer;
import nl.framegengine.core.utils.Constants;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.glViewport;

public class RenderManager {
    private final WindowManager window;
    private ComponentRenderer componentRenderer;
    public ShadowRenderer shadowRenderer;
    private GuiRenderer guiRenderer;
    private FontRenderer fontRenderer;
    private FrameBuffer frameBuffer;
    private FrameBuffer editorBuffer;
    private SkyboxRenderer skyboxRenderer;
    private DebugRenderer debugRenderer;
    private final RenderMetrics metrics;
    private boolean recordMetrics = false;
    public static float aspectRatio = 1.77f;
    private Camera renderCamera = null;

    private static RenderManager instance = null;

    public RenderManager() {
        window = WindowManager.getInstance();
        metrics = new RenderMetrics();
        instance = this;
    }

    public static synchronized void createInstance() {
        if (instance != null) {
            throw new IllegalStateException("RenderManager already initialized");
        }
        instance = new RenderManager();
    }

    public static RenderManager getInstance() {
        return instance;
    }

    public void init() throws Exception {
        componentRenderer = new ComponentRenderer();
        shadowRenderer = new ShadowRenderer();

        guiRenderer = new GuiRenderer();
        fontRenderer = new FontRenderer();
        skyboxRenderer = new SkyboxRenderer(new String[]{"textures/skyboxes/clouds1/right.png", "textures/skyboxes/clouds1/left.png", "textures/skyboxes/clouds1/top.png", "textures/skyboxes/clouds1/bottom.png", "textures/skyboxes/clouds1/back.png", "textures/skyboxes/clouds1/front.png"});
        debugRenderer = new DebugRenderer();
        componentRenderer.init();
        shadowRenderer.init();
        debugRenderer.init();
        componentRenderer.setShadowMapID(shadowRenderer.getShadowMapID());

        regenerateFrameBuffer();
        PostProcessing.init();
    }

    public void render(Scene currentScene){
        if (recordMetrics) metrics.frameStart();

        if(window.isResize()){
            glViewport(0, 0, window.getWidth(), window.getHeight());
            regenerateFrameBuffer();
            window.setResize(false);
            window.updateProjectionMatrix();
            PostProcessing.updateResolution();
            aspectRatio = (float)window.getWidth() / (float)window.getHeight();
        }

        if(renderCamera == null){
            renderCamera = Camera.mainCamera;
            setRenderCamera(renderCamera);
        }

        shadowRenderer.render(currentScene);
        componentRenderer.setShadowSpaceMatrix(shadowRenderer.getToShadowMapSpaceMatrix());

        //3D rendering
        frameBuffer.bindFrameBuffer();
        clear();

        //Rendering of scene
        skyboxRenderer.render();
        componentRenderer.render();
        debugRenderer.render();

        frameBuffer.unbindFrameBuffer();

        //Post Processing
        PostProcessing.render(frameBuffer.getColourTexture());

        if(!window.isStandalone()) {
            clear();
            editorBuffer.bindFrameBuffer();
            PostProcessing.renderOutput();
        }

        //End of 3D rendering

        //Overlay
        guiRenderer.render(currentScene.getGuiObjects());
        fontRenderer.render(currentScene.getTextObjects());

        if(!window.isStandalone()) editorBuffer.unbindFrameBuffer();

        if (recordMetrics) metrics.frameEnd();
    }

    public void clear(){
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    public void cleanUp(){
        PostProcessing.cleanUp();
        componentRenderer.cleanUp();
        frameBuffer.cleanUp();
        if(!window.isStandalone()) editorBuffer.cleanUp();
        guiRenderer.cleanUp();
        fontRenderer.cleanUp();
        skyboxRenderer.cleanUp();
        shadowRenderer.cleanUp();
        debugRenderer.cleanUp();
        instance = null;
        renderCamera = null;
    }

    private void regenerateFrameBuffer(){
        frameBuffer = new FrameBuffer(window.getWidth(), window.getHeight(), FrameBuffer.DEPTH_RENDER_BUFFER);
        if(!window.isStandalone()){
            editorBuffer = new FrameBuffer(window.getWidth(), window.getHeight(), FrameBuffer.NONE);
            EditorWindow.getInstance().setGameFBOID(editorBuffer.getColourTexture());
        }
    }

    public void queueRender(RenderComponent renderComponent){
        componentRenderer.queue(renderComponent);
        shadowRenderer.queue(renderComponent);
    }

    public void debugCube(Vector3f position, Vector3f size){
        debugRenderer.drawCube(position, size);
    }

    public void debugCube(Vector3f position, Quaternionf rotation, Vector3f size){
        debugRenderer.drawCube(position, rotation, size);
    }

    public void debugCube(Vector3f position){
        debugRenderer.drawCube(position, Constants.VECTOR3_ONE);
    }

    public void debugCube(Vector3f position, Quaternionf rotation){
        debugRenderer.drawCube(position, rotation, Constants.VECTOR3_ONE);
    }

    public void dequeueRender(RenderComponent renderComponent){
        componentRenderer.dequeue(renderComponent);
        shadowRenderer.dequeue(renderComponent);
    }

    public void setRenderCamera(Camera renderCamera){
        shadowRenderer.setMainCamera(renderCamera);
        componentRenderer.setMainCamera(renderCamera);
        skyboxRenderer.setMainCamera(renderCamera);
        debugRenderer.setMainCamera(renderCamera);
    }

    public void recordMetrics(boolean recordState){
        this.recordMetrics = recordState;
        if(recordMetrics){
            componentRenderer.setMetrics(this.metrics);
            shadowRenderer.setMetrics(this.metrics);
            guiRenderer.setMetrics(this.metrics);
            fontRenderer.setMetrics(this.metrics);
            skyboxRenderer.setMetrics(this.metrics);
        }
        componentRenderer.recordMetrics(recordMetrics);
        shadowRenderer.recordMetrics(recordMetrics);
        guiRenderer.recordMetrics(recordMetrics);
        fontRenderer.recordMetrics(recordMetrics);
        skyboxRenderer.setMetrics(this.metrics);
    }

    public String getMetrics() {
        return recordMetrics ? metrics.getMetrics() : "Metrics not recorded";
    }
}
