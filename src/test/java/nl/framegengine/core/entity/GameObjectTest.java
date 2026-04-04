package nl.framegengine.core.entity;

import nl.framegengine.core.components.Component;
import nl.framegengine.core.utils.Constants;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.json.JsonObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.*;

class GameObjectTest {

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
    // Transform — root object basics
    // ======================================================================

    @Test
    void rootObject_getPosition_matchesSetPosition() {
        GameObject go = new GameObject();
        go.setPosition(3f, -1f, 7f);

        Vector3f pos = go.getPosition();

        assertEquals(3f,  pos.x, EPSILON);
        assertEquals(-1f, pos.y, EPSILON);
        assertEquals(7f,  pos.z, EPSILON);
    }

    @Test
    void rootObject_getRotation_isIdentityAfterConstruction() {
        GameObject go = new GameObject();

        Quaternionf rot = go.getRotation();

        assertEquals(0f, rot.x, EPSILON);
        assertEquals(0f, rot.y, EPSILON);
        assertEquals(0f, rot.z, EPSILON);
        assertEquals(1f, rot.w, EPSILON);
    }

    @Test
    void rootObject_getScale_isOneOneOneAfterConstruction() {
        GameObject go = new GameObject();

        Vector3f scale = go.getScale();

        assertEquals(1f, scale.x, EPSILON);
        assertEquals(1f, scale.y, EPSILON);
        assertEquals(1f, scale.z, EPSILON);
    }

    @Test
    void setPosition_getLocalPosition_areConsistent() {
        GameObject go = new GameObject();
        go.setPosition(5f, 2f, -3f);

        Vector3f local = go.getLocalPosition();

        assertEquals(5f,  local.x, EPSILON);
        assertEquals(2f,  local.y, EPSILON);
        assertEquals(-3f, local.z, EPSILON);
    }

    // ======================================================================
    // Transform — rotation
    // ======================================================================

    @Test
    void setRotation_eulerVector_roundTripMatchesInput() {
        GameObject go = new GameObject();
        go.setRotation(new Vector3f(30f, 45f, 0f));

        Vector3f euler = go.getLocalEulerAngles();

        assertEquals(30f, euler.x, 0.01f);
        assertEquals(45f, euler.y, 0.01f);
        assertEquals(0f,  euler.z, 0.01f);
    }

    @Test
    void addRotation_accumulatesCorrectly() {
        GameObject go = new GameObject();
        go.setRotation(new Vector3f(0f, 45f, 0f));
        go.addRotation(new Vector3f(0f, 45f, 0f));

        // Euler decomposition of a 90° Y rotation is not stable due to gimbal lock.
        // Assert via the forward vector: 90° Y applied to FORWARD (0,0,-1) → (-1,0,0).
        Vector3f forward = go.getForward();
        assertEquals(-1f, forward.x, 0.01f);
        assertEquals(0f,  forward.y, 0.01f);
        assertEquals(0f,  forward.z, 0.01f);
    }

    @Test
    void lookAt_forwardVectorPointsAwayFromTarget() {
        GameObject go = new GameObject();
        go.setPosition(0f, 0f, 0f);

        go.lookAt(new Vector3f(0f, 0f, -5f));

        // lookAt sets the rotation so FORWARD aligns with (currentPos - target),
        // i.e. (0,0,5) normalised → (0,0,1).
        Vector3f forward = go.getForward();
        assertEquals(0f, forward.x, 0.01f);
        assertEquals(0f, forward.y, 0.01f);
        assertEquals(1f, forward.z, 0.01f);
    }

    @Test
    void identityRotation_directionVectors_areOrthonormal() {
        GameObject go = new GameObject();

        Vector3f forward = go.getForward();
        Vector3f right   = go.getRight();
        Vector3f up      = go.getUp();

        assertEquals(1f, forward.length(), EPSILON);
        assertEquals(1f, right.length(),   EPSILON);
        assertEquals(1f, up.length(),      EPSILON);

        assertEquals(0f, forward.dot(right), EPSILON);
        assertEquals(0f, forward.dot(up),    EPSILON);
        assertEquals(0f, right.dot(up),      EPSILON);

        assertEquals(Constants.VECTOR3_FORWARD.x, forward.x, EPSILON);
        assertEquals(Constants.VECTOR3_FORWARD.y, forward.y, EPSILON);
        assertEquals(Constants.VECTOR3_FORWARD.z, forward.z, EPSILON);
    }

    // ======================================================================
    // Transform — child hierarchy
    // ======================================================================

    @Test
    void child_getPosition_accountsForParentTranslation() {
        GameObject parent = new GameObject();
        GameObject child  = new GameObject();
        parent.setPosition(10f, 0f, 0f);
        child.setPosition(1f, 0f, 0f);
        parent.addChild(child);

        Vector3f worldPos = child.getPosition();

        assertEquals(11f, worldPos.x, EPSILON);
        assertEquals(0f,  worldPos.y, EPSILON);
        assertEquals(0f,  worldPos.z, EPSILON);
    }

    @Test
    void child_getPosition_accountsForParentRotation() {
        GameObject parent = new GameObject();
        GameObject child  = new GameObject();
        parent.setRotation(new Vector3f(0f, 90f, 0f));
        child.setPosition(1f, 0f, 0f);
        parent.addChild(child);

        Vector3f worldPos = child.getPosition();

        assertEquals(0f,  worldPos.x, 0.01f);
        assertEquals(0f,  worldPos.y, 0.01f);
        assertEquals(-1f, worldPos.z, 0.01f);
    }

    @Test
    void child_getScale_accountsForParentScale() {
        GameObject parent = new GameObject();
        GameObject child  = new GameObject();
        parent.setScale(2f);
        child.setScale(3f);
        parent.addChild(child);

        Vector3f worldScale = child.getScale();

        assertEquals(6f, worldScale.x, EPSILON);
        assertEquals(6f, worldScale.y, EPSILON);
        assertEquals(6f, worldScale.z, EPSILON);
    }

    @Test
    void setWorldPosition_childEndsUpAtSpecifiedWorldPosition() {
        GameObject parent = new GameObject();
        GameObject child  = new GameObject();
        parent.setPosition(5f, 0f, 0f);
        parent.addChild(child);

        child.setWorldPosition(new Vector3f(8f, 0f, 0f));

        Vector3f worldPos = child.getPosition();
        assertEquals(8f, worldPos.x, EPSILON);
        assertEquals(0f, worldPos.y, EPSILON);
        assertEquals(0f, worldPos.z, EPSILON);
    }

    // ======================================================================
    // Transform — parent/child wiring
    // ======================================================================

    @Test
    void addChild_childAppearsInChildrenAndParentIsSet() {
        GameObject parent = new GameObject();
        GameObject child  = new GameObject();

        parent.addChild(child);

        assertTrue(parent.getChildren().contains(child));
        assertEquals(parent, child.getParent());
    }

    @Test
    void setParent_oldParentLosesChild_newParentGainsIt() {
        GameObject oldParent = new GameObject();
        GameObject newParent = new GameObject();
        GameObject child     = new GameObject();
        oldParent.addChild(child);

        child.setParent(newParent);

        assertFalse(oldParent.getChildren().contains(child));
        assertTrue(newParent.getChildren().contains(child));
        assertEquals(newParent, child.getParent());
    }

    @Test
    void setParentByGUID_parentAlreadyExists_setsParentImmediately() {
        GameObject parent = new GameObject();
        GameObject child  = new GameObject();

        child.setParentByGUID(parent.getGuid());

        assertEquals(parent, child.getParent());
        assertTrue(parent.getChildren().contains(child));
    }

    @Test
    void setParentByGUID_deferredParenting_resolvesWhenAddWaitingChildrenCalled() throws Exception {
        String futureGuid = "future-parent-guid";
        GameObject child = new GameObject();
        child.setParentByGUID(futureGuid);

        assertNull(child.getParent());

        GameObject parent = new GameObject();
        parent.setGuid(futureGuid);

        Method method = GameObject.class.getDeclaredMethod("addWaitingChildren");
        method.setAccessible(true);
        method.invoke(parent);

        assertEquals(parent, child.getParent());
        assertTrue(parent.getChildren().contains(child));
    }

    @Test
    void isSelfOrChild_returnsTrueForSelfAndDirectChild() {
        GameObject parent = new GameObject();
        GameObject child  = new GameObject();
        GameObject other  = new GameObject();
        parent.addChild(child);

        assertTrue(parent.isSelfOrChild(parent));
        assertTrue(parent.isSelfOrChild(child));
        assertFalse(parent.isSelfOrChild(other));
    }

    @Test
    void getMatrix_decomposeMatchesGetters() {
        GameObject go = new GameObject();
        go.setPosition(1f, 2f, 3f);
        go.setScale(2f, 3f, 4f);

        var matrix = go.getMatrix();

        Vector3f translation = matrix.getTranslation(new Vector3f());
        Vector3f scale       = matrix.getScale(new Vector3f());

        assertEquals(1f, translation.x, EPSILON);
        assertEquals(2f, translation.y, EPSILON);
        assertEquals(3f, translation.z, EPSILON);
        assertEquals(2f, scale.x, EPSILON);
        assertEquals(3f, scale.y, EPSILON);
        assertEquals(4f, scale.z, EPSILON);
    }

    // ======================================================================
    // Component system (§4.2)
    // ======================================================================

    private static class TrackingComponent extends Component {
        int enableCount  = 0;
        int disableCount = 0;

        @Override public void enable()  { super.enable();  enableCount++;  }
        @Override public void disable() { super.disable(); disableCount++; }
    }

    @Test
    void addComponent_isRetrievableByClass() {
        GameObject go = new GameObject();
        TrackingComponent comp = new TrackingComponent();

        go.addComponent(comp);

        assertSame(comp, go.getComponent(TrackingComponent.class));
    }

    @Test
    void addComponent_sameInstanceTwice_returnsNullAndSizeUnchanged() {
        GameObject go = new GameObject();
        TrackingComponent comp = new TrackingComponent();
        go.addComponent(comp);

        Component result = go.addComponent(comp);

        assertNull(result);
        assertEquals(1, go.getComponents().size());
    }

    @Test
    void removeComponent_componentNoLongerInSet() {
        GameObject go = new GameObject();
        TrackingComponent comp = new TrackingComponent();
        go.addComponent(comp);

        go.removeComponent(comp);

        assertFalse(go.getComponents().contains(comp));
    }

    @Test
    void getComponent_unknownType_returnsNull() {
        GameObject go = new GameObject();

        Component result = go.getComponent(TrackingComponent.class);

        assertNull(result);
    }

    @Test
    void setEnabled_false_componentReceivesDisable() {
        GameObject go = new GameObject();
        TrackingComponent comp = new TrackingComponent();
        go.addComponent(comp);

        go.setEnabled(false);

        assertEquals(1, comp.disableCount);
        assertFalse(go.isEnabled());
    }

    @Test
    void setEnabled_true_componentReceivesEnable() {
        GameObject go = new GameObject();
        TrackingComponent comp = new TrackingComponent();
        go.addComponent(comp);
        go.setEnabled(false);

        go.setEnabled(true);

        assertEquals(1, comp.enableCount);
        assertTrue(go.isEnabled());
    }

    @Test
    void callUpdate_propagatesUpParentChain() {
        GameObject parent = new GameObject();
        GameObject child  = new GameObject();
        parent.addChild(child);
        parent.willUpdate = false;

        child.callUpdate();

        assertTrue(parent.willUpdate);
    }

    // ======================================================================
    // GUID registry (§4.3)
    // ======================================================================

    @Test
    void getByGUID_findsObjectByAutoGeneratedGuid() {
        GameObject go = new GameObject();

        assertSame(go, GameObject.getByGUID(go.getGuid()));
    }

    @Test
    void setGuid_updatesRegistry_oldGuidRemoved() {
        GameObject go = new GameObject();
        String oldGuid = go.getGuid();

        go.setGuid("new-guid");

        assertNull(GameObject.getByGUID(oldGuid));
        assertSame(go, GameObject.getByGUID("new-guid"));
    }

    @Test
    void remove_guidRemovedFromRegistry() {
        GameObject go = new GameObject();
        String guid = go.getGuid();

        go.remove();

        assertNull(GameObject.getByGUID(guid));
    }

    // ======================================================================
    // Serialization (§4.4)
    // ======================================================================

    @Test
    void serializeToJson_containsGuid() {
        GameObject go = new GameObject("TestObject");

        JsonObject json = go.serializeToJson();

        assertTrue(json.containsKey("guid"), "json must contain 'guid'");
        assertEquals(go.getGuid(), json.getString("guid"));
    }

    @Test
    void serializeToJson_containsNonDefaultName() {
        GameObject go = new GameObject("MyObject");

        JsonObject json = go.serializeToJson();

        // name differs from default ("GameObject") so it must be present
        assertTrue(json.containsKey("name"), "json must contain 'name' when it differs from default");
        assertEquals("MyObject", json.getString("name"));
    }

    @Test
    void serializeToJson_containsNonDefaultLocalPosition() {
        GameObject go = new GameObject();
        go.setPosition(1f, 2f, 3f);

        JsonObject json = go.serializeToJson();

        assertTrue(json.containsKey("localPosition"), "json must contain 'localPosition' when non-zero");
        JsonObject pos = json.getJsonObject("localPosition");
        assertEquals(1f, (float) pos.getJsonNumber("x").doubleValue(), EPSILON);
        assertEquals(2f, (float) pos.getJsonNumber("y").doubleValue(), EPSILON);
        assertEquals(3f, (float) pos.getJsonNumber("z").doubleValue(), EPSILON);
    }

    @Test
    void serializeToJson_containsNonDefaultScale() {
        GameObject go = new GameObject();
        go.setScale(2f, 3f, 4f);

        JsonObject json = go.serializeToJson();

        assertTrue(json.containsKey("scale"), "json must contain 'scale' when non-default");
        JsonObject scale = json.getJsonObject("scale");
        assertEquals(2f, (float) scale.getJsonNumber("x").doubleValue(), EPSILON);
        assertEquals(3f, (float) scale.getJsonNumber("y").doubleValue(), EPSILON);
        assertEquals(4f, (float) scale.getJsonNumber("z").doubleValue(), EPSILON);
    }

    @Test
    void serializeToJson_containsNonDefaultLocalRotation() {
        GameObject go = new GameObject();
        go.setRotation(new Vector3f(0f, 90f, 0f));

        JsonObject json = go.serializeToJson();

        assertTrue(json.containsKey("localRotation"), "json must contain 'localRotation' when non-identity");
    }

    @Test
    void serializeToJson_omitsFieldsEqualToDefault() {
        // Default name, zero position, identity rotation, unit scale
        GameObject go = new GameObject();

        JsonObject json = go.serializeToJson();

        // name=="GameObject" matches default → must be absent
        assertFalse(json.containsKey("name"), "default name must be omitted");
        // (0,0,0) matches default → absent
        assertFalse(json.containsKey("localPosition"), "default position must be omitted");
        // identity quaternion matches default → absent
        assertFalse(json.containsKey("localRotation"), "default rotation must be omitted");
        // (1,1,1) matches default → absent
        assertFalse(json.containsKey("scale"), "default scale must be omitted");
    }

    @Test
    void serializeToJson_withParent_containsParentGuid() {
        GameObject parent = new GameObject();
        GameObject child  = new GameObject();
        parent.addChild(child);

        JsonObject json = child.serializeToJson();

        assertTrue(json.containsKey("parentGuid"), "json must contain 'parentGuid' when object has a parent");
        assertEquals(parent.getGuid(), json.getString("parentGuid"));
    }

    @Test
    void deserializeFromJson_restoresName() {
        GameObject source = new GameObject("RestoredName");
        String json = source.serializeToJson().toString();

        GameObject target = new GameObject();
        target.deserializeFromJson(json);

        assertEquals("RestoredName", target.getName());
    }

    @Test
    void deserializeFromJson_restoresPosition() {
        GameObject source = new GameObject();
        source.setPosition(4f, 5f, 6f);
        String json = source.serializeToJson().toString();

        GameObject target = new GameObject();
        target.deserializeFromJson(json);

        Vector3f pos = target.getLocalPosition();
        assertEquals(4f, pos.x, EPSILON);
        assertEquals(5f, pos.y, EPSILON);
        assertEquals(6f, pos.z, EPSILON);
    }

    @Test
    void roundTrip_preservesAllTransformFields() {
        GameObject source = new GameObject("RoundTrip");
        source.setPosition(1f, -2f, 3f);
        source.setScale(2f, 0.5f, 1.5f);
        source.setRotation(new Vector3f(30f, 45f, 0f));

        String json = source.serializeToJson().toString();

        GameObject target = new GameObject();
        target.deserializeFromJson(json);

        assertEquals("RoundTrip", target.getName());

        Vector3f pos = target.getLocalPosition();
        assertEquals(1f,  pos.x, EPSILON);
        assertEquals(-2f, pos.y, EPSILON);
        assertEquals(3f,  pos.z, EPSILON);

        Vector3f scale = target.getLocalScale();
        assertEquals(2f,   scale.x, EPSILON);
        assertEquals(0.5f, scale.y, EPSILON);
        assertEquals(1.5f, scale.z, EPSILON);

        // Compare via forward vector rather than Euler angles to avoid gimbal ambiguity
        Vector3f srcForward = source.getForward();
        Vector3f dstForward = target.getForward();
        assertEquals(srcForward.x, dstForward.x, 0.01f);
        assertEquals(srcForward.y, dstForward.y, 0.01f);
        assertEquals(srcForward.z, dstForward.z, 0.01f);
    }

    @Test
    void roundTrip_withParentGuid_parentRelationRestored() {
        GameObject parent = new GameObject("Parent");
        GameObject child  = new GameObject("Child");
        parent.addChild(child);

        // Serialize child — will contain parentGuid
        String childJson = child.serializeToJson().toString();

        // Deserialize into a fresh object; parent is already in the registry
        GameObject restoredChild = new GameObject();
        restoredChild.deserializeFromJson(childJson);

        assertEquals(parent, restoredChild.getParent());
    }

    // ======================================================================
    // renderCameraSquaredDistance (§11)
    // ======================================================================

    @Test
    void setRenderCameraSquaredDistance_float_getterReturnsSetValue() {
        GameObject go = new GameObject();

        go.setRenderCameraSquaredDistance(25.0f);

        assertEquals(25.0f, go.getRenderCameraSquaredDistance(), EPSILON);
    }

    @Test
    void setRenderCameraSquaredDistance_float_overwritesPreviousValue() {
        GameObject go = new GameObject();
        go.setRenderCameraSquaredDistance(100.0f);

        go.setRenderCameraSquaredDistance(0.0f);

        assertEquals(0.0f, go.getRenderCameraSquaredDistance(), EPSILON);
    }

    @Test
    void setRenderCameraSquaredDistance_vector_storesCorrectSquaredDistance() {
        // Object at (3, 4, 0), camera at origin — squared distance = 3²+4²+0² = 25
        GameObject go = new GameObject();
        go.setPosition(3.0f, 4.0f, 0.0f);

        go.setRenderCameraSquaredDistance(new Vector3f(0, 0, 0));

        assertEquals(25.0f, go.getRenderCameraSquaredDistance(), 1e-4f);
    }

    @Test
    void setRenderCameraSquaredDistance_vector_cameraAtSamePosition_storesZero() {
        GameObject go = new GameObject();
        go.setPosition(5.0f, 5.0f, 5.0f);

        go.setRenderCameraSquaredDistance(new Vector3f(5.0f, 5.0f, 5.0f));

        assertEquals(0.0f, go.getRenderCameraSquaredDistance(), EPSILON);
    }
}
