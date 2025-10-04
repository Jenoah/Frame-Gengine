package nl.framegengine.editor.sceneComponents;

import nl.framegengine.core.components.Component;
import nl.framegengine.core.components.constraint.MoveOnAxis;
import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.entity.GameObject;
import nl.framegengine.core.input.MouseInput;
import nl.framegengine.core.physics.Raycast;
import nl.framegengine.core.rendering.RenderManager;
import nl.framegengine.core.utils.Calculus;
import nl.framegengine.core.utils.Constants;

import static nl.framegengine.core.physics.Raycast.fromCameraByMouse;

public class GizmoMovement extends Component {

    private final MoveOnAxis moveOnAxis;

    private final GameObject xAxis, yAxis, zAxis;
    private Camera camera;
    private boolean isDragging = false;

    public GizmoMovement(MoveOnAxis moveOnAxis, GameObject xAxis, GameObject yAxis, GameObject zAxis){
        this.moveOnAxis = moveOnAxis;
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        this.zAxis = zAxis;
        runInEditor = true;
    }

    @Override
    public void update() {
        super.update();
        if(camera == null) camera = RenderManager.getRenderCamera();

        if(MouseInput.isLbReleased()) isDragging = false;
        if(!MouseInput.isLbDown()) return;

        Raycast.Ray mouseRay = fromCameraByMouse(camera);

        if(MouseInput.isLbClicked()) {
            if (Raycast.intersectFromMouse(mouseRay, xAxis)) {
                isDragging = true;
                moveOnAxis.setConstraintAxis(Constants.VECTOR3_RIGHT);
                moveOnAxis.setOffset(Calculus.subtractVectors(root.getPosition(), Raycast.closestPointOnLine(root.getPosition(), Constants.VECTOR3_RIGHT, mouseRay)));
            } else if (Raycast.intersectFromMouse(mouseRay, yAxis)) {
                isDragging = true;
                moveOnAxis.setConstraintAxis(Constants.VECTOR3_UP);
                moveOnAxis.setOffset(Calculus.subtractVectors(root.getPosition(), Raycast.closestPointOnLine(root.getPosition(), Constants.VECTOR3_UP, mouseRay)));

            } else if (Raycast.intersectFromMouse(mouseRay, zAxis)) {
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
}
