package nl.framegengine.core.lighting;

import nl.framegengine.core.entity.GameObject;
import nl.framegengine.core.entity.SceneManager;
import org.joml.Vector3f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.*;

class LightTest {

    private static final float EPSILON = 1e-5f;

    @AfterEach
    void clearRegistry() throws Exception {
        Field instancedObjects = GameObject.class.getDeclaredField("instancedObjects");
        instancedObjects.setAccessible(true);
        ((Hashtable<?, ?>) instancedObjects.get(null)).clear();

        Field parentWhenPresent = GameObject.class.getDeclaredField("parentWhenPresent");
        parentWhenPresent.setAccessible(true);
        ((Hashtable<?, ?>) parentWhenPresent.get(null)).clear();

        SceneManager.currentScene = null;
    }

    // ======================================================================
    // §13.1 setValuesByDistance
    // ======================================================================

    @Test
    void setValuesByDistance_zero_constantOneLinearOneExponentOne() {
        Light light = new Light();

        light.setValuesByDistance(0f);

        assertEquals(1.0f, light.getConstant(), EPSILON);
        assertEquals(1.0f, light.getLinear(),   EPSILON);
        assertEquals(1.0f, light.getExponent(),  EPSILON);
    }

    @Test
    void setValuesByDistance_one_expectedAttenuation() {
        Light light = new Light();

        light.setValuesByDistance(1f);

        // d=1 -> distancePlusOne=2 -> linear=0.5, exponent=0.25
        assertEquals(1.0f,  light.getConstant(), EPSILON);
        assertEquals(0.5f,  light.getLinear(),   EPSILON);
        assertEquals(0.25f, light.getExponent(),  EPSILON);
    }

    @Test
    void setValuesByDistance_nine_expectedAttenuation() {
        Light light = new Light();

        light.setValuesByDistance(9f);

        // d=9 -> distancePlusOne=10 -> linear=0.1, exponent=0.01
        assertEquals(1.0f, light.getConstant(), EPSILON);
        assertEquals(0.1f, light.getLinear(),   EPSILON);
        assertEquals(0.01f, light.getExponent(), EPSILON);
    }

    @Test
    void setValuesByDistance_largeDistance_linearAndExponentApproachZero() {
        Light light = new Light();

        light.setValuesByDistance(999f);

        assertEquals(1.0f, light.getConstant(), EPSILON);
        assertTrue(light.getLinear()   < 0.01f, "linear should be near zero for large distance");
        assertTrue(light.getExponent() < 0.01f, "exponent should be near zero for large distance");
    }

    // ======================================================================
    // §13.2 Light getters / setters
    // ======================================================================

    @Test
    void setColor_getColor_returnsSameReference() {
        Light light = new Light();
        Vector3f color = new Vector3f(0.5f, 0.3f, 0.1f);

        light.setColor(color);

        assertSame(color, light.getColor());
    }

    @Test
    void setIntensity_getIntensity_roundTrip() {
        Light light = new Light();

        light.setIntensity(3.5f);

        assertEquals(3.5f, light.getIntensity(), EPSILON);
    }

    @Test
    void setConstant_setLinear_setExponent_roundTrips() {
        Light light = new Light();

        light.setConstant(1.0f);
        light.setLinear(0.7f);
        light.setExponent(0.14f);

        assertEquals(1.0f,  light.getConstant(), EPSILON);
        assertEquals(0.7f,  light.getLinear(),   EPSILON);
        assertEquals(0.14f, light.getExponent(),  EPSILON);
    }

    @Test
    void fullConstructor_setsAllFieldsCorrectly() {
        Vector3f color    = new Vector3f(1f, 0f, 0f);
        Vector3f position = new Vector3f(2f, 3f, 4f);

        GameObject lightObject = new GameObject("Light");
        lightObject.setPosition(position);
        Light light = new Light(color, 2.0f, 1.0f, 0.5f, 0.25f);
        lightObject.addComponent(light);

        assertSame(color, light.getColor());
        assertEquals(2.0f,  light.getIntensity(), EPSILON);
        assertEquals(1.0f,  light.getConstant(),  EPSILON);
        assertEquals(0.5f,  light.getLinear(),    EPSILON);
        assertEquals(0.25f, light.getExponent(),   EPSILON);
        assertEquals(2f, light.getRoot().getPosition().x, EPSILON);
        assertEquals(3f, light.getRoot().getPosition().y, EPSILON);
        assertEquals(4f, light.getRoot().getPosition().z, EPSILON);
    }

    @Test
    void distanceConstructor_attenuationMatchesSetValuesByDistanceFormula() {
        Vector3f color    = new Vector3f(1f, 1f, 1f);
        Vector3f position = new Vector3f(0f, 0f, 0f);
        float distance = 4f;

        GameObject lightObject = new GameObject("Light");
        lightObject.setPosition(position);
        Light light = new Light(color, 1.0f, distance);
        lightObject.addComponent(light);

        // Expected: distancePlusOne=5, linear=0.2, exponent=0.04
        assertEquals(1.0f,  light.getConstant(), EPSILON);
        assertEquals(0.2f,  light.getLinear(),   EPSILON);
        assertEquals(0.04f, light.getExponent(),  EPSILON);
    }

    // ======================================================================
    // §13.3 SpotLight getters / setters
    // ======================================================================

    @Test
    void spotLight_setCutOff_getCutOff_roundTrip() {
        SpotLight spot = new SpotLight();

        spot.setCutOff(30f);

        assertEquals(30f, spot.getCutOff(), EPSILON);
    }

    @Test
    void spotLight_setOuterCutOff_getOuterCutOff_roundTrip() {
        SpotLight spot = new SpotLight();

        spot.setOuterCutOff(45f);

        assertEquals(45f, spot.getOuterCutOff(), EPSILON);
    }

    @Test
    void spotLight_fullConstructor_setsCutOffAndOuterCutOff() {
        Vector3f color    = new Vector3f(0f, 1f, 0f);
        Vector3f position = new Vector3f(1f, 2f, 3f);

        GameObject lightObject = new GameObject("Light");
        lightObject.setPosition(position);
        SpotLight spot = new SpotLight(color, position, 1.5f, 1.0f, 0.5f, 0.25f, 20f, 35f);
        lightObject.addComponent(spot);

        assertEquals(20f, spot.getCutOff(),     EPSILON);
        assertEquals(35f, spot.getOuterCutOff(), EPSILON);
    }

    @Test
    void spotLight_distanceConstructor_attenuationAndConeAnglesSet() {
        Vector3f color    = new Vector3f(1f, 1f, 0f);
        Vector3f position = new Vector3f(0f, 0f, 0f);

        GameObject lightObject = new GameObject("Light");
        lightObject.setPosition(position);
        SpotLight spot = new SpotLight(color, 1.0f, 9f, 15f, 25f);
        lightObject.addComponent(spot);


        // Attenuation via setValuesByDistance(9): linear=0.1, exponent=0.01
        assertEquals(1.0f,  spot.getConstant(), EPSILON);
        assertEquals(0.1f,  spot.getLinear(),   EPSILON);
        assertEquals(0.01f, spot.getExponent(),  EPSILON);
        assertEquals(15f,   spot.getCutOff(),     EPSILON);
        assertEquals(25f,   spot.getOuterCutOff(), EPSILON);
    }
}
