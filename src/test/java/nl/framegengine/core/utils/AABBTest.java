package nl.framegengine.core.utils;

import nl.framegengine.core.entity.GameObject;
import org.joml.Vector3f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.*;

class AABBTest {

    private static final float EPSILON = 1e-5f;

    /**
     * Clear the static GameObject registry between tests so each test starts clean.
     * Uses reflection until the package-private resetForTesting() seam (task 1.11) is added.
     */
    @AfterEach
    void clearGameObjectRegistry() throws Exception {
        Field field = GameObject.class.getDeclaredField("instancedObjects");
        field.setAccessible(true);
        ((Hashtable<?, ?>) field.get(null)).clear();
    }

    // --- Constructor (min, max) ---

    @Test
    void constructor_setsSize() {
        AABB aabb = new AABB(new Vector3f(-1, -2, -3), new Vector3f(1, 2, 3));

        assertEquals(2f, aabb.getSize().x, EPSILON);
        assertEquals(4f, aabb.getSize().y, EPSILON);
        assertEquals(6f, aabb.getSize().z, EPSILON);
    }

    @Test
    void constructor_setsLength() {
        AABB aabb = new AABB(new Vector3f(0, 0, 0), new Vector3f(1, 0, 0));

        assertEquals(1f, aabb.getLength(), EPSILON);
    }

    // --- set(AABB) ---

    @Test
    void set_copiesMinAndMax() {
        AABB source = new AABB(new Vector3f(-1, -1, -1), new Vector3f(1, 1, 1));
        AABB copy = new AABB();
        copy.set(source);

        assertEquals(source.min.x, copy.min.x, EPSILON);
        assertEquals(source.min.y, copy.min.y, EPSILON);
        assertEquals(source.min.z, copy.min.z, EPSILON);
        assertEquals(source.max.x, copy.max.x, EPSILON);
        assertEquals(source.max.y, copy.max.y, EPSILON);
        assertEquals(source.max.z, copy.max.z, EPSILON);
    }

    @Test
    void set_isDeep_mutatingCopyDoesNotChangeSource() {
        AABB source = new AABB(new Vector3f(-1, -1, -1), new Vector3f(1, 1, 1));
        AABB copy = new AABB();
        copy.set(source);

        copy.min.set(99, 99, 99);

        assertEquals(-1f, source.min.x, EPSILON);
    }

    // --- offset(Vector3f) ---

    @Test
    void offset_shiftsMinAndMax() {
        AABB aabb = new AABB(new Vector3f(0, 0, 0), new Vector3f(2, 2, 2));
        aabb.offset(new Vector3f(1, 2, 3));

        assertEquals(1f, aabb.min.x, EPSILON);
        assertEquals(2f, aabb.min.y, EPSILON);
        assertEquals(3f, aabb.min.z, EPSILON);
        assertEquals(3f, aabb.max.x, EPSILON);
        assertEquals(4f, aabb.max.y, EPSILON);
        assertEquals(5f, aabb.max.z, EPSILON);
    }

    // --- getCenter() ---

    @Test
    void getCenter_returnsMidpointBetweenMinAndMax() {
        AABB aabb = new AABB(new Vector3f(-2, 0, -4), new Vector3f(2, 4, 0));

        Vector3f center = aabb.getCenter();

        assertEquals(0f, center.x, EPSILON);
        assertEquals(2f, center.y, EPSILON);
        assertEquals(-2f, center.z, EPSILON);
    }

    // --- toWorld() ---

    @Test
    void toWorld_nullParent_returnsEqualCopy() {
        AABB aabb = new AABB(new Vector3f(-1, -1, -1), new Vector3f(1, 1, 1));

        AABB world = aabb.toWorld();

        assertEquals(aabb.min.x, world.min.x, EPSILON);
        assertEquals(aabb.min.y, world.min.y, EPSILON);
        assertEquals(aabb.min.z, world.min.z, EPSILON);
        assertEquals(aabb.max.x, world.max.x, EPSILON);
        assertEquals(aabb.max.y, world.max.y, EPSILON);
        assertEquals(aabb.max.z, world.max.z, EPSILON);
    }

    @Test
    void toWorld_withTranslatedParent_worldAABBShiftedByParentPosition() {
        GameObject parent = new GameObject();
        parent.setPosition(new Vector3f(10, 5, 0));

        AABB local = new AABB(new Vector3f(-1, -1, -1), new Vector3f(1, 1, 1));
        local.setParentObject(parent);

        AABB world = local.toWorld();

        assertEquals(9f,  world.min.x, EPSILON);
        assertEquals(4f,  world.min.y, EPSILON);
        assertEquals(-1f, world.min.z, EPSILON);
        assertEquals(11f, world.max.x, EPSILON);
        assertEquals(6f,  world.max.y, EPSILON);
        assertEquals(1f,  world.max.z, EPSILON);
    }

    @Test
    void toWorld_withScaledParent_worldAABBScaled() {
        GameObject parent = new GameObject();
        parent.setPosition(new Vector3f(0, 0, 0));
        parent.setScale(new Vector3f(2, 2, 2));

        AABB local = new AABB(new Vector3f(-1, -1, -1), new Vector3f(1, 1, 1));
        local.setParentObject(parent);

        AABB world = local.toWorld();

        assertEquals(-2f, world.min.x, EPSILON);
        assertEquals(-2f, world.min.y, EPSILON);
        assertEquals(-2f, world.min.z, EPSILON);
        assertEquals(2f,  world.max.x, EPSILON);
        assertEquals(2f,  world.max.y, EPSILON);
        assertEquals(2f,  world.max.z, EPSILON);
    }
}
