package nl.framegengine.editor.widgets;

import nl.framegengine.editor.ui.NanoVGContext;
import org.lwjgl.BufferUtils;
import org.lwjgl.nanovg.NVGColor;

import java.nio.FloatBuffer;

import static org.lwjgl.nanovg.NanoVG.*;

/**
 * Low-level NanoVG drawing utilities used by all editor widgets.
 *
 * <p>All methods are <em>static</em> and operate on a raw NanoVG context handle
 * ({@code long vg}) obtained from {@link NanoVGContext#getHandle()}.  They do
 * <em>not</em> call {@code nvgBeginFrame} or {@code nvgEndFrame} — callers are
 * responsible for the frame lifecycle.
 *
 * <p>Methods follow a consistent pattern:
 * <ol>
 *   <li>Accept a {@code long vg} as the first argument.</li>
 *   <li>Use {@link NVGStyle} constants for default sizes; accept explicit
 *       overrides where flexibility is needed.</li>
 *   <li>Leave the NanoVG state clean after returning (save/restore where needed).</li>
 * </ol>
 *
 * <p>Task 2.1 — Core Drawing Primitives &amp; Theme.
 */
public final class NVGDrawHelper {

    /** Reusable float buffer for {@code nvgTextBounds} — not thread-safe. */
    private static final FloatBuffer TEXT_BOUNDS_BUF = BufferUtils.createFloatBuffer(4);

    private NVGDrawHelper() {}

    // -------------------------------------------------------------------------
    // Rounded rectangles
    // -------------------------------------------------------------------------

    /**
     * Draws a filled rounded rectangle using {@link NVGStyle#cornerRadius}.
     *
     * @param vg     NanoVG context handle
     * @param x      left edge
     * @param y      top edge
     * @param w      width
     * @param h      height
     * @param color  fill colour
     */
    public static void drawRoundedRect(long vg, float x, float y, float w, float h, NVGColor color) {
        drawRoundedRect(vg, x, y, w, h, NVGStyle.getInstance().cornerRadius, color);
    }

    /**
     * Draws a filled rounded rectangle with an explicit corner radius.
     *
     * @param vg     NanoVG context handle
     * @param x      left edge
     * @param y      top edge
     * @param w      width
     * @param h      height
     * @param radius corner radius in pixels
     * @param color  fill colour
     */
    public static void drawRoundedRect(long vg, float x, float y, float w, float h,
                                       float radius, NVGColor color) {
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, w, h, radius);
        nvgFillColor(vg, color);
        nvgFill(vg);
    }

    /**
     * Draws a rounded rectangle border (stroke only, no fill).
     *
     * @param vg          NanoVG context handle
     * @param x           left edge
     * @param y           top edge
     * @param w           width
     * @param h           height
     * @param radius      corner radius in pixels
     * @param strokeWidth stroke thickness in pixels
     * @param color       stroke colour
     */
    public static void drawRoundedRectBorder(long vg, float x, float y, float w, float h,
                                             float radius, float strokeWidth, NVGColor color) {
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, w, h, radius);
        nvgStrokeColor(vg, color);
        nvgStrokeWidth(vg, strokeWidth);
        nvgStroke(vg);
    }

    /**
     * Draws a filled rounded rectangle and then strokes its border.
     * Combines {@link #drawRoundedRect} and {@link #drawRoundedRectBorder} in a
     * single begin/path sequence for efficiency.
     *
     * @param vg          NanoVG context handle
     * @param x           left edge
     * @param y           top edge
     * @param w           width
     * @param h           height
     * @param radius      corner radius in pixels
     * @param fill        fill colour
     * @param strokeWidth stroke thickness in pixels
     * @param stroke      stroke colour
     */
    public static void drawRoundedRectWithBorder(long vg, float x, float y, float w, float h,
                                                 float radius, NVGColor fill,
                                                 float strokeWidth, NVGColor stroke) {
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, w, h, radius);
        nvgFillColor(vg, fill);
        nvgFill(vg);
        nvgStrokeColor(vg, stroke);
        nvgStrokeWidth(vg, strokeWidth);
        nvgStroke(vg);
    }

    // -------------------------------------------------------------------------
    // Text
    // -------------------------------------------------------------------------

    /**
     * Draws a single line of text, left-aligned, baseline at {@code (x, y + ascender)}.
     *
     * <p>Font face, size, and alignment must be set by the caller before invoking this
     * method, or use {@link #drawText(long, float, float, float, String, NVGColor)} which
     * sets size automatically.
     *
     * @param vg    NanoVG context handle
     * @param x     left origin
     * @param y     top origin (text is drawn at {@code y + fontSize * 0.75} baseline)
     * @param text  string to render
     * @param color text colour
     */
    public static void drawText(long vg, float x, float y, String text, NVGColor color) {
        nvgFillColor(vg, color);
        nvgText(vg, x, y, text);
    }

    /**
     * Draws a single line of text at the given size, left+middle aligned relative to
     * the provided bounding box top-left.  The text baseline is vertically centred
     * within the widget height {@code h}.
     *
     * @param vg       NanoVG context handle
     * @param x        left edge of the widget
     * @param y        top edge of the widget
     * @param h        widget height (used to centre text vertically)
     * @param text     string to render
     * @param color    text colour
     */
    public static void drawText(long vg, float x, float y, float h, String text, NVGColor color) {
        NVGStyle style = NVGStyle.getInstance();
        nvgFontSize(vg, style.fontSizeDefault);
        nvgFontFace(vg, NanoVGContext.DEFAULT_FONT);
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        nvgFillColor(vg, color);
        nvgText(vg, x + style.paddingX, y + h / 2f, text);
    }

    /**
     * Draws text clipped to a rectangular region using NanoVG scissor.
     *
     * <p>The existing scissor state is saved and restored around the clip region so
     * this can be nested safely inside a scroll region.
     *
     * @param vg       NanoVG context handle
     * @param x        clip region left edge
     * @param y        clip region top edge
     * @param w        clip region width
     * @param h        clip region height
     * @param textX    text draw origin X
     * @param textY    text draw origin Y (baseline-centred within {@code h})
     * @param text     string to render
     * @param color    text colour
     */
    public static void drawTextClipped(long vg, float x, float y, float w, float h,
                                       float textX, float textY, String text, NVGColor color) {
        nvgSave(vg);
        nvgIntersectScissor(vg, x, y, w, h);
        NVGStyle style = NVGStyle.getInstance();
        nvgFontSize(vg, style.fontSizeDefault);
        nvgFontFace(vg, NanoVGContext.DEFAULT_FONT);
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        nvgFillColor(vg, color);
        nvgText(vg, textX, textY, text);
        nvgRestore(vg);
    }

    // -------------------------------------------------------------------------
    // Separators
    // -------------------------------------------------------------------------

    /**
     * Draws a full-width horizontal separator line.
     *
     * @param vg    NanoVG context handle
     * @param x     left edge
     * @param y     vertical centre of the line
     * @param w     width
     * @param color line colour
     */
    public static void drawSeparator(long vg, float x, float y, float w, NVGColor color) {
        nvgBeginPath(vg);
        nvgMoveTo(vg, x, y);
        nvgLineTo(vg, x + w, y);
        nvgStrokeColor(vg, color);
        nvgStrokeWidth(vg, NVGStyle.getInstance().separatorHeight);
        nvgStroke(vg);
    }

    // -------------------------------------------------------------------------
    // Icon stubs
    // -------------------------------------------------------------------------

    /**
     * Icon rendering stub — draws a small placeholder glyph for an arrow pointing right.
     *
     * <p>This will be replaced with proper icon rendering (glyph atlas or SVG) in a
     * later task.  The stub ensures widget layout code can call icon methods now and
     * have them produce visible output during development.
     *
     * @param vg    NanoVG context handle
     * @param cx    centre X
     * @param cy    centre Y
     * @param size  icon size (width and height)
     * @param color icon colour
     */
    public static void drawIconArrowRight(long vg, float cx, float cy, float size, NVGColor color) {
        float half = size * 0.35f;
        nvgBeginPath(vg);
        nvgMoveTo(vg, cx - half, cy - half);
        nvgLineTo(vg, cx + half, cy);
        nvgLineTo(vg, cx - half, cy + half);
        nvgClosePath(vg);
        nvgFillColor(vg, color);
        nvgFill(vg);
    }

    /**
     * Icon rendering stub — draws a small placeholder glyph for an arrow pointing down.
     *
     * @param vg    NanoVG context handle
     * @param cx    centre X
     * @param cy    centre Y
     * @param size  icon size
     * @param color icon colour
     */
    public static void drawIconArrowDown(long vg, float cx, float cy, float size, NVGColor color) {
        float half = size * 0.35f;
        nvgBeginPath(vg);
        nvgMoveTo(vg, cx - half, cy - half);
        nvgLineTo(vg, cx + half, cy - half);
        nvgLineTo(vg, cx, cy + half);
        nvgClosePath(vg);
        nvgFillColor(vg, color);
        nvgFill(vg);
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    /**
     * Measures the pixel width of a string at the default font size.
     *
     * <p>Requires a valid NanoVG frame to be active (called between
     * {@code nvgBeginFrame} and {@code nvgEndFrame}).
     *
     * @param vg   NanoVG context handle
     * @param text string to measure
     * @return advance width in pixels
     */
    public static float measureTextWidth(long vg, String text) {
        NVGStyle style = NVGStyle.getInstance();
        nvgFontSize(vg, style.fontSizeDefault);
        nvgFontFace(vg, NanoVGContext.DEFAULT_FONT);
        TEXT_BOUNDS_BUF.clear();
        return nvgTextBounds(vg, 0, 0, text, TEXT_BOUNDS_BUF);
    }

    /**
     * Returns {@code true} if the point {@code (px, py)} is inside the axis-aligned
     * rectangle defined by {@code (x, y, w, h)}.
     *
     * <p>Convenience helper used by widget hit-testing — keeps the
     * {@code x + w} / {@code y + h} arithmetic out of individual widget classes.
     *
     * @param px point X
     * @param py point Y
     * @param x  rect left
     * @param y  rect top
     * @param w  rect width
     * @param h  rect height
     * @return {@code true} if the point is inside (inclusive of edges)
     */
    public static boolean isPointInRect(float px, float py, float x, float y, float w, float h) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }
}
