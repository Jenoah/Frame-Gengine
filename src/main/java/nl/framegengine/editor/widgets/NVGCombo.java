package nl.framegengine.editor.widgets;

import static org.lwjgl.nanovg.NanoVG.*;

/**
 * Dropdown combo-box widget.
 *
 * <p>Displays the currently selected item as a button. Clicking it opens an
 * overlay list rendered <em>on top</em> of other content; clicking an item
 * selects it and closes the list; clicking outside dismisses the list without
 * changing the selection.
 *
 * <p>The overlay is drawn during the same NanoVG frame as the combo button —
 * callers must ensure they call {@link #render} <em>last</em> among widgets
 * that overlap the dropdown area (i.e. draw order is painter's algorithm).
 *
 * <h3>Usage pattern</h3>
 * <pre>{@code
 * private static final String[] ASPECTS = {"16:9","4:3","1:1"};
 * private int selectedAspect = 0;
 * private final NVGCombo aspectCombo = new NVGCombo();
 *
 * // Inside renderFrame():
 * if (aspectCombo.render(vg, x, y, w, h, ASPECTS, selectedAspect)) {
 *     selectedAspect = aspectCombo.getSelectedIndex();
 * }
 * }</pre>
 *
 * <p>Task 2.4 — Checkbox, Combo, Selectable.
 */
public class NVGCombo {

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private boolean open         = false;
    private int     selectedIndex = 0;
    private boolean hovered      = false;
    private boolean buttonPressed = false;

    private final float[] mousePos = new float[2];

    // -------------------------------------------------------------------------
    // Layout constants
    // -------------------------------------------------------------------------

    /** Height of each dropdown item row (px). */
    private static final float ITEM_HEIGHT = 22f;

    /** Extra pixels the dropdown extends beyond the button width. */
    private static final float DROPDOWN_EXTRA_W = 0f;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Renders the combo box and its overlay when open.
     *
     * @param vg            NanoVG context handle
     * @param x             left edge of the button
     * @param y             top edge of the button
     * @param w             width of the button (and dropdown)
     * @param h             button height (0 = {@link NVGStyle#widgetHeight})
     * @param items         the full item list (must not be {@code null} or empty)
     * @param currentIndex  currently selected index (clamped if out of range)
     * @return {@code true} on the frame a new item is selected
     */
    public boolean render(long vg, float x, float y, float w, float h,
                          String[] items, int currentIndex) {
        if (h <= 0) h = NVGStyle.getInstance().widgetHeight;
        if (items == null || items.length == 0) return false;

        selectedIndex = Math.max(0, Math.min(items.length - 1, currentIndex));

        EditorInput.getMousePos(mousePos);
        float mx = mousePos[0];
        float my = mousePos[1];
        boolean leftDown = EditorInput.isLeftDown();

        boolean selectionChanged = updateState(x, y, w, h, mx, my, leftDown, items);

        drawButton(vg, x, y, w, h, items[selectedIndex]);
        if (open) {
            drawDropdown(vg, x, y + h, w, items);
        }

        return selectionChanged;
    }

    /** Returns the index of the currently selected item. */
    public int getSelectedIndex() {
        return selectedIndex;
    }

    /** Returns whether the dropdown is currently open. */
    public boolean isOpen() {
        return open;
    }

    /** Returns whether the cursor is over the combo button (not the dropdown). */
    public boolean isHovered() {
        return hovered;
    }

    /** Programmatically closes the dropdown without changing the selection. */
    public void close() {
        open = false;
    }

    // -------------------------------------------------------------------------
    // Package-private for testing
    // -------------------------------------------------------------------------

    /**
     * Drives the state machine with injected input.
     *
     * @return {@code true} if a new item was selected this frame
     */
    boolean updateState(float x, float y, float w, float h,
                        float mx, float my, boolean leftDown,
                        String[] items) {
        boolean wasButtonPressed = buttonPressed;
        hovered = NVGDrawHelper.isPointInRect(mx, my, x, y, w, h);

        if (hovered && leftDown) {
            buttonPressed = true;
        } else if (!leftDown) {
            buttonPressed = false;
        }

        // Toggle open on button click (press + release while hovered)
        if (wasButtonPressed && !leftDown && hovered) {
            open = !open;
            return false;
        }

        if (!open) return false;

        // --- Dropdown interaction ---
        float dropY = y + h;
        float dropH = items.length * ITEM_HEIGHT;
        float dropW  = w + DROPDOWN_EXTRA_W;

        // Click inside dropdown
        if (leftDown && NVGDrawHelper.isPointInRect(mx, my, x, dropY, dropW, dropH)) {
            int itemIndex = (int) ((my - dropY) / ITEM_HEIGHT);
            itemIndex = Math.max(0, Math.min(items.length - 1, itemIndex));
            // Only commit on mouse-up (wait for release)
            // We detect release: !leftDown is handled below; here leftDown is true
            // so we track which item is being pressed — handled on next frame via
            // the pendingItemPress field.
            pendingItemPress = itemIndex;
            return false;
        }

        // Release — commit pending item selection
        if (!leftDown && pendingItemPress >= 0) {
            int chosen = pendingItemPress;
            pendingItemPress = -1;
            if (NVGDrawHelper.isPointInRect(mx, my, x, dropY, dropW, dropH)) {
                selectedIndex = chosen;
                open = false;
                return true;
            }
        }

        // Click outside → dismiss
        if (leftDown && !NVGDrawHelper.isPointInRect(mx, my, x, y, w, h + dropH)) {
            open = false;
            pendingItemPress = -1;
        }

        return false;
    }

    private int pendingItemPress = -1;

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    private void drawButton(long vg, float x, float y, float w, float h, String label) {
        NVGStyle style = NVGStyle.getInstance();

        var bg     = open     ? style.colorWidgetBgActive
                  : hovered   ? style.colorWidgetBgHovered
                              : style.colorWidgetBg;
        var border = open     ? style.colorBorderActive
                  : hovered   ? style.colorBorderHovered
                              : style.colorBorder;

        NVGDrawHelper.drawRoundedRectWithBorder(vg, x, y, w, h,
                style.cornerRadius, bg, style.borderWidth, border);

        // Label
        nvgFontSize(vg, style.fontSizeDefault);
        nvgFontFace(vg, nl.framegengine.editor.ui.NanoVGContext.DEFAULT_FONT);
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        nvgFillColor(vg, style.colorText);
        nvgText(vg, x + style.paddingX, y + h / 2f, label);

        // Arrow
        float arrowCx = x + w - style.paddingX - 5f;
        float arrowCy = y + h / 2f;
        NVGDrawHelper.drawIconArrowDown(vg, arrowCx, arrowCy, 8f, style.colorText);
    }

    private void drawDropdown(long vg, float x, float dropY, float w, String[] items) {
        NVGStyle style  = NVGStyle.getInstance();
        float dropH     = items.length * ITEM_HEIGHT;
        float dropW     = w + DROPDOWN_EXTRA_W;

        // Dropdown background
        NVGDrawHelper.drawRoundedRectWithBorder(vg, x, dropY, dropW, dropH,
                style.cornerRadius, style.colorWidgetBg, style.borderWidth, style.colorBorder);

        EditorInput.getMousePos(mousePos);
        float my = mousePos[1];

        for (int i = 0; i < items.length; i++) {
            float rowY   = dropY + i * ITEM_HEIGHT;
            boolean rowHovered = NVGDrawHelper.isPointInRect(mousePos[0], my,
                    x, rowY, dropW, ITEM_HEIGHT);

            if (rowHovered || i == selectedIndex) {
                var rowBg = (i == selectedIndex) ? style.colorAccent : style.colorWidgetBgHovered;
                NVGDrawHelper.drawRoundedRect(vg, x + 1, rowY + 1, dropW - 2, ITEM_HEIGHT - 2,
                        style.cornerRadius * 0.5f, rowBg);
            }

            var textColor = (i == selectedIndex) ? style.colorTextOnAccent : style.colorText;
            nvgFontSize(vg, style.fontSizeDefault);
            nvgFontFace(vg, nl.framegengine.editor.ui.NanoVGContext.DEFAULT_FONT);
            nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
            nvgFillColor(vg, textColor);
            nvgText(vg, x + style.paddingX, rowY + ITEM_HEIGHT / 2f, items[i]);
        }
    }
}
