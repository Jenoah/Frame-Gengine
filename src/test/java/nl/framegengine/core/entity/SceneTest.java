package nl.framegengine.core.entity;

import org.joml.Vector3f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SceneTest {

    private Scene scene;

    @BeforeEach
    void setUp() {
        scene = new Scene(null);
        SceneManager.currentScene = scene;
    }

    @AfterEach
    void tearDown() throws Exception {
        Field instancedObjects = GameObject.class.getDeclaredField("instancedObjects");
        instancedObjects.setAccessible(true);
        ((Hashtable<?, ?>) instancedObjects.get(null)).clear();

        Field parentWhenPresent = GameObject.class.getDeclaredField("parentWhenPresent");
        parentWhenPresent.setAccessible(true);
        ((Hashtable<?, ?>) parentWhenPresent.get(null)).clear();

        SceneManager.currentScene = null;
    }

    // ======================================================================
    // addGameObject (§4.5)
    // ======================================================================

    @Test
    void addGameObject_appearsInGameObjectsAndRootGameObjects() {
        GameObject go = new GameObject("A");

        scene.addGameObject(go);

        assertTrue(scene.getGameObjects().contains(go));
        assertTrue(scene.getRootGameObjects().contains(go));
    }

    @Test
    void addGameObject_withChildHierarchy_allNodesInGameObjects_onlyRootInRootGameObjects() {
        GameObject parent = new GameObject("Parent");
        GameObject child  = new GameObject("Child");
        parent.addChild(child);

        scene.addGameObject(parent);

        assertTrue(scene.getGameObjects().contains(parent));
        assertTrue(scene.getGameObjects().contains(child));
        assertTrue(scene.getRootGameObjects().contains(parent));
        assertFalse(scene.getRootGameObjects().contains(child));
    }

    @Test
    void addGameObject_sameObjectTwice_noDuplicate() {
        GameObject go = new GameObject("Dup");
        scene.addGameObject(go);

        scene.addGameObject(go);

        assertEquals(1, scene.getGameObjects().stream().filter(g -> g == go).count());
    }

    // ======================================================================
    // removeGameObject (§4.5)
    // ======================================================================

    @Test
    void removeGameObject_byReference_removedFromAllLists() {
        GameObject go = new GameObject("ToRemove");
        scene.addGameObject(go);

        scene.removeGameObject(go);

        assertFalse(scene.getGameObjects().contains(go));
        assertFalse(scene.getRootGameObjects().contains(go));
    }

    @Test
    void removeGameObject_byName_findsAndRemovesCorrectObject() {
        GameObject go = new GameObject("Named");
        scene.addGameObject(go);

        GameObject removed = scene.removeGameObject("Named");

        assertSame(go, removed);
        assertFalse(scene.getGameObjects().contains(go));
    }

    @Test
    void removeGameObject_byName_unknownName_returnsNull() {
        GameObject removed = scene.removeGameObject("DoesNotExist");

        assertNull(removed);
    }

    // ======================================================================
    // getGameObjectByName (§4.5)
    // ======================================================================

    @Test
    void getGameObjectByName_returnsCorrectObject() {
        GameObject go = new GameObject("FindMe");
        scene.addGameObject(go);

        assertSame(go, scene.getGameObjectByName("FindMe"));
    }

    @Test
    void getGameObjectByName_unknownName_returnsNull() {
        assertNull(scene.getGameObjectByName("Ghost"));
    }

    // ======================================================================
    // removeFromRoot (§4.5)
    // ======================================================================

    @Test
    void removeFromRoot_removesFromRootGameObjectsOnly() {
        GameObject go = new GameObject("Root");
        scene.addGameObject(go);

        scene.removeFromRoot(go);

        assertFalse(scene.getRootGameObjects().contains(go));
        // Still present in the full list
        assertTrue(scene.getGameObjects().contains(go));
    }

    // ======================================================================
    // processGameObjects (§4.5)
    // ======================================================================

    @Test
    void processGameObjects_rebuildsRootGameObjectsFromGameObjects() {
        GameObject root  = new GameObject("Root");
        GameObject child = new GameObject("Child");
        root.addChild(child);

        // Add both directly to the flat list, bypassing addGameObject logic
        scene.getGameObjects().add(root);
        scene.getGameObjects().add(child);
        // Dirty the root list so we can verify it is rebuilt
        scene.getRootGameObjects().clear();

        scene.processGameObjects();

        assertTrue(scene.getRootGameObjects().contains(root));
        assertFalse(scene.getRootGameObjects().contains(child));
    }

    // ======================================================================
    // VAO id tracking (§4.5)
    // ======================================================================

    @Test
    void addVaoId_hasVaoId_returnsTrue() {
        scene.addVaoId(42);

        assertTrue(scene.hasVaoId(42));
    }

    @Test
    void hasVaoId_absentId_returnsFalse() {
        assertFalse(scene.hasVaoId(99));
    }

    @Test
    void removeVaoId_idNoLongerPresent() {
        scene.addVaoId(7);

        scene.removeVaoId(7);

        assertFalse(scene.hasVaoId(7));
    }

    // ======================================================================
    // serializeToJson (§4.6)
    // ======================================================================

    @Test
    void serializeToJson_containsRequiredTopLevelFields() {
        scene.setLevelName("TestLevel");

        JsonObject json = scene.serializeToJson();

        assertTrue(json.containsKey("levelName"),   "must contain 'levelName'");
        assertTrue(json.containsKey("fogGradient"), "must contain 'fogGradient'");
        assertTrue(json.containsKey("fogDensity"),  "must contain 'fogDensity'");
        assertTrue(json.containsKey("fogColor"),    "must contain 'fogColor'");
        assertTrue(json.containsKey("gameObjects"), "must contain 'gameObjects'");
        assertEquals("TestLevel", json.getString("levelName"));
    }

    @Test
    void serializeToJson_excludesGameObjectsWhereCanBeSavedIsFalse() {
        GameObject saveable   = new GameObject("Saveable");
        GameObject unsaveable = new GameObject("Unsaveable");
        unsaveable.canBeSaved(false);
        scene.addGameObject(saveable);
        scene.addGameObject(unsaveable);

        JsonArray gameObjects = scene.serializeToJson().getJsonArray("gameObjects");

        // Collect names from the serialised array
        long saveableCount   = gameObjects.stream()
                .filter(v -> v.asJsonObject().containsKey("name") &&
                             v.asJsonObject().getString("name").equals("Saveable"))
                .count();
        long unsaveableCount = gameObjects.stream()
                .filter(v -> v.asJsonObject().containsKey("name") &&
                             v.asJsonObject().getString("name").equals("Unsaveable"))
                .count();

        assertEquals(1, saveableCount,   "saveable object must appear in gameObjects array");
        assertEquals(0, unsaveableCount, "unsaveable object must be excluded from gameObjects array");
    }

    // ======================================================================
    // syncSortedGameObjects (§11)
    // ======================================================================

    @Test
    void syncSortedGameObjects_threeObjects_orderedNearestFirst() {
        GameObject near = new GameObject("Near");
        GameObject mid  = new GameObject("Mid");
        GameObject far  = new GameObject("Far");

        near.setRenderCameraSquaredDistance(1.0f);
        mid.setRenderCameraSquaredDistance(4.0f);
        far.setRenderCameraSquaredDistance(9.0f);

        // Add in reverse order to verify sort is actually applied
        scene.addGameObject(far);
        scene.addGameObject(near);
        scene.addGameObject(mid);

        scene.syncSortedGameObjects();

        List<GameObject> sorted = scene.getSortedGameObjects();
        assertSame(near, sorted.get(0));
        assertSame(mid,  sorted.get(1));
        assertSame(far,  sorted.get(2));
    }

    @Test
    void syncSortedGameObjects_twoObjectsEqualDistance_bothPresent() {
        GameObject a = new GameObject("A");
        GameObject b = new GameObject("B");

        a.setRenderCameraSquaredDistance(5.0f);
        b.setRenderCameraSquaredDistance(5.0f);

        scene.addGameObject(a);
        scene.addGameObject(b);

        scene.syncSortedGameObjects();

        List<GameObject> sorted = scene.getSortedGameObjects();
        assertEquals(2, sorted.size());
        assertTrue(sorted.contains(a));
        assertTrue(sorted.contains(b));
    }

    @Test
    void addGameObject_afterSync_newObjectAppearsInSortedList() {
        GameObject first = new GameObject("First");
        first.setRenderCameraSquaredDistance(10.0f);
        scene.addGameObject(first);
        scene.syncSortedGameObjects();

        GameObject lateAdd = new GameObject("LateAdd");
        lateAdd.setRenderCameraSquaredDistance(2.0f);
        scene.addGameObject(lateAdd);

        assertTrue(scene.getSortedGameObjects().contains(lateAdd));
    }

    @Test
    void addGameObject_afterSync_reSyncReordersCorrectly() {
        GameObject first = new GameObject("First");
        first.setRenderCameraSquaredDistance(10.0f);
        scene.addGameObject(first);
        scene.syncSortedGameObjects();

        GameObject lateAdd = new GameObject("LateAdd");
        lateAdd.setRenderCameraSquaredDistance(2.0f);
        scene.addGameObject(lateAdd);

        scene.syncSortedGameObjects();

        List<GameObject> sorted = scene.getSortedGameObjects();
        assertSame(lateAdd, sorted.get(0));
        assertSame(first,   sorted.get(1));
    }
}
