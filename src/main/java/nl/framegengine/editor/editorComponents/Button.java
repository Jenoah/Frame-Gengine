package nl.framegengine.editor.editorComponents;

import imgui.ImGui;

public class Button {

    public static boolean regular(String text, float posX, float posY){
        ImGui.setCursorPos(posX, posY);
        return regular(text, false);
    }

    public static boolean regular(String text, float posX, float posY, boolean autoWidth){
        ImGui.setCursorPos(posX, posY);
        return regular(text, autoWidth);
    }

    public static boolean regular(String text){
        return regular(text, false);
    }

    public static boolean regular(String text, boolean autoWidth){
        if(!autoWidth) {
            return ImGui.button(text);
        }

        float width = ImGui.getContentRegionAvailX();
        float height = ImGui.calcTextSize(text).y + 16f;

        return ImGui.button(text, width, height);
    }
}
