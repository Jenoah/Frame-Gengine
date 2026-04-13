package nl.framegengine.editor.widgets;

import nl.framegengine.editor.EditorWindow;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Thin wrapper around GLFW input queries scoped to the editor window.
 *
 * <p>Editor widgets must <em>not</em> use {@link nl.framegengine.core.input.MouseInput}
 * because that class applies a viewport offset and only processes events when the
 * game panel has focus.  This helper queries GLFW directly on the editor window
 * so it works regardless of game-panel focus state.
 *
 * <p>All methods are static and safe to call from any widget render method,
 * provided {@link EditorWindow#getInstance()} is non-null.
 *
 * <p>Task 2.2 — Button, Label, Separator.
 */
public final class EditorInput {

    private EditorInput() {}

    // -------------------------------------------------------------------------
    // Cursor position
    // -------------------------------------------------------------------------

    /**
     * Returns the current cursor X position in screen (framebuffer) pixels,
     * relative to the top-left corner of the editor window.
     */
    public static float getMouseX() {
        double[] x = new double[1];
        double[] y = new double[1];
        glfwGetCursorPos(windowPtr(), x, y);
        return (float) x[0];
    }

    /**
     * Returns the current cursor Y position in screen (framebuffer) pixels,
     * relative to the top-left corner of the editor window.
     */
    public static float getMouseY() {
        double[] x = new double[1];
        double[] y = new double[1];
        glfwGetCursorPos(windowPtr(), x, y);
        return (float) y[0];
    }

    /**
     * Fills {@code out} with the current cursor position.
     * {@code out[0]} = X, {@code out[1]} = Y.
     *
     * <p>Prefer this over calling {@link #getMouseX()} and {@link #getMouseY()}
     * separately to avoid two GLFW calls per widget.
     *
     * @param out float array of length &ge; 2
     */
    public static void getMousePos(float[] out) {
        double[] x = new double[1];
        double[] y = new double[1];
        glfwGetCursorPos(windowPtr(), x, y);
        out[0] = (float) x[0];
        out[1] = (float) y[0];
    }

    // -------------------------------------------------------------------------
    // Button state
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the left mouse button is currently held down.
     */
    public static boolean isLeftDown() {
        return glfwGetMouseButton(windowPtr(), GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
    }

    /**
     * Returns {@code true} if the right mouse button is currently held down.
     */
    public static boolean isRightDown() {
        return glfwGetMouseButton(windowPtr(), GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS;
    }

    // -------------------------------------------------------------------------
    // Private helper
    // -------------------------------------------------------------------------

    private static long windowPtr() {
        return EditorWindow.getInstance().getWindowPtr();
    }
}
