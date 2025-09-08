package nl.framegengine.core.engine;

import nl.framegengine.core.input.MouseInput;
import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.entity.SceneManager;
import nl.framegengine.editor.ImGuiHelper;
import org.joml.Math;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;

public class EngineManager {
    private boolean running = false;

    private WindowManager window;
    private GLFWErrorCallback errorCallback;
    private static ILogic gameLogic;

    // Consider making these instance fields if multiple EngineManagers are possible
    private double lastLoopTime = 0.0;
    private float fpsUpdateTimer = 0.0f;
    private static int fps = 0;
    private static float deltaTime = 0.0f;
    private static int currentFrameCount = 0;

    private static final float MIN_DELTA_TIME = 1.0f / 10000f; // 1ms minimum, adjust as needed

    private void init(final ILogic IGameLogic, boolean standaloneWindow) throws Exception {
        errorCallback = GLFWErrorCallback.createPrint(System.err);
        GLFW.glfwSetErrorCallback(errorCallback);

        window = WindowManager.getInstance();
        gameLogic = IGameLogic;
        window.init();
        window.updateProjectionMatrix();

        gameLogic.init();
        MouseInput.init();
        SceneManager.currentScene.postStart();

        if(!window.isStandalone()) ImGuiHelper.hideProgressBar();

        lastLoopTime = getCurrentTime();
    }

    public void start(final ILogic gameLogic, boolean standaloneWindow) throws Exception {
        if (running) return;
        init(gameLogic, standaloneWindow);
        Debug.log("Starting Engine...");
        run();
    }

    public void run() {
        if(!window.isStandalone()){
            renderSingleFrame();
            return;
        }

        running = true;
        try {
            while (running) {
                renderSingleFrame();
            }
        } finally {
            if(running){
                Debug.log("Stopping engine");
                cleanup();
            }
        }
    }

    private void renderSingleFrame(){
        if (window.windowShouldClose() && running){
            stop();
            return;
        }
        updateDeltaTime();
        handleInput();
        updateGame();
        renderFrame();
        calculateFps();
    }

    public void stop() {
        if(window.isStandalone() && !running) return;
        Debug.log("Stopping engine...");
        running = false;
        cleanup();
    }

    private void handleInput() {
        MouseInput.input();
        gameLogic.input();
    }

    private void updateGame() {
        gameLogic.update(deltaTime);
    }

    private void calculateFps(){
        currentFrameCount++;
        if (getCurrentTime() - fpsUpdateTimer >= 1f) {
            fps = currentFrameCount;
            currentFrameCount = 0;
            fpsUpdateTimer += 1f;
        }
    }

    private void renderFrame() {
        gameLogic.render();
        window.update();
    }

    private void cleanup() {
        Debug.log("Cleaning up...");
        MouseInput.cleanUp();
        window.cleanUp();
        gameLogic.cleanUp();
        if (errorCallback != null) errorCallback.free();
    }

    public static int getFps() {
        return fps;
    }

    public static double getCurrentTime() {
        return GLFW.glfwGetTime();
    }

    private void updateDeltaTime() {
        final double currentTime = getCurrentTime();
        deltaTime = (float) (currentTime - lastLoopTime);
        deltaTime = Math.max(deltaTime, MIN_DELTA_TIME); // Clamp to avoid extremely small values
        lastLoopTime = currentTime;
    }

    public static float getDeltaTime() {
        return deltaTime;
    }

    public static float getDeltaTimeMS() {
        return (deltaTime * 100000f) / 100f;
    }

    public static ILogic getGameLogic(){
        return gameLogic;
    }
}