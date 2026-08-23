package nl.framegengine.editor.editorComponents;

import imgui.ImGui;

public class Collapse {

    public static boolean Regular(String text, float posX, float posY){
        ImGui.setCursorPos(posX, posY);
        return Regular(text);
    }

    public static boolean Regular(String text){
        return ImGui.collapsingHeader(text);
    }

    public static CollapseWithButton WithButton(String text){
        String[] guids = text.split("##");
        String guid = guids[guids.length - 1];

        return WithButton(text, guid);
    }

    public static CollapseWithButton WithButton(String text, String id){
        CollapseWithButton collapseWithButton = new CollapseWithButton();

        collapseWithButton.isExpanded = ImGui.getStateStorage().getBool(id.hashCode(), false);

        if (ImGui.button((collapseWithButton.isExpanded ? Icons.CARET_DOWN : Icons.CARET_RIGHT) + "##arrow_" + id.hashCode())) {
                collapseWithButton.isExpanded = !collapseWithButton.isExpanded;
                ImGui.getStateStorage().setBool(id.hashCode(), collapseWithButton.isExpanded);
        }

        ImGui.sameLine(0, 2);
        collapseWithButton.isPressed = Button.regular(text, true);

        return collapseWithButton;
    }

    public static class CollapseWithButton {
        public boolean isExpanded;
        public boolean isPressed;
    }
}
