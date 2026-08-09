package nl.framegengine.editor.editorComponents;

import imgui.ImGui;

public class Button {

    public static boolean Regular(String text, float posX, float posY){
        ImGui.setCursorPos(posX, posY);
        return Regular(text, false);
    }

    public static boolean Regular(String text, float posX, float posY, boolean autoWidth){
        ImGui.setCursorPos(posX, posY);
        return Regular(text, autoWidth);
    }

    public static boolean Regular(String text){
        return Regular(text, false);
    }

    public static boolean Regular(String text, boolean autoWidth){
        if(!autoWidth) {
            return ImGui.button(text);
        }

        float width = ImGui.getContentRegionAvailX();
        float height = ImGui.calcTextSize(text).y + 16f;

        return ImGui.button(text, width, height);
    }
}
