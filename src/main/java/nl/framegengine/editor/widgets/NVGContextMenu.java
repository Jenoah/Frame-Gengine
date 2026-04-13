package nl.framegengine.editor.widgets;

import static org.lwjgl.nanovg.NanoVG.*;

/**
 * Right-click context menu popup.
 *
 * <p>The menu is shown by calling {@link #open(float, float)} with the screen
 * position where it should appear (typically the right-click cursor position).
 * While open, {@link #render} draws the popup and returns the index of a
 * clicked item (or {@code -1} if nothing was selected this frame).
 * The menu dismisses itself when a click occurs outside its bounds.
 *
 * <p>The overlay is drawn in the same NanoVG frame as the rest of the UI;
 * callers should render the context menu <em>last</em> so it paints on top.
 *
 * <h3>Usage pattern</h3>
 * <pre>{@code
 * private static final String[] MENU_ITEMS = {"Rename", "Duplicate", "Delete"};
 * private final NVGContextMenu ctxMenu = new NVGContextMenu();
 *
 * // Right-click detection (caller's responsibility):
 * if (EditorInput.isRightClicked()) {
 *     ctxMenu.open(mouseX, mouseY);
 * }
 *
 * // Inside renderFrame() — render last:
 * int chosen = ctxMenu.render(vg, MENU_ITEMS);
 * if (chosen == 2) deleteSelectedObject();
 * }</pre>
 *
 * <p>Task 2.6 — Context Menu, Modal, Progress Bar.
 */
public class NVGContextMenu {

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private boolean open       = false;
    private float   originX    = 0f;
    private float   originY    = 0f;
    private int     hoveredRow = -1;
    private int     pressedRow = -1;

    private final float[] mousePos = new float[2];

    // -------------------------------------------------------------------------
    // Layout constants
    // -------------------------------------------------------------------------

    private static final float ITEM_HEIGHT  = 22f;
    private static final float MIN_WIDTH    = 120f;
    private static final float PADDING_V    = 4f;   // top/bottom padding inside popup

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Opens the context menu at the given screen position.
     * If the menu is already open, it moves to the new position.
     *
     * @param x left edge of the popup
     * @param y top edge of the popup
     */
    public void open(float x, float y) {
        this.originX    = x;
        this.originY    = y;
        this.open       = true;
        this.hoveredRow = -1;
        this.pressedRow = -1;
    }

    /** Closes the menu without selecting anything. */
    public void close() {
        open       = false;
        hoveredRow = -1;
        pressedRow = -1;
    }

    /** Returns {@code true} if the menu is currently visible. */
    public boolean isOpen() {
        return open;
    }

    /**
     * Renders the context menu and processes input.
     * Must be called every frame; does nothing (and returns {@code -1}) when closed.
     *
     * @param vg    NanoVG context handle
     * @param items menu item labels (must not be {@code null} or empty)
     * @return the 0-based index of the item clicked this frame, or {@code -1}
     */
    public int render(long vg, String[] items) {
        if (!open || items == null || items.length == 0) return -1;

        EditorInput.getMousePos(mousePos);
        float mx = mousePos[0];
        float my = mousePos[1];
        boolean leftDown = EditorInput.isLeftDown();

        float w = computeWidth(items);
        float h = PADDING_V * 2f + items.length * ITEM_HEIGHT;

        int selected = updateState(originX, originY, w, h, mx, my, leftDown, items);
        draw(vg, originX, originY, w, h, items);
        return selected;
    }

    // -------------------------------------------------------------------------
    // Package-private for testing
    // -------------------------------------------------------------------------

    /**
     * Drives the state machine with injected input values.
     *
     * @return the index of the item clicked this frame, or {@code -1}
     */
    int updateState(float x, float y, float w, float h,
                    float mx, float my, boolean leftDown,
                    String[] items) {

        boolean insideMenu = NVGDrawHelper.isPointInRect(mx, my, x, y, w, h);

        // Determine hovered row
        hoveredRow = -1;
        if (insideMenu) {
            int row = (int) ((my - (y + PADDING_V)) / ITEM_HEIGHT);
            if (row >= 0 && row < items.length) hoveredRow = row;
        }

        // Press
        if (leftDown) {
            if (hoveredRow >= 0) {
                pressedRow = hoveredRow;
            } else if (!insideMenu) {
                // Click outside — dismiss
                close();
                return -1;
            }
        }

        // Release — commit selection
        if (!leftDown && pressedRow >= 0) {
            int chosen = pressedRow;
            pressedRow = -1;
            if (chosen == hoveredRow) {
                close();
                return chosen;
            }
        }

        if (!leftDown) pressedRow = -1;

        return -1;
    }

    /** Returns the index of the currently hovered item, or {@code -1}. */
    int getHoveredRow() { return hoveredRow; }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    private void draw(long vg, float x, float y, float w, float h, String[] items) {
        NVGStyle style = NVGStyle.getInstance();

        // Popup background + border
        NVGDrawHelper.drawRoundedRectWithBorder(vg, x, y, w, h,
                style.cornerRadius, style.colorWidgetBg, style.borderWidth, style.colorBorder);

        for (int i = 0; i < items.length; i++) {
            float rowY = y + PADDING_V + i * ITEM_HEIGHT;

            if (i == hoveredRow || i == pressedRow) {
                NVGDrawHelper.drawRoundedRect(vg, x + 2, rowY, w - 4, ITEM_HEIGHT,
                        style.cornerRadius * 0.5f, style.colorWidgetBgHovered);
            }

            nvgFontSize(vg, style.fontSizeDefault);
            nvgFontFace(vg, nl.framegengine.editor.ui.NanoVGContext.DEFAULT_FONT);
            nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
            nvgFillColor(vg, style.colorText);
            nvgText(vg, x + style.paddingX, rowY + ITEM_HEIGHT / 2f, items[i]);
        }
    }

    private float computeWidth(String[] items) {
        // Width is approximated — real measurement needs a live vg context.
        // Use a fixed character-width estimate: 7px per char + horizontal padding.
        float maxChars = 0;
        for (String item : items) maxChars = Math.max(maxChars, item.length());
        return Math.max(MIN_WIDTH, maxChars * 7f + NVGStyle.getInstance().paddingX * 2f);
    }
}
