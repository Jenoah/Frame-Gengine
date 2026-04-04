package nl.framegengine.core.visual;

import org.junit.jupiter.api.Test;

import javax.json.JsonObject;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class TextureTest {

    // ------------------------------------------------------------------
    // Reflection helpers — boolean flags have no public getters.
    // ------------------------------------------------------------------

    private static boolean getBooleanField(Texture texture, String name) throws Exception {
        Field f = Texture.class.getDeclaredField(name);
        f.setAccessible(true);
        return (boolean) f.get(texture);
    }

    // ======================================================================
    // §8.1  No-arg construction — field defaults
    // ======================================================================

    @Test
    void noArgConstructor_idIsZero() {
        Texture texture = new Texture();

        assertEquals(0, texture.getId());
    }

    @Test
    void noArgConstructor_pointFilterIsFalse() throws Exception {
        Texture texture = new Texture();

        assertFalse(getBooleanField(texture, "pointFilter"));
    }

    @Test
    void noArgConstructor_flippedIsFalse() throws Exception {
        Texture texture = new Texture();

        assertFalse(getBooleanField(texture, "flipped"));
    }

    @Test
    void noArgConstructor_repeatIsTrue() throws Exception {
        Texture texture = new Texture();

        assertTrue(getBooleanField(texture, "repeat"));
    }

    @Test
    void noArgConstructor_isNormalMapIsFalse() throws Exception {
        Texture texture = new Texture();

        assertFalse(getBooleanField(texture, "isNormalMap"));
    }

    @Test
    void noArgConstructor_isDataTextureIsFalse() throws Exception {
        Texture texture = new Texture();

        assertFalse(getBooleanField(texture, "isDataTexture"));
    }

    @Test
    void noArgConstructor_guidIsNull() {
        Texture texture = new Texture();

        assertNull(texture.getGuid());
    }

    // ======================================================================
    // §8.2  setGuid / getGuid round-trip
    // ======================================================================

    @Test
    void setGuid_getGuid_roundTrip() {
        Texture texture = new Texture();

        texture.setGuid("test-guid-1234");

        assertEquals("test-guid-1234", texture.getGuid());
    }

    @Test
    void setGuid_replacesExistingGuid() {
        Texture texture = new Texture();
        texture.setGuid("first-guid");

        texture.setGuid("second-guid");

        assertEquals("second-guid", texture.getGuid());
    }

    // ======================================================================
    // §8.3  serializeToJson
    // ======================================================================

    @Test
    void serializeToJson_excludesIdAndTexturePath() {
        Texture texture = new Texture();
        texture.setGuid("some-guid");

        JsonObject json = texture.serializeToJson();

        assertFalse(json.containsKey("id"),          "id should be excluded");
        assertFalse(json.containsKey("texturePath"), "texturePath should be excluded");
    }

    @Test
    void serializeToJson_containsGuidWhenSet() {
        Texture texture = new Texture();
        texture.setGuid("my-guid");

        JsonObject json = texture.serializeToJson();

        assertTrue(json.containsKey("guid"), "guid should be present when set");
        assertEquals("my-guid", json.getString("guid"));
    }

    @Test
    void serializeToJson_nonDefaultBooleanFlagsPresent() throws Exception {
        // Set every flag to its non-default value so they all appear in the output.
        Texture texture = new Texture();
        texture.setGuid("g");
        setField(texture, "pointFilter",   true);   // default false → appears
        setField(texture, "flipped",       true);   // default false → appears
        setField(texture, "repeat",        false);  // default true  → appears
        setField(texture, "isNormalMap",   true);   // default false → appears
        setField(texture, "isDataTexture", true);   // default false → appears

        JsonObject json = texture.serializeToJson();

        assertTrue(json.containsKey("pointFilter"),   "pointFilter should appear when non-default");
        assertTrue(json.containsKey("flipped"),       "flipped should appear when non-default");
        assertTrue(json.containsKey("repeat"),        "repeat should appear when non-default");
        assertTrue(json.containsKey("isNormalMap"),   "isNormalMap should appear when non-default");
        assertTrue(json.containsKey("isDataTexture"), "isDataTexture should appear when non-default");
    }

    @Test
    void serializeToJson_defaultBooleanFlagsAbsent() {
        // All flags at their defaults — none should appear in the diff-based output.
        Texture texture = new Texture();
        texture.setGuid("g");

        JsonObject json = texture.serializeToJson();

        // pointFilter=false (default), flipped=false (default), isNormalMap=false (default),
        // isDataTexture=false (default) — all skipped by objectToJson diff logic.
        assertFalse(json.containsKey("pointFilter"),   "pointFilter at default should be absent");
        assertFalse(json.containsKey("flipped"),       "flipped at default should be absent");
        assertFalse(json.containsKey("isNormalMap"),   "isNormalMap at default should be absent");
        assertFalse(json.containsKey("isDataTexture"), "isDataTexture at default should be absent");
        // repeat=true is the default — also absent
        assertFalse(json.containsKey("repeat"),        "repeat at default should be absent");
    }

    // ------------------------------------------------------------------

    private static void setField(Texture texture, String name, boolean value) throws Exception {
        Field f = Texture.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(texture, value);
    }
}
