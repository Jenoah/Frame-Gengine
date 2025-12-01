package nl.framegengine.core.utils;

import nl.framegengine.core.entity.GameObject;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class AABB {
    public final Vector3f min = new Vector3f(0);
    public final Vector3f max = new Vector3f(0);
    private Vector3f size = new Vector3f(0);
    private float length = 1;
    private GameObject parentObject = null;
    private AABB worldAABB = null;

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
        this.size.set(Math.abs(min.x - max.x), Math.abs(min.y - max.y), Math.abs(min.z - max.z));
        this.length = aabb.getLength();
        this.parentObject = aabb.getParentObject();
        return this;
    }

    public AABB offset(Vector3f offset){
        this.min.add(offset);
        this.max.add(offset);

        return this;
    }

    public Vector3f getCenter(){
        return new Vector3f(min).lerp(max, 0.5f);
    }

    public GameObject getParentObject() {
        return parentObject;
    }

    public AABB setParentObject(GameObject parentObject) {
        this.parentObject = parentObject;
        return this;
    }

    public final Vector3f getWorldOffset(){
        if(parentObject != null) return parentObject.getPosition();
        return Constants.VECTOR3_ZERO;
    }

    public AABB toWorld() {
        if (parentObject == null){
            return new AABB(this); // No parent transform
        }

        // Build transformation matrix
        Matrix4f transform = new Matrix4f()
                .identity()
                .translate(parentObject.getPosition())
                .rotate(parentObject.getRotation())
                .scale(parentObject.getScale());

        // Local AABB corners (8 points)
        Vector3f[] corners = new Vector3f[]{
                new Vector3f(min.x, min.y, min.z),
                new Vector3f(min.x, min.y, max.z),
                new Vector3f(min.x, max.y, min.z),
                new Vector3f(min.x, max.y, max.z),
                new Vector3f(max.x, min.y, min.z),
                new Vector3f(max.x, min.y, max.z),
                new Vector3f(max.x, max.y, min.z),
                new Vector3f(max.x, max.y, max.z)
        };

        // Transform and compute new min/max
        Vector3f wmin = new Vector3f(Float.POSITIVE_INFINITY);
        Vector3f wmax = new Vector3f(Float.NEGATIVE_INFINITY);
        Vector3f tmp = new Vector3f();

        for (Vector3f c : corners) {
            transform.transformPosition(c, tmp);

            wmin.min(tmp);
            wmax.max(tmp);
        }

        if(worldAABB == null) worldAABB = new AABB(wmin, wmax).setParentObject(parentObject);

        worldAABB.min.set(wmin);
        worldAABB.max.set(wmax);

        return worldAABB;
    }
}