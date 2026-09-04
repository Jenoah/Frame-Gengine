package nl.framegengine.editor.editorComponents;

import imgui.ImGui;
import imgui.flag.ImGuiCol;

public class Text {

    public static void ColoredAndCentered(String text, float r, float g, float b) {
        float columnWidth = ImGui.getContentRegionAvailX();
        float textWidth = ImGui.calcTextSize(text).x;

        ImGui.setCursorPosX(
                ImGui.getCursorPosX() + (columnWidth - textWidth) * 0.5f
        );

        ImGui.pushStyleColor(ImGuiCol.Text, r, g, b, 1.0f);
        ImGui.text(text);
        ImGui.popStyleColor();
    }
}
