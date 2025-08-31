package nl.framegengine.customScripts;

import nl.framegengine.core.engine.EngineManager;
import nl.framegengine.core.engine.WindowManager;
import nl.framegengine.core.components.Component;
import nl.framegengine.core.debugging.Debug;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

public class MoveWithArrows extends Component {

    private final WindowManager windowManager;
    private float moveSpeed = 5f;

    public MoveWithArrows() {
        this.windowManager = WindowManager.getInstance();
        Debug.log("Starting movement");
    }

    @Override
    public void update() {
        super.update();

        float moveSpeedDelta = moveSpeed * EngineManager.getDeltaTime();

        if(windowManager.isKeyPressed(GLFW.GLFW_KEY_UP)){
            root.translateLocal(new Vector3f(0, 0, -moveSpeedDelta));
        }
        if(windowManager.isKeyPressed(GLFW.GLFW_KEY_DOWN)){
            root.translateLocal(new Vector3f(0, 0, moveSpeedDelta));
        }
        if(windowManager.isKeyPressed(GLFW.GLFW_KEY_LEFT)){
            root.translateLocal(new Vector3f(-moveSpeedDelta, 0, 0));
        }
        if(windowManager.isKeyPressed(GLFW.GLFW_KEY_RIGHT)){
            root.translateLocal(new Vector3f(moveSpeedDelta, 0, 0));
        }
    }
}