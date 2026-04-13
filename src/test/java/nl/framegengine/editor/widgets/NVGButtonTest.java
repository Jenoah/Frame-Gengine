package nl.framegengine.editor.widgets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NVGButton} state-machine logic.
 *
 * <p>Because {@link NVGButton#render} issues NanoVG draw calls and queries
 * {@link EditorInput} (which requires a live GLFW window), tests here use a
 * {@link TestableButton} subclass that overrides the input-query and draw steps
 * so no GL context is needed.
 */
class NVGButtonTest {

    // -------------------------------------------------------------------------
    // Test double — injects controlled mouse position and button state
    // -------------------------------------------------------------------------

    /**
     * Subclass of {@link NVGButton} that exposes the state-machine logic without
     * requiring a NanoVG context or GLFW window.
     */
    private static class TestableButton extends NVGButton {

        float injectedMx = 0;
        float injectedMy = 0;
        boolean injectedLeftDown = false;

        /**
         * Drives the state machine with injected input values.
         * Returns {@code true} if a click is registered.
         */
        boolean tick(float bx, float by, float bw, float bh) {
            return updateState(bx, by, bw, bh, injectedMx, injectedMy, injectedLeftDown);
        }
    }

    private TestableButton btn;

    @BeforeEach
    void setUp() {
        btn = new TestableButton();
    }

    // -------------------------------------------------------------------------
    // Hover
    // -------------------------------------------------------------------------

    @Test
    void hover_cursorInside_isHoveredTrue() {
        btn.injectedMx = 50;
        btn.injectedMy = 50;
        btn.tick(10, 10, 80, 80);
        assertTrue(btn.isHovered());
    }

    @Test
    void hover_cursorOutside_isHoveredFalse() {
        btn.injectedMx = 200;
        btn.injectedMy = 200;
        btn.tick(10, 10, 80, 80);
        assertFalse(btn.isHovered());
    }

    // -------------------------------------------------------------------------
    // Press
    // -------------------------------------------------------------------------

    @Test
    void press_cursorInsideAndLeftDown_isPressedTrue() {
        btn.injectedMx = 50;
        btn.injectedMy = 50;
        btn.injectedLeftDown = true;
        btn.tick(10, 10, 80, 80);
        assertTrue(btn.isPressed());
    }

    @Test
    void press_cursorOutsideAndLeftDown_isPressedFalse() {
        btn.injectedMx = 200;
        btn.injectedMy = 200;
        btn.injectedLeftDown = true;
        btn.tick(10, 10, 80, 80);
        assertFalse(btn.isPressed());
    }

    // -------------------------------------------------------------------------
    // Click detection
    // -------------------------------------------------------------------------

    @Test
    void click_pressedThenReleasedWhileHovered_returnsTrue() {
        // Frame 1: hover + press
        btn.injectedMx = 50;
        btn.injectedMy = 50;
        btn.injectedLeftDown = true;
        boolean c1 = btn.tick(10, 10, 80, 80);
        assertFalse(c1, "No click on press-down frame");

        // Frame 2: hover + release
        btn.injectedLeftDown = false;
        boolean c2 = btn.tick(10, 10, 80, 80);
        assertTrue(c2, "Click should fire on release while hovered");
    }

    @Test
    void click_pressedThenMovedOutsideThenReleased_returnsFalse() {
        // Frame 1: hover + press
        btn.injectedMx = 50;
        btn.injectedMy = 50;
        btn.injectedLeftDown = true;
        btn.tick(10, 10, 80, 80);

        // Frame 2: moved outside, still pressed
        btn.injectedMx = 200;
        btn.injectedMy = 200;
        btn.tick(10, 10, 80, 80);

        // Frame 3: released outside
        btn.injectedLeftDown = false;
        boolean clicked = btn.tick(10, 10, 80, 80);
        assertFalse(clicked, "Click must not fire when released outside");
    }

    @Test
    void click_releaseWithoutPriorPress_returnsFalse() {
        btn.injectedMx = 50;
        btn.injectedMy = 50;
        btn.injectedLeftDown = false;
        boolean clicked = btn.tick(10, 10, 80, 80);
        assertFalse(clicked, "Release without prior press must not fire click");
    }
}
