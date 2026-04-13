package nl.framegengine.editor.widgets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NVGDrawHelper} pure-logic methods.
 *
 * <p>No GLFW or NanoVG context is required — only methods that perform
 * arithmetic without touching GL state are covered here.
 */
class NVGDrawHelperTest {

    // -------------------------------------------------------------------------
    // isPointInRect
    // -------------------------------------------------------------------------

    @Test
    void isPointInRect_insideCenter_returnsTrue() {
        assertTrue(NVGDrawHelper.isPointInRect(50, 50, 10, 10, 80, 80));
    }

    @Test
    void isPointInRect_onLeftEdge_returnsTrue() {
        assertTrue(NVGDrawHelper.isPointInRect(10, 50, 10, 10, 80, 80));
    }

    @Test
    void isPointInRect_onRightEdge_returnsTrue() {
        assertTrue(NVGDrawHelper.isPointInRect(90, 50, 10, 10, 80, 80));
    }

    @Test
    void isPointInRect_onTopEdge_returnsTrue() {
        assertTrue(NVGDrawHelper.isPointInRect(50, 10, 10, 10, 80, 80));
    }

    @Test
    void isPointInRect_onBottomEdge_returnsTrue() {
        assertTrue(NVGDrawHelper.isPointInRect(50, 90, 10, 10, 80, 80));
    }

    @Test
    void isPointInRect_outsideLeft_returnsFalse() {
        assertFalse(NVGDrawHelper.isPointInRect(9, 50, 10, 10, 80, 80));
    }

    @Test
    void isPointInRect_outsideRight_returnsFalse() {
        assertFalse(NVGDrawHelper.isPointInRect(91, 50, 10, 10, 80, 80));
    }

    @Test
    void isPointInRect_outsideAbove_returnsFalse() {
        assertFalse(NVGDrawHelper.isPointInRect(50, 9, 10, 10, 80, 80));
    }

    @Test
    void isPointInRect_outsideBelow_returnsFalse() {
        assertFalse(NVGDrawHelper.isPointInRect(50, 91, 10, 10, 80, 80));
    }

    @Test
    void isPointInRect_zeroSizeRect_onlyOriginMatches() {
        assertTrue(NVGDrawHelper.isPointInRect(10, 20, 10, 20, 0, 0));
        assertFalse(NVGDrawHelper.isPointInRect(11, 20, 10, 20, 0, 0));
    }
}
