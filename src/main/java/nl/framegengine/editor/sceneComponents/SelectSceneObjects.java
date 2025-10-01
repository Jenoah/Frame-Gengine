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
    Camera camera;
    HierarchyPanel hierarchyPanel = null;

    public SelectSceneObjects(HierarchyPanel hierarchyPanel){
        runInEditor = true;
        this.hierarchyPanel = hierarchyPanel;
    }

    public SelectSceneObjects(Camera camera, HierarchyPanel hierarchyPanel){
        runInEditor = true;
        this.camera = camera;
        this.hierarchyPanel = hierarchyPanel;
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
            if(go.isShowInEditor() && Raycast.intersectFromMouse(camera, go)){
                hierarchyPanel.setCurrentlySelectedGameObject(go);
                return;
            }
        }
        hierarchyPanel.setCurrentlySelectedGameObject(null);
    }
}
