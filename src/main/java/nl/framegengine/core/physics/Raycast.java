package nl.framegengine.core.physics;

import nl.framegengine.core.engine.WindowManager;
import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.entity.GameObject;
import nl.framegengine.core.input.MouseInput;
import nl.framegengine.core.utils.AABB;
import nl.framegengine.core.utils.ObjectPool;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Raycast {
    public static class Ray {
        public final Vector3f origin = new Vector3f();
        public final Vector3f direction = new Vector3f();

        public Ray(Vector3f origin, Vector3f direction) {
            this.origin.set(origin);
            this.direction.set(direction);
        }
    }

    public static Ray fromCameraByMouse(Camera camera){
        Vector2f mousePosition = new Vector2f(MouseInput.getMousePositionInPixels());

        //Convert to normalized coordinates
        mousePosition.x = (2f * mousePosition.x) / WindowManager.getInstance().getWidth() - 1f;
        mousePosition.y = 1f - (2f * mousePosition.y) / WindowManager.getInstance().getHeight();

        Matrix4f invertedProjectionMatrix = ObjectPool.MATRIX4F_OBJECT_POOL.obtain().set(WindowManager.getInstance().getProjectionMatrix()).invert();
        Matrix4f invertedViewMatrix = ObjectPool.MATRIX4F_OBJECT_POOL.obtain().set(camera.getViewMatrix()).invert();

        Vector4f nearPoint = new Vector4f(mousePosition.x, mousePosition.y, -1f, 1f); // Near plane
        Vector4f farPoint = new Vector4f(mousePosition.x, mousePosition.y, 1f, 1f);   // Far plane

        invertedProjectionMatrix.transform(nearPoint);
        invertedProjectionMatrix.transform(farPoint);

        nearPoint.div(nearPoint.w);
        farPoint.div(farPoint.w);

        invertedViewMatrix.transform(nearPoint);
        invertedViewMatrix.transform(farPoint);

        ObjectPool.MATRIX4F_OBJECT_POOL.free(invertedProjectionMatrix);
        ObjectPool.MATRIX4F_OBJECT_POOL.free(invertedViewMatrix);

        Vector3f origin = ObjectPool.VECTOR3F_POOL.obtain();
        Vector3f target = ObjectPool.VECTOR3F_POOL.obtain();
        Vector3f rayDirection = ObjectPool.VECTOR3F_POOL.obtain();

        nearPoint.xyz(origin);
        farPoint.xyz(target);

        Vector3f tempTargetOriginDifference = target.sub(origin);

        rayDirection.set(tempTargetOriginDifference.normalize());
        Ray ray = new Ray(camera.getPosition(), rayDirection);

        ObjectPool.VECTOR3F_POOL.free(origin);
        ObjectPool.VECTOR3F_POOL.free(target);
        ObjectPool.VECTOR3F_POOL.free(rayDirection);

        return ray;
    }

    public static boolean intersectFromMouse(Camera camera, GameObject gameObject) {
        return intersectFromMouse(camera, gameObject.getAabb());
    }

    public static boolean intersectFromMouse(Camera camera, AABB aabb) {
        if(camera == null || aabb == null) return false;
        Ray ray = fromCameraByMouse(camera);

        //Set aabb of object that is being tested to the testingAABB to allow for converting to world position
        AABB worldAABB = new AABB(aabb);
        worldAABB.offset(aabb.getWorldOffset());

        // Initialize tMin and tMax to track the intersection distances along the ray
        float tMin = Float.NEGATIVE_INFINITY;
        float tMax = Float.POSITIVE_INFINITY;

        // Iterate through each axis (x, y, z)
        for (int i = 0; i < 3; i++) {
            float rayOrigin = ray.origin.get(i);
            float rayDirection = ray.direction.get(i);

            // Get slab bounds for current axis
            float slabMin = worldAABB.min.get(i);
            float slabMax = worldAABB.max.get(i);

            // Check if ray is parallel to the slab (rayDirection == 0)
            if (rayDirection == 0) {
                if (rayOrigin < slabMin || rayOrigin > slabMax) {
                    return false;  // No intersection if ray is outside of bounds
                }
            } else {
                // Calculate intersection points (near and far)
                float t1 = (slabMin - rayOrigin) / rayDirection;
                float t2 = (slabMax - rayOrigin) / rayDirection;

                // Ensure t1 is the smaller and t2 is the larger value
                if (t1 > t2) {
                    float temp = t1;
                    t1 = t2;
                    t2 = temp;
                }

                // Update tMin and tMax to reflect the valid range of intersection
                tMin = Math.max(tMin, t1);
                tMax = Math.min(tMax, t2);

                // Early exit if the ray misses the slab in any dimension
                if (tMin > tMax) {
                    return false;
                }
            }
        }

        // If tMin < tMax, the ray intersects the slab
        return tMin <= tMax;
    }
}
