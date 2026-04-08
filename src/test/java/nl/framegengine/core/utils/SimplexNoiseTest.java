package nl.framegengine.core.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimplexNoiseTest {

    private int originalSeed;

    @BeforeEach
    void saveSeed() {
        originalSeed = SimplexNoise.RANDOMSEED;
    }

    @AfterEach
    void restoreSeed() {
        SimplexNoise.RANDOMSEED = originalSeed;
    }

    // ======================================================================
    // Same seed → same output
    // ======================================================================

    @Test
    void sameSeed_sameOutputForGivenInputs() {
        SimplexNoise.RANDOMSEED = 7;
        float first = new SimplexNoise().noise(1.5f, 2.5f);

        SimplexNoise.RANDOMSEED = 7;
        float second = new SimplexNoise().noise(1.5f, 2.5f);

        assertEquals(first, second, 0f,
                "same seed must produce identical noise values");
    }

    // ======================================================================
    // Different seeds → different output
    // ======================================================================

    @Test
    void differentSeeds_differentOutputForSameInputs() {
        SimplexNoise.RANDOMSEED = 0;
        float withSeedZero = new SimplexNoise().noise(3.0f, 4.0f);

        SimplexNoise.RANDOMSEED = 42;
        float withSeed42 = new SimplexNoise().noise(3.0f, 4.0f);

        assertNotEquals(withSeedZero, withSeed42,
                "different seeds should produce different noise values");
    }

    // ======================================================================
    // Output range [-1, 1]
    // ======================================================================

    @Test
    void noise_outputWithinRangeMinusOneToOne() {
        SimplexNoise.RANDOMSEED = 0;
        SimplexNoise sn = new SimplexNoise();

        float[] xs = {-10f, -1f, 0f, 0.5f, 1f, 3.7f, 10f};
        float[] ys = {-10f, -1f, 0f, 0.5f, 1f, 3.7f, 10f};

        for (float x : xs) {
            for (float y : ys) {
                float value = sn.noise(x, y);
                assertTrue(value >= -1f && value <= 1f,
                        "noise(" + x + ", " + y + ") = " + value + " is outside [-1, 1]");
            }
        }
    }

    // ======================================================================
    // noise(0, 0) == 0.0f
    // ======================================================================

    @Test
    void noise_originReturnsZero() {
        // At (0,0): x0=y0=0 so dot contribution n0=0;
        // t1 and t2 are both negative so n1=n2=0 — result is always 0 regardless of seed.
        SimplexNoise.RANDOMSEED = 0;
        float value = new SimplexNoise().noise(0f, 0f);

        assertEquals(0.0f, value, 0f);
    }
}
