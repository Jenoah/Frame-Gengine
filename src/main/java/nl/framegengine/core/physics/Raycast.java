package nl.framegengine.core.physics;

import nl.framegengine.core.engine.WindowManager;
import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.input.MouseInput;
import nl.framegengine.core.utils.ObjectPool;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Raycast {
    private final Vector3f direction = new Vector3f();

    public Raycast(){}

    public final Vector3f getDirection(){
        return direction;
    }

    public void fromCameraByMouse(Camera camera){
        Vector2f mousePosition = MouseInput.getMousePositionInViewport();
        mousePosition.x = (2f * mousePosition.x) / WindowManager.getInstance().getWidth() - 1f;
        mousePosition.y = (2f * mousePosition.y) / WindowManager.getInstance().getHeight() - 1f;

        Matrix4f invertedProjectionMatrix = ObjectPool.MATRIX4F_OBJECT_POOL.obtain().set(WindowManager.getInstance().getProjectionMatrix());
        Vector4f normalizedDeviceCoords = ObjectPool.VECTOR4F_POOL.obtain().set(mousePosition.x, mousePosition.y, -1f, 1f);
        Vector4f eyeCoords = invertedProjectionMatrix.invert()
                .transform(normalizedDeviceCoords);
        eyeCoords.w = -1f;
        eyeCoords.z = 0f;

        Matrix4f invertedViewMatrix = ObjectPool.MATRIX4F_OBJECT_POOL.obtain().set(camera.getViewMatrix()).invert();
        Vector4f rayWorld = invertedViewMatrix.transform(eyeCoords);

        ObjectPool.MATRIX4F_OBJECT_POOL.free(invertedProjectionMatrix);
        ObjectPool.MATRIX4F_OBJECT_POOL.free(invertedViewMatrix);
        ObjectPool.VECTOR4F_POOL.free(normalizedDeviceCoords);
        direction.set(rayWorld.x, rayWorld.y, rayWorld.z).normalize();
    }
}
