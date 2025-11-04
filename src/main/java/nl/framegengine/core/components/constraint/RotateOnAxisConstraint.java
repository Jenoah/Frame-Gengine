package nl.framegengine.core.components.constraint;

import nl.framegengine.core.components.Component;
import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.utils.Calculus;
import nl.framegengine.core.utils.Constants;
import nl.framegengine.core.utils.ObjectPool;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class RotateOnAxisConstraint extends Component {
    private final Vector3f constraintAxis = new Vector3f(0, 1, 0);
    private final Quaternionf offset = new Quaternionf().identity();

    public RotateOnAxisConstraint(){}

    public void rotate2D(Vector2f currentMousePosition, Vector2f initialMousePosition, Camera camera){
        if (root == null) return;

        Vector2f gizmoCenterScreen = camera.projectToScreen(root.getPosition());

        Vector2f initialScreenPosition = ObjectPool.VECTOR2F_POOL.obtain()
                .set(initialMousePosition).sub(gizmoCenterScreen).normalize();
        Vector2f currentScreenPosition = ObjectPool.VECTOR2F_POOL.obtain()
                .set(currentMousePosition).sub(gizmoCenterScreen).normalize();

        float deltaAngle = Calculus.signedAngle2D(initialScreenPosition, currentScreenPosition);

        Vector3f viewAxis = ObjectPool.VECTOR3F_POOL.obtain().set(constraintAxis).mulDirection(camera.getViewMatrix());
        if (viewAxis.z < 0f) deltaAngle = -deltaAngle;

        applyRotation(deltaAngle);

        ObjectPool.VECTOR3F_POOL.free(viewAxis);
        ObjectPool.VECTOR2F_POOL.free(initialScreenPosition);
        ObjectPool.VECTOR2F_POOL.free(currentScreenPosition);
    }

    public void applyRotation(float deltaAngle) {
        Quaternionf delta = ObjectPool.QUATERNIONF_OBJECT_POOL.obtain().set(Constants.QUATERNION_IDENTITY).fromAxisAngleRad(constraintAxis, deltaAngle);
        Quaternionf target = ObjectPool.QUATERNIONF_OBJECT_POOL.obtain().set(offset).mul(delta);
        root.setRotation(target);

        ObjectPool.QUATERNIONF_OBJECT_POOL.free(delta);
        ObjectPool.QUATERNIONF_OBJECT_POOL.free(target);
    }

    public final Vector3f getConstraintAxis() {
        return constraintAxis;
    }

    public void setConstraintAxis(Vector3f constraintAxis) {
        this.constraintAxis.set(constraintAxis);
        this.constraintAxis.normalize();
    }

    public final Quaternionf getOffset() {
        return offset;
    }

    public void setOffset(Quaternionf offset) {
        this.offset.set(offset);
    }
}
