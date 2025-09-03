package nl.framegengine.core.entity;

import nl.framegengine.core.utils.IJsonSerializable;
import nl.framegengine.core.components.Component;
import nl.framegengine.core.components.RenderComponent;
import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.rendering.RenderManager;
import nl.framegengine.core.utils.AABB;
import nl.framegengine.core.utils.Constants;
import nl.framegengine.core.utils.JsonHelper;
import nl.framegengine.core.utils.ObjectPool;
import org.joml.*;
import org.joml.Math;

import javax.json.*;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class GameObject implements IJsonSerializable {
    private String name = "GameObject";
    private final Vector3f localPosition = new Vector3f();
    private final Quaternionf localRotation = new Quaternionf();
    private final Vector3f scale = new Vector3f(1f);
    private float radius = 1f;
    private AABB aabb;
    private final Vector3f center = new Vector3f(0);
    private String guid;
    private boolean isEnabled = true;
    private boolean isStatic = false;
    protected boolean drawDebugWireframe = false;

    private static final Dictionary<String, GameObject> instancedObjects = new Hashtable<>();

    private final List<GameObject> children;
    private GameObject parent;

    protected boolean willUpdate = false;
    private boolean isRemoving = false;

    protected final Set<Component> components = new HashSet<>();

    public GameObject() {
        this.children = new ArrayList<>();
        setGuid();
        instancedObjects.put(getGuid(), this);
    }

    public GameObject(String name){
        this.name = name;
        this.children = new ArrayList<>();
        setGuid();
        instancedObjects.put(getGuid(), this);
    }

    public void initiate(){
        for (Component component : components){
            component.initiate();
        }
    }

    public Vector3f getPosition() {
        if (parent == null) {
            return new Vector3f(localPosition);
        } else {
            Quaternionf parentRotation = parent.getRotation();
            Vector3f rotatedPosition = new Vector3f(localPosition).rotate(parentRotation);
            return new Vector3f(parent.getPosition()).add(rotatedPosition);
        }
    }

    public Vector3f getLocalPosition(){
        return localPosition;
    }

    public GameObject setPosition(Vector3f position) {
        this.localPosition.set(position);
        callUpdate();
        return this;
    }

    public GameObject setPosition(float x, float y) {
        this.localPosition.set(x, y, 0);
        callUpdate();
        return this;
    }

    public GameObject setPosition(float x, float y, float z) {
        this.localPosition.set(x, y, z);
        callUpdate();
        return this;
    }

    public void setWorldPosition(Vector3f worldPosition) {
        if (parent == null) {
            setPosition(worldPosition);
        } else {
            Vector3f parentWorldPos = parent.getPosition();
            Quaternionf parentWorldRot = parent.getRotation();

            Vector3f relativePos = ObjectPool.VECTOR3F_POOL.obtain().set(worldPosition).sub(parentWorldPos);
            relativePos.rotate(parentWorldRot.conjugate()); // inverse rotate to get local position

            setPosition(relativePos);
            ObjectPool.VECTOR3F_POOL.free(relativePos);
        }
        callUpdate();
    }

    public GameObject translateLocal(Vector3f position){
        this.localPosition.add(position);
        callUpdate();
        return this;
    }

    public Quaternionf getRotation() {
        if (parent == null) {
            return new Quaternionf(localRotation);
        } else {
            return new Quaternionf(parent.getRotation()).mul(localRotation);
        }
    }

    public Vector3f getEulerAngles(){
        Vector3f eulerAngles = new Vector3f();
        getRotation().getEulerAnglesXYZ(eulerAngles);
        eulerAngles.mul((float) Math.toDegrees(1));
        return eulerAngles;
    }

    public Vector3f getLocalEulerAngles(){
        Vector3f eulerAngles = new Vector3f();
        localRotation.getEulerAnglesXYZ(eulerAngles);
        eulerAngles.mul((float) Math.toDegrees(1));
        return eulerAngles;
    }

    public Quaternionf getLocalRotation(){
        return new Quaternionf(localRotation);
    }

    public Vector3f getForward(){
        return new Quaternionf(getRotation()).transform(new Vector3f(Constants.VECTOR3_FORWARD));
    }

    public Vector3f getRight(){
        return new Quaternionf(getRotation()).transform(new Vector3f(Constants.VECTOR3_RIGHT));
    }

    public Vector3f getUp(){
        return new Quaternionf(getRotation()).transform(new Vector3f(Constants.VECTOR3_UP));
    }

    public GameObject setRotation(Quaternionf rotation){
        this.localRotation.set(rotation);
        callUpdate();
        return this;
    }

    public GameObject setWorldRotation(Quaternionf worldRotation) {
        if (parent == null) {
            setRotation(worldRotation);
        } else {
            Quaternionf parentWorldRot = parent.getRotation();
            Quaternionf inverseParentRot = parentWorldRot.conjugate();

            Quaternionf localRot = ObjectPool.QUATERNIONF_OBJECT_POOL.obtain().set(inverseParentRot).mul(worldRotation);
            setRotation(localRot);
            ObjectPool.QUATERNIONF_OBJECT_POOL.free(localRot);
        }
        callUpdate();
        return this;
    }

    public GameObject setRotation(Vector3f rotation) {
        Vector3f radians = ObjectPool.VECTOR3F_POOL.obtain().set(rotation).mul((float) Math.toRadians(1));
        localRotation.identity().rotateXYZ(radians.x, radians.y, radians.z).normalize();
        ObjectPool.VECTOR3F_POOL.free(radians);
        callUpdate();
        return this;
    }

    public GameObject addRotation(Vector3f rotation){
        Vector3f radians = new Vector3f(rotation).mul((float) Math.toRadians(1));

        localRotation.mul(
                new Quaternionf().identity().rotateXYZ(
                        radians.x,
                        radians.y,
                        radians.z
                )).normalize();
        callUpdate();
        return this;
    }

    public GameObject addRotation(Quaternionf rotation){
        localRotation.mul(rotation).normalize();
        callUpdate();
        return this;
    }

    public GameObject lookAt(Vector3f target){
        Vector3f currentPosition = getPosition();
        Vector3f forward = ObjectPool.VECTOR3F_POOL.obtain().set(currentPosition).sub(target).normalize();

        if (forward.lengthSquared() < 1e-6) return this;

        Quaternionf quaternion = ObjectPool.QUATERNIONF_OBJECT_POOL.obtain().identity().rotationTo(Constants.VECTOR3_FORWARD, forward);
        setRotation(quaternion);
        ObjectPool.VECTOR3F_POOL.free(forward);
        ObjectPool.QUATERNIONF_OBJECT_POOL.free(quaternion);
        callUpdate();
        return this;
    }

    public void lookAtDirection(Vector3f direction) {
        direction.normalize();
        if(direction.lengthSquared() <= 0.01) direction.set(0, 0, -1);
        setRotation(new Quaternionf().rotateTo(new Vector3f(Constants.VECTOR3_FORWARD), direction));
        callUpdate();
    }

    public void lookAtDirection(Quaternionf direction) {
        direction.normalize();
        Vector3f targetRotation = ObjectPool.VECTOR3F_POOL.obtain();
        direction.getEulerAnglesXYZ(targetRotation);
        if(targetRotation.lengthSquared() <= 0.01) targetRotation.set(0, 0, -1);
        setRotation(new Quaternionf().rotateTo(new Vector3f(Constants.VECTOR3_FORWARD), targetRotation));
        ObjectPool.VECTOR3F_POOL.free(targetRotation);
        callUpdate();
    }

    public Vector3f getScale() {
        return scale;
    }

    public GameObject setScale(float scale) {
        this.scale.set(scale);
        if(aabb != null && getComponent(RenderComponent.class) != null) getComponent(RenderComponent.class).calculateAABB();

        callUpdate();
        return this;
    }

    public GameObject setScale(Vector3f scale) {
        this.scale.set(scale);
        if(aabb != null && getComponent(RenderComponent.class) != null) getComponent(RenderComponent.class).calculateAABB();
        callUpdate();
        return this;
    }

    public GameObject setScale(float x, float y, float z) {
        this.scale.set(x, y, z);
        if(aabb != null && getComponent(RenderComponent.class) != null) getComponent(RenderComponent.class).calculateAABB();

        callUpdate();
        return this;
    }

    public GameObject setScale(float x, float y) {
        this.scale.set(x, y, 0);
        if(aabb != null && getComponent(RenderComponent.class) != null) getComponent(RenderComponent.class).calculateAABB();
        callUpdate();
        return this;
    }

    public GameObject addChild(GameObject child){
        if (child.parent != null) {
            child.parent.children.remove(child);
        }
        child.parent = this;
        this.children.add(child);
        if(SceneManager.currentScene != null) SceneManager.currentScene.removeFromRoot(child);
        return this;
    }

    public GameObject getChild(int childIndex){
        return children.get(childIndex);
    }

    public List<GameObject> getChildren(){
        return children;
    }

    public GameObject setParent(GameObject parent){
        if (this.parent != null) {
            this.parent.children.remove(this);
        }
        this.parent = parent;
        if (parent != null) {
            parent.children.add(this);
            if(SceneManager.currentScene != null) SceneManager.currentScene.removeFromRoot(this);
        }else{
            if(SceneManager.currentScene != null) SceneManager.currentScene.getRootGameObjects().add(this);
        }
        return this;
    }

    public GameObject getParent(){
        return this.parent;
    }

    public void update(){
        if(drawDebugWireframe && aabb != null && RenderManager.getInstance() != null){
            AABB worldAABB = new AABB(getAabb()).offset(getPosition());
            RenderManager.getInstance().debugCube(worldAABB.getCenter(), getRotation(), worldAABB.getSize());
        }

        if(!isEnabled || components.isEmpty()) return;
        for(Component component : components) component.update();
    }

    public boolean isEnabled() {
        if(parent != null){
            return isEnabled && parent.isEnabled();
        }
        return isEnabled;
    }

    public GameObject setEnabled(boolean enabled) {
        isEnabled = enabled;
        if(!isEnabled) onDisable();
        if(isEnabled) onEnable();
        return this;
    }

    protected GameObject onDisable(){
        components.forEach((Component::disable));
        children.forEach((GameObject::onDisable));
        return this;
    }

    protected GameObject onEnable(){
        components.forEach((Component::enable));
        children.forEach((GameObject::onEnable));
        return this;
    }

    public void cleanUp(){
        components.forEach(Component::cleanUp);
        children.forEach(child -> child.components.forEach(Component::cleanUp));
    }

    public Set<Component> getComponents(){
        return components;
    }

    @SuppressWarnings("unchecked")
    public <C extends Component> C getComponent(Class<C> componentClass) {
        for (Component c : components) {
            if (componentClass.isInstance(c)) {
                return (C) c;
            }
        }
        return null;
    }

    public Component addComponent(Component component){
        if(this.components.contains(component)){
            Debug.log("GameObject already contains component of type " + component.getClass().getSimpleName());
            return null;
        }

        component.setRoot(this);
        this.components.add(component);
        return component;
    }

    public void removeComponent(Component component){
        if(!this.components.contains(component)){
            Debug.log("GameObject does not contains component of type " + component.getClass().getSimpleName());
            return;
        }

        component.disable();
        component.cleanUp();
        this.components.remove(component);
    }

    public boolean isStatic() {
        return isStatic;
    }

    public GameObject setStatic(boolean isStatic) {
        this.isStatic = isStatic;
        RenderComponent renderComponent = getComponent(RenderComponent.class);
        if (renderComponent != null) {
            renderComponent.getMeshMaterialSets().forEach(meshMaterialSet ->
                    meshMaterialSet.getMesh().setStatic(isStatic)
            );
        }
        return this;
    }

    @Override
    public String toString(){
        return this.getClass().getSimpleName();
    }

    public void callUpdate(){
        this.willUpdate = true;
    }

    protected void onUpdateTransform(){
        children.forEach(go -> {
            go.callUpdate();
            go.onUpdateTransform();
        });
    }

    public final float getRadius(){
        return radius;
    }

    public void setRadius(float radius){
        this.radius = radius;
    }

    public final Vector3f getCenter() {
        return center;
    }

    public void setCenter(Vector3f center) {
        this.center.set(center);
    }

    public AABB getAabb() {
        return aabb;
    }

    public void setAabb(AABB aabb) {
        this.aabb = aabb;
    }

    public final String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDrawDebugWireframe(boolean drawDebugWireframe) {
        this.drawDebugWireframe = drawDebugWireframe;
    }

    public final boolean isDrawDebugWireframe() {
        return drawDebugWireframe;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(){
        setGuid(String.valueOf(java.util.UUID.randomUUID()));
    }

    public void setGuid(String guid) {
        this.guid = guid;
        instancedObjects.remove(getGuid());
        instancedObjects.put(getGuid(), this);
    }

    public static GameObject getByGUID(String guid){
        return instancedObjects.get(guid);
    }

    public void remove(){
        if(isRemoving) return;
        isRemoving = true;

        for (Component component : components.stream().toList()) {
            removeComponent(component);
        }
        GameObject.instancedObjects.remove(guid);
        if(SceneManager.currentScene != null){
            SceneManager.currentScene.removeFromRoot(this);
            SceneManager.currentScene.removeGameObject(this);
        }
    }

    @Override
    public JsonObject serializeToJson() {
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        JsonObject jsonObject = JsonHelper.objectToJson(this, new String[]{"children", "parent", "instancedObjects"});
        jsonObject.forEach(jsonObjectBuilder::add);
        if(parent != null) jsonObjectBuilder.add("parentGuid", Json.createValue(parent.guid));
        return jsonObjectBuilder.build();
    }

    @Override
    public IJsonSerializable deserializeFromJson(String json) {
        JsonReader jsonReader = Json.createReader(new StringReader(json));
        JsonObject jsonInfo = jsonReader.readObject();
        if(JsonHelper.hasJsonKey(jsonInfo, "guid")) setGuid(jsonInfo.getString("guid"));
        try {
            JsonHelper.loadVariableIntoObject(this, jsonInfo);
        } catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        if(JsonHelper.hasJsonKey(jsonInfo, "parentGuid")) setParent(GameObject.getByGUID(jsonInfo.getString("parentGuid")));
        return this;
    }
}
