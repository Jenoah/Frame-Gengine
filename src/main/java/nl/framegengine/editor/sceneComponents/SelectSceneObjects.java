package nl.framegengine.editor.sceneComponents;

import nl.framegengine.core.components.Component;
import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.entity.GameObject;
import nl.framegengine.core.entity.SceneManager;
import nl.framegengine.core.input.MouseInput;
import nl.framegengine.core.physics.Raycast;
import nl.framegengine.core.rendering.RenderManager;
import nl.framegengine.editor.panels.HierarchyPanel;

public class SelectSceneObjects extends Component {
    private Camera camera;
    private final HierarchyPanel hierarchyPanel;
    private final GameObject gizmoObject;


    public SelectSceneObjects(HierarchyPanel hierarchyPanel, GameObject gizmoObject){
        runInEditor = true;
        this.hierarchyPanel = hierarchyPanel;
        this.gizmoObject = gizmoObject;
    }

    public SelectSceneObjects(Camera camera, HierarchyPanel hierarchyPanel, GameObject gizmoObject){
        runInEditor = true;
        this.camera = camera;
        this.hierarchyPanel = hierarchyPanel;
        this.gizmoObject = gizmoObject;
    }

    @Override
    public void initiate() {
        super.initiate();
    }

    @Override
    public void update() {
        super.update();
        if(camera == null) RenderManager.getRenderCamera();

        if(!(MouseInput.isLbClicked() && SceneManager.currentScene != null)) return;

        for (GameObject go : SceneManager.currentScene.getGameObjects()) {
            if((gizmoObject.isSelfOrChild(go) || go.isShowInEditor()) && Raycast.intersectFromMouse(camera, go)){
                hierarchyPanel.setCurrentlySelectedGameObject(go);
                gizmoObject.setEnabled(true);
                return;
            }
        }
        hierarchyPanel.setCurrentlySelectedGameObject(null);
        gizmoObject.setEnabled(false);
    }
}
