package nl.framegengine.core.utils;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import static org.junit.jupiter.api.Assertions.*;

class JsonHelperTest {

    private static final float EPSILON = 1e-6f;

    // --- vector3ToJsonObject / jsonToVector3f round-trip ---

    @Test
    void vector3_roundTrip_preservesComponents() {
        Vector3f original = new Vector3f(1.5f, -2.25f, 0.001f);

        JsonObject json = JsonHelper.vector3ToJsonObject(original);
        Vector3f restored = JsonHelper.jsonToVector3f(json);

        assertEquals(original.x, restored.x, EPSILON);
        assertEquals(original.y, restored.y, EPSILON);
        assertEquals(original.z, restored.z, EPSILON);
    }

    @Test
    void vector3_roundTrip_zeroVector() {
        Vector3f original = new Vector3f(0, 0, 0);

        JsonObject json = JsonHelper.vector3ToJsonObject(original);
        Vector3f restored = JsonHelper.jsonToVector3f(json);

        assertEquals(0f, restored.x, EPSILON);
        assertEquals(0f, restored.y, EPSILON);
        assertEquals(0f, restored.z, EPSILON);
    }

    // --- vector4ToJsonObject / jsonToVector4f round-trip ---

    @Test
    void vector4_roundTrip_preservesComponents() {
        Vector4f original = new Vector4f(1f, 2f, 3f, 4f);

        JsonObject json = JsonHelper.vector4ToJsonObject(original);
        Vector4f restored = JsonHelper.jsonToVector4f(json);

        assertEquals(original.x, restored.x, EPSILON);
        assertEquals(original.y, restored.y, EPSILON);
        assertEquals(original.z, restored.z, EPSILON);
        assertEquals(original.w, restored.w, EPSILON);
    }

    // --- quaternionToJsonObject / jsonToQuaternionf round-trip ---

    @Test
    void quaternion_roundTrip_preservesComponents() {
        Quaternionf original = new Quaternionf(0.1f, 0.2f, 0.3f, 0.9274f);

        JsonObject json = JsonHelper.quaternionToJsonObject(original);
        Quaternionf restored = JsonHelper.jsonToQuaternionf(json);

        assertEquals(original.x, restored.x, EPSILON);
        assertEquals(original.y, restored.y, EPSILON);
        assertEquals(original.z, restored.z, EPSILON);
        assertEquals(original.w, restored.w, EPSILON);
    }

    @Test
    void quaternion_missingW_defaultsToOne() {
        // jsonToQuaternionf uses default 1.0 for w if key is absent
        JsonObject json = Json.createObjectBuilder()
                .add("x", 0f).add("y", 0f).add("z", 0f)
                .build();

        Quaternionf q = JsonHelper.jsonToQuaternionf(json);

        assertEquals(1f, q.w, EPSILON);
    }

    // --- jsonToFloat ---

    @Test
    void jsonToFloat_missingKey_returnsDefault() {
        JsonObject obj = Json.createObjectBuilder().build();

        float result = JsonHelper.jsonToFloat(obj, "nonexistent", 42f);

        assertEquals(42f, result, EPSILON);
    }

    @Test
    void jsonToFloat_nullValue_returnsDefault() {
        JsonObject obj = Json.createObjectBuilder()
                .addNull("key")
                .build();

        float result = JsonHelper.jsonToFloat(obj, "key", 7f);

        assertEquals(7f, result, EPSILON);
    }

    @Test
    void jsonToFloat_presentNumberValue_returnsParsedFloat() {
        JsonObject obj = Json.createObjectBuilder()
                .add("val", 3.14)
                .build();

        float result = JsonHelper.jsonToFloat(obj, "val", 0f);

        assertEquals(3.14f, result, 1e-4f);
    }

    // --- hasJsonKey ---

    @Test
    void hasJsonKey_presentNonNullKey_returnsTrue() {
        JsonObject obj = Json.createObjectBuilder()
                .add("key", "value")
                .build();

        assertTrue(JsonHelper.hasJsonKey(obj, "key"));
    }

    @Test
    void hasJsonKey_absentKey_returnsFalse() {
        JsonObject obj = Json.createObjectBuilder().build();

        assertFalse(JsonHelper.hasJsonKey(obj, "missing"));
    }

    @Test
    void hasJsonKey_nullValue_returnsFalse() {
        JsonObject obj = Json.createObjectBuilder()
                .addNull("key")
                .build();

        assertFalse(JsonHelper.hasJsonKey(obj, "key"));
    }
}
