package nl.framegengine.core.physics;

import nl.framegengine.core.engine.WindowManager;
import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.entity.GameObject;
import nl.framegengine.core.entity.SceneManager;
import nl.framegengine.core.input.MouseInput;
import nl.framegengine.core.utils.AABB;
import nl.framegengine.core.utils.Calculus;
import nl.framegengine.core.utils.ObjectPool;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;

public class Raycast {
    public static class Ray {
        public final Vector3f origin = new Vector3f();
        public final Vector3f direction = new Vector3f();

        public Ray(Vector3f origin, Vector3f direction) {
            this.origin.set(origin);
            this.direction.set(direction);
        }

        public Ray set(Ray ray){
            this.origin.set(ray.origin);
            this.direction.set(ray.direction);
            return this;
        }

        @Override
        public String toString() {
            return "Origin: " + origin.x + ", " + origin.y + ", " + origin.z + " || Direction: " + direction.x + ", " + direction.y + ", " + direction.z;
        }
    }

    public static class RayHit {
        public final Vector3f locationWorldSpace = new Vector3f();
        public GameObject gameObject = null;

        public RayHit(Vector3f locationWorldSpace, GameObject hitObject) {
            this.locationWorldSpace.set(locationWorldSpace);
            this.gameObject = hitObject;
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

    public static boolean intersectRay(Ray ray, GameObject gameObject) {
        Float hitValue = intersectRay(ray, gameObject.getAabb());
        return hitValue != null && hitValue > 0;
    }

    public static boolean intersectFromMouse(Camera camera, GameObject gameObject) {
        Ray ray = fromCameraByMouse(camera);
        Float hitValue = intersectRay(ray, gameObject.getAabb());
        return hitValue != null && hitValue > 0;
    }

    public static Float intersectRay(Ray ray, AABB aabb) {
        if (aabb == null) return null;

        AABB worldAABB = new AABB(aabb);
        worldAABB.offset(aabb.getWorldOffset());

        float tMin = Float.NEGATIVE_INFINITY;
        float tMax = Float.POSITIVE_INFINITY;

        for (int i = 0; i < 3; i++) {
            float rayOrigin = ray.origin.get(i);
            float rayDirection = ray.direction.get(i);

            float slabMin = worldAABB.min.get(i);
            float slabMax = worldAABB.max.get(i);

            if (rayDirection == 0) {
                if (rayOrigin < slabMin || rayOrigin > slabMax) {
                    return null;
                }
            } else {
                float t1 = (slabMin - rayOrigin) / rayDirection;
                float t2 = (slabMax - rayOrigin) / rayDirection;
                if (t1 > t2) {
                    float temp = t1;
                    t1 = t2;
                    t2 = temp;
                }
                tMin = Math.max(tMin, t1);
                tMax = Math.min(tMax, t2);
                if (tMin > tMax) {
                    return null;
                }
            }
        }
        if (tMin < 0) {
            // Intersection behind the ray origin
            return null;
        }
        return tMin;
    }

    public static RayHit getGameObject(Ray ray) {
        return getGameObject(ray, null);
    }

    public static RayHit getGameObject(Ray ray, List<GameObject> excludedObjects){
        if(SceneManager.currentScene == null) return null;

        GameObject closestObject = null;
        float closestDistance = Float.POSITIVE_INFINITY;

        for (GameObject gameObject : SceneManager.currentScene.getSortedGameObjects()) {
            if((excludedObjects != null && excludedObjects.contains(gameObject)) || !gameObject.isShowInEditor() || !gameObject.isEnabled()) continue;
            Float t = intersectRay(ray, gameObject.getAabb());
            if (t != null && t < closestDistance) {
                closestDistance = t;
                closestObject = gameObject;
            }
        }

        return new RayHit(Calculus.addVectors(ray.origin, Calculus.multiplyVector(ray.direction, closestDistance)), closestObject);
    }

    public static Vector3f closestPointOnLine(Vector3f currentPosition, Vector3f constraintAxis, Ray ray) {
        return closestPointOnLine(currentPosition, constraintAxis, ray.origin, ray.direction);
    }

    public static Vector3f closestPointOnLine(Vector3f currentPosition, Vector3f constraintAxis, Vector3f targetPosition, Vector3f targetDirection) {
        constraintAxis = ObjectPool.VECTOR3F_POOL.obtain().set(constraintAxis).normalize();
        targetDirection = ObjectPool.VECTOR3F_POOL.obtain().set(targetDirection).normalize();

        Vector3f linePointRayOriginDifference = ObjectPool.VECTOR3F_POOL.obtain().set(currentPosition).sub(targetPosition);

        float a = constraintAxis.dot(constraintAxis); // should be 1 since normalized
        float b = constraintAxis.dot(targetDirection);
        float c = targetDirection.dot(targetDirection); // should be 1 since normalized
        float d = constraintAxis.dot(linePointRayOriginDifference);
        float e = targetDirection.dot(linePointRayOriginDifference);

        float denominator = a * c - b * b;

        float moveDelta;
        if (denominator < 1e-6f) {
            // Lines are almost parallel
            moveDelta = 0.0f; // Use linePoint as closest point on line
        } else {
            moveDelta = (b * e - c * d) / denominator;
        }

        // The closest point on the line is linePoint + sc * lineDirNorm
        Vector3f closestPoint = new Vector3f(constraintAxis).mul(moveDelta).add(currentPosition);

        ObjectPool.VECTOR3F_POOL.free(targetDirection);
        ObjectPool.VECTOR3F_POOL.free(constraintAxis);
        ObjectPool.VECTOR3F_POOL.free(linePointRayOriginDifference);

        return closestPoint;
    }
}
