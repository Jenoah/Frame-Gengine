package nl.framegengine.editor.editorComponents;

import imgui.ImGui;

public class Button {

    public static boolean Regular(String text, float posX, float posY){
        ImGui.setCursorPos(posX, posY);
        return Regular(text);
    }

    public static boolean Regular(String text){
        float buttonWidth = ImGui.calcTextSize(text).x + 16f;
        float buttonHeight = ImGui.calcTextSize(text).y + 16f;
        return ImGui.button(text, buttonWidth, buttonHeight);
    }
}
