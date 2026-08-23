package nl.framegengine.editor.panels;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImInt;
import imgui.type.ImString;
import nl.framegengine.core.utils.FileHelper;
import nl.framegengine.editor.*;
import nl.framegengine.editor.editorComponents.Button;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameSettingsPopupPanel extends EditorPanel {

    private boolean isShowing = false;
    private boolean shouldHide = false;
    private String projectName = "";
    private ImString projectNameBuffer = new ImString(256);

    private String[] textureNames = new String[0];

    public GameSettingsPopupPanel(int posX, int posY, int sizeX, int sizeY) {
        super(posX, posY, sizeX, sizeY);
        ManifestHelper.addEventCallback(() -> updateTextureList());
    }

    @Override
    public void prepareFrame(){ }
    @Override
    public void endFrame(){ }

    @Override
    public void renderFrame() {
        if(isShowing){
            ImGui.openPopup("Project Settings");
            isShowing = false;
        }
        ImGui.setNextWindowSize(sizeX, sizeY);
        if(ImGui.beginPopupModal("Project Settings", ImGuiWindowFlags.AlwaysAutoResize)) {
            ImGui.separatorText("Game Settings");

            //Project Name
            ImGui.inputText("Project name", projectNameBuffer);
            ImGui.newLine();

            //Icon
            String currentSelectedName = FileHelper.getFileName(ManifestHelper.getPathByGuid(ManifestHelper.manifestFileType.TEXTURE, EngineSettings.currentProjectIconGuid)) + "##" + EngineSettings.currentProjectIconGuid;
            ImInt currentSelectedItem = new ImInt(Arrays.stream(textureNames).toList().indexOf(currentSelectedName));

            if (ImGui.combo("Icon", currentSelectedItem, textureNames)) {
                String textureGUID = ImGuiHelper.guidFromName(textureNames[currentSelectedItem.get()]);
                EngineSettings.currentProjectIconGuid = textureGUID;
            }
            ImGui.setCursorPos(16, sizeY - 32);
            if(Button.regular("Save settings")){
                saveSettings();
                EngineSettings.saveSettings();
                hide();
            }
            ImGui.sameLine();
            if(Button.regular("Cancel")){
                hide();
            }

            if (shouldHide) {
                shouldHide = false;
                ImGui.closeCurrentPopup();
            }
            ImGui.endPopup();
        }
    }

    public void show(){
        projectNameBuffer.set(EngineSettings.currentProjectName);
        isShowing = true;
    }

    public void hide(){
        shouldHide = true;
    }

    public void saveSettings(){
        EngineSettings.currentProjectName = projectNameBuffer.get();
    }

    public void updateTextureList(){
        List<String> manifestItems = new ArrayList<>();
        ManifestHelper.getTextures().forEach(manifestItem -> {
            manifestItems.add(manifestItem.get("filename")+"##"+manifestItem.get("guid"));
        });
        textureNames = manifestItems.toArray(new String[0]);
    }
}
