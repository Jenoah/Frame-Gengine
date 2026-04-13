package nl.framegengine.editor.widgets;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVG.nvgFill;

/**
 * Toggle checkbox widget with an inline label.
 *
 * <p>The checkbox box is drawn on the left; the label follows to its right.
 * The entire row (box + label) is the clickable hit area.
 *
 * <h3>Usage pattern</h3>
 * <pre>{@code
 * private boolean wireframe = false;
 * private final NVGCheckbox wireframeCheckbox = new NVGCheckbox();
 *
 * // Inside renderFrame():
 * if (wireframeCheckbox.render(vg, x, y, w, h, "Wireframe", wireframe)) {
 *     wireframe = wireframeCheckbox.isChecked();
 * }
 * }</pre>
 *
 * <p>Task 2.4 — Checkbox, Combo, Selectable.
 */
public class NVGCheckbox {

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private boolean checked  = false;
    private boolean hovered  = false;
    private boolean pressed  = false;

    private final float[] mousePos = new float[2];

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Renders the checkbox and processes input.
     *
     * @param vg           NanoVG context handle
     * @param x            left edge of the full row
     * @param y            top edge of the full row
     * @param w            width of the full row (hit area)
     * @param h            row height (0 = {@link NVGStyle#widgetHeight})
     * @param label        label text displayed to the right of the box
     * @param currentValue the current checked state (drives the display this frame)
     * @return {@code true} on the frame the toggle fires (click released while hovered)
     */
    public boolean render(long vg, float x, float y, float w, float h,
                          String label, boolean currentValue) {
        if (h <= 0) h = NVGStyle.getInstance().widgetHeight;
        this.checked = currentValue;

        EditorInput.getMousePos(mousePos);
        boolean toggled = updateState(x, y, w, h, mousePos[0], mousePos[1], EditorInput.isLeftDown());
        if (toggled) checked = !checked;

        draw(vg, x, y, w, h, label);
        return toggled;
    }

    /** Returns the current checked state. Updated immediately when a toggle fires. */
    public boolean isChecked() {
        return checked;
    }

    /** Returns whether the cursor is currently over the checkbox row. */
    public boolean isHovered() {
        return hovered;
    }

    // -------------------------------------------------------------------------
    // Package-private for testing
    // -------------------------------------------------------------------------

    /**
     * Drives the state machine with injected input values.
     *
     * @return {@code true} if a toggle (click) fired this frame
     */
    boolean updateState(float x, float y, float w, float h,
                        float mx, float my, boolean leftDown) {
        boolean wasPressed = pressed;
        hovered = NVGDrawHelper.isPointInRect(mx, my, x, y, w, h);

        if (hovered && leftDown) {
            pressed = true;
        } else if (!leftDown) {
            pressed = false;
        }

        return wasPressed && !leftDown && hovered;
    }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    private void draw(long vg, float x, float y, float w, float h, String label) {
        NVGStyle style = NVGStyle.getInstance();

        // Box dimensions — square, vertically centred in the row
        float boxSize = h - style.paddingY * 2f;
        float boxX    = x + style.paddingX;
        float boxY    = y + (h - boxSize) / 2f;

        // Box background
        var bg = (hovered || pressed) ? style.colorWidgetBgHovered : style.colorWidgetBg;
        NVGDrawHelper.drawRoundedRectWithBorder(vg, boxX, boxY, boxSize, boxSize,
                style.cornerRadius * 0.6f, bg, style.borderWidth,
                hovered ? style.colorBorderHovered : style.colorBorder);

        // Check mark (filled accent box inset 3px when checked)
        if (checked) {
            float inset = 3f;
            NVGDrawHelper.drawRoundedRect(vg,
                    boxX + inset, boxY + inset,
                    boxSize - inset * 2f, boxSize - inset * 2f,
                    style.cornerRadius * 0.3f,
                    style.colorAccent);
        }

        // Label
        float labelX = boxX + boxSize + style.paddingX;
        nvgFontSize(vg, style.fontSizeDefault);
        nvgFontFace(vg, nl.framegengine.editor.ui.NanoVGContext.DEFAULT_FONT);
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        nvgFillColor(vg, style.colorText);
        nvgText(vg, labelX, y + h / 2f, label);
    }
}
