package nl.framegengine.editor;

import nl.framegengine.editor.panels.GamePanel;

public class EditorLauncher{
    public static void main(String[] args){
        EngineSettings.loadEngineConfig();
        EngineSettings.loadSettings();

        EditorWindow editorWindow = new EditorWindow();
        editorWindow.init();
        editorWindow.setEditorLayout(new EditorLayout());
        editorWindow.editorLayout.getEditorPanelOfType(GamePanel.class).startEngine();
        editorWindow.editorLayout.recalculatePanels();
        editorWindow.run();
        editorWindow.cleanUp();
        System.exit(0);
    }
}
