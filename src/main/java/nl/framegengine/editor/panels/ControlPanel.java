package nl.framegengine.editor.panels;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImInt;
import nl.framegengine.editor.EditorPanel;
import nl.framegengine.editor.ImGuiHelper;
import nl.framegengine.editor.editorComponents.Button;
import nl.framegengine.editor.editorComponents.Icons;

public class ControlPanel extends EditorPanel {

    private GamePanel gamePanel;
    private final String[] aspectRatios = new String[]{"16 x 9", "16 x 10", "Freeform"};
    private final ImInt currentAspectRatio = new ImInt(0);

    public ControlPanel(int posX, int posY, int sizeX, int sizeY) {
        super(posX, posY, sizeX, sizeY);
        addWindowFlag(ImGuiWindowFlags.NoTitleBar);
        addWindowFlag(ImGuiWindowFlags.NoScrollbar);
        addWindowFlag(ImGuiWindowFlags.NoScrollWithMouse);
    }

    @Override
    public void renderFrame() {
        float buttonWidth = 64;
        float buttonHeight = 32; // Height for buttons
        int buttonCount = 4;
        int comboWidth = ImGuiHelper.calculateTextWidth(aspectRatios) + 32;
        int spacing = 20;

        float totalWidth = buttonWidth * buttonCount + comboWidth + buttonCount * spacing;

        float startX = (sizeX - totalWidth) * 0.5f;
        float startY = (sizeY - buttonHeight) * 0.5f;

        ImGui.setCursorPos(startX, startY);

        if (Button.regular(Icons.PLAY)) {
            if (gamePanel != null) gamePanel.startGame();
        }
        ImGui.sameLine(0, spacing);

        if (Button.regular(Icons.STOP)) {
            if (gamePanel != null) gamePanel.stopGame();
        }
        ImGui.sameLine(0, spacing);

        if (Button.regular("Stats")) {
            if (gamePanel != null) gamePanel.toggleStats();
        }
        ImGui.sameLine(0, spacing);

        if (Button.regular("Wireframe")) {
            if (gamePanel != null) gamePanel.toggleWireframe();
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
