package nl.framegengine.editor.widgets;

import static org.lwjgl.nanovg.NanoVG.*;

/**
 * Blocking modal overlay popup with a title bar and close button.
 *
 * <p>When open, the modal draws a semi-transparent dimming overlay over the
 * entire panel area, then renders a centred dialog box on top.  The caller is
 * responsible for rendering child widgets inside the modal's content area
 * (obtained via {@link #getContentX()}, {@link #getContentY()},
 * {@link #getContentW()}, {@link #getContentH()}).
 *
 * <h3>Usage pattern</h3>
 * <pre>{@code
 * private final NVGModal settingsModal = new NVGModal(400, 260);
 * private boolean showSettings = false;
 *
 * // Open trigger (e.g. menu item click):
 * if (showSettings) settingsModal.open();
 *
 * // Inside renderFrame() — render last, after all panel widgets:
 * if (settingsModal.render(vg, panelX, panelY, panelW, panelH, "Game Settings")) {
 *     // modal is open — render content widgets inside content area
 *     nameField.render(vg,
 *         settingsModal.getContentX(), settingsModal.getContentY(), ...);
 * }
 * if (settingsModal.isCloseRequested()) showSettings = false;
 * }</pre>
 *
 * <p>Task 2.6 — Context Menu, Modal, Progress Bar.
 */
public class NVGModal {

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    private final float dialogW;
    private final float dialogH;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private boolean open             = false;
    private boolean closeRequested   = false;
    private boolean closeBtnHovered  = false;
    private boolean closeBtnPressed  = false;

    // Computed each frame from panel bounds
    private float dialogX, dialogY;

    private final float[] mousePos = new float[2];

    // Layout constants
    private static final float TITLE_HEIGHT  = 28f;
    private static final float CLOSE_BTN_SIZE = 18f;
    private static final float DIM_ALPHA      = 160f;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates a modal with a fixed dialog size.
     *
     * @param dialogW dialog width in pixels
     * @param dialogH dialog height in pixels (including title bar)
     */
    public NVGModal(float dialogW, float dialogH) {
        this.dialogW = dialogW;
        this.dialogH = dialogH;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Opens the modal. Clears any pending close request. */
    public void open() {
        open           = true;
        closeRequested = false;
    }

    /** Closes the modal programmatically. */
    public void close() {
        open           = false;
        closeRequested = false;
    }

    /** Returns {@code true} if the modal is currently visible. */
    public boolean isOpen() {
        return open;
    }

    /**
     * Returns {@code true} on the frame the close button was clicked.
     * Resets to {@code false} on the next call to {@link #render}.
     * The caller should react to this by calling {@link #close()} and
     * updating their own state.
     */
    public boolean isCloseRequested() {
        return closeRequested;
    }

    // -------------------------------------------------------------------------
    // Content area accessors (valid after render() returns true)
    // -------------------------------------------------------------------------

    /** Left edge of the modal content area (below the title bar). */
    public float getContentX() { return dialogX + NVGStyle.getInstance().paddingX; }

    /** Top edge of the modal content area (below the title bar). */
    public float getContentY() { return dialogY + TITLE_HEIGHT + NVGStyle.getInstance().paddingY; }

    /** Width of the modal content area. */
    public float getContentW() { return dialogW - NVGStyle.getInstance().paddingX * 2f; }

    /** Height of the modal content area. */
    public float getContentH() {
        return dialogH - TITLE_HEIGHT - NVGStyle.getInstance().paddingY * 2f;
    }

    /**
     * Renders the modal overlay.
     *
     * @param vg      NanoVG context handle
     * @param panelX  left edge of the panel that owns this modal
     * @param panelY  top edge of the panel
     * @param panelW  panel width (used to dim and centre the dialog)
     * @param panelH  panel height
     * @param title   dialog title text
     * @return {@code true} if the modal is open (caller should render content widgets)
     */
    public boolean render(long vg, float panelX, float panelY,
                          float panelW, float panelH, String title) {
        if (!open) return false;

        closeRequested = false;

        // Centre dialog in panel
        dialogX = panelX + (panelW - dialogW) / 2f;
        dialogY = panelY + (panelH - dialogH) / 2f;

        EditorInput.getMousePos(mousePos);
        float mx = mousePos[0];
        float my = mousePos[1];
        boolean leftDown = EditorInput.isLeftDown();

        updateState(panelX, panelY, panelW, panelH, mx, my, leftDown);
        draw(vg, panelX, panelY, panelW, panelH, title);
        return true;
    }

    // -------------------------------------------------------------------------
    // Package-private for testing
    // -------------------------------------------------------------------------

    /**
     * Drives the state machine with injected input values.
     * {@code dialogX}/{@code dialogY} must be set before calling.
     */
    void updateState(float panelX, float panelY, float panelW, float panelH,
                     float mx, float my, boolean leftDown) {
        // Close button hit area
        float btnX = dialogX + dialogW - CLOSE_BTN_SIZE - NVGStyle.getInstance().paddingX;
        float btnY = dialogY + (TITLE_HEIGHT - CLOSE_BTN_SIZE) / 2f;

        boolean wasPressed = closeBtnPressed;
        closeBtnHovered = NVGDrawHelper.isPointInRect(mx, my, btnX, btnY,
                CLOSE_BTN_SIZE, CLOSE_BTN_SIZE);

        if (closeBtnHovered && leftDown) {
            closeBtnPressed = true;
        } else if (!leftDown) {
            closeBtnPressed = false;
        }

        if (wasPressed && !leftDown && closeBtnHovered) {
            closeRequested = true;
            open = false;
        }
    }

    /** Exposes computed dialog position for tests (set during render). */
    void setDialogPosition(float x, float y) {
        this.dialogX = x;
        this.dialogY = y;
    }

    boolean isCloseBtnHovered() { return closeBtnHovered; }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    private void draw(long vg, float panelX, float panelY,
                      float panelW, float panelH, String title) {
        NVGStyle style = NVGStyle.getInstance();

        // Dim overlay
        nvgBeginPath(vg);
        nvgRect(vg, panelX, panelY, panelW, panelH);
        nvgFillColor(vg, org.lwjgl.nanovg.NanoVG.nvgRGBA(
                (byte) 0, (byte) 0, (byte) 0, (byte) (int) DIM_ALPHA,
                org.lwjgl.nanovg.NVGColor.calloc()));
        nvgFill(vg);

        // Dialog box
        NVGDrawHelper.drawRoundedRectWithBorder(vg, dialogX, dialogY, dialogW, dialogH,
                style.cornerRadius, style.colorPanelBg, style.borderWidth, style.colorBorderActive);

        // Title bar background
        NVGDrawHelper.drawRoundedRect(vg, dialogX, dialogY, dialogW, TITLE_HEIGHT,
                style.cornerRadius, style.colorWidgetBgActive);

        // Title text
        nvgFontSize(vg, style.fontSizeHeader);
        nvgFontFace(vg, nl.framegengine.editor.ui.NanoVGContext.DEFAULT_FONT);
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        nvgFillColor(vg, style.colorText);
        nvgText(vg, dialogX + style.paddingX, dialogY + TITLE_HEIGHT / 2f, title);

        // Close button
        float btnX = dialogX + dialogW - CLOSE_BTN_SIZE - style.paddingX;
        float btnY = dialogY + (TITLE_HEIGHT - CLOSE_BTN_SIZE) / 2f;
        var btnBg = closeBtnPressed ? style.colorWidgetBgActive
                  : closeBtnHovered ? style.colorWidgetBgHovered
                                    : style.colorWidgetBg;
        NVGDrawHelper.drawRoundedRect(vg, btnX, btnY, CLOSE_BTN_SIZE, CLOSE_BTN_SIZE,
                style.cornerRadius * 0.5f, btnBg);

        // "×" glyph
        nvgFontSize(vg, style.fontSizeDefault);
        nvgTextAlign(vg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
        nvgFillColor(vg, style.colorText);
        nvgText(vg, btnX + CLOSE_BTN_SIZE / 2f, btnY + CLOSE_BTN_SIZE / 2f, "x");

        // Content area separator
        NVGDrawHelper.drawSeparator(vg, dialogX, dialogY + TITLE_HEIGHT,
                dialogW, style.colorBorder);
    }
}
