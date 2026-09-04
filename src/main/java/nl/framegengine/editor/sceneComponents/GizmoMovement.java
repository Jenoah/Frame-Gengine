package nl.framegengine.editor.sceneComponents;

import imgui.extension.imguizmo.ImGuizmo;
import imgui.extension.imguizmo.flag.Operation;
import nl.framegengine.core.components.Component;
import nl.framegengine.core.engine.WindowManager;
import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.rendering.RenderManager;
import nl.framegengine.core.utils.ObjectPool;
import org.joml.*;

import static imgui.extension.imguizmo.flag.Mode.LOCAL;
import static imgui.extension.imguizmo.flag.Mode.WORLD;

public class GizmoMovement extends Component {

    private int mode = Operation.TRANSLATE;
    private boolean local = true;
    private WindowManager windowManager;
    private Camera camera = null;

    private final float[] model = new float[16];
    private final float[] view = new float[16];
    private final float[] projection = new float[16];

    @Override
    public void initiate() {
        super.initiate();
        windowManager = WindowManager.getInstance();
    }

    public void drawGizmo(){
        if(SelectSceneObjects.selectedObject == null || windowManager == null) return;
        if(camera == null) camera = RenderManager.getRenderCamera();

        ImGuizmo.beginFrame();
        ImGuizmo.setDrawList();

        SelectSceneObjects.selectedObject.getMatrix().get(model);
        camera.getViewMatrix().get(view);
        camera.getProjectionMatrix().get(projection);

        ImGuizmo.manipulate(
                view,
                projection,
                mode,
                local ? LOCAL : WORLD,
                model
        );

        if(ImGuizmo.isUsing()){
            Matrix4f modelMatrix = ObjectPool.MATRIX4F_OBJECT_POOL.obtain();
            modelMatrix.set(model);
            SelectSceneObjects.selectedObject.setMatrix(modelMatrix);
            ObjectPool.MATRIX4F_OBJECT_POOL.free(modelMatrix);
        }
    }

    public static boolean isDragging(){
        return ImGuizmo.isUsing();
    }

    public void disableGizmo(){
        camera = null;
        mode = Operation.TRANSLATE;
        local = true;
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
