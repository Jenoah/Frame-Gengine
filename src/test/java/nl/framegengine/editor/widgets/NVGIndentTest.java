package nl.framegengine.editor.widgets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NVGIndent} layout cursor logic.
 *
 * <p>No rendering or GLFW context is required — {@link NVGIndent} is pure math.
 */
class NVGIndentTest {

    private NVGIndent indent;

    @BeforeEach
    void setUp() {
        indent = new NVGIndent();
    }

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    void initialLevel_isZero() {
        assertEquals(0, indent.getLevel());
    }

    @Test
    void initialDepth_isZero() {
        assertEquals(0f, indent.depth(), 0.001f);
    }

    // -------------------------------------------------------------------------
    // push / pop
    // -------------------------------------------------------------------------

    @Test
    void push_incrementsLevelByOne() {
        indent.push();
        assertEquals(1, indent.getLevel());
    }

    @Test
    void push_multipleTimes_accumulatesLevel() {
        indent.push();
        indent.push();
        indent.push();
        assertEquals(3, indent.getLevel());
    }

    @Test
    void pop_afterPush_decrementsLevel() {
        indent.push();
        indent.pop();
        assertEquals(0, indent.getLevel());
    }

    @Test
    void pop_atZero_clampedToZero() {
        indent.pop();  // should not go negative
        assertEquals(0, indent.getLevel());
    }

    @Test
    void pop_moreThanPush_clampedToZero() {
        indent.push();
        indent.pop();
        indent.pop();
        indent.pop();
        assertEquals(0, indent.getLevel());
    }

    // -------------------------------------------------------------------------
    // depth
    // -------------------------------------------------------------------------

    @Test
    void depth_twoLevels_equalsDoubleIndentWidth() {
        float step = NVGStyle.getInstance().indentWidth;
        indent.push();
        indent.push();
        assertEquals(step * 2, indent.depth(), 0.001f);
    }

    // -------------------------------------------------------------------------
    // x
    // -------------------------------------------------------------------------

    @Test
    void x_atLevelZero_equalsBaseX() {
        assertEquals(50f, indent.x(50f), 0.001f);
    }

    @Test
    void x_atLevelOne_equalsBaseXPlusOneStep() {
        float step = NVGStyle.getInstance().indentWidth;
        indent.push();
        assertEquals(50f + step, indent.x(50f), 0.001f);
    }

    // -------------------------------------------------------------------------
    // reset
    // -------------------------------------------------------------------------

    @Test
    void reset_afterMultiplePushes_returnsLevelToZero() {
        indent.push();
        indent.push();
        indent.push();
        indent.reset();
        assertEquals(0, indent.getLevel());
        assertEquals(0f, indent.depth(), 0.001f);
    }
}
