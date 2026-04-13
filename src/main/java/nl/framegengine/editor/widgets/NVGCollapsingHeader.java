package nl.framegengine.editor.widgets;

import static org.lwjgl.nanovg.NanoVG.*;

/**
 * Collapsing section header with an arrow toggle and a label.
 *
 * <p>Each instance owns its own open/closed state — no external ID map needed.
 * The header row is clickable; clicking it toggles open/closed.
 *
 * <h3>Usage pattern</h3>
 * <pre>{@code
 * private final NVGCollapsingHeader transformHeader = new NVGCollapsingHeader(true);
 *
 * // Inside renderFrame():
 * if (transformHeader.render(vg, x, y, w, h, "Transform")) {
 *     // render child widgets beneath the header
 *     posField.render(vg, x + indent, y + h + ..., ...);
 * }
 * }</pre>
 *
 * <p>Task 2.5 — Collapsing Header, Indent.
 */
public class NVGCollapsingHeader {

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private boolean open;
    private boolean hovered = false;
    private boolean pressed = false;

    private final float[] mousePos = new float[2];

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /** Creates a header that starts in the <em>open</em> state. */
    public NVGCollapsingHeader() {
        this(true);
    }

    /**
     * Creates a header with an explicit initial open/closed state.
     *
     * @param initiallyOpen {@code true} to start expanded
     */
    public NVGCollapsingHeader(boolean initiallyOpen) {
        this.open = initiallyOpen;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Renders the header row and processes input.
     *
     * @param vg    NanoVG context handle
     * @param x     left edge
     * @param y     top edge
     * @param w     width
     * @param h     header row height (0 = {@link NVGStyle#widgetHeight})
     * @param label section title
     * @return {@code true} if the section is currently open (caller should render children)
     */
    public boolean render(long vg, float x, float y, float w, float h, String label) {
        if (h <= 0) h = NVGStyle.getInstance().widgetHeight;

        EditorInput.getMousePos(mousePos);
        boolean toggled = updateState(x, y, w, h, mousePos[0], mousePos[1], EditorInput.isLeftDown());
        if (toggled) open = !open;

        draw(vg, x, y, w, h, label);
        return open;
    }

    /** Returns whether the section is currently open. */
    public boolean isOpen() {
        return open;
    }

    /** Programmatically sets the open/closed state. */
    public void setOpen(boolean open) {
        this.open = open;
    }

    /** Returns whether the cursor is over the header row. */
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

        // Background — slightly elevated from panel bg when hovered
        var bg = hovered ? style.colorWidgetBgHovered : style.colorPanelBg;
        NVGDrawHelper.drawRoundedRect(vg, x, y, w, h, style.cornerRadius * 0.5f, bg);

        // Arrow icon (right = closed, down = open)
        float arrowCx = x + style.paddingX + 5f;
        float arrowCy = y + h / 2f;
        if (open) {
            NVGDrawHelper.drawIconArrowDown(vg, arrowCx, arrowCy, 9f, style.colorText);
        } else {
            NVGDrawHelper.drawIconArrowRight(vg, arrowCx, arrowCy, 9f, style.colorText);
        }

        // Label — bold-sized font to distinguish from regular widget labels
        float labelX = arrowCx + 10f;
        nvgFontSize(vg, style.fontSizeHeader);
        nvgFontFace(vg, nl.framegengine.editor.ui.NanoVGContext.DEFAULT_FONT);
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        nvgFillColor(vg, style.colorText);
        nvgText(vg, labelX, y + h / 2f, label);

        // Bottom separator line
        NVGDrawHelper.drawSeparator(vg, x, y + h - 0.5f, w, style.colorBorder);
    }
}
