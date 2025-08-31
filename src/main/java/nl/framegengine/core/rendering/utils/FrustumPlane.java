package nl.framegengine.core.rendering.utils;

import org.joml.Vector3f;

public class FrustumPlane {
    public Vector3f normal;
    private float d;

    public FrustumPlane(Vector3f normal, float d) {
        this.normal = normal;
        this.d = d;
    }

    public void normalize() {
        float length = normal.length();
        normal.normalize();
        d /= length;
    }

    public float getDistanceTo(Vector3f point) {
        return normal.dot(point) + d;
    }

    public boolean isSphereOutside(Vector3f center, float radius) {
        float distance = getDistanceTo(center);
        return distance < -radius;
    }
}
