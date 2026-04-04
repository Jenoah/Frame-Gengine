package nl.framegengine.core.utils;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConversionTest {

    private static final float EPSILON = 1e-5f;

    // ======================================================================
    // floatArrayToVector3Array / toFloatArray(Vector3f[]) round-trip
    // ======================================================================

    @Test
    void floatArrayToVector3Array_toFloatArray_roundTrip() {
        float[] original = {1f, 2f, 3f, 4f, 5f, 6f};

        Vector3f[] vectors = Conversion.floatArrayToVector3Array(original);
        float[] result = Conversion.toFloatArray(vectors);

        assertArrayEquals(original, result, EPSILON);
    }

    @Test
    void floatArrayToVector3Array_singleVector_correctComponents() {
        float[] input = {7f, 8f, 9f};

        Vector3f[] vectors = Conversion.floatArrayToVector3Array(input);

        assertEquals(1, vectors.length);
        assertEquals(7f, vectors[0].x, EPSILON);
        assertEquals(8f, vectors[0].y, EPSILON);
        assertEquals(9f, vectors[0].z, EPSILON);
    }

    // ======================================================================
    // floatArrayToVector2Array / toFloatArray(Vector2f[]) round-trip
    // ======================================================================

    @Test
    void floatArrayToVector2Array_toFloatArray_roundTrip() {
        float[] original = {0.1f, 0.2f, 0.3f, 0.4f};

        Vector2f[] vectors = Conversion.floatArrayToVector2Array(original);
        float[] result = Conversion.toFloatArray(vectors);

        assertArrayEquals(original, result, EPSILON);
    }

    @Test
    void floatArrayToVector2Array_singleVector_correctComponents() {
        float[] input = {3f, 4f};

        Vector2f[] vectors = Conversion.floatArrayToVector2Array(input);

        assertEquals(1, vectors.length);
        assertEquals(3f, vectors[0].x, EPSILON);
        assertEquals(4f, vectors[0].y, EPSILON);
    }

    // ======================================================================
    // v3ToFloatArray(List<Vector3f>)
    // ======================================================================

    @Test
    void v3ToFloatArray_packsValuesCorrectly() {
        List<Vector3f> vectors = Arrays.asList(
                new Vector3f(1f, 2f, 3f),
                new Vector3f(4f, 5f, 6f)
        );

        float[] result = Conversion.v3ToFloatArray(vectors);

        assertArrayEquals(new float[]{1f, 2f, 3f, 4f, 5f, 6f}, result, EPSILON);
    }

    @Test
    void v3ToFloatArray_emptyList_returnsEmptyArray() {
        float[] result = Conversion.v3ToFloatArray(Collections.emptyList());

        assertEquals(0, result.length);
    }

    // ======================================================================
    // v2ToFloatArray(List<Vector2f>)
    // ======================================================================

    @Test
    void v2ToFloatArray_packsValuesCorrectly() {
        List<Vector2f> vectors = Arrays.asList(
                new Vector2f(0.5f, 0.25f),
                new Vector2f(1.0f, 0.75f)
        );

        float[] result = Conversion.v2ToFloatArray(vectors);

        assertArrayEquals(new float[]{0.5f, 0.25f, 1.0f, 0.75f}, result, EPSILON);
    }

    // ======================================================================
    // toIntArray(List<Integer>)
    // ======================================================================

    @Test
    void toIntArray_emptyList_returnsEmptyArray() {
        int[] result = Conversion.toIntArray(Collections.emptyList());

        assertEquals(0, result.length);
    }

    @Test
    void toIntArray_normalList_correctValues() {
        int[] result = Conversion.toIntArray(Arrays.asList(10, 20, 30));

        assertArrayEquals(new int[]{10, 20, 30}, result);
    }

    // ======================================================================
    // toFloatArray(List<Float>)
    // ======================================================================

    @Test
    void toFloatArray_listOfFloats_nullElementProducesNaN() {
        List<Float> list = Arrays.asList(1.0f, null, 3.0f);

        float[] result = Conversion.toFloatArray(list);

        assertEquals(1.0f, result[0], EPSILON);
        assertTrue(Float.isNaN(result[1]));
        assertEquals(3.0f, result[2], EPSILON);
    }

    @Test
    void toFloatArray_listOfFloats_noNulls_correctValues() {
        float[] result = Conversion.toFloatArray(Arrays.asList(1.5f, 2.5f));

        assertArrayEquals(new float[]{1.5f, 2.5f}, result, EPSILON);
    }

    // ======================================================================
    // angleTo360degrees
    // ======================================================================

    @Test
    void angleTo360degrees_zero_returnsZero() {
        assertEquals(0f, Conversion.angleTo360degrees(0f), EPSILON);
    }

    @Test
    void angleTo360degrees_negativeRadians_returnsPositiveDegrees() {
        // -PI/2 radians = -90 degrees -> normalised to 270
        float result = Conversion.angleTo360degrees((float) -Math.PI / 2);

        assertEquals(270f, result, 1e-3f);
    }

    @Test
    void angleTo360degrees_greaterThan2PI_wrapsCorrectly() {
        // 3*PI radians = 540 degrees -> 540 % 360 = 180
        float result = Conversion.angleTo360degrees((float) (3 * Math.PI));

        assertEquals(180f, result, 1e-3f);
    }

    @Test
    void angleTo360degrees_positiveHalfPi_returns90() {
        float result = Conversion.angleTo360degrees((float) (Math.PI / 2));

        assertEquals(90f, result, 1e-3f);
    }

    // ======================================================================
    // floatTwoDecimals
    // ======================================================================

    @Test
    void floatTwoDecimals_positiveInteger_twoZeroDecimalPlaces() {
        assertEquals("3.00", Conversion.floatTwoDecimals(3.0f));
    }

    @Test
    void floatTwoDecimals_negativeValue() {
        assertEquals("-1.50", Conversion.floatTwoDecimals(-1.5f));
    }

    @Test
    void floatTwoDecimals_truncatesNotRounds() {
        // 1.999f truncates to 1.99, not 2.00
        assertEquals("1.99", Conversion.floatTwoDecimals(1.999f));
    }

    @Test
    void floatTwoDecimals_decimalPartLessThan10_zeropadded() {
        // 1.0625f is exact in IEEE 754; (int)(0.0625 * 100) = 6 -> zero-padded to "06"
        assertEquals("1.06", Conversion.floatTwoDecimals(1.0625f));
    }

    // ======================================================================
    // toVector3I / toVector3F
    // ======================================================================

    @Test
    void toVector3I_truncatesTowardZero_positive() {
        Vector3i result = Conversion.toVector3I(new Vector3f(2.9f, 1.1f, 0.99f));

        assertEquals(2, result.x);
        assertEquals(1, result.y);
        assertEquals(0, result.z);
    }

    @Test
    void toVector3I_truncatesTowardZero_negative() {
        Vector3i result = Conversion.toVector3I(new Vector3f(-1.7f, -0.5f, -3.0f));

        assertEquals(-1, result.x);
        assertEquals(0,  result.y);
        assertEquals(-3, result.z);
    }

    @Test
    void toVector3F_integerComponentsBecomeFloats() {
        Vector3f result = Conversion.toVector3F(new Vector3i(3, -2, 7));

        assertEquals(3f,  result.x, EPSILON);
        assertEquals(-2f, result.y, EPSILON);
        assertEquals(7f,  result.z, EPSILON);
    }
}
