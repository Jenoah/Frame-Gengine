package nl.framegengine.editor.panels;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImInt;
import nl.framegengine.editor.EditorPanel;
import nl.framegengine.editor.ImGuiHelper;
import org.joml.Math;

public class ControlPanel extends EditorPanel {

    private GamePanel gamePanel;
    private String[] aspectRatios = new String[]{"16 x 9", "16 x 10", "Freeform"};
    private ImInt currentAspectRatio = new ImInt(0);

    public ControlPanel(int posX, int posY, int sizeX, int sizeY) {
        super(posX, posY, sizeX, sizeY);
        addWindowFlag(ImGuiWindowFlags.NoTitleBar);
        addWindowFlag(ImGuiWindowFlags.NoScrollbar);
        addWindowFlag(ImGuiWindowFlags.NoScrollWithMouse);
    }

    @Override
    public void renderFrame() {
        int buttonWidth = 64;
        int buttonHeight = 32; // Height for buttons
        int buttonCount = 3;
        int comboWidth = ImGuiHelper.calculateTextWidth(aspectRatios) + 32;
        int spacing = 20;

        int totalWidth = buttonWidth * buttonCount + comboWidth + buttonCount * spacing;

        float startX = (sizeX - totalWidth) * 0.5f;
        float startY = (sizeY - buttonHeight) * 0.5f;

        ImGui.setCursorPos(startX, startY);

        if (ImGui.button("Play", buttonWidth, buttonHeight)) {
            if (gamePanel != null) gamePanel.startGame();
        }
        ImGui.sameLine(0, spacing);

        if (ImGui.button("Stop", buttonWidth, buttonHeight)) {
            if (gamePanel != null) gamePanel.stopGame();
        }
        ImGui.sameLine(0, spacing);

        if (ImGui.button("Toggle Stats", buttonWidth, buttonHeight)) {
            if (gamePanel != null) gamePanel.toggleStats();
        }
        ImGui.sameLine(0, spacing);

        // Aspect Ratio Combo: The ImGui Java binding differs from C++ here
        ImGui.setNextItemWidth(comboWidth);
        if (ImGui.combo("Aspect ratio", currentAspectRatio, aspectRatios)) {
            switch (currentAspectRatio.get()) {
                case 0:
                    gamePanel.setAspectRatio(16f / 9f);
                    break;
                case 1:
                    gamePanel.setAspectRatio(16f / 10f);
                    break;
                default:
                    gamePanel.setAspectRatio(0);
                    break;
            }
        }
    }

    public void setGamePanel(GamePanel gamePanel){
        this.gamePanel = gamePanel;
    }
}
