package nl.framegengine.core.physics;

import nl.framegengine.core.entity.GameObject;
import nl.framegengine.core.entity.Scene;
import nl.framegengine.core.entity.SceneManager;
import nl.framegengine.core.physics.Raycast.Ray;
import nl.framegengine.core.physics.Raycast.RayHit;
import nl.framegengine.core.utils.AABB;
import org.joml.Vector3f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.*;

class RaycastTest {

    // ------------------------------------------------------------------
    // Static state cleanup — GameObject registers itself in a static
    // Hashtable on construction; reset it after each test.
    // ------------------------------------------------------------------
    @BeforeEach
    void setUp() {
        SceneManager.currentScene = null;
    }

    @AfterEach
    void clearGameObjectRegistry() throws Exception {
        Field instancedObjects = GameObject.class.getDeclaredField("instancedObjects");
        instancedObjects.setAccessible(true);
        ((Hashtable<?, ?>) instancedObjects.get(null)).clear();

        Field parentWhenPresent = GameObject.class.getDeclaredField("parentWhenPresent");
        parentWhenPresent.setAccessible(true);
        ((Hashtable<?, ?>) parentWhenPresent.get(null)).clear();

        SceneManager.currentScene = null;
    }

    // Convenience: unit AABB centred at origin, no parent (world offset = zero).
    private static AABB unitBox() {
        return new AABB(new Vector3f(-0.5f), new Vector3f(0.5f));
    }

    // -----------------------------------------------------------------------
    // §5.1.1  Ray aimed directly at AABB centre — returns positive t
    // -----------------------------------------------------------------------
    @Test
    void intersectRay_rayAimedAtAABBCentre_returnsPositiveT() {
        AABB box = unitBox();
        Ray ray = new Ray(new Vector3f(0, 0, -5), new Vector3f(0, 0, 1));

        Float t = Raycast.intersectRay(ray, box);

        assertNotNull(t, "ray aimed at box should produce a hit");
        assertTrue(t > 0, "hit distance should be positive");
        assertEquals(4.5f, t, 1e-5f, "ray should hit front face at t=4.5");
    }

    // -----------------------------------------------------------------------
    // §5.1.2  Ray origin inside AABB — returns null (tMin < 0 rule)
    // -----------------------------------------------------------------------
    @Test
    void intersectRay_originInsideAABB_returnsNull() {
        AABB box = unitBox();
        Ray ray = new Ray(new Vector3f(0, 0, 0), new Vector3f(0, 0, 1));

        Float t = Raycast.intersectRay(ray, box);

        assertNull(t, "ray originating inside the box should return null");
    }

    // -----------------------------------------------------------------------
    // §5.1.3  Ray parallel to one face, outside that face — returns null
    // -----------------------------------------------------------------------
    @Test
    void intersectRay_parallelAndOutsideFace_returnsNull() {
        AABB box = unitBox();
        // Ray travels along X, but is offset in Y so it passes above the box.
        Ray ray = new Ray(new Vector3f(-5, 2, 0), new Vector3f(1, 0, 0));

        Float t = Raycast.intersectRay(ray, box);

        assertNull(t, "ray parallel to and outside a face should miss the box");
    }

    // -----------------------------------------------------------------------
    // §5.1.4  Ray parallel to one face, inside that face's slab — valid hit
    // -----------------------------------------------------------------------
    @Test
    void intersectRay_parallelAndInsideSlab_returnsHit() {
        AABB box = unitBox();
        // Ray travels along X at Y=0, Z=0 — stays inside the Y and Z slabs.
        Ray ray = new Ray(new Vector3f(-5, 0, 0), new Vector3f(1, 0, 0));

        Float t = Raycast.intersectRay(ray, box);

        assertNotNull(t, "ray parallel to a face but inside the slab should hit");
        assertTrue(t > 0);
        assertEquals(4.5f, t, 1e-5f);
    }

    // -----------------------------------------------------------------------
    // §5.1.5  Ray missing AABB entirely — returns null
    // -----------------------------------------------------------------------
    @Test
    void intersectRay_rayMissesAABB_returnsNull() {
        AABB box = unitBox();
        // Ray travels in +Z but offset far to the side.
        Ray ray = new Ray(new Vector3f(10, 10, -5), new Vector3f(0, 0, 1));

        Float t = Raycast.intersectRay(ray, box);

        assertNull(t, "ray that misses the box entirely should return null");
    }

    // -----------------------------------------------------------------------
    // §5.1.6  null AABB argument — returns null
    // -----------------------------------------------------------------------
    @Test
    void intersectRay_nullAABB_returnsNull() {
        Ray ray = new Ray(new Vector3f(0, 0, -5), new Vector3f(0, 0, 1));

        Float t = Raycast.intersectRay(ray, (AABB) null);

        assertNull(t, "null AABB should return null immediately");
    }

    // -----------------------------------------------------------------------
    // §5.1.7  intersectRay(Ray, GameObject) — false when object has no AABB
    // -----------------------------------------------------------------------
    @Test
    void intersectRay_gameObjectWithNoAABB_returnsFalse() {
        GameObject go = new GameObject("NoAABB");
        // aabb is null by default
        Ray ray = new Ray(new Vector3f(0, 0, -5), new Vector3f(0, 0, 1));

        boolean hit = Raycast.intersectRay(ray, go);

        assertFalse(hit, "object with no AABB should not register a hit");
    }

    // ======================================================================
    // §5.2  getGameObject — scene integration
    // ======================================================================

    // Convenience: build a unit AABB and attach it to a new GameObject placed
    // at the given world position.  The AABB is centred at the object position
    // with half-extents of 0.5 in every axis.
    private static GameObject objectWithBoxAt(String name, float x, float y, float z) {
        GameObject go = new GameObject(name);
        go.setPosition(new Vector3f(x, y, z));
        // AABB min/max are in local space; getWorldOffset() resolves to the
        // object's world position so the world box is [pos-0.5, pos+0.5].
        AABB aabb = new AABB(new Vector3f(-0.5f), new Vector3f(0.5f));
        go.setAabb(aabb);
        return go;
    }

    @Test
    void getGameObject_singleObjectInScene_returnsCorrectRayHit() {
        Scene scene = new Scene(null);
        GameObject target = objectWithBoxAt("target", 0, 0, 5);
        scene.addGameObject(target);
        SceneManager.currentScene = scene;

        // Ray fired from behind the origin along +Z — should hit the box at z=5.
        Ray ray = new Ray(new Vector3f(0, 0, -5), new Vector3f(0, 0, 1));

        RayHit hit = Raycast.getGameObject(ray);

        assertNotNull(hit);
        assertSame(target, hit.gameObject, "should return the only object in the scene");
        assertTrue(hit.locationWorldSpace.z > 0, "hit location should be in front of ray origin");
    }

    @Test
    void getGameObject_twoObjectsAtDifferentDistances_returnsCloserObject() {
        Scene scene = new Scene(null);
        // near object at z=3, far object at z=8 — ray travels in +Z from z=-5.
        GameObject near = objectWithBoxAt("near", 0, 0, 3);
        GameObject far  = objectWithBoxAt("far",  0, 0, 8);
        scene.addGameObject(near);
        scene.addGameObject(far);
        SceneManager.currentScene = scene;

        Ray ray = new Ray(new Vector3f(0, 0, -5), new Vector3f(0, 0, 1));

        RayHit hit = Raycast.getGameObject(ray);

        assertNotNull(hit);
        assertSame(near, hit.gameObject, "should return the closer of the two objects");
    }

    @Test
    void getGameObject_disabledObjectInScene_notReturned() {
        Scene scene = new Scene(null);
        GameObject disabled = objectWithBoxAt("disabled", 0, 0, 3);
        disabled.setEnabled(false);
        scene.addGameObject(disabled);
        SceneManager.currentScene = scene;

        Ray ray = new Ray(new Vector3f(0, 0, -5), new Vector3f(0, 0, 1));

        RayHit hit = Raycast.getGameObject(ray);

        assertNotNull(hit);
        assertNull(hit.gameObject, "disabled object should be skipped");
    }

    @Test
    void getGameObject_emptyScene_returnsRayHitWithNullGameObject() {
        SceneManager.currentScene = new Scene(null);

        Ray ray = new Ray(new Vector3f(0, 0, -5), new Vector3f(0, 0, 1));

        RayHit hit = Raycast.getGameObject(ray);

        assertNotNull(hit, "getGameObject should never return null itself");
        assertNull(hit.gameObject, "no objects in scene — gameObject should be null");
    }

    // ======================================================================
    // §5.3  closestPointOnLine
    // ======================================================================

    private static final float EPSILON = 1e-4f;

    @Test
    void closestPointOnLine_perpendicularRayThroughLinePoint_returnsLinePointItself() {
        // Line: through (0,0,0) along the Y-axis.
        // Ray:  origin (0,0,5) traveling in -Z — perpendicular to the Y-axis and
        //       passing exactly through the line point.
        // Expected closest point on the line = (0,0,0).
        Vector3f result = Raycast.closestPointOnLine(
                new Vector3f(0, 0, 0),   // currentPosition (line point)
                new Vector3f(0, 1, 0),   // constraintAxis  (Y-axis direction)
                new Vector3f(0, 0, 5),   // targetPosition  (ray origin)
                new Vector3f(0, 0, -1)   // targetDirection (toward line point)
        );

        assertEquals(0f, result.x, EPSILON);
        assertEquals(0f, result.y, EPSILON);
        assertEquals(0f, result.z, EPSILON);
    }

    @Test
    void closestPointOnLine_parallelLines_returnsLineOrigin() {
        // Line: through (1,0,0) along +Z.
        // Ray:  origin (2,0,0) also traveling along +Z — parallel to the line.
        // Denominator collapses to 0 → moveDelta = 0 → result = currentPosition.
        Vector3f result = Raycast.closestPointOnLine(
                new Vector3f(1, 0, 0),   // currentPosition
                new Vector3f(0, 0, 1),   // constraintAxis (+Z)
                new Vector3f(2, 0, 0),   // targetPosition
                new Vector3f(0, 0, 1)    // targetDirection (+Z) — parallel
        );

        assertEquals(1f, result.x, EPSILON);
        assertEquals(0f, result.y, EPSILON);
        assertEquals(0f, result.z, EPSILON);
    }

    @Test
    void closestPointOnLine_general3DCase_returnsHandCalculatedResult() {
        // Line: through (3,0,0) along +X.
        // Ray:  origin (0,2,0) traveling in -Y (straight down).
        // The two lines are skew. Closest point on the constraint line:
        //   b = (1,0,0)·(0,-1,0) = 0  →  denominator = 1
        //   diff = (3,0,0)-(0,2,0) = (3,-2,0)
        //   d = (1,0,0)·(3,-2,0) = 3
        //   e = (0,-1,0)·(3,-2,0) = 2
        //   moveDelta = (0*2 - 1*3) / 1 = -3
        //   result = (3,0,0) + (1,0,0)*(-3) = (0,0,0)
        Vector3f result = Raycast.closestPointOnLine(
                new Vector3f(3, 0, 0),   // currentPosition
                new Vector3f(1, 0, 0),   // constraintAxis (+X)
                new Vector3f(0, 2, 0),   // targetPosition (ray origin)
                new Vector3f(0, -1, 0)   // targetDirection (-Y)
        );

        assertEquals(0f, result.x, EPSILON);
        assertEquals(0f, result.y, EPSILON);
        assertEquals(0f, result.z, EPSILON);
    }
}
