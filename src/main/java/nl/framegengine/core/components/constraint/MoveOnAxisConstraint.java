package nl.framegengine.core.components.constraint;

import nl.framegengine.core.components.Component;
import nl.framegengine.core.physics.Raycast;
import nl.framegengine.core.utils.Calculus;
import nl.framegengine.core.utils.ObjectPool;
import org.joml.Vector3f;

public class MoveOnAxisConstraint extends Component {

    private final Vector3f constraintAxis = new Vector3f(0, 1, 0);
    private final Vector3f offset = new Vector3f(0);

    public MoveOnAxisConstraint(){}

    public void move(Vector3f targetPosition, Vector3f targetDirection){
        if(root == null) return;
        Vector3f currentPosition = ObjectPool.VECTOR3F_POOL.obtain().set(root.getPosition());
        Vector3f worldPosition = ObjectPool.VECTOR3F_POOL.obtain().set(Raycast.closestPointOnLine(currentPosition, constraintAxis, targetPosition, targetDirection));

        worldPosition.set(Calculus.addVectors(worldPosition, offset));
        root.setWorldPosition(worldPosition);

        ObjectPool.VECTOR3F_POOL.free(currentPosition);
        ObjectPool.VECTOR3F_POOL.free(worldPosition);
    }

    public final Vector3f getConstraintAxis() {
        return constraintAxis;
    }

    public void setConstraintAxis(Vector3f constraintAxis) {
        this.constraintAxis.set(constraintAxis);
        this.constraintAxis.normalize();
    }

    public final Vector3f getOffset() {
        return offset;
    }

    public void setOffset(Vector3f offset) {
        this.offset.set(offset);
    }

}
