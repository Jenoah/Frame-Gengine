package nl.framegengine.editor.panels;

import imgui.ImGui;
import nl.framegengine.core.debugging.ConsoleColors;
import nl.framegengine.core.debugging.Debug;
import nl.framegengine.editor.EditorPanel;
import nl.framegengine.editor.editorComponents.Icons;

public class ConsolePanel extends EditorPanel {

    private int previousLogEntriesCount = 0;

    public ConsolePanel(int posX, int posY, int sizeX, int sizeY) {
        super(posX, posY, sizeX, sizeY);
        windowName = Icons.TERMINAL + " Console";
    }

    @Override
    public void renderFrame() {
        ImGui.setWindowFontScale(1.2f);
        for (Debug.LogEntry entry : Debug.getLog().stream().toList()) {
            for (ConsoleColors seg : entry.segments) {
                ImGui.textColored(seg.color[0], seg.color[1], seg.color[2], seg.color[3], seg.text);
                ImGui.sameLine(0, 0);
            }
            ImGui.newLine();
        }
        if(previousLogEntriesCount != Debug.getLog().size()){
            ImGui.setScrollHereY(1.0f);
            previousLogEntriesCount = Debug.getLog().size();
        }

        ImGui.setCursorScreenPos(posX + sizeX - 192, posY + 48);
        if(ImGui.button("Clear", 72, 32)){
            Debug.clearLogs();
        }
    }
}