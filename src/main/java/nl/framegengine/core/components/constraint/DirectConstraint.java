package nl.framegengine.core.components.constraint;

import nl.framegengine.core.components.Component;
import nl.framegengine.core.entity.GameObject;
import org.joml.Quaternionf;

public class DirectConstraint extends Component {

    private GameObject connectedObject = null;

    public boolean followPosition = true;
    public boolean followRotation = true;
    public boolean followScale = false;

    private Quaternionf rotationOffset = new Quaternionf().identity();

    @Override
    public void update() {
        super.update();

        if(connectedObject != null && root.hasUpdated()){
            if(followPosition) connectedObject.setWorldPosition(root.getPosition());
            //TODO: Offset follow rotation by rotationOffset value
            if(followRotation) connectedObject.setWorldRotation(root.getRotation());
            if(followScale) connectedObject.setScale(root.getScale());
        }
    }

    public void setConnectedObject(GameObject connectedObject){
        this.connectedObject = connectedObject;
        this.rotationOffset = connectedObject.getRotation();
    }
}
