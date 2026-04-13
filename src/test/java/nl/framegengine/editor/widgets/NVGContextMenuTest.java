package nl.framegengine.editor.widgets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NVGContextMenu} state-machine logic.
 *
 * <p>Tests use the package-private {@code updateState} method so no GLFW window
 * or NanoVG context is required. The popup is positioned at (0, 0) with a
 * fixed width (120) and height derived from item count × 22px + 8px padding.
 */
class NVGContextMenuTest {

    private static final String[] ITEMS = {"Rename", "Duplicate", "Delete"};

    // Menu at (0,0), width=120, height = 4+4 + 3*22 = 74
    private static final float MX = 0, MY = 0, MW = 120, MH = 74;

    private NVGContextMenu menu;

    @BeforeEach
    void setUp() {
        menu = new NVGContextMenu();
        menu.open(MX, MY);
    }

    // -------------------------------------------------------------------------
    // Open / close
    // -------------------------------------------------------------------------

    @Test
    void open_setsIsOpenTrue() {
        assertTrue(menu.isOpen());
    }

    @Test
    void close_setsIsOpenFalse() {
        menu.close();
        assertFalse(menu.isOpen());
    }

    @Test
    void render_whenClosed_returnsMinusOne() {
        menu.close();
        int result = menu.updateState(MX, MY, MW, MH, 50, 20, false, ITEMS);
        assertEquals(-1, result);
    }

    // -------------------------------------------------------------------------
    // Hover
    // -------------------------------------------------------------------------

    @Test
    void hover_cursorOverFirstItem_hoveredRowIsZero() {
        // Item 0 spans y = 4..26 (PADDING_V=4, ITEM_HEIGHT=22)
        menu.updateState(MX, MY, MW, MH, 60, 15, false, ITEMS);
        assertEquals(0, menu.getHoveredRow());
    }

    @Test
    void hover_cursorOverThirdItem_hoveredRowIsTwo() {
        // Item 2 starts at 4 + 2*22 = 48
        menu.updateState(MX, MY, MW, MH, 60, 59, false, ITEMS);
        assertEquals(2, menu.getHoveredRow());
    }

    @Test
    void hover_cursorOutside_hoveredRowIsMinusOne() {
        menu.updateState(MX, MY, MW, MH, 500, 500, false, ITEMS);
        assertEquals(-1, menu.getHoveredRow());
    }

    // -------------------------------------------------------------------------
    // Item selection
    // -------------------------------------------------------------------------

    @Test
    void select_pressAndReleaseOnItem_returnsItemIndex() {
        float itemY = MY + 4 + 1 * 22 + 11; // centre of item 1
        menu.updateState(MX, MY, MW, MH, 60, itemY, true,  ITEMS); // press
        int result = menu.updateState(MX, MY, MW, MH, 60, itemY, false, ITEMS); // release
        assertEquals(1, result);
        assertFalse(menu.isOpen(), "Menu should close after selection");
    }

    @Test
    void select_pressedItemReleasedOutside_returnsMinusOne() {
        float itemY = MY + 4 + 11; // centre of item 0
        menu.updateState(MX, MY, MW, MH, 60, itemY, true, ITEMS); // press item 0
        int result = menu.updateState(MX, MY, MW, MH, 500, 500, false, ITEMS); // release outside
        assertEquals(-1, result);
    }

    // -------------------------------------------------------------------------
    // Dismiss on click outside
    // -------------------------------------------------------------------------

    @Test
    void clickOutside_dismissesMenu() {
        menu.updateState(MX, MY, MW, MH, 500, 500, true, ITEMS);
        assertFalse(menu.isOpen());
    }

    @Test
    void clickInside_doesNotDismiss() {
        menu.updateState(MX, MY, MW, MH, 60, 15, true, ITEMS);
        assertTrue(menu.isOpen());
    }
}
