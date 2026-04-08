package nl.framegengine.core.utils;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CalculusTest {

    private static final float EPSILON = 1e-6f;

    // --- addVectors(Vector3f, Vector3f) ---

    @Test
    void addVectors_Vector3f_basicSum() {
        Vector3f result = Calculus.addVectors(new Vector3f(1, 2, 3), new Vector3f(4, 5, 6));
        assertEquals(5f, result.x, EPSILON);
        assertEquals(7f, result.y, EPSILON);
        assertEquals(9f, result.z, EPSILON);
    }

    @Test
    void addVectors_Vector3f_commutative() {
        Vector3f a = new Vector3f(1, 2, 3);
        Vector3f b = new Vector3f(7, -1, 0.5f);
        Vector3f ab = Calculus.addVectors(a, b);
        Vector3f ba = Calculus.addVectors(b, a);
        assertEquals(ab.x, ba.x, EPSILON);
        assertEquals(ab.y, ba.y, EPSILON);
        assertEquals(ab.z, ba.z, EPSILON);
    }

    @Test
    void addVectors_Vector3f_identityZero() {
        Vector3f a = new Vector3f(3, -2, 1);
        Vector3f result = Calculus.addVectors(a, new Vector3f(0, 0, 0));
        assertEquals(a.x, result.x, EPSILON);
        assertEquals(a.y, result.y, EPSILON);
        assertEquals(a.z, result.z, EPSILON);
    }

    // --- subtractVectors(Vector3f, Vector3f) ---

    @Test
    void subtractVectors_Vector3f_zeroSubtraction() {
        Vector3f a = new Vector3f(5, -3, 2);
        Vector3f result = Calculus.subtractVectors(a, new Vector3f(0, 0, 0));
        assertEquals(a.x, result.x, EPSILON);
        assertEquals(a.y, result.y, EPSILON);
        assertEquals(a.z, result.z, EPSILON);
    }

    @Test
    void subtractVectors_Vector3f_selfSubtraction() {
        Vector3f a = new Vector3f(5, -3, 2);
        Vector3f result = Calculus.subtractVectors(a, a);
        assertEquals(0f, result.x, EPSILON);
        assertEquals(0f, result.y, EPSILON);
        assertEquals(0f, result.z, EPSILON);
    }

    // --- multiplyVector(Vector3f, float) ---

    @Test
    void multiplyVector_scalarZero_returnsZeroVector() {
        Vector3f result = Calculus.multiplyVector(new Vector3f(5, -3, 2), 0f);
        assertEquals(0f, result.x, EPSILON);
        assertEquals(0f, result.y, EPSILON);
        assertEquals(0f, result.z, EPSILON);
    }

    @Test
    void multiplyVector_scalarOne_returnsIdentical() {
        Vector3f a = new Vector3f(5, -3, 2);
        Vector3f result = Calculus.multiplyVector(a, 1f);
        assertEquals(a.x, result.x, EPSILON);
        assertEquals(a.y, result.y, EPSILON);
        assertEquals(a.z, result.z, EPSILON);
    }

    @Test
    void multiplyVector_scalarNegative_negatesVector() {
        Vector3f result = Calculus.multiplyVector(new Vector3f(1, 2, 3), -1f);
        assertEquals(-1f, result.x, EPSILON);
        assertEquals(-2f, result.y, EPSILON);
        assertEquals(-3f, result.z, EPSILON);
    }

    // --- multiplyVector(Vector3f, Vector3f) ---

    @Test
    void multiplyVector_componentWise() {
        Vector3f result = Calculus.multiplyVector(new Vector3f(2, 3, 4), new Vector3f(5, 6, 7));
        assertEquals(10f, result.x, EPSILON);
        assertEquals(18f, result.y, EPSILON);
        assertEquals(28f, result.z, EPSILON);
    }

    // --- signedAngle2D ---

    @Test
    void signedAngle2D_zeroAngle() {
        // Same direction → 0
        float angle = Calculus.signedAngle2D(new Vector2f(1, 0), new Vector2f(1, 0));
        assertEquals(0f, angle, EPSILON);
    }

    @Test
    void signedAngle2D_90degrees() {
        // cross = 1*1 - 0*0 = 1; dot = 0; atan2(-1, 0) = -π/2
        float angle = Calculus.signedAngle2D(new Vector2f(1, 0), new Vector2f(0, 1));
        assertEquals((float) (-Math.PI / 2), angle, EPSILON);
    }

    @Test
    void signedAngle2D_180degrees() {
        // cross=0, dot=-1; atan2(-0,-1) = -π
        float angle = Calculus.signedAngle2D(new Vector2f(1, 0), new Vector2f(-1, 0));
        assertEquals((float) -Math.PI, angle, EPSILON);
    }

    @Test
    void signedAngle2D_minus90degrees() {
        // cross = 1*(-1) - 0*0 = -1; dot=0; atan2(1,0) = π/2
        float angle = Calculus.signedAngle2D(new Vector2f(1, 0), new Vector2f(0, -1));
        assertEquals((float) (Math.PI / 2), angle, EPSILON);
    }
}
