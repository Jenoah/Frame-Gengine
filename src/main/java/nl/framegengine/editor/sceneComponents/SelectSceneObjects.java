package nl.framegengine.editor.sceneComponents;

import nl.framegengine.core.components.Component;
import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.physics.Raycast;
import nl.framegengine.core.rendering.RenderManager;

public class SelectSceneObjects extends Component {

    Raycast mouseRay;
    Camera camera;

    @Override
    public void initiate() {
        super.initiate();
        mouseRay = new Raycast();
    }

    @Override
    public void update() {
        super.update();
        if(camera == null) RenderManager.getRenderCamera();

        mouseRay.fromCameraByMouse(camera);
        //TODO: Add an entire collision / physics engine in order to determine over which game object you are hovering. Sad pepe noise ;-(
    }
}
