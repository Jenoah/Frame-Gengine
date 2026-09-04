package nl.framegengine.editor.panels;

import imgui.ImGui;
import imgui.ImVec4;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiStyleVar;
import nl.framegengine.core.components.visual.RenderComponent;
import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.entity.GameObject;
import nl.framegengine.core.entity.SceneManager;
import nl.framegengine.core.lighting.DirectionalLight;
import nl.framegengine.core.lighting.PointLight;
import nl.framegengine.core.lighting.SpotLight;
import nl.framegengine.core.modelLoaders.StaticMeshLoader;
import nl.framegengine.core.visual.Mesh;
import nl.framegengine.core.visual.MeshMaterialSet;
import nl.framegengine.editor.EditorPanel;
import nl.framegengine.editor.ImGuiHelper;
import nl.framegengine.editor.editorComponents.Button;
import nl.framegengine.editor.editorComponents.Collapse;
import nl.framegengine.editor.editorComponents.Icons;
import nl.framegengine.editor.sceneComponents.SelectSceneObjects;
import org.joml.Math;
import org.joml.Vector3f;
import java.util.List;
import java.util.Set;

public class HierarchyPanel extends EditorPanel {
    private final ImVec4 activeButtonTextColor = new ImVec4(1f, 1f, 1f, 1f);
    private final ImVec4 inactiveButtonTextColor = new ImVec4(.75f, .75f, .75f, 1f);
    private final ImVec4 selectedButtonTextColor = new ImVec4(1, .5f, .5f, 1f);

    private final ImVec4 standardButtonBackgroundColor = new ImVec4(1f, 1f, 1f, 0f);
    private final ImVec4 hoverButtonBackgroundColor = new ImVec4(0f, 0f, 0f, 1f);
    private InfoPanel infoPanel;
    private GameObject currentlySelectedGameObject = null;

    private int frameCount = 0;
    private List<GameObject> hierarchyObjects;

    private final String contextMenuStrID = "hierarchyContextMenuID";
    private final String contextObjectMenuStrID = "hierarchyObjectContextMenuID";

    public HierarchyPanel(int posX, int posY, int sizeX, int sizeY) {
        super(posX, posY, sizeX, sizeY);
        windowName = Icons.FILTER + " Hierarchy";
    }

    public void setCurrentlySelectedGameObject(GameObject currentlySelectedGameObject) {
        //Todo: Fix (spot)lights not always emitting light on all objects until initial selection of an object
        this.currentlySelectedGameObject = currentlySelectedGameObject;
        infoPanel.setCurrentlySelectedObject(currentlySelectedGameObject);
        SelectSceneObjects.selectedObject = currentlySelectedGameObject;
    }

    @Override
    public void renderFrame() {
        showContextMenu();

        frameCount++;
        if(SceneManager.currentScene == null) return;
        if(frameCount > 60){
            hierarchyObjects = SceneManager.currentScene.getRootGameObjects();
            frameCount = 0;
        }
        if(hierarchyObjects == null) return;

        ImGui.setWindowFontScale(1.1f);

        ImGui.pushStyleColor(ImGuiCol.Button, standardButtonBackgroundColor);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, hoverButtonBackgroundColor);
        ImGui.pushStyleVar(ImGuiStyleVar.ButtonTextAlign, 0f, 0.5f);

        for (GameObject go : hierarchyObjects) {
            DrawObject(go, 0);
        }
        ImGui.popStyleColor(2);
        ImGui.popStyleVar();

        if(ImGui.isWindowHovered() && !ImGui.isAnyItemHovered() && ImGui.isMouseReleased(ImGuiMouseButton.Left)){
            setCurrentlySelectedGameObject(null);
        }
    }

    private void DrawObject(GameObject go, int level){
        if(!go.isShowInEditor()) return;
        boolean hasChildren = !go.getChildren().isEmpty();
        String goLabel = (level > 0 ? Icons.ARROW_BAR_RIGHT + " " : "") + Icons.GetIcon(go) + " " + go.getName() + "##" + go.getGuid();

        if(currentlySelectedGameObject == go){
            ImGui.pushStyleColor(ImGuiCol.Text, selectedButtonTextColor);
        }else if(!go.isEnabled()){
            ImGui.pushStyleColor(ImGuiCol.Text, inactiveButtonTextColor);
        }else{
            ImGui.pushStyleColor(ImGuiCol.Text, activeButtonTextColor);
        }

        if(!hasChildren){
            if (Button.regular(goLabel, true)) {
                setCurrentlySelectedGameObject(go);
            }
        }else {
            Collapse.CollapseWithButton collapse = Collapse.WithButton(goLabel, go.getGuid());
            if(collapse.isPressed) setCurrentlySelectedGameObject(go);
            if(collapse.isExpanded){
                int indentationAmount = 32 * Math.max(0, level - 1) + 1;
                ImGui.indent(indentationAmount);
                go.getChildren().forEach(child -> {
                    DrawObject(child, level + 1);
                });
                ImGui.unindent(indentationAmount);
            }
        }

        ImGui.popStyleColor();


    }

    public void setInfoPanel(InfoPanel infoPanel){
        this.infoPanel = infoPanel;
    }

    private void showContextMenu(){
        if(SceneManager.currentScene == null) return;

        if(ImGui.isWindowHovered() && ImGui.isMouseReleased(ImGuiMouseButton.Right)){
            if(currentlySelectedGameObject != null){
                ImGui.openPopup(contextObjectMenuStrID);
            }else {
                ImGui.openPopup(contextMenuStrID);
            }
        }

        showContextMenuEmpty();
        showContextMenuObject();
    }

    private void showContextMenuObject(){
        if (ImGui.beginPopupContextItem(contextObjectMenuStrID)) {
            ImGui.separatorText("Object settings");
            if (ImGui.menuItem("Remove")) {
                //TODO: Add confirmation box
                if(SceneManager.currentScene != null){
                    currentlySelectedGameObject.remove();
                    currentlySelectedGameObject = null;
                    infoPanel.setCurrentlySelectedObject(null);
                }
            }
            if (ImGui.menuItem("Rename")) {
                if(SceneManager.currentScene != null){
                    ImGuiHelper.setInputFieldModal(name -> currentlySelectedGameObject.setName(name));
                    ImGui.closeCurrentPopup();
                }
            }
            ImGui.endPopup();
        }
    }

    private void showContextMenuEmpty(){
        if (ImGui.beginPopupContextItem(contextMenuStrID)) {
            ImGui.separatorText("Add new");
            if (ImGui.beginMenu("Shape")) {
                if (ImGui.menuItem("Cube")) {
                    if(SceneManager.currentScene != null){
                        Set<MeshMaterialSet> meshMaterialSets = StaticMeshLoader.load(Mesh.BUILTIN_PREFIX + "/models/cube.obj");
                        if(meshMaterialSets != null) {
                            GameObject cubeObject = new GameObject("Cube");
                            cubeObject.addComponent(new RenderComponent(meshMaterialSets));
                            SceneManager.currentScene.addEntity(cubeObject);
                        }
                    }
                    ImGui.closeCurrentPopup();
                }
                if (ImGui.menuItem("Quad")) {
                    if(SceneManager.currentScene != null){
                        Set<MeshMaterialSet> meshMaterialSets = StaticMeshLoader.load(Mesh.BUILTIN_PREFIX + "/models/quad.obj");
                        if(meshMaterialSets != null) {
                            GameObject cubeObject = new GameObject("Quad");
                            cubeObject.addComponent(new RenderComponent(meshMaterialSets));
                            SceneManager.currentScene.addEntity(cubeObject);
                        }
                    }
                    ImGui.closeCurrentPopup();
                }
                if (ImGui.menuItem("Sphere")) {
                    if(SceneManager.currentScene != null){
                        Set<MeshMaterialSet> meshMaterialSets = StaticMeshLoader.load(Mesh.BUILTIN_PREFIX + "/models/sphere.obj");
                        if(meshMaterialSets != null) {
                            GameObject sphereObject = new GameObject("Sphere");
                            sphereObject.addComponent(new RenderComponent(meshMaterialSets));
                            SceneManager.currentScene.addEntity(sphereObject);
                        }
                    }
                    ImGui.closeCurrentPopup();
                }
                ImGui.endMenu();
            }
            if (ImGui.beginMenu("Built-in")) {
                if (ImGui.menuItem("Camera")) {
                    GameObject cameraGO = new GameObject("Camera");
                    Camera cameraObject = new Camera();
                    cameraGO.addComponent(cameraObject);
                    SceneManager.currentScene.addEntity(cameraGO);
                    ImGui.closeCurrentPopup();
                }
                if (ImGui.beginMenu("Light")) {
                    if (ImGui.menuItem("Directional light")) {
                        GameObject directionalLightObject = new GameObject("Directional light");
                        DirectionalLight directionalLight = new DirectionalLight(new Vector3f(1f, 0.6f, 0), new Vector3f(-1, -1, -1).normalize(), 10);
                        directionalLightObject.addComponent(directionalLight);
                        directionalLightObject.setPosition(0, 1, 0);
                        directionalLight.showProxy();
                        SceneManager.currentScene.setDirectionalLight(directionalLight);
                        SceneManager.currentScene.addEntity(directionalLightObject);
                        SceneManager.currentScene.updateLights();
                        ImGui.closeCurrentPopup();
                    }
                    if (ImGui.menuItem("Point light")) {
                        GameObject pointLightObject = new GameObject("Point light");
                        PointLight pointLight = new PointLight(new Vector3f(1, 1, 0), 5, 15);
                        pointLightObject.setPosition(new Vector3f(0f, 1f, 0f));
                        pointLightObject.addComponent(pointLight);
                        pointLight.showProxy();
                        SceneManager.currentScene.addPointLight(pointLight);
                        SceneManager.currentScene.addEntity(pointLightObject);
                        SceneManager.currentScene.updateLights();
                        ImGui.closeCurrentPopup();
                    }
                    if (ImGui.menuItem("Spot light")) {
                        GameObject spotLightObject = new GameObject("Spot light");
                        spotLightObject.setPosition(new Vector3f(0f, 1f, 0f));
                        SpotLight spotLight = new SpotLight(new Vector3f(1, 1, 0), 3f, 10f, 0.8660254f, 0.81915206f);
                        spotLightObject.addComponent(spotLight);
                        spotLight.showProxy();
                        SceneManager.currentScene.addSpotLight(spotLight);
                        SceneManager.currentScene.addEntity(spotLightObject);
                        SceneManager.currentScene.updateLights();
                        ImGui.closeCurrentPopup();
                    }
                    ImGui.endMenu();
                }
                ImGui.endMenu();
            }
            ImGui.endPopup();
        }
    }
}
