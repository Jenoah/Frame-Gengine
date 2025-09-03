package nl.framegengine.core.rendering.utils;

import org.joml.Vector3f;

public class FrustumPlane {
    public Vector3f normal = new Vector3f();
    private float d;

    public FrustumPlane(){ }

    public FrustumPlane(Vector3f normal, float d) {
        set(normal, d);
    }

    public void normalize() {
        float length = normal.length();
        normal.normalize();
        d /= length;
    }

    public FrustumPlane set(Vector3f normal, float d){
        this.normal.set(normal);
        this.d = d;
        return this;
    }

    public float getDistanceTo(Vector3f point) {
        return normal.dot(point) + d;
    }

    public boolean isSphereOutside(Vector3f center, float radius) {
        float distance = getDistanceTo(center);
        return distance < -radius;
    }
}
