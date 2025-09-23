package nl.framegengine.editor.sceneComponents;

import nl.framegengine.core.components.Component;
import nl.framegengine.core.engine.EngineManager;
import nl.framegengine.core.engine.WindowManager;
import nl.framegengine.core.entity.GameObject;
import nl.framegengine.core.input.MouseInput;
import nl.framegengine.core.utils.Constants;
import nl.framegengine.core.utils.ObjectPool;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

public class ScenePreviewCameraControls extends Component {
    private WindowManager windowManager;

    private final Vector3f moveDelta = new Vector3f();
    private float pitch = 0;
    private float yaw = 0;
    public float moveSpeed = 6f;

    public ScenePreviewCameraControls() {
        runInEditor = true;
    }

    @Override
    public void initiate() {
        if (hasInitiated) return;
        super.initiate();

        this.windowManager = WindowManager.getInstance();
    }

    @Override
    public Component setRoot(GameObject root) {
        super.setRoot(root);
        Vector3f initialRotation = ObjectPool.VECTOR3F_POOL.obtain();
        root.getRotation().getEulerAnglesXYZ(initialRotation);
        pitch = initialRotation.x;
        yaw = initialRotation.y;
        ObjectPool.VECTOR3F_POOL.free(initialRotation);
        return this;
    }

    @Override
    public void update() {
        super.update();

        if(MouseInput.isRbDown()) rotate(MouseInput.getMouseDelta());

        move();
    }

    private void move(){
        if(windowManager == null) return;
        moveDelta.set(0, 0, 0);
        float moveSpeed = windowManager.isKeyPressed(GLFW.GLFW_KEY_LEFT_SHIFT) ?
                this.moveSpeed * EngineManager.getDeltaTime() * 4f :
                this.moveSpeed * EngineManager.getDeltaTime();

        //Forwards / Backwards
        if(windowManager.isKeyPressed(GLFW.GLFW_KEY_W)){
            moveDelta.z = -1;
        }else if(windowManager.isKeyPressed(GLFW.GLFW_KEY_S)){
            moveDelta.z = 1;
        }
        //Left / right
        if(windowManager.isKeyPressed(GLFW.GLFW_KEY_A)){
            moveDelta.x = -1;
        } else if(windowManager.isKeyPressed(GLFW.GLFW_KEY_D)){
            moveDelta.x = 1;
        }
        //Up / down
        if(windowManager.isKeyPressed(GLFW.GLFW_KEY_SPACE) || windowManager.isKeyPressed(GLFW.GLFW_KEY_E)){
            moveDelta.y = 1;
        }else if(windowManager.isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL) || windowManager.isKeyPressed(GLFW.GLFW_KEY_Q)){
            moveDelta.y = -1;
        }

        if(moveDelta.length() > 0) moveDelta.normalize(moveSpeed);

        Vector3f targetPosition = ObjectPool.VECTOR3F_POOL.obtain().set(0);

        if(moveDelta.z != 0){
            targetPosition.x += (float) Math.sin(yaw) * -1f * moveDelta.z;
            targetPosition.z += (float) Math.cos(yaw) * moveDelta.z;
        }

        if(moveDelta.x != 0){
            targetPosition.x += (float) Math.sin((yaw - Constants.DEGREES_90_IN_RADIANS)) * -1f * moveDelta.x;
            targetPosition.z += (float) Math.cos((yaw - Constants.DEGREES_90_IN_RADIANS)) * moveDelta.x;
        }

        targetPosition.y += moveDelta.y;

        root.translateLocal(targetPosition);
    }

    public void rotate(Vector2f mouseDelta){
        pitch += mouseDelta.x * Constants.MOUSE_SENSITIVITY * EngineManager.getDeltaTime();
        yaw += mouseDelta.y * Constants.MOUSE_SENSITIVITY * EngineManager.getDeltaTime();

        pitch = Math.clamp(pitch, -Constants.DEGREES_90_IN_RADIANS, Constants.DEGREES_90_IN_RADIANS);

        Quaternionf targetRotation = ObjectPool.QUATERNIONF_OBJECT_POOL.obtain().identity().rotateX(pitch).rotateY(yaw).normalize();

        root.setRotation(targetRotation);

        ObjectPool.QUATERNIONF_OBJECT_POOL.free(targetRotation);
    }

}
