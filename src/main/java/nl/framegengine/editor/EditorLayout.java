package nl.framegengine.editor;

import imgui.ImGui;
import nl.framegengine.editor.panels.*;

public class EditorLayout {
    private final EditorPanel[] editorPanels = new EditorPanel[7];
    private GameSettingsPopupPanel gameSettingsPopupPanel;

    public EditorLayout(){
        gameSettingsPopupPanel = new GameSettingsPopupPanel(0,0, 0, 0);

        editorPanels[0] = new HierarchyPanel(0, 0, 0, 0);
        editorPanels[1] = new GamePanel(0, 0, 0, 0);
        editorPanels[2] = new ConsolePanel(0, 0, 0, 0);
        editorPanels[3] = new InfoPanel(0, 0, 0, 0);
        editorPanels[4] = new ProjectPanel(0, 0, 0, 0);
        editorPanels[5] = new ControlPanel(0, 0, 0, 0);
        editorPanels[6] = gameSettingsPopupPanel;

        recalculatePanels();

        ((HierarchyPanel)editorPanels[0]).setInfoPanel(((InfoPanel)editorPanels[3]));
        ((ControlPanel)editorPanels[5]).setGamePanel((GamePanel)editorPanels[1]);
        gameSettingsPopupPanel.updateTextureList();
    }

    public void recalculatePanels(){
        editorPanels[0].setSizeAndPosition(0, 18, fromPercentageX(20), fromPercentageY(70) - 18);
        editorPanels[1].setSizeAndPosition(fromPercentageX(20), 18, fromPercentageX(60), fromPercentageY(60) - 18);
        editorPanels[2].setSizeAndPosition(fromPercentageX(20), fromPercentageY(70), fromPercentageX(85), fromPercentageY(30));
        editorPanels[3].setSizeAndPosition(fromPercentageX(80), 18, fromPercentageX(20), fromPercentageY(70) - 18);
        editorPanels[4].setSizeAndPosition(0, fromPercentageY(70), fromPercentageX(20), fromPercentageY(30));
        editorPanels[5].setSizeAndPosition(fromPercentageX(20), fromPercentageY(60), fromPercentageX(60), fromPercentageY(10));
        editorPanels[6].setSizeAndPosition(0, 0, fromPercentageX(50), fromPercentageY(80));

        ((GamePanel)editorPanels[1]).recalculateResolution();
    }

    public static int fromPercentageX(int absoluteX){
        return (int)(EditorWindow.windowWidth / 100f * absoluteX / EditorWindow.windowScaleX);
    }

    public static int fromPercentageY(int absoluteY){
        return (int)(EditorWindow.windowHeight / 100f * absoluteY / EditorWindow.windowScaleY);
    }

    public void renderLayout(){
        for (EditorPanel editorPanel : editorPanels) {
            editorPanel.prepareFrame();
            editorPanel.renderFrame();
            editorPanel.endFrame();
        }
        renderMenuBar();
        ImGuiHelper.drawProgressBar();
    }

    private void renderMenuBar(){
        ImGui.beginMainMenuBar();
        if(ImGui.beginMenu("Project")){
            if(ImGui.menuItem("New")) EngineSettings.createNewProject();
            if(ImGui.menuItem("Open")) EngineSettings.loadProject();
            if(ImGui.menuItem("Save")) EngineSettings.saveSettings();
            if(ImGui.menuItem("Settings")) gameSettingsPopupPanel.show();
            if(ImGui.beginMenu("Export")) {
                if (ImGui.menuItem("Export Mac")) EngineSettings.buildProjectMac();
                if (ImGui.menuItem("Export Windows")) EngineSettings.buildProjectWindows();
                ImGui.endMenu();
            }
            ImGui.endMenu();
        }
        ImGui.endMainMenuBar();
    }
}
