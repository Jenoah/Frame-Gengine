package nl.framegengine.core.components;

import nl.framegengine.core.entity.GameObject;
import nl.framegengine.core.entity.SceneManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.json.JsonObject;
import java.lang.reflect.Field;
import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.*;

class ComponentTest {

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
    // initiate()
    // ======================================================================

    @Test
    void initiate_hasInitiatedIsFalseBeforeFirstCall() {
        TestComponent c = new TestComponent();

        assertFalse(c.hasInitiated);
    }

    @Test
    void initiate_setsHasInitiatedTrueOnFirstCall() {
        TestComponent c = new TestComponent();

        c.initiate();

        assertTrue(c.hasInitiated);
    }

    @Test
    void initiate_secondCallIsIdempotent() {
        TestComponent c = new TestComponent();
        c.initiate();

        c.initiate(); // must not throw, state must remain true

        assertTrue(c.hasInitiated);
    }

    // ======================================================================
    // enable() / disable()
    // ======================================================================

    @Test
    void disable_thenEnable_togglesIsEnabled() throws Exception {
        TestComponent c = new TestComponent();
        GameObject root = new GameObject();
        c.setRoot(root);

        c.disable();
        assertFalse(getIsEnabled(c), "should be disabled after disable()");

        c.enable();
        assertTrue(getIsEnabled(c), "should be enabled after enable()");
    }

    // ======================================================================
    // getEnabled()
    // ======================================================================

    @Test
    void getEnabled_rootAndComponentBothEnabled_returnsTrue() {
        TestComponent c = new TestComponent();
        GameObject root = new GameObject();
        c.setRoot(root);
        // Both default to enabled

        assertTrue(c.getEnabled());
    }

    @Test
    void getEnabled_rootDisabled_returnsFalseEvenIfComponentEnabled() {
        TestComponent c = new TestComponent();
        GameObject root = new GameObject();
        c.setRoot(root);
        root.setEnabled(false);

        assertFalse(c.getEnabled());
    }

    @Test
    void getEnabled_componentDisabled_returnsFalseEvenIfRootEnabled() {
        TestComponent c = new TestComponent();
        GameObject root = new GameObject();
        c.setRoot(root);
        c.disable();

        assertFalse(c.getEnabled());
    }

    // ======================================================================
    // setRoot / getRoot
    // ======================================================================

    @Test
    void setRoot_getRoot_roundTrip() {
        TestComponent c = new TestComponent();
        GameObject root = new GameObject();

        c.setRoot(root);

        assertSame(root, c.getRoot());
    }

    // ======================================================================
    // serializeToJson()
    // ======================================================================

    @Test
    void serializeToJson_doesNotContainHasInitiated() {
        TestComponent c = new TestComponent();
        c.initiate(); // set hasInitiated = true so it would appear if not excluded

        JsonObject json = c.serializeToJson();

        assertFalse(json.containsKey("hasInitiated"),
                "hasInitiated must be excluded from serialized output");
    }

    // ======================================================================
    // Helper
    // ======================================================================

    private static boolean getIsEnabled(Component c) throws Exception {
        Field f = Component.class.getDeclaredField("isEnabled");
        f.setAccessible(true);
        return (boolean) f.get(c);
    }
}
