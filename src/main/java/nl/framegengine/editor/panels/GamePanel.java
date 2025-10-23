package nl.framegengine.editor.panels;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import nl.framegengine.core.components.constraint.DirectConstraint;
import nl.framegengine.core.components.constraint.MoveOnAxisConstraint;
import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.engine.EngineManager;
import nl.framegengine.core.engine.WindowManager;
import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.entity.GameObject;
import nl.framegengine.core.entity.Scene;
import nl.framegengine.core.entity.SceneManager;
import nl.framegengine.core.input.MouseInput;
import nl.framegengine.core.rendering.RenderManager;
import nl.framegengine.core.utils.Constants;
import nl.framegengine.editor.*;
import nl.framegengine.editor.sceneComponents.GizmoMovement;
import nl.framegengine.editor.sceneComponents.ScenePreviewCameraControls;
import nl.framegengine.editor.sceneComponents.SelectSceneObjects;

import java.util.Arrays;

public class GamePanel extends EditorPanel {

    private EditorGameLauncher editorGameLauncher;
    private int aspectWidth = 0;
    private int aspectHeight = 0;
    private float aspectRatio = 1.7778f;
    private boolean showStats = false;
    private String editingSceneJson = null;
    private HierarchyPanel hierarchyPanel = null;

    private final int[] fpsValues = new int[10];
    private int fpsIteration = 0;
    private int fpsAverage = 0;

    private final float[] frameTimeValues = new float[10];
    private int frameTimeIteration = 0;
    private float frameTimeAverage = 0f;

    private GizmoMovement gizmoMovement = null;

    private final ImVec2 availableWindowSpace = new ImVec2();

    float offsetX = 36f;
    float offsetY = 0f;

    private boolean updateMouseOffset = false;

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

            availableWindowSpace.set(ImGui.getContentRegionAvail());

            offsetX = ImGui.getCursorPosX() + Math.max(((availableWindowSpace.x - aspectWidth) / 2.0f), 0);
            offsetY = ImGui.getCursorPosY() + Math.max(((availableWindowSpace.y - aspectHeight) / 2.0f), 0);

            ImGui.setCursorPosX(offsetX);
            ImGui.setCursorPosY(offsetY);

            ImGui.image(EditorWindow.getInstance().getGameFBOID(), aspectWidth, aspectHeight, 0, 1, 1, 0);
            inFocus = ImGui.isItemHovered();
        }

        if(updateMouseOffset){
            MouseInput.setMouseOffset((int) (posX + offsetX), (int) (posY + offsetY));
            updateMouseOffset = false;
        }

        updateFpsAverage();
        updateFrameTimeAverage();
        ImGui.setCursorPos(8, 24);
        ImGui.text("FPS: " + fpsAverage);
        ImGui.setCursorPos(8, 36);
        ImGui.text("Frametime: " + frameTimeAverage);

        if(showStats){
            ImGui.setCursorPos(8, 48);
            ImGui.pushTextWrapPos(sizeX / 2f);
            ImGui.text("Stats: " + RenderManager.getMetrics());
            ImGui.popTextWrapPos();
        }

        if(!EngineSettings.isInGame){
            ImGui.setCursorPos(sizeX / 2f - 48f, 24);
            if(ImGui.button("M", 32f, 32f) && gizmoMovement != null){
                gizmoMovement.setTransformMode(GizmoMovement.TransformMode.Move);
            }
            ImGui.setCursorPos(sizeX / 2f, 24);
            if(ImGui.button("R", 32f, 32f) && gizmoMovement != null){
                gizmoMovement.setTransformMode(GizmoMovement.TransformMode.Rotate);
            }
            ImGui.setCursorPos(sizeX / 2f + 48f, 24);
            if(ImGui.button("S", 32f, 32f) && gizmoMovement != null){
                gizmoMovement.setTransformMode(GizmoMovement.TransformMode.Scale);
            }
        }
    }

    private void updateFpsAverage(){
        fpsValues[fpsIteration] = EngineManager.getFps();
        fpsIteration++;
        if(fpsIteration >= fpsValues.length){
            fpsIteration = 0;
            fpsAverage = Arrays.stream(fpsValues).sum() / fpsValues.length;
        }
    }

    private void updateFrameTimeAverage(){
        frameTimeValues[frameTimeIteration] = EngineManager.getFrameTimeMS();
        frameTimeIteration++;
        if(frameTimeIteration >= frameTimeValues.length){
            frameTimeIteration = 0;
            frameTimeAverage = 0f;
            for (float frameTimeValue : frameTimeValues) frameTimeAverage += frameTimeValue;
            frameTimeAverage /= frameTimeValues.length;
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
        //removeGizmo();
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
            GameObject gizmo = addGizmo();
            addEditorCamera(gizmo);
            gizmo.setEnabled(false);
        }
    }

    private void addEditorCamera(GameObject gizmo){
        if(SceneManager.currentScene == null) return;
        Camera editorCamera = new Camera();
        editorCamera.setPosition(SceneManager.currentScene.getEditorCameraPosition());
        editorCamera.setRotation(SceneManager.currentScene.getEditorCameraRotation());
        editorCamera.setName(EngineSettings.editorCameraName);
        editorCamera.addComponent(new ScenePreviewCameraControls());
        editorCamera.addComponent(new SelectSceneObjects(editorCamera,
                EditorWindow.getEditorLayout().getEditorPanelOfType(HierarchyPanel.class), gizmo));
        editorCamera.setShowInEditor(false);
        editorCamera.canBeSaved(false);
        SceneManager.currentScene.addEntity(editorCamera);
        RenderManager.setRenderCamera(editorCamera);
    }

    private GameObject addGizmo(){
        if(SceneManager.currentScene == null) return null;
        GameObject gizmo = new GameObject(EngineSettings.editorGizmoName);
        gizmo.translateLocal(Constants.VECTOR3_UP);

        MoveOnAxisConstraint moveGizmoOnAxis = new MoveOnAxisConstraint();
        DirectConstraint directConstraint = new DirectConstraint();
        MoveOnAxis moveGizmoOnAxis = new MoveOnAxis();
        gizmoMovement = new GizmoMovement(moveGizmoOnAxis);
        directConstraint.runInEditor = true;
        if(hierarchyPanel != null) hierarchyPanel.setGizmo(gizmo);

        gizmo.addComponent(directConstraint);
        gizmo.addComponent(moveGizmoOnAxis);
        gizmo.addComponent(gizmoMovement);
        gizmo.setShowInEditor(false);
        gizmo.canBeSaved(false);
        SceneManager.currentScene.addEntity(gizmo);

        return gizmo;
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
        gizmoMovement = null;
        GameObject gizmo = SceneManager.currentScene.getGameObjectByName(EngineSettings.editorGizmoName);
        if(gizmo == null) return;
        if(hierarchyPanel != null) hierarchyPanel.setGizmo(null);
        gizmo.remove();
    }

    public void setAspectRatio(float aspectRatio){
        this.aspectRatio = aspectRatio;
        recalculateResolution(true);
    }

    public void recalculateResolution(){
        recalculateResolution(true);
    }

    public void recalculateResolution(boolean refreshGameInstance) {
        int availableHeight = sizeY - 20;

        float targetWidth = availableHeight * aspectRatio;
        if (targetWidth <= sizeX) {
            aspectWidth = Math.round(targetWidth);
            aspectHeight = availableHeight;
        } else {
            aspectWidth = sizeX;
            aspectHeight = Math.round((float)sizeX / aspectRatio);
        }

        if(refreshGameInstance && WindowManager.getInstance() != null){
            WindowManager.getInstance().setWindowSize(aspectWidth, aspectHeight);
        }

        updateMouseOffset = true;
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
                GameObject gizmo = addGizmo();
                gizmo.setEnabled(false);
                addEditorCamera(gizmo);
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
