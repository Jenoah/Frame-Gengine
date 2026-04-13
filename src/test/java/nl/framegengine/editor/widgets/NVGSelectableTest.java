package nl.framegengine.editor.widgets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NVGSelectable} state-machine logic.
 *
 * <p>Tests use the package-private {@code updateState} method so no GLFW window
 * or NanoVG context is required.
 */
class NVGSelectableTest {

    private static final float X = 10, Y = 10, W = 100, H = 24;

    private NVGSelectable sel;

    @BeforeEach
    void setUp() {
        sel = new NVGSelectable();
    }

    // -------------------------------------------------------------------------
    // Hover
    // -------------------------------------------------------------------------

    @Test
    void hover_cursorInside_isHoveredTrue() {
        sel.updateState(X, Y, W, H, 50, 20, false);
        assertTrue(sel.isHovered());
    }

    @Test
    void hover_cursorOutside_isHoveredFalse() {
        sel.updateState(X, Y, W, H, 300, 300, false);
        assertFalse(sel.isHovered());
    }

    // -------------------------------------------------------------------------
    // Click
    // -------------------------------------------------------------------------

    @Test
    void click_pressedThenReleasedInside_returnsTrue() {
        sel.updateState(X, Y, W, H, 50, 20, true);
        boolean clicked = sel.updateState(X, Y, W, H, 50, 20, false);
        assertTrue(clicked);
    }

    @Test
    void click_releasedOutside_returnsFalse() {
        sel.updateState(X, Y, W, H, 50, 20, true);          // press inside
        sel.updateState(X, Y, W, H, 300, 300, true);        // drag outside
        boolean clicked = sel.updateState(X, Y, W, H, 300, 300, false);
        assertFalse(clicked);
    }

    @Test
    void click_releaseWithoutPriorPress_returnsFalse() {
        boolean clicked = sel.updateState(X, Y, W, H, 50, 20, false);
        assertFalse(clicked);
    }

    // -------------------------------------------------------------------------
    // Multiple rows — only the row under the cursor fires
    // -------------------------------------------------------------------------

    @Test
    void multipleRows_onlyClickedRowReturnsTrue() {
        NVGSelectable row0 = new NVGSelectable();
        NVGSelectable row1 = new NVGSelectable();
        float row1Y = Y + H;

        row0.updateState(X, Y,     W, H, 50, row1Y + 5, true);
        row1.updateState(X, row1Y, W, H, 50, row1Y + 5, true);

        boolean r0 = row0.updateState(X, Y,     W, H, 50, row1Y + 5, false);
        boolean r1 = row1.updateState(X, row1Y, W, H, 50, row1Y + 5, false);

        assertFalse(r0, "Row 0 must not fire when cursor is on row 1");
        assertTrue(r1,  "Row 1 must fire click");
    }
}
