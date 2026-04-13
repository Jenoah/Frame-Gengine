package nl.framegengine.editor.widgets;

import org.lwjgl.nanovg.NVGColor;

import static org.lwjgl.nanovg.NanoVG.nvgRGBA;

/**
 * Central theme for all NanoVG editor widgets.
 *
 * <p>Stores all colours, font sizes, padding and geometry constants used by
 * the widget library.  Values are grouped by widget state:
 * <ul>
 *   <li><b>normal</b>   — default, unfocused, uninteracted state.</li>
 *   <li><b>hovered</b>  — cursor is over the widget.</li>
 *   <li><b>active</b>   — widget is being pressed / is focused.</li>
 *   <li><b>disabled</b> — widget is non-interactive.</li>
 * </ul>
 *
 * <p>All {@link NVGColor} instances are allocated once at construction time and
 * reused — never allocate per-frame colours directly in widget code; call
 * {@link #getInstance()} and use the pre-built fields instead.
 *
 * <p>Task 2.1 — Core Drawing Primitives &amp; Theme.
 */
public final class NVGStyle {

    // -------------------------------------------------------------------------
    // Singleton
    // -------------------------------------------------------------------------

    private static NVGStyle instance;

    /** Returns the shared singleton, creating it on first call. */
    public static NVGStyle getInstance() {
        if (instance == null) {
            instance = new NVGStyle();
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // Geometry constants
    // -------------------------------------------------------------------------

    /** Corner radius for buttons, inputs, and headers (px). */
    public final float cornerRadius = 4f;

    /** Default widget height (px). */
    public final float widgetHeight = 24f;

    /** Horizontal padding inside a widget (px). */
    public final float paddingX = 8f;

    /** Vertical padding inside a widget (px). */
    public final float paddingY = 4f;

    /** Thickness of widget border strokes (px). */
    public final float borderWidth = 1f;

    /** Height of a separator line (px). */
    public final float separatorHeight = 1f;

    /** Indent step width (px) — used by NVGIndent/NVGUnindent. */
    public final float indentWidth = 16f;

    // -------------------------------------------------------------------------
    // Typography
    // -------------------------------------------------------------------------

    /** Font size for standard widget labels (px). */
    public final float fontSizeDefault = 13f;

    /** Font size for panel titles / collapsing headers (px). */
    public final float fontSizeHeader = 14f;

    /** Font size for small / secondary text (px). */
    public final float fontSizeSmall = 11f;

    // -------------------------------------------------------------------------
    // Colours — background fills
    // -------------------------------------------------------------------------

    /** Panel / window background. */
    public final NVGColor colorPanelBg;

    /** Widget background in normal state. */
    public final NVGColor colorWidgetBg;

    /** Widget background when hovered. */
    public final NVGColor colorWidgetBgHovered;

    /** Widget background when active (pressed / focused). */
    public final NVGColor colorWidgetBgActive;

    /** Widget background when disabled. */
    public final NVGColor colorWidgetBgDisabled;

    // -------------------------------------------------------------------------
    // Colours — borders
    // -------------------------------------------------------------------------

    /** Widget border in normal state. */
    public final NVGColor colorBorder;

    /** Widget border when hovered. */
    public final NVGColor colorBorderHovered;

    /** Widget border when active. */
    public final NVGColor colorBorderActive;

    // -------------------------------------------------------------------------
    // Colours — text
    // -------------------------------------------------------------------------

    /** Primary text colour. */
    public final NVGColor colorText;

    /** Secondary / dimmed text (hints, disabled labels). */
    public final NVGColor colorTextDisabled;

    /** Text rendered on an accent-coloured (active) background. */
    public final NVGColor colorTextOnAccent;

    // -------------------------------------------------------------------------
    // Colours — accent / interactive
    // -------------------------------------------------------------------------

    /** Accent fill for selected, active, or toggle-on states. */
    public final NVGColor colorAccent;

    /** Accent fill when hovered. */
    public final NVGColor colorAccentHovered;

    /** Highlighted / selected row background (e.g. NVGSelectable). */
    public final NVGColor colorSelectionBg;

    // -------------------------------------------------------------------------
    // Colours — scroll bar
    // -------------------------------------------------------------------------

    /** Scrollbar track background. */
    public final NVGColor colorScrollbarTrack;

    /** Scrollbar thumb in normal state. */
    public final NVGColor colorScrollbarThumb;

    /** Scrollbar thumb when hovered. */
    public final NVGColor colorScrollbarThumbHovered;

    // -------------------------------------------------------------------------
    // Colours — semantic text (console output)
    // -------------------------------------------------------------------------

    /** Log text: info / default. */
    public final NVGColor colorLogInfo;

    /** Log text: warning. */
    public final NVGColor colorLogWarning;

    /** Log text: error. */
    public final NVGColor colorLogError;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    private NVGStyle() {
        // Panel / window
        colorPanelBg            = rgba(30,  30,  30,  255);

        // Widget backgrounds
        colorWidgetBg           = rgba(50,  50,  50,  255);
        colorWidgetBgHovered    = rgba(65,  65,  65,  255);
        colorWidgetBgActive     = rgba(80,  80,  80,  255);
        colorWidgetBgDisabled   = rgba(40,  40,  40,  180);

        // Borders
        colorBorder             = rgba(70,  70,  70,  255);
        colorBorderHovered      = rgba(120, 120, 120, 255);
        colorBorderActive       = rgba(100, 140, 200, 255);

        // Text
        colorText               = rgba(220, 220, 220, 255);
        colorTextDisabled       = rgba(120, 120, 120, 255);
        colorTextOnAccent       = rgba(255, 255, 255, 255);

        // Accent
        colorAccent             = rgba(66,  133, 244, 255);
        colorAccentHovered      = rgba(90,  155, 255, 255);
        colorSelectionBg        = rgba(66,  133, 244,  80);

        // Scrollbar
        colorScrollbarTrack     = rgba(35,  35,  35,  200);
        colorScrollbarThumb     = rgba(80,  80,  80,  200);
        colorScrollbarThumbHovered = rgba(110, 110, 110, 220);

        // Semantic log colours
        colorLogInfo            = rgba(220, 220, 220, 255);
        colorLogWarning         = rgba(255, 200,  60, 255);
        colorLogError           = rgba(255,  80,  80, 255);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /**
     * Allocates a new {@link NVGColor} with the given RGBA byte components.
     * Each component is in the range [0, 255].
     */
    private static NVGColor rgba(int r, int g, int b, int a) {
        return nvgRGBA((byte) r, (byte) g, (byte) b, (byte) a, NVGColor.calloc());
    }
}
