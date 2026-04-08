package nl.framegengine.core.utils;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransformationTest {

    private static final float EPSILON = 1e-5f;

    // --- toModelMatrix(position, rotation, scale) ---

    @Test
    void toModelMatrix_identityRotation_translationComponentMatchesPosition() {
        Vector3f position = new Vector3f(3, 4, 5);
        Quaternionf rotation = new Quaternionf().identity();
        Vector3f scale = new Vector3f(2, 2, 2);

        Matrix4f m = Transformation.toModelMatrix(position, rotation, scale);

        // In a column-major Matrix4f the translation is in column 3: m30, m31, m32
        assertEquals(3f, m.m30(), EPSILON);
        assertEquals(4f, m.m31(), EPSILON);
        assertEquals(5f, m.m32(), EPSILON);
    }

    @Test
    void toModelMatrix_uniformScale_diagonalScaledForIdentityRotation() {
        Vector3f position = new Vector3f(0, 0, 0);
        Quaternionf rotation = new Quaternionf().identity();
        Vector3f scale = new Vector3f(3, 3, 3);

        Matrix4f m = Transformation.toModelMatrix(position, rotation, scale);

        // With identity rotation the upper-left diagonal equals the scale
        assertEquals(3f, m.m00(), EPSILON);
        assertEquals(3f, m.m11(), EPSILON);
        assertEquals(3f, m.m22(), EPSILON);
    }

    @Test
    void toModelMatrix_nonTrivialRotation_upperLeftMatchesRotationMatrix() {
        Vector3f position = new Vector3f(1, 2, 3);
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.toRadians(45));
        Vector3f scale = new Vector3f(1, 1, 1);

        Matrix4f m = Transformation.toModelMatrix(position, rotation, scale);

        // 45° around Z: col-0 x/y should be (cos45, sin45), col-1 x/y should be (-sin45, cos45)
        float cos45 = (float) Math.cos(Math.toRadians(45));
        float sin45 = (float) Math.sin(Math.toRadians(45));

        assertEquals( cos45,  m.m00(), EPSILON);
        assertEquals( sin45,  m.m01(), EPSILON);
        assertEquals(-sin45,  m.m10(), EPSILON);
        assertEquals( cos45,  m.m11(), EPSILON);

        // Translation must still be preserved
        assertEquals(1f, m.m30(), EPSILON);
        assertEquals(2f, m.m31(), EPSILON);
        assertEquals(3f, m.m32(), EPSILON);
    }

    // --- rotateDirection ---

    @Test
    void rotateDirection_forward_by90degreesAroundY_producesExpectedVector() {
        // rotateYXZ with yaw=90°, pitch=0, roll=0 rotates (0,0,-1) to (-1,0,0)
        Vector3f forward = new Vector3f(0, 0, -1);
        Vector3f eulerDeg = new Vector3f(0, 90, 0); // pitch=0, yaw=90, roll=0

        Vector3f result = Transformation.rotateDirection(forward, eulerDeg);

        assertEquals(-1f, result.x, EPSILON);
        assertEquals( 0f, result.y, EPSILON);
        assertEquals( 0f, result.z, EPSILON);
    }

    @Test
    void rotateDirection_zeroRotation_returnsUnchangedVector() {
        Vector3f input = new Vector3f(1, 2, 3);
        Vector3f result = Transformation.rotateDirection(input, new Vector3f(0, 0, 0));

        assertEquals(input.x, result.x, EPSILON);
        assertEquals(input.y, result.y, EPSILON);
        assertEquals(input.z, result.z, EPSILON);
    }

    @Test
    void rotateDirection_doesNotMutateInputVector() {
        Vector3f input = new Vector3f(0, 0, -1);
        Transformation.rotateDirection(input, new Vector3f(0, 90, 0));

        // input must be unchanged — rotateDirection creates an internal copy
        assertEquals( 0f, input.x, EPSILON);
        assertEquals( 0f, input.y, EPSILON);
        assertEquals(-1f, input.z, EPSILON);
    }
}
