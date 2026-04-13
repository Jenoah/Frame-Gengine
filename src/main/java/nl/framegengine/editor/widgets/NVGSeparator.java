package nl.framegengine.editor.widgets;

/**
 * Stateless NanoVG horizontal separator widget.
 *
 * <p>Draws a single horizontal rule across the full available width, vertically
 * centred within the provided height.  Typically used to visually divide sections
 * inside a panel.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * private final NVGSeparator separator = new NVGSeparator();
 *
 * // Inside renderFrame() — draws a rule at the current cursor row:
 * separator.render(vg, x, y, w);
 * }</pre>
 *
 * <p>Task 2.2 — Button, Label, Separator.
 */
public class NVGSeparator {

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Renders a horizontal separator line using the theme's border colour.
     *
     * <p>The line height is {@link NVGStyle#separatorHeight}.  The total
     * vertical space consumed is {@link NVGStyle#paddingY} * 2 + separatorHeight;
     * callers should advance their layout cursor by that amount after calling
     * this method.
     *
     * @param vg NanoVG context handle
     * @param x  left edge in pixels
     * @param y  top edge of the separator row in pixels
     * @param w  width in pixels
     */
    public void render(long vg, float x, float y, float w) {
        NVGStyle style = NVGStyle.getInstance();
        float midY = y + style.paddingY + style.separatorHeight / 2f;
        NVGDrawHelper.drawSeparator(vg, x, midY, w, style.colorBorder);
    }

    /**
     * Returns the total vertical space (in pixels) consumed by one separator,
     * including padding above and below the line.
     *
     * <p>Use this to advance the layout cursor after calling {@link #render}.
     */
    public static float getHeight() {
        NVGStyle style = NVGStyle.getInstance();
        return style.paddingY * 2f + style.separatorHeight;
    }
}
