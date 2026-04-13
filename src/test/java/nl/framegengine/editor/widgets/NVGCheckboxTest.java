package nl.framegengine.editor.widgets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NVGCheckbox} state-machine logic.
 *
 * <p>Tests use the package-private {@code updateState} method so no GLFW window
 * or NanoVG context is required.
 */
class NVGCheckboxTest {

    private static final float X = 10, Y = 10, W = 100, H = 24;

    private NVGCheckbox box;

    @BeforeEach
    void setUp() {
        box = new NVGCheckbox();
    }

    // -------------------------------------------------------------------------
    // Hover
    // -------------------------------------------------------------------------

    @Test
    void hover_cursorInside_isHoveredTrue() {
        box.updateState(X, Y, W, H, 50, 20, false);
        assertTrue(box.isHovered());
    }

    @Test
    void hover_cursorOutside_isHoveredFalse() {
        box.updateState(X, Y, W, H, 200, 200, false);
        assertFalse(box.isHovered());
    }

    // -------------------------------------------------------------------------
    // Toggle (click)
    // -------------------------------------------------------------------------

    @Test
    void click_pressedThenReleasedInside_returnsTrue() {
        boolean t1 = box.updateState(X, Y, W, H, 50, 20, true);
        assertFalse(t1, "No toggle on press-down frame");

        boolean t2 = box.updateState(X, Y, W, H, 50, 20, false);
        assertTrue(t2, "Toggle should fire on release while hovered");
    }

    @Test
    void click_releasedOutside_returnsFalse() {
        box.updateState(X, Y, W, H, 50, 20, true);          // press inside
        box.updateState(X, Y, W, H, 200, 200, true);        // drag outside
        boolean t = box.updateState(X, Y, W, H, 200, 200, false); // release outside
        assertFalse(t);
    }

    @Test
    void click_releaseWithoutPriorPress_returnsFalse() {
        boolean t = box.updateState(X, Y, W, H, 50, 20, false);
        assertFalse(t);
    }
}
