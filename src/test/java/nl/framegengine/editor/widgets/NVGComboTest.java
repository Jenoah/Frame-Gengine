package nl.framegengine.editor.widgets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NVGCombo} state-machine logic.
 *
 * <p>Tests use the package-private {@code updateState} method so no GLFW window
 * or NanoVG context is required.
 */
class NVGComboTest {

    private static final float X = 10, Y = 10, W = 100, H = 24;
    private static final String[] ITEMS = {"Alpha", "Beta", "Gamma"};

    private NVGCombo combo;

    @BeforeEach
    void setUp() {
        combo = new NVGCombo();
    }

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    void initialState_closedAndSelectedZero() {
        assertFalse(combo.isOpen());
        assertEquals(0, combo.getSelectedIndex());
    }

    // -------------------------------------------------------------------------
    // Open / close via button
    // -------------------------------------------------------------------------

    @Test
    void buttonClick_opensDropdown() {
        combo.updateState(X, Y, W, H, 50, 20, true,  ITEMS);
        combo.updateState(X, Y, W, H, 50, 20, false, ITEMS);
        assertTrue(combo.isOpen());
    }

    @Test
    void buttonClickAgain_closesDropdown() {
        combo.updateState(X, Y, W, H, 50, 20, true,  ITEMS);
        combo.updateState(X, Y, W, H, 50, 20, false, ITEMS); // open
        combo.updateState(X, Y, W, H, 50, 20, true,  ITEMS);
        combo.updateState(X, Y, W, H, 50, 20, false, ITEMS); // close
        assertFalse(combo.isOpen());
    }

    @Test
    void close_whileOpen_closesDropdown() {
        combo.updateState(X, Y, W, H, 50, 20, true,  ITEMS);
        combo.updateState(X, Y, W, H, 50, 20, false, ITEMS);
        assertTrue(combo.isOpen());
        combo.close();
        assertFalse(combo.isOpen());
    }

    // -------------------------------------------------------------------------
    // Dismiss by clicking outside
    // -------------------------------------------------------------------------

    @Test
    void clickOutside_whileOpen_closesDropdown() {
        combo.updateState(X, Y, W, H, 50, 20, true,  ITEMS);
        combo.updateState(X, Y, W, H, 50, 20, false, ITEMS); // open
        combo.updateState(X, Y, W, H, 500, 500, true, ITEMS);
        assertFalse(combo.isOpen());
    }

    // -------------------------------------------------------------------------
    // Item selection
    // -------------------------------------------------------------------------

    @Test
    void selectItem_pressAndReleaseOnItem_updatesSelectedIndexAndCloses() {
        // Open the dropdown
        combo.updateState(X, Y, W, H, 50, 20, true,  ITEMS);
        combo.updateState(X, Y, W, H, 50, 20, false, ITEMS);
        assertTrue(combo.isOpen());

        // Items start at Y+H = 34, each row 22px tall.
        // Item index 1 (Beta) row starts at 34+22=56; centre = 56+11 = 67.
        float itemY = Y + H + 22 + 11f;
        boolean changed;

        changed = combo.updateState(X, Y, W, H, 50, itemY, true,  ITEMS); // press
        assertFalse(changed, "No selection on press-down frame");

        changed = combo.updateState(X, Y, W, H, 50, itemY, false, ITEMS); // release
        assertTrue(changed, "Selection should commit on release");
        assertEquals(1, combo.getSelectedIndex());
        assertFalse(combo.isOpen());
    }

    @Test
    void selectItem_noChangeWhenReleasedOutsideDropdown() {
        combo.updateState(X, Y, W, H, 50, 20, true,  ITEMS);
        combo.updateState(X, Y, W, H, 50, 20, false, ITEMS); // open

        float itemY = Y + H + 11f; // inside first item
        combo.updateState(X, Y, W, H, 50, itemY, true, ITEMS); // press item 0

        // Move cursor outside before releasing
        boolean changed = combo.updateState(X, Y, W, H, 500, 500, false, ITEMS);
        assertFalse(changed, "Must not select when released outside dropdown");
    }
}
