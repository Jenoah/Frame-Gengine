package nl.framegengine.editor.panels;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import nl.framegengine.core.components.constraint.DirectConstraint;
import nl.framegengine.core.components.visual.RenderComponent;
import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.engine.EngineManager;
import nl.framegengine.core.engine.WindowManager;
import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.entity.GameObject;
import nl.framegengine.core.entity.Scene;
import nl.framegengine.core.entity.SceneManager;
import nl.framegengine.core.input.MouseInput;
import nl.framegengine.core.modelLoaders.OBJLoader.OBJLoader;
import nl.framegengine.core.rendering.RenderManager;
import nl.framegengine.core.shaders.ShaderManager;
import nl.framegengine.core.utils.Constants;
import nl.framegengine.core.visual.MeshMaterialSet;
import nl.framegengine.core.visual.Texture;
import nl.framegengine.core.visual.TextureLoader;
import nl.framegengine.editor.*;
import nl.framegengine.editor.sceneComponents.ScenePreviewCameraControls;
import nl.framegengine.editor.sceneComponents.SelectSceneObjects;

import java.util.Set;

public class GamePanel extends EditorPanel {

    private EditorGameLauncher editorGameLauncher;
    private int aspectWidth = 0;
    private int aspectHeight = 0;
    private float aspectRatio = 1.7778f;
    private boolean showStats = false;
    private String editingSceneJson = null;
    private HierarchyPanel hierarchyPanel = null;

    public GamePanel(int posX, int posY, int sizeX, int sizeY) {
        super(posX, posY, sizeX, sizeY);
        recalculateResolution();

        addWindowFlag(ImGuiWindowFlags.NoNavFocus);
    }

    @Override
    public void prepareFrame(){
        ImGui.setNextWindowPos(posX, posY);
        ImGui.setNextWindowSize(sizeX, sizeY);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, new ImVec2(0, 0));
        ImGui.begin(windowName, windowFlags);
        ImGui.popStyleVar();
    }

    @Override
    public void renderFrame() {
        if(editorGameLauncher != null) editorGameLauncher.render();

        if(EditorWindow.getInstance().getGameFBOID() != -1){

            ImVec2 avail = ImGui.getContentRegionAvail();

            float offsetX = (avail.x - aspectWidth) / 2.0f;
            float offsetY = (avail.y - (aspectHeight - 20)) / 2.0f;

            offsetX = Math.max(offsetX, 0);
            offsetY = Math.max(offsetY, 0);

            ImGui.setCursorPosX(ImGui.getCursorPosX() + offsetX);
            ImGui.setCursorPosY(ImGui.getCursorPosY() + offsetY);

            ImGui.image(EditorWindow.getInstance().getGameFBOID(), aspectWidth, aspectHeight - 20, 0, 1, 1, 0);
            inFocus = ImGui.isItemHovered();
        }

        ImGui.setCursorPos(8, 24);
        ImGui.text("FPS: " + EngineManager.getFps());

        if(showStats){
            ImGui.setCursorPos(8, 48);
            ImGui.pushTextWrapPos(sizeX / 2f);
            ImGui.text("Stats: " + RenderManager.getMetrics());
            ImGui.popTextWrapPos();
        }
    }

    @Override
    public void endFrame() {
        super.endFrame();
        if (WindowManager.getInstance() != null) {
            WindowManager.getInstance().setFocus(inFocus);
        }
    }

    public void startGame(){
        if(SceneManager.currentScene == null || EngineSettings.isInGame) return;

        ImGuiHelper.showProgressBar("Loading Game");
        removeEditorCamera();
        removeGizmo();
        editingSceneJson = SceneManager.currentScene.serializeToJson().toString();
        Scene gameplayScene = new Scene();
        gameplayScene.deserializeFromJson(editingSceneJson);
        gameplayScene.setLevelName(gameplayScene.getLevelName() + " - Gameplay");
        SceneManager.setCurrentScene(gameplayScene);
        EngineSettings.isInGame = true;
        ImGuiHelper.hideProgressBar();
    }

    public void stopGame(){
        if(EngineSettings.isInGame){
            EngineSettings.isInGame = false;
            SceneManager.setCurrentScene((Scene)new Scene().deserializeFromJson(editingSceneJson));
            addGizmo();
            addEditorCamera();
        }
    }

    private void addEditorCamera(){
        if(SceneManager.currentScene == null) return;
        Camera editorCamera = new Camera();
        editorCamera.setPosition(SceneManager.currentScene.getEditorCameraPosition());
        editorCamera.setRotation(SceneManager.currentScene.getEditorCameraRotation());
        editorCamera.setName(EngineSettings.editorCameraName);
        editorCamera.addComponent(new ScenePreviewCameraControls());
        editorCamera.addComponent(new SelectSceneObjects(editorCamera,
                EditorWindow.getEditorLayout().getEditorPanelOfType(HierarchyPanel.class)));
        editorCamera.setShowInEditor(false);
        SceneManager.currentScene.addEntity(editorCamera);
        RenderManager.setRenderCamera(editorCamera);
    }

    private void addGizmo(){
        if(SceneManager.currentScene == null) return;
        GameObject gizmo = new GameObject(EngineSettings.editorGizmoName);
        gizmo.setScale(0.5f);
        gizmo.translateLocal(Constants.VECTOR3_UP);
        Set<MeshMaterialSet> gizmoMms = OBJLoader.loadOBJModel("/models/gizmo.obj", new Texture(TextureLoader.loadTexture("textures/color_palette.png", true)));
        gizmoMms.forEach(mms -> {
            mms.material.setShader(ShaderManager.unlitShader);
            mms.material.setOnTop(true);
            mms.material.castShadow(false);
            mms.material.receiveShadows(false);
        });
        RenderComponent renderComponent = new RenderComponent(gizmoMms);
        DirectConstraint directConstraint = new DirectConstraint();
        directConstraint.runInEditor = true;
        if(hierarchyPanel != null) hierarchyPanel.setGizmo(gizmo);

        gizmo.addComponent(renderComponent);
        gizmo.addComponent(directConstraint);
        //gizmo.setShowInEditor(false);
        SceneManager.currentScene.addEntity(gizmo);
    }

    private void removeEditorCamera(){
        if(SceneManager.currentScene == null) return;
        GameObject editorCamera = SceneManager.currentScene.getGameObjectByName(EngineSettings.editorCameraName);
        if(editorCamera == null) return;
        SceneManager.currentScene.setEditorCameraPosition(editorCamera.getPosition());
        SceneManager.currentScene.setEditorCameraRotation(editorCamera.getRotation());
        SceneManager.currentScene.removeGameObject(editorCamera);
    }

    private void removeGizmo(){
        if(SceneManager.currentScene == null) return;
        GameObject gizmo = SceneManager.currentScene.getGameObjectByName(EngineSettings.editorGizmoName);
        if(gizmo == null) return;
        Debug.log("Removing gizmo from " + SceneManager.currentScene.getLevelName());
        if(hierarchyPanel != null) hierarchyPanel.setGizmo(null);
        SceneManager.currentScene.removeGameObject(gizmo);
        //TODO: Fix gizmo not unloading mesh properly
    }

    public void setAspectRatio(float aspectRatio){
        this.aspectRatio = aspectRatio;
        recalculateResolution(true);
    }

    public void recalculateResolution(){
        recalculateResolution(false);
        MouseInput.setMouseOffset(posX, posY);
    }

    public void recalculateResolution(boolean refreshGameInstance) {
        if (aspectRatio <= 0) {
            aspectWidth = sizeX;
            aspectHeight = sizeY;
        }else {
            aspectWidth = Math.round(sizeY * aspectRatio);
            aspectHeight = sizeY;
            if (aspectWidth > sizeX) {
                aspectWidth = sizeX;
                aspectHeight = Math.round(sizeX / aspectRatio);
            }
        }

        if(refreshGameInstance && WindowManager.getInstance() != null){
            WindowManager.getInstance().setWindowSize(aspectWidth, aspectHeight);
        }
    }

    public void startEngine(){
        if(EngineSettings.currentLevelPath.isEmpty()){
            Debug.logError("Cannot start game. No level selected");
            return;
        }
        if(editorGameLauncher == null) {
            ImGuiHelper.showProgressBar("Booting game...");
            editorGameLauncher = new EditorGameLauncher();
            editorGameLauncher.run(sizeX, sizeY - 20);
            if(!EngineSettings.isInGame){
                addGizmo();
                addEditorCamera();
            }
        }

    }

    public void toggleStats(){
        showStats = !showStats;
        RenderManager.recordMetrics(showStats);
    }

    public void enableStats(){
        showStats = true;
        RenderManager.recordMetrics(true);
    }

    public void disableStats(){
        showStats = false;
        RenderManager.recordMetrics(false);
    }

    public void toggleWireframe(){
        RenderManager.toggleWireframe();
    }

    public void setHierarchyPanel(HierarchyPanel hierarchyPanel){
        this.hierarchyPanel = hierarchyPanel;
    }
}
