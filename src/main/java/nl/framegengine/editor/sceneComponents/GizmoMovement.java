package nl.framegengine.editor.sceneComponents;

import imgui.extension.imguizmo.ImGuizmo;
import imgui.extension.imguizmo.flag.Operation;
import nl.framegengine.core.components.Component;
import nl.framegengine.core.engine.WindowManager;
import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.entity.GameObject;
import nl.framegengine.core.utils.ObjectPool;
import org.joml.*;

import static imgui.extension.imguizmo.flag.Mode.LOCAL;
import static imgui.extension.imguizmo.flag.Mode.WORLD;

public class GizmoMovement extends Component {

    private int mode = Operation.TRANSLATE;
    private boolean local = true;
    private WindowManager windowManager;

    @Override
    public void initiate() {
        super.initiate();
        windowManager = WindowManager.getInstance();
    }

    public void render(Camera camera, GameObject selected) {

        if(selected == null)
            return;

        float[] model = selected.getMatrix().get(new float[16]);

        ImGuizmo.manipulate(
                camera.getViewMatrix().get(new float[16]),
                windowManager.getProjectionMatrix().get(new float[16]),
                mode,
                local ? LOCAL : WORLD,
                model
        );

        if(ImGuizmo.isUsing()){
            Matrix4f modelMatrix = ObjectPool.MATRIX4F_OBJECT_POOL.obtain();
            modelMatrix.set(model);
            selected.setMatrix(new Matrix4f(modelMatrix));
            ObjectPool.MATRIX4F_OBJECT_POOL.free(modelMatrix);
        }
    }

    public void SetTransformMode(TransformMode transformMode){
        switch (transformMode){
            case TRANSLATE -> mode = Operation.TRANSLATE;
            case ROTATE -> mode = Operation.ROTATE;
            case SCALE -> mode = Operation.SCALE;
        }
    }

    public enum TransformMode {
        TRANSLATE,
        ROTATE,
        SCALE

    }
}
