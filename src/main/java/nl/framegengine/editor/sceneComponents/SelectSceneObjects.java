package nl.framegengine.editor.sceneComponents;

import nl.framegengine.core.components.Component;
import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.entity.GameObject;
import nl.framegengine.core.entity.SceneManager;
import nl.framegengine.core.input.MouseInput;
import nl.framegengine.core.physics.Raycast;
import nl.framegengine.core.rendering.RenderManager;
import nl.framegengine.editor.panels.HierarchyPanel;

public class SelectSceneObjects extends Component {
    Camera camera;
    GameObject gizmoObject = null;
    HierarchyPanel hierarchyPanel = null;

    public SelectSceneObjects(GameObject gizmoObject, HierarchyPanel hierarchyPanel){
        runInEditor = true;
        this.gizmoObject = gizmoObject;
        this.hierarchyPanel = hierarchyPanel;
    }

    public SelectSceneObjects(Camera camera, GameObject gizmoObject, HierarchyPanel hierarchyPanel){
        runInEditor = true;
        this.camera = camera;
        this.gizmoObject = gizmoObject;
        this.hierarchyPanel = hierarchyPanel;
    }

    @Override
    public void initiate() {
        super.initiate();
        if(gizmoObject == null) Debug.logError("Gizmo not set");
    }

    @Override
    public void update() {
        super.update();
        if(gizmoObject == null){ return; }
        if(camera == null) RenderManager.getRenderCamera();

        if(!MouseInput.isLbDown() || SceneManager.currentScene == null) return;
        SceneManager.currentScene.getGameObjects().forEach(go -> {
            if(go.isShowInEditor() && Raycast.intersectFromMouse(camera, go)){
                hierarchyPanel.setCurrentlySelectedGameObject(go);
            }
        });
    }
}
