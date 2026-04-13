package nl.framegengine.editor.widgets;

import static org.lwjgl.nanovg.NanoVG.*;

/**
 * Highlight-on-hover selectable list item.
 *
 * <p>Used for items in tree views, file lists, object hierarchies, and similar
 * single-column lists.  Each instance is <em>stateless</em> with respect to
 * selection — the caller owns the selected state and passes it in each frame.
 *
 * <h3>Usage pattern</h3>
 * <pre>{@code
 * private int selectedObject = -1;
 * private final NVGSelectable selectable = new NVGSelectable();
 *
 * for (int i = 0; i < objects.size(); i++) {
 *     if (selectable.render(vg, x, y + i * rowH, w, rowH,
 *                           objects.get(i).getName(), i == selectedObject)) {
 *         selectedObject = i;
 *     }
 * }
 * }</pre>
 *
 * <p>Task 2.4 — Checkbox, Combo, Selectable.
 */
public class NVGSelectable {

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private boolean hovered = false;
    private boolean pressed = false;

    private final float[] mousePos = new float[2];

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Renders the selectable row and processes input.
     *
     * @param vg       NanoVG context handle
     * @param x        left edge
     * @param y        top edge
     * @param w        width
     * @param h        row height (0 = {@link NVGStyle#widgetHeight})
     * @param label    row text
     * @param selected whether this item is currently selected
     * @return {@code true} on the frame this item is clicked
     */
    public boolean render(long vg, float x, float y, float w, float h,
                          String label, boolean selected) {
        if (h <= 0) h = NVGStyle.getInstance().widgetHeight;

        EditorInput.getMousePos(mousePos);
        boolean clicked = updateState(x, y, w, h, mousePos[0], mousePos[1], EditorInput.isLeftDown());

        draw(vg, x, y, w, h, label, selected);
        return clicked;
    }

    /** Returns whether the cursor is currently over this row. */
    public boolean isHovered() {
        return hovered;
    }

    // -------------------------------------------------------------------------
    // Package-private for testing
    // -------------------------------------------------------------------------

    /**
     * Drives the state machine with injected input.
     *
     * @return {@code true} if a click fired this frame
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

    private void draw(long vg, float x, float y, float w, float h,
                      String label, boolean selected) {
        NVGStyle style = NVGStyle.getInstance();

        // Background highlight
        if (selected) {
            NVGDrawHelper.drawRoundedRect(vg, x, y, w, h, style.cornerRadius * 0.5f,
                    style.colorAccent);
        } else if (pressed) {
            NVGDrawHelper.drawRoundedRect(vg, x, y, w, h, style.cornerRadius * 0.5f,
                    style.colorWidgetBgActive);
        } else if (hovered) {
            NVGDrawHelper.drawRoundedRect(vg, x, y, w, h, style.cornerRadius * 0.5f,
                    style.colorSelectionBg);
        }

        // Label
        var textColor = selected ? style.colorTextOnAccent : style.colorText;
        nvgFontSize(vg, style.fontSizeDefault);
        nvgFontFace(vg, nl.framegengine.editor.ui.NanoVGContext.DEFAULT_FONT);
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        nvgFillColor(vg, textColor);
        nvgText(vg, x + style.paddingX, y + h / 2f, label);
    }
}
