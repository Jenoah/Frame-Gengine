package nl.framegengine.editor.widgets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NVGCollapsingHeader} state-machine logic.
 *
 * <p>Tests use the package-private {@code updateState} method so no GLFW window
 * or NanoVG context is required.
 */
class NVGCollapsingHeaderTest {

    private static final float X = 10, Y = 10, W = 200, H = 24;

    private NVGCollapsingHeader header;

    @BeforeEach
    void setUp() {
        header = new NVGCollapsingHeader();
    }

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    void defaultConstructor_startsOpen() {
        assertTrue(header.isOpen());
    }

    @Test
    void constructor_closedInitially_startsClose() {
        assertFalse(new NVGCollapsingHeader(false).isOpen());
    }

    // -------------------------------------------------------------------------
    // Hover
    // -------------------------------------------------------------------------

    @Test
    void hover_cursorInside_isHoveredTrue() {
        header.updateState(X, Y, W, H, 50, 20, false);
        assertTrue(header.isHovered());
    }

    @Test
    void hover_cursorOutside_isHoveredFalse() {
        header.updateState(X, Y, W, H, 300, 300, false);
        assertFalse(header.isHovered());
    }

    // -------------------------------------------------------------------------
    // Toggle (click)
    // -------------------------------------------------------------------------

    @Test
    void click_pressedThenReleased_togglesFromOpenToClosed() {
        assertTrue(header.isOpen());

        header.updateState(X, Y, W, H, 50, 20, true);
        boolean toggled = header.updateState(X, Y, W, H, 50, 20, false);

        assertTrue(toggled, "Toggle should fire on release while hovered");
        // State is managed by render(), not updateState() — updateState just returns the event.
        // Simulate render() toggle:
        if (toggled) header.setOpen(!header.isOpen());
        assertFalse(header.isOpen());
    }

    @Test
    void click_pressedThenReleased_togglesFromClosedToOpen() {
        header.setOpen(false);
        header.updateState(X, Y, W, H, 50, 20, true);
        boolean toggled = header.updateState(X, Y, W, H, 50, 20, false);

        assertTrue(toggled);
        if (toggled) header.setOpen(!header.isOpen());
        assertTrue(header.isOpen());
    }

    @Test
    void click_releasedOutside_doesNotToggle() {
        header.updateState(X, Y, W, H, 50, 20, true);          // press inside
        header.updateState(X, Y, W, H, 300, 300, true);        // drag outside
        boolean toggled = header.updateState(X, Y, W, H, 300, 300, false);
        assertFalse(toggled);
    }

    @Test
    void click_releaseWithoutPriorPress_doesNotToggle() {
        boolean toggled = header.updateState(X, Y, W, H, 50, 20, false);
        assertFalse(toggled);
    }

    // -------------------------------------------------------------------------
    // setOpen
    // -------------------------------------------------------------------------

    @Test
    void setOpen_false_closesHeader() {
        header.setOpen(false);
        assertFalse(header.isOpen());
    }

    @Test
    void setOpen_true_opensHeader() {
        header.setOpen(false);
        header.setOpen(true);
        assertTrue(header.isOpen());
    }
}
