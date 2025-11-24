package nl.framegengine.core.utils;

import nl.framegengine.core.entity.GameObject;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class AABB {
    public final Vector3f min = new Vector3f(0);
    public final Vector3f max = new Vector3f(0);
    private final Vector3f size = new Vector3f(0);
    private final Vector3f center = new Vector3f(0);
    private float length = 1;
    private GameObject parentObject = null;

    public AABB(Vector3f min, Vector3f max) {
        this.min.set(min);
        this.max.set(max);
        this.size.set(Math.abs(min.x - max.x), Math.abs(min.y - max.y), Math.abs(min.z - max.z));
        this.length = Vector3f.distance(min.x, min.y, min.z, max.x, max.y, max.z);
    }

    public AABB(AABB aabb) {
        set(aabb);
    }

    public AABB() { }

    public final Vector3f getSize(){
        return this.size;
    }

    public final float getLength(){
        return length;
    }

    public AABB set(AABB aabb){
        this.min.set(aabb.min);
        this.max.set(aabb.max);
        this.size.set(aabb.getSize());
        this.length = aabb.getLength();
        this.parentObject = aabb.getParentObject();
        return this;
    }

    public AABB offset(Vector3f offset){
        this.min.add(offset);
        this.max.add(offset);

        return this;
    }

    public AABB recalculate(Quaternionf rotation) {
        Vector3f[] corners = new Vector3f[8];
        corners[0] = new Vector3f(min.x, min.y, min.z);
        corners[1] = new Vector3f(max.x, min.y, min.z);
        corners[2] = new Vector3f(max.x, max.y, min.z);
        corners[3] = new Vector3f(min.x, max.y, min.z);
        corners[4] = new Vector3f(min.x, min.y, max.z);
        corners[5] = new Vector3f(max.x, min.y, max.z);
        corners[6] = new Vector3f(max.x, max.y, max.z);
        corners[7] = new Vector3f(min.x, max.y, max.z);

        Vector3f newMin = new Vector3f(Float.MAX_VALUE);
        Vector3f newMax = new Vector3f(Float.MIN_VALUE);

        for (Vector3f corner : corners) {
            rotation.transform(corner);
            newMin.min(corner);
            newMax.max(corner);
        }

        this.min.set(newMin);
        this.max.set(newMax);
        this.size.set(Math.abs(min.x - max.x), Math.abs(min.y - max.y), Math.abs(min.z - max.z));
        this.length = Vector3f.distance(min.x, min.y, min.z, max.x, max.y, max.z);
        return this;
    }

    public final Vector3f getCenter(){
        min.lerp(max, 0.5f, center);
        return center;
    }

    public GameObject getParentObject() {
        return parentObject;
    }

    public void setParentObject(GameObject parentObject) {
        this.parentObject = parentObject;
    }

    public final Vector3f getWorldOffset(){
        if(parentObject != null) return parentObject.getPosition();
        return Constants.VECTOR3_ZERO;
    }
}