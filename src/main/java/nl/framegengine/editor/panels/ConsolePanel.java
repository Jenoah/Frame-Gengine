package nl.framegengine.editor.panels;

import imgui.ImGui;
import nl.framegengine.editor.EditorPanel;
import nl.framegengine.core.debugging.ConsoleColors;
import nl.framegengine.core.debugging.Debug;

public class ConsolePanel extends EditorPanel {

    private int previousLogEntriesCount = 0;

    public ConsolePanel(int posX, int posY, int sizeX, int sizeY) {
        super(posX, posY, sizeX, sizeY);
    }

    @Override
    public void renderFrame() {
        ImGui.setWindowFontScale(1.2f);
        for (Debug.LogEntry entry : Debug.GetLog().stream().toList()) {
            for (ConsoleColors seg : entry.segments) {
                ImGui.textColored(seg.color[0], seg.color[1], seg.color[2], seg.color[3], seg.text);
                ImGui.sameLine(0, 0);
            }
            ImGui.newLine();
        }
        if(previousLogEntriesCount != Debug.GetLog().size()){
            ImGui.setScrollHereY(1.0f);
            previousLogEntriesCount = Debug.GetLog().size();
        }

    }
}