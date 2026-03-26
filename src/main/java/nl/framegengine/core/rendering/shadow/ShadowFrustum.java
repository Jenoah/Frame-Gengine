package nl.framegengine.core.rendering.shadow;

import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.engine.WindowManager;
import nl.framegengine.core.utils.Constants;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class ShadowFrustum {

    private final WindowManager window;

    private float minX, maxX;
    private float minY, maxY;
    private float minZ, maxZ;
    private Matrix4f lightViewMatrix = new Matrix4f();
    private Camera cam;
    private Vector3f cameraPosition = new Vector3f(0);

    private float farHeight, farWidth, nearHeight, nearWidth;
    private final Vector3f center = new Vector3f();

    protected ShadowFrustum() {
        this.window = WindowManager.getInstance();
        calculateWidthsAndHeights();
    }

    protected Vector3f computeFrustumCenter() {
        if (cam == null) return center.set(0);
        cameraPosition.set(cam.getPosition());
        Vector3f forward = cam.getForward();
        // Center is the midpoint between near and far planes along the view direction
        float halfDist = (Constants.Z_NEAR + Constants.SHADOW_DISTANCE) / 2f;
        center.set(cameraPosition).fma(halfDist, forward);
        return center;
    }

    /**
     * Transforms the camera frustum corners into light space and computes the AABB.
     * Must be called after the light view matrix has been built from the frustum center.
     */
    protected void update(Matrix4f lightViewMatrix) {
        if(cam == null) return;
        this.lightViewMatrix.set(lightViewMatrix);

        Vector3f forwardVector = cam.getForward();

        Vector3f centerNear = new Vector3f(cameraPosition).fma(Constants.Z_NEAR, forwardVector);
        Vector3f centerFar  = new Vector3f(cameraPosition).fma(Constants.SHADOW_DISTANCE, forwardVector);

        Vector3f[] points = calculateFrustumVertices(forwardVector, centerNear, centerFar);
        boolean first = true;
        for (Vector3f point : points) {
            if (first) {
                minX = maxX = point.x;
                minY = maxY = point.y;
                minZ = maxZ = point.z;
                first = false;
                continue;
            }
            if (point.x > maxX) maxX = point.x; else if (point.x < minX) minX = point.x;
            if (point.y > maxY) maxY = point.y; else if (point.y < minY) minY = point.y;
            if (point.z > maxZ) maxZ = point.z; else if (point.z < minZ) minZ = point.z;
        }

        minX -= Constants.SHADOW_FRUSTUM_PADDING;
        maxX += Constants.SHADOW_FRUSTUM_PADDING;
        minY -= Constants.SHADOW_FRUSTUM_PADDING;
        maxY += Constants.SHADOW_FRUSTUM_PADDING;
        minZ -= Constants.SHADOW_OFFSET;
        maxZ += Constants.SHADOW_OFFSET;
    }

    private Vector3f[] calculateFrustumVertices(Vector3f forward, Vector3f centerNear, Vector3f centerFar) {
        Vector3f right = new Vector3f(forward).cross(Constants.VECTOR3_UP).normalize();
        Vector3f up = new Vector3f(right).cross(forward).normalize();
        Vector3f down = new Vector3f(up).negate();
        Vector3f left = new Vector3f(right).negate();

        Vector3f farTop = new Vector3f(up).mul(farHeight).add(centerFar);
        Vector3f farBottom = new Vector3f(down).mul(farHeight).add(centerFar);
        Vector3f nearTop = new Vector3f(up).mul(nearHeight).add(centerNear);
        Vector3f nearBottom = new Vector3f(down).mul(nearHeight).add(centerNear);

        Vector3f[] points = new Vector3f[8];
        points[0] = calculateLightSpaceFrustumCorner(farTop, right, farWidth);
        points[1] = calculateLightSpaceFrustumCorner(farTop, left, farWidth);
        points[2] = calculateLightSpaceFrustumCorner(farBottom, right, farWidth);
        points[3] = calculateLightSpaceFrustumCorner(farBottom, left, farWidth);
        points[4] = calculateLightSpaceFrustumCorner(nearTop, right, nearWidth);
        points[5] = calculateLightSpaceFrustumCorner(nearTop, left, nearWidth);
        points[6] = calculateLightSpaceFrustumCorner(nearBottom, right, nearWidth);
        points[7] = calculateLightSpaceFrustumCorner(nearBottom, left, nearWidth);
        return points;
    }

    private Vector3f calculateLightSpaceFrustumCorner(Vector3f startPoint, Vector3f direction, float width) {
        Vector3f point = new Vector3f(direction).mul(width).add(startPoint);
        Vector4f point4f = new Vector4f(point, 1.0f);
        lightViewMatrix.transform(point4f).xyz(point);

        return point;
    }


    private void calculateWidthsAndHeights() {
        float tanHalfFov = (float) Math.tan(Constants.FOV / 2.0);
        farHeight = Constants.SHADOW_DISTANCE * tanHalfFov;
        nearHeight = Constants.Z_NEAR * tanHalfFov;
        farWidth = farHeight * getAspectRatio();
        nearWidth = nearHeight * getAspectRatio();
    }

    protected float getWidth() {
        return maxX - minX;
    }

    protected float getHeight() {
        return maxY - minY;
    }

    protected float getLength() {
        return maxZ - minZ;
    }

    protected float getMinX() { return minX; }
    protected float getMaxX() { return maxX; }
    protected float getMinY() { return minY; }
    protected float getMaxY() { return maxY; }
    protected float getMinZ() { return minZ; }
    protected float getMaxZ() { return maxZ; }

    protected Vector3f getCenter() { return center; }

    private float getAspectRatio() {
        return (float) window.getWidth() / (float) window.getHeight();
    }

    public void setCamera(Camera camera) {
        this.cam = camera;
    }
}
