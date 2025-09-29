package nl.framegengine.core.input;

import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.engine.WindowManager;
import nl.framegengine.editor.EngineSettings;
import org.joml.Vector2d;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;

public class MouseInput {

    private static final Vector2d previousPosition = new Vector2d(-1);
    private static final Vector2f currentPosition = new Vector2f(-1);
    private static final Vector2f mouseDelta = new Vector2f(0);
    private static final Vector2f mouseOffset = new Vector2f(0);

    private static boolean lbDown = false;
    private static boolean rbDown = false;

    private static WindowManager windowManager;

    private static GLFWCursorPosCallback prevCursorPosCallback;
    private static GLFWMouseButtonCallback prevMouseButtonCallback;

    public static void init(){
        windowManager = WindowManager.getInstance();

        prevCursorPosCallback = GLFW.glfwSetCursorPosCallback(WindowManager.getInstance().getWindow(), null);
        prevMouseButtonCallback = GLFW.glfwSetMouseButtonCallback(WindowManager.getInstance().getWindow(), null);

        // Now set your own callbacks
        GLFW.glfwSetCursorPosCallback(windowManager.getWindow(), (window, xPos, yPos) -> {
            if (windowManager.getFocus()) {
                if(!EngineSettings.isCompiled){
                    currentPosition.x = (float) xPos - mouseOffset.x;
                    currentPosition.y = (float) yPos - mouseOffset.y;
                }else {
                    currentPosition.x = (float) xPos;
                    currentPosition.y = (float) yPos;
                }
            }
            // Call previous callback if set
            if (prevCursorPosCallback != null)
                prevCursorPosCallback.invoke(window, xPos, yPos);
        });

        GLFW.glfwSetMouseButtonCallback(windowManager.getWindow(), (window, button, action, mods) -> {
            if (WindowManager.getInstance().getFocus()) {
                lbDown = button == GLFW.GLFW_MOUSE_BUTTON_1 && action == GLFW.GLFW_PRESS;
                rbDown = button == GLFW.GLFW_MOUSE_BUTTON_2 && action == GLFW.GLFW_PRESS;
            }
            if (prevMouseButtonCallback != null)
                prevMouseButtonCallback.invoke(window, button, action, mods);
        });
    }

    public static void setMouseOffset(int x, int y){
        Debug.log("Mouse offset set to " + x + " and " + y);

        mouseOffset.set(x, y);
    }

    public static void input(){
        mouseDelta.x = 0;
        mouseDelta.y = 0;

        if(previousPosition.x > 0 && previousPosition.y > 0){
            double xDelta = currentPosition.x - previousPosition.x;
            double yDelta = currentPosition.y - previousPosition.y;
            boolean rotateX = xDelta != 0;
            boolean rotateY = yDelta != 0;
            if(rotateX){
                mouseDelta.y = (float)xDelta;
            }
            if(rotateY){
                mouseDelta.x = (float)yDelta;
            }
        }

        previousPosition.x = currentPosition.x;
        previousPosition.y = currentPosition.y;
    }

    public static boolean isRbDown() {
        return rbDown;
    }

    public static boolean isLbDown() {
        return lbDown;
    }

    public static Vector2f getMouseDelta() {
        return mouseDelta;
    }

    public static void cleanUp(){
        GLFW.glfwSetCursorPosCallback(windowManager.getWindow(), prevCursorPosCallback);
        GLFW.glfwSetMouseButtonCallback(windowManager.getWindow(), prevMouseButtonCallback);

        prevCursorPosCallback = null;
        prevMouseButtonCallback = null;
    }

    public static Vector2f getMousePositionInViewport(){
        float mousePositionX = (float) (1.0f / windowManager.getWidth() * currentPosition.x);
        float mousePositionY = (float) (1.0f / windowManager.getHeight() * currentPosition.y);

        return new Vector2f(mousePositionX, mousePositionY);
    }

    public static Vector2f getMousePositionInPixels(){
        return currentPosition;
    }

    public static void hide(){
        if (GLFW.glfwGetInputMode(windowManager.getWindow(), GLFW.GLFW_CURSOR) != GLFW.GLFW_CURSOR_DISABLED) {
            GLFW.glfwSetInputMode(windowManager.getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        }
    }

    public static void show(){
        if (GLFW.glfwGetInputMode(windowManager.getWindow(), GLFW.GLFW_CURSOR) != GLFW.GLFW_CURSOR_NORMAL) {
            GLFW.glfwSetInputMode(windowManager.getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        }
    }
}
