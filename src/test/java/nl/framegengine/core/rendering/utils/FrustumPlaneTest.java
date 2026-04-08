package nl.framegengine.core.rendering.utils;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FrustumPlaneTest {

    private static final float EPSILON = 1e-5f;

    // -----------------------------------------------------------------------
    // §3.1  getDistanceTo — point on plane returns 0
    // -----------------------------------------------------------------------
    @Test
    void getDistanceTo_pointOnPlane_returnsZero() {
        // Plane: Y = 3  →  normal (0,1,0), d = -3
        FrustumPlane plane = new FrustumPlane(new Vector3f(0, 1, 0), -3f);

        float dist = plane.getDistanceTo(new Vector3f(5, 3, -7));

        assertEquals(0f, dist, EPSILON);
    }

    // -----------------------------------------------------------------------
    // §3.2  getDistanceTo — point in front (positive side) vs. behind (negative)
    // -----------------------------------------------------------------------
    @Test
    void getDistanceTo_pointInFront_returnsPositive() {
        // Plane: Z = 0  →  normal (0,0,1), d = 0
        FrustumPlane plane = new FrustumPlane(new Vector3f(0, 0, 1), 0f);

        float distFront = plane.getDistanceTo(new Vector3f(0, 0, 4));
        float distBehind = plane.getDistanceTo(new Vector3f(0, 0, -4));

        assertEquals(4f, distFront, EPSILON, "point in front should return positive distance");
        assertEquals(-4f, distBehind, EPSILON, "point behind should return negative distance");
    }

    // -----------------------------------------------------------------------
    // §3.3  isSphereOutside — sphere fully inside half-space returns false
    // -----------------------------------------------------------------------
    @Test
    void isSphereOutside_sphereFullyInsideHalfSpace_returnsFalse() {
        // Plane: Y = 0, normal pointing up. Centre at Y=5, radius=2 → distance=5, well inside.
        FrustumPlane plane = new FrustumPlane(new Vector3f(0, 1, 0), 0f);

        boolean outside = plane.isSphereOutside(new Vector3f(0, 5, 0), 2f);

        assertFalse(outside);
    }

    // -----------------------------------------------------------------------
    // §3.4  isSphereOutside — sphere entirely outside returns true
    // -----------------------------------------------------------------------
    @Test
    void isSphereOutside_sphereEntirelyOutside_returnsTrue() {
        // Plane: Y = 0, normal pointing up. Centre at Y=-5, radius=2 → distance=-5, outside by 3.
        FrustumPlane plane = new FrustumPlane(new Vector3f(0, 1, 0), 0f);

        boolean outside = plane.isSphereOutside(new Vector3f(0, -5, 0), 2f);

        assertTrue(outside);
    }

    // -----------------------------------------------------------------------
    // §3.5  isSphereOutside — sphere straddles plane (partially inside) returns false
    // -----------------------------------------------------------------------
    @Test
    void isSphereOutside_sphereStraddlesPlane_returnsFalse() {
        // Plane: Y = 0, normal pointing up. Centre at Y=-1, radius=3 → distance=-1, -1 < -3 is false.
        FrustumPlane plane = new FrustumPlane(new Vector3f(0, 1, 0), 0f);

        boolean outside = plane.isSphereOutside(new Vector3f(0, -1, 0), 3f);

        assertFalse(outside, "sphere that straddles the plane is not fully outside");
    }

    // -----------------------------------------------------------------------
    // §3.6  normalize() — normal becomes unit length, distances scale proportionally
    // -----------------------------------------------------------------------
    @Test
    void normalize_scaledNormal_producesUnitNormalAndCorrectD() {
        // normal = (0, 3, 0), d = -9  → plane Y=3 but unnormalised by factor 3
        FrustumPlane plane = new FrustumPlane(new Vector3f(0, 3, 0), -9f);

        plane.normalize();

        assertEquals(1f, plane.normal.length(), EPSILON, "normal should be unit length after normalize");

        // After normalising, getDistanceTo a point on the plane (0, 3, 0) should still be 0
        float distOnPlane = plane.getDistanceTo(new Vector3f(0, 3, 0));
        assertEquals(0f, distOnPlane, EPSILON, "point on plane should have zero distance after normalize");

        // Point 2 units above the plane should have distance 2
        float distAbove = plane.getDistanceTo(new Vector3f(0, 5, 0));
        assertEquals(2f, distAbove, EPSILON, "distance above normalised plane should equal geometric distance");
    }
}
