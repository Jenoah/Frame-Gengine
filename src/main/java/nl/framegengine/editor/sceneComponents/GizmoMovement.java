package nl.framegengine.editor.sceneComponents;

import nl.framegengine.core.components.Component;
import nl.framegengine.core.components.constraint.MoveOnAxis;
import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.entity.GameObject;
import nl.framegengine.core.input.MouseInput;
import nl.framegengine.core.modelLoaders.OBJLoader.OBJLoader;
import nl.framegengine.core.physics.Raycast;
import nl.framegengine.core.rendering.RenderManager;
import nl.framegengine.core.rendering.renderers.DebugRenderer;
import nl.framegengine.core.utils.Calculus;
import nl.framegengine.core.utils.Constants;
import nl.framegengine.core.utils.ObjectPool;
import nl.framegengine.core.visual.Mesh;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static nl.framegengine.core.physics.Raycast.fromCameraByMouse;

public class GizmoMovement extends Component {

    private final MoveOnAxis moveOnAxis;

    private final GameObject xAxis, yAxis, zAxis;
    private DebugRenderer.DebugMesh xAxisLine, yAxisLine, zAxisLine;
    private DebugRenderer.DebugMesh xAxisHandle, yAxisHandle, zAxisHandle;
    private Camera camera;
    private boolean isDragging = false;

    public GizmoMovement(MoveOnAxis moveOnAxis){
        this.moveOnAxis = moveOnAxis;
        runInEditor = true;
        this.xAxis = new GameObject("x-axis");
        this.yAxis = new GameObject("y-axis");
        this.zAxis = new GameObject("z-axis");
        coneMesh = OBJLoader.loadOBJModel("/models/cone.obj").stream().findFirst().get().getMesh();
    }

    @Override
    public void update() {
        super.update();
        if(camera == null) camera = RenderManager.getRenderCamera();

        if(MouseInput.isLbReleased()) isDragging = false;
        if(!MouseInput.isLbDown()) return;

        Raycast.Ray mouseRay = fromCameraByMouse(camera);

        if(MouseInput.isLbClicked()) {
            if (Raycast.intersectRay(mouseRay, xAxis)) {
                isDragging = true;
                moveOnAxis.setConstraintAxis(Constants.VECTOR3_RIGHT);
                moveOnAxis.setOffset(Calculus.subtractVectors(root.getPosition(), Raycast.closestPointOnLine(root.getPosition(), Constants.VECTOR3_RIGHT, mouseRay)));
            } else if (Raycast.intersectRay(mouseRay, yAxis)) {
                isDragging = true;
                moveOnAxis.setConstraintAxis(Constants.VECTOR3_UP);
                moveOnAxis.setOffset(Calculus.subtractVectors(root.getPosition(), Raycast.closestPointOnLine(root.getPosition(), Constants.VECTOR3_UP, mouseRay)));

            } else if (Raycast.intersectRay(mouseRay, zAxis)) {
                isDragging = true;
                moveOnAxis.setConstraintAxis(Constants.VECTOR3_FORWARD);
                moveOnAxis.setOffset(Calculus.subtractVectors(root.getPosition(), Raycast.closestPointOnLine(root.getPosition(), Constants.VECTOR3_FORWARD, mouseRay)));
            }
        }

        if(isDragging) move(mouseRay);
    }

    private void move(Raycast.Ray mouseRay){
        moveOnAxis.move(mouseRay.origin, mouseRay.direction);
    }

    public final boolean isCurrentlyMoving(){
        return isDragging;
    }
}
