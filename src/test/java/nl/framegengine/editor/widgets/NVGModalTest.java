package nl.framegengine.editor.widgets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NVGModal} state-machine logic.
 *
 * <p>Tests use the package-private {@code updateState} and
 * {@code setDialogPosition} methods so no GLFW window or NanoVG context
 * is required.
 */
class NVGModalTest {

    // Dialog dimensions and position used across tests
    private static final float DW = 400, DH = 260;
    private static final float DX = 100, DY = 100; // set via setDialogPosition

    // Close button: btnX = DX + DW - 18 - paddingX(8) = DX+374, btnY = DY+(28-18)/2 = DY+5
    private static final float BTN_X = DX + DW - 18 - 8;  // 474
    private static final float BTN_Y = DY + (28 - 18) / 2f; // 105
    private static final float BTN_SIZE = 18f;

    private NVGModal modal;

    @BeforeEach
    void setUp() {
        modal = new NVGModal(DW, DH);
        modal.open();
        modal.setDialogPosition(DX, DY);
    }

    // -------------------------------------------------------------------------
    // Open / close
    // -------------------------------------------------------------------------

    @Test
    void open_setsIsOpenTrue() {
        assertTrue(modal.isOpen());
    }

    @Test
    void close_setsIsOpenFalse() {
        modal.close();
        assertFalse(modal.isOpen());
    }

    @Test
    void closeRequested_falseInitially() {
        assertFalse(modal.isCloseRequested());
    }

    // -------------------------------------------------------------------------
    // Close button hover
    // -------------------------------------------------------------------------

    @Test
    void hover_cursorOverCloseButton_isCloseBtnHoveredTrue() {
        modal.updateState(0, 0, 600, 400, BTN_X + 5, BTN_Y + 5, false);
        assertTrue(modal.isCloseBtnHovered());
    }

    @Test
    void hover_cursorAwayFromCloseButton_isCloseBtnHoveredFalse() {
        modal.updateState(0, 0, 600, 400, DX + 10, DY + 50, false);
        assertFalse(modal.isCloseBtnHovered());
    }

    // -------------------------------------------------------------------------
    // Close button click
    // -------------------------------------------------------------------------

    @Test
    void closeButton_pressedThenReleased_setsCloseRequestedAndClosesModal() {
        // press
        modal.updateState(0, 0, 600, 400, BTN_X + 5, BTN_Y + 5, true);
        assertFalse(modal.isCloseRequested());

        // release
        modal.updateState(0, 0, 600, 400, BTN_X + 5, BTN_Y + 5, false);
        assertTrue(modal.isCloseRequested());
        assertFalse(modal.isOpen());
    }

    @Test
    void closeButton_releasedOutsideButton_doesNotClose() {
        modal.updateState(0, 0, 600, 400, BTN_X + 5, BTN_Y + 5, true);  // press on btn
        modal.updateState(0, 0, 600, 400, DX + 10, DY + 50, false);      // release elsewhere
        assertFalse(modal.isCloseRequested());
        assertTrue(modal.isOpen());
    }

    @Test
    void closeButton_releaseWithoutPriorPress_doesNotClose() {
        modal.updateState(0, 0, 600, 400, BTN_X + 5, BTN_Y + 5, false);
        assertFalse(modal.isCloseRequested());
        assertTrue(modal.isOpen());
    }

    // -------------------------------------------------------------------------
    // Content area dimensions
    // -------------------------------------------------------------------------

    @Test
    void contentArea_xAndYAccountForTitleBarAndPadding() {
        float padding = NVGStyle.getInstance().paddingX;
        assertEquals(DX + padding, modal.getContentX(), 0.001f);
        assertEquals(DY + 28 + NVGStyle.getInstance().paddingY, modal.getContentY(), 0.001f);
    }

    @Test
    void contentArea_widthAndHeightAreReduced() {
        float padding = NVGStyle.getInstance().paddingX;
        assertEquals(DW - padding * 2f, modal.getContentW(), 0.001f);
        assertTrue(modal.getContentH() < DH);
    }
}
