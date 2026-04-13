package nl.framegengine.editor.widgets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NVGProgressBar} fill-width calculation.
 *
 * <p>{@link NVGProgressBar} is stateless and purely visual; only the
 * package-private {@code computeFillWidth} helper is testable without a
 * NanoVG context.
 */
class NVGProgressBarTest {

    private static final float W = 200f;

    // -------------------------------------------------------------------------
    // Fill width computation
    // -------------------------------------------------------------------------

    @Test
    void fillWidth_zeroProgress_isZero() {
        assertEquals(0f, NVGProgressBar.computeFillWidth(W, 0f), 0.001f);
    }

    @Test
    void fillWidth_fullProgress_equalsBarWidth() {
        assertEquals(W, NVGProgressBar.computeFillWidth(W, 1f), 0.001f);
    }

    @Test
    void fillWidth_halfProgress_isHalfBarWidth() {
        assertEquals(W / 2f, NVGProgressBar.computeFillWidth(W, 0.5f), 0.001f);
    }

    @Test
    void fillWidth_progressAboveOne_clampedToBarWidth() {
        assertEquals(W, NVGProgressBar.computeFillWidth(W, 2f), 0.001f);
    }

    @Test
    void fillWidth_negativeProgress_clampedToZero() {
        assertEquals(0f, NVGProgressBar.computeFillWidth(W, -0.5f), 0.001f);
    }

    @Test
    void fillWidth_quarterProgress_isQuarterBarWidth() {
        assertEquals(W * 0.25f, NVGProgressBar.computeFillWidth(W, 0.25f), 0.001f);
    }
}
