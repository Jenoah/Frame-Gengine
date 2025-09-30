package nl.framegengine.core.components.constraint;

import nl.framegengine.core.components.Component;
import nl.framegengine.core.entity.GameObject;

public class DirectConstraint extends Component {

    private GameObject connectedObject = null;

    public boolean followPosition = true;
    public boolean followRotation = true;
    public boolean followScale = false;

    @Override
    public void update() {
        super.update();

        if(connectedObject != null && root.hasUpdated()){
            if(followPosition) connectedObject.setWorldPosition(root.getPosition());
            if(followRotation) connectedObject.setWorldRotation(root.getRotation());
            if(followScale) connectedObject.setScale(root.getScale());
        }
    }

    public void setConnectedObject(GameObject connectedObject){
        this.connectedObject = connectedObject;
    }
}
