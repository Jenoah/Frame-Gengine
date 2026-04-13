package nl.framegengine.editor.widgets;

import org.lwjgl.nanovg.NVGColor;

/**
 * Stateless NanoVG text label widget.
 *
 * <p>Renders a single line of text vertically centred within a bounding box.
 * An optional colour override lets callers supply a custom tint (e.g. for
 * coloured console output) without modifying the theme.
 *
 * <p>This widget has no interactive state — it is purely a drawing utility.
 * No instance state is maintained; all methods are effectively static in
 * behaviour.  An instance is still required so that panel code can hold
 * widget objects uniformly alongside other widget types.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * private final NVGLabel label = new NVGLabel();
 *
 * // Inside renderFrame():
 * label.render(vg, x, y, w, h, "Hello, world!");
 *
 * // With colour override:
 * label.render(vg, x, y, w, h, "Error text", nvgRGBA(255, 80, 80, 255, color));
 * }</pre>
 *
 * <p>Task 2.2 — Button, Label, Separator.
 */
public class NVGLabel {

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Renders a text label using the theme's default text colour.
     *
     * @param vg    NanoVG context handle
     * @param x     left edge of the bounding box
     * @param y     top edge of the bounding box
     * @param w     width of the bounding box (used for clipping)
     * @param h     height of the bounding box (used to centre text vertically)
     * @param text  string to render
     */
    public void render(long vg, float x, float y, float w, float h, String text) {
        render(vg, x, y, w, h, text, null);
    }

    /**
     * Renders a text label with an optional colour override.
     *
     * @param vg           NanoVG context handle
     * @param x            left edge of the bounding box
     * @param y            top edge of the bounding box
     * @param w            width of the bounding box (used for clipping)
     * @param h            height of the bounding box (used to centre text vertically)
     * @param text         string to render
     * @param colorOverride if non-{@code null}, used instead of {@link NVGStyle#colorText}
     */
    public void render(long vg, float x, float y, float w, float h,
                       String text, NVGColor colorOverride) {
        NVGStyle style = NVGStyle.getInstance();
        NVGColor color = colorOverride != null ? colorOverride : style.colorText;

        NVGDrawHelper.drawTextClipped(
                vg,
                x, y, w, h,
                x + style.paddingX, y + h / 2f,
                text, color);
    }
}
