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
    private static WindowManager window;
    private static ComponentRenderer componentRenderer;
    public static ShadowRenderer shadowRenderer;
    private static GuiRenderer guiRenderer;
    private static FontRenderer fontRenderer;
    private static FrameBuffer frameBuffer;
    private static FrameBuffer editorBuffer;
    private static SkyboxRenderer skyboxRenderer;
    private static DebugRenderer debugRenderer;
    private static RenderMetrics metrics;
    private static boolean recordMetrics = false;
    public static float aspectRatio = 1.77f;
    private static Camera renderCamera = null;

    public static void init() throws Exception {
        if(window == null && WindowManager.getInstance() != null) window = WindowManager.getInstance();
        if(metrics == null) metrics = new RenderMetrics();

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

    public static void render(Scene currentScene){
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

    public static void clear(){
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    public static void cleanUp(){
        PostProcessing.cleanUp();
        componentRenderer.cleanUp();
        frameBuffer.cleanUp();
        if(!window.isStandalone()) editorBuffer.cleanUp();
        guiRenderer.cleanUp();
        fontRenderer.cleanUp();
        skyboxRenderer.cleanUp();
        shadowRenderer.cleanUp();
        debugRenderer.cleanUp();
        renderCamera = null;
    }

    private static void regenerateFrameBuffer(){
        frameBuffer = new FrameBuffer(window.getWidth(), window.getHeight(), FrameBuffer.DEPTH_RENDER_BUFFER);
        if(!window.isStandalone()){
            editorBuffer = new FrameBuffer(window.getWidth(), window.getHeight(), FrameBuffer.NONE);
            EditorWindow.getInstance().setGameFBOID(editorBuffer.getColourTexture());
        }
    }

    public static void queueRender(RenderComponent renderComponent){
        componentRenderer.queue(renderComponent);
        shadowRenderer.queue(renderComponent);
    }

    public static void debugCube(Vector3f position, Vector3f size){
        debugRenderer.drawCube(position, size);
    }

    public static void debugCube(Vector3f position, Quaternionf rotation, Vector3f size){
        debugRenderer.drawCube(position, rotation, size);
    }

    public static void debugCube(Vector3f position){
        debugRenderer.drawCube(position, Constants.VECTOR3_ONE);
    }

    public static void debugCube(Vector3f position, Quaternionf rotation){
        debugRenderer.drawCube(position, rotation, Constants.VECTOR3_ONE);
    }

    public static void dequeueRender(RenderComponent renderComponent){
        componentRenderer.dequeue(renderComponent);
        shadowRenderer.dequeue(renderComponent);
    }

    public static void setRenderCamera(Camera renderCamera){
        shadowRenderer.setMainCamera(renderCamera);
        componentRenderer.setMainCamera(renderCamera);
        skyboxRenderer.setMainCamera(renderCamera);
        debugRenderer.setMainCamera(renderCamera);
    }

    public static void recordMetrics(boolean recordState){
        recordMetrics = recordState;
        if(recordMetrics){
            componentRenderer.setMetrics(metrics);
            shadowRenderer.setMetrics(metrics);
            guiRenderer.setMetrics(metrics);
            fontRenderer.setMetrics(metrics);
            skyboxRenderer.setMetrics(metrics);
        }
        componentRenderer.recordMetrics(recordMetrics);
        shadowRenderer.recordMetrics(recordMetrics);
        guiRenderer.recordMetrics(recordMetrics);
        fontRenderer.recordMetrics(recordMetrics);
    }

    public static String getMetrics() {
        return recordMetrics ? metrics.getMetrics() : "Metrics not recorded";
    }
}
