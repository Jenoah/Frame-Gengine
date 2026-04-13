package nl.framegengine.editor.widgets;

import org.lwjgl.nanovg.NVGColor;

/**
 * Stateful NanoVG button widget.
 *
 * <p>Each instance tracks its own hover and press state across frames.
 * Call {@link #render(long, float, float, float, float, String)} every frame
 * inside an active NanoVG frame; the method returns {@code true} on the frame
 * the button is <em>clicked</em> (left-button released while still hovered).
 *
 * <h3>Usage pattern</h3>
 * <pre>{@code
 * // Declare once per button (field or local with stable lifetime):
 * private final NVGButton myButton = new NVGButton();
 *
 * // Inside renderFrame():
 * if (myButton.render(vg, x, y, w, h, "Click Me")) {
 *     // handle click
 * }
 * }</pre>
 *
 * <p>Task 2.2 — Button, Label, Separator.
 */
public class NVGButton {

    // -------------------------------------------------------------------------
    // Per-instance state
    // -------------------------------------------------------------------------

    private boolean hovered = false;
    private boolean pressed = false;

    // Reusable mouse-position array to avoid per-frame allocation.
    private final float[] mousePos = new float[2];

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Renders the button and updates hover/press state.
     *
     * @param vg     NanoVG context handle
     * @param x      left edge in pixels
     * @param y      top edge in pixels
     * @param w      width in pixels
     * @param h      height in pixels (pass {@code 0} to use {@link NVGStyle#widgetHeight})
     * @param label  button text
     * @return {@code true} on the frame the button is clicked (press released while hovered)
     */
    public boolean render(long vg, float x, float y, float w, float h, String label) {
        if (h <= 0) h = NVGStyle.getInstance().widgetHeight;

        EditorInput.getMousePos(mousePos);
        boolean clicked = updateState(x, y, w, h, mousePos[0], mousePos[1], EditorInput.isLeftDown());

        draw(vg, x, y, w, h, label);
        return clicked;
    }

    /**
     * Updates hover/press state given injected input values and returns whether
     * a click occurred this frame.
     *
     * <p>Extracted as a {@code protected} method so unit tests can drive the
     * state machine without a live GLFW window or NanoVG context.
     *
     * @param x        button left edge
     * @param y        button top edge
     * @param w        button width
     * @param h        button height
     * @param mx       current cursor X
     * @param my       current cursor Y
     * @param leftDown whether the left mouse button is currently held
     * @return {@code true} if a click fired this frame
     */
    protected boolean updateState(float x, float y, float w, float h,
                                  float mx, float my, boolean leftDown) {
        boolean wasPressed = pressed;
        hovered = NVGDrawHelper.isPointInRect(mx, my, x, y, w, h);

        if (hovered && leftDown) {
            pressed = true;
        } else if (!leftDown) {
            pressed = false;
        }

        // Click = was pressed last frame, left button now released, still hovered.
        return wasPressed && !leftDown && hovered;
    }

    /**
     * Returns whether the cursor is currently over this button.
     * Valid after the most recent call to {@link #render}.
     */
    public boolean isHovered() {
        return hovered;
    }

    /**
     * Returns whether the button is currently being held down.
     * Valid after the most recent call to {@link #render}.
     */
    public boolean isPressed() {
        return pressed;
    }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    private void draw(long vg, float x, float y, float w, float h, String label) {
        NVGStyle style = NVGStyle.getInstance();

        NVGColor bg     = pressed  ? style.colorWidgetBgActive
                        : hovered  ? style.colorWidgetBgHovered
                                   : style.colorWidgetBg;
        NVGColor border = pressed  ? style.colorBorderActive
                        : hovered  ? style.colorBorderHovered
                                   : style.colorBorder;

        NVGDrawHelper.drawRoundedRectWithBorder(
                vg, x, y, w, h,
                style.cornerRadius,
                bg,
                style.borderWidth,
                border);

        NVGDrawHelper.drawText(vg, x, y, h, label, style.colorText);
    }
}
