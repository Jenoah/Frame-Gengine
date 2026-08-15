package nl.framegengine.editor.editorComponents;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;

public class Panel {
    private static float panelWidth;
    private static final float paddingX = ImGui.getStyle().getWindowPaddingX();
    private static final float paddingY = ImGui.getStyle().getWindowPaddingY();

    public static void startPanel(){
        ImDrawList drawList = ImGui.getWindowDrawList();

        panelWidth = ImGui.getContentRegionAvailX();

        drawList.channelsSplit(2);
        drawList.channelsSetCurrent(1);

        ImGui.beginGroup();

        ImGui.setCursorPosX(ImGui.getCursorPosX() + paddingX);
        ImGui.setCursorPosY(ImGui.getCursorPosY() + paddingY);
    }

    public static void startPanel(int width){
        ImDrawList drawList = ImGui.getWindowDrawList();

        panelWidth = width;

        drawList.channelsSplit(2);
        drawList.channelsSetCurrent(1);

        ImGui.beginGroup();

        ImGui.setCursorPosX(ImGui.getCursorPosX() + paddingX);
        ImGui.setCursorPosY(ImGui.getCursorPosY() + paddingY);
    }

    public static void endPanel() {
        ImDrawList drawList = ImGui.getWindowDrawList();

        // Bottom padding
        ImGui.dummy(0, paddingY);

        ImGui.endGroup();

        ImVec2 min = ImGui.getItemRectMin();
        ImVec2 max = ImGui.getItemRectMax();

        // Force the panel to span the entire available width.
        max.x = min.x + panelWidth;

        drawList.channelsSetCurrent(0);

        drawList.addRectFilled(
                min.x,
                min.y,
                max.x,
                max.y,
                ImGui.getColorU32(ImGuiCol.ChildBg),
                6.0f
        );

        drawList.addRect(
                min.x,
                min.y,
                max.x,
                max.y,
                ImGui.getColorU32(ImGuiCol.Border),
                6.0f
        );

        drawList.channelsMerge();
        ImGui.spacing();
    }

    public static float getPaddingX() {
        return paddingX;
    }

    public static float getPaddingY() {
        return paddingY;
    }

    public static float getPanelWidth() {
        return panelWidth;
    }
}
