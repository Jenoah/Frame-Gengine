package nl.framegengine.editor.widgets;

import static org.lwjgl.nanovg.NanoVG.*;

/**
 * Horizontal progress bar with an optional overlay text label.
 *
 * <p>Stateless — the current progress value is passed in on every call.
 * No interaction; purely visual.
 *
 * <h3>Usage pattern</h3>
 * <pre>{@code
 * private final NVGProgressBar loadBar = new NVGProgressBar();
 *
 * // Inside renderFrame():
 * loadBar.render(vg, x, y, w, h, progress, "Loading assets...");
 * // or without label:
 * loadBar.render(vg, x, y, w, h, progress, null);
 * }</pre>
 *
 * <p>Task 2.6 — Context Menu, Modal, Progress Bar.
 */
public class NVGProgressBar {

    /**
     * Renders the progress bar.
     *
     * @param vg       NanoVG context handle
     * @param x        left edge
     * @param y        top edge
     * @param w        width
     * @param h        height (0 = {@link NVGStyle#widgetHeight})
     * @param progress fraction complete in the range {@code [0.0, 1.0]}; clamped
     * @param label    optional overlay text; {@code null} or empty = no text
     */
    public void render(long vg, float x, float y, float w, float h,
                       float progress, String label) {
        if (h <= 0) h = NVGStyle.getInstance().widgetHeight;
        progress = Math.max(0f, Math.min(1f, progress));

        draw(vg, x, y, w, h, progress, label);
    }

    // -------------------------------------------------------------------------
    // Package-private — exposed for testing the fill-width calculation
    // -------------------------------------------------------------------------

    /**
     * Computes the filled bar width in pixels for the given progress and total width.
     * Extracted so unit tests can verify the math without a GL context.
     */
    static float computeFillWidth(float w, float progress) {
        return w * Math.max(0f, Math.min(1f, progress));
    }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    private void draw(long vg, float x, float y, float w, float h,
                      float progress, String label) {
        NVGStyle style = NVGStyle.getInstance();

        // Track (background)
        NVGDrawHelper.drawRoundedRectWithBorder(vg, x, y, w, h,
                style.cornerRadius, style.colorWidgetBg, style.borderWidth, style.colorBorder);

        // Filled portion
        float fillW = computeFillWidth(w, progress);
        if (fillW > 0f) {
            // Clip fill to inside the border
            float inset = style.borderWidth;
            float innerX = x + inset;
            float innerY = y + inset;
            float innerH = h - inset * 2f;
            float innerFillW = Math.min(fillW - inset, w - inset * 2f);

            if (innerFillW > 0f) {
                nvgSave(vg);
                nvgIntersectScissor(vg, innerX, innerY, w - inset * 2f, innerH);
                NVGDrawHelper.drawRoundedRect(vg, innerX, innerY, innerFillW, innerH,
                        style.cornerRadius * 0.6f, style.colorAccent);
                nvgRestore(vg);
            }
        }

        // Overlay label
        if (label != null && !label.isEmpty()) {
            nvgFontSize(vg, style.fontSizeDefault);
            nvgFontFace(vg, nl.framegengine.editor.ui.NanoVGContext.DEFAULT_FONT);
            nvgTextAlign(vg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
            nvgFillColor(vg, style.colorTextOnAccent);
            nvgText(vg, x + w / 2f, y + h / 2f, label);
        }
    }
}
