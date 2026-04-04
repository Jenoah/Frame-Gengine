package nl.framegengine.core.utils;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DebugEntityTest {

    private static final float EPSILON = 1e-5f;

    // ======================================================================
    // Single-arg constructor (Vector3f)
    // ======================================================================

    @Test
    void singleArgConstructor_positionStoredAndDefaultsApplied() {
        Vector3f pos = new Vector3f(1f, 2f, 3f);

        DebugEntity entity = new DebugEntity(pos);

        assertEquals(1f, entity.getPosition().x, EPSILON);
        assertEquals(2f, entity.getPosition().y, EPSILON);
        assertEquals(3f, entity.getPosition().z, EPSILON);
        assertEquals(1f, entity.getScale().x, EPSILON, "default scale x must be 1");
        assertEquals(1f, entity.getScale().y, EPSILON, "default scale y must be 1");
        assertEquals(1f, entity.getScale().z, EPSILON, "default scale z must be 1");
        assertTrue(new Quaternionf().identity().equals(entity.getRotation(), EPSILON),
                "default rotation must be identity");
        assertEquals(DebugEntity.DebugShape.CUBE, entity.getShape(),
                "default shape must be CUBE");
    }

    // ======================================================================
    // Constructor (Vector3f, Vector3f)
    // ======================================================================

    @Test
    void positionAndScaleConstructor_bothStoredCorrectly() {
        Vector3f pos   = new Vector3f(4f, 5f, 6f);
        Vector3f scale = new Vector3f(2f, 3f, 4f);

        DebugEntity entity = new DebugEntity(pos, scale);

        assertEquals(4f, entity.getPosition().x, EPSILON);
        assertEquals(5f, entity.getPosition().y, EPSILON);
        assertEquals(6f, entity.getPosition().z, EPSILON);
        assertEquals(2f, entity.getScale().x, EPSILON);
        assertEquals(3f, entity.getScale().y, EPSILON);
        assertEquals(4f, entity.getScale().z, EPSILON);
    }

    // ======================================================================
    // Constructor (Vector3f, float)
    // ======================================================================

    @Test
    void positionAndFloatScaleConstructor_scaleSetUniformlyOnAllAxes() {
        DebugEntity entity = new DebugEntity(new Vector3f(0f), 3f);

        assertEquals(3f, entity.getScale().x, EPSILON);
        assertEquals(3f, entity.getScale().y, EPSILON);
        assertEquals(3f, entity.getScale().z, EPSILON);
    }

    // ======================================================================
    // Constructor (Vector3f, Quaternionf)
    // ======================================================================

    @Test
    void positionAndRotationConstructor_rotationStoredCorrectly() {
        Quaternionf rot = new Quaternionf().rotateY((float) Math.PI / 4);

        DebugEntity entity = new DebugEntity(new Vector3f(0f), rot);

        assertTrue(rot.equals(entity.getRotation(), EPSILON),
                "stored rotation must match the source quaternion");
    }

    // ======================================================================
    // Defensive copy — mutating source does not affect stored position
    // ======================================================================

    @Test
    void constructor_defensiveCopy_mutatingSourceDoesNotChangeStoredPosition() {
        Vector3f source = new Vector3f(1f, 2f, 3f);
        DebugEntity entity = new DebugEntity(source);

        source.set(99f, 99f, 99f);

        assertEquals(1f, entity.getPosition().x, EPSILON);
        assertEquals(2f, entity.getPosition().y, EPSILON);
        assertEquals(3f, entity.getPosition().z, EPSILON);
    }

    // ======================================================================
    // getShape() returns the shape passed to constructor
    // ======================================================================

    @Test
    void constructor_withExplicitShape_getShapeReturnsIt() {
        DebugEntity entity = new DebugEntity(
                new Vector3f(0f), new Vector3f(1f), DebugEntity.DebugShape.CUBE);

        assertEquals(DebugEntity.DebugShape.CUBE, entity.getShape());
    }
}
