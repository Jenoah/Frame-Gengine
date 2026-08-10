package nl.framegengine.editor.theme;

import imgui.ImFontConfig;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiStyle;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import nl.framegengine.core.utils.FileHelper;

public class Styling {
    public static void ApplyStyle(){
        ImGuiStyle style = ImGui.getStyle();

        style.setWindowRounding(7.0f);
        style.setChildRounding(6.0f);
        style.setFrameRounding(5.0f);
        style.setPopupRounding(6.0f);
        style.setScrollbarRounding(5.0f);
        style.setGrabRounding(5.0f);
        style.setTabRounding(5.0f);

        style.setWindowBorderSize(1.0f);
        style.setChildBorderSize(0.0f);
        style.setPopupBorderSize(1.0f);
        style.setFrameBorderSize(0.0f);
        style.setTabBorderSize(0.0f);

        style.setWindowPadding(10.0f, 10.0f);
        style.setFramePadding(8.0f, 6.0f);
        style.setCellPadding(6.0f, 4.0f);
        style.setItemSpacing(8.0f, 6.0f);
        style.setItemInnerSpacing(6.0f, 4.0f);

        style.setScrollbarSize(12.0f);
        style.setGrabMinSize(10.0f);
    }

    public static void ApplyColors(){
        ImGuiStyle style = ImGui.getStyle();

        style.setColor(ImGuiCol.WindowBg,       0.094f, 0.102f, 0.122f, 1.0f);
        style.setColor(ImGuiCol.ChildBg,        0.125f, 0.141f, 0.169f, 1.0f);
        style.setColor(ImGuiCol.PopupBg,        0.110f, 0.122f, 0.145f, 1.0f);

        style.setColor(ImGuiCol.MenuBarBg,      0.094f, 0.102f, 0.122f, 1.0f);

        style.setColor(ImGuiCol.TitleBg,        0.082f, 0.094f, 0.114f, 1.0f);
        style.setColor(ImGuiCol.TitleBgActive,  0.082f, 0.094f, 0.114f, 1.0f);

        style.setColor(ImGuiCol.FrameBg,        0.082f, 0.094f, 0.114f, 1.0f);
        style.setColor(ImGuiCol.FrameBgHovered, 0.161f, 0.184f, 0.220f, 1.0f);
        style.setColor(ImGuiCol.FrameBgActive,  0.188f, 0.220f, 0.278f, 1.0f);

        style.setColor(ImGuiCol.Button,         0.125f, 0.141f, 0.169f, 1.0f);
        style.setColor(ImGuiCol.ButtonHovered,  0.161f, 0.184f, 0.220f, 1.0f);
        style.setColor(ImGuiCol.ButtonActive,   0.188f, 0.220f, 0.278f, 1.0f);

        style.setColor(ImGuiCol.Header,         0.125f, 0.141f, 0.169f, 1.0f);
        style.setColor(ImGuiCol.HeaderHovered,  0.161f, 0.184f, 0.220f, 1.0f);
        style.setColor(ImGuiCol.HeaderActive,   0.231f, 0.510f, 0.965f, 1.0f);

        style.setColor(ImGuiCol.Border,         0.105f, 0.121f, 0.149f, 1.0f);

        style.setColor(ImGuiCol.Text,           0.898f, 0.906f, 0.922f, 1.0f);
        style.setColor(ImGuiCol.TextDisabled,   0.420f, 0.447f, 0.490f, 1.0f);
    }

    public static void ApplyFonts(){
        ImGuiIO io = ImGui.getIO();
        byte[] jostFont = FileHelper.loadResourceAsBytes("/fonts/Jost-Regular.ttf");
        io.getFonts().addFontFromMemoryTTF(jostFont, 18.0f);

        ImFontConfig iconConfig = new ImFontConfig();
        iconConfig.setMergeMode(true);
        iconConfig.setPixelSnapH(true);
        iconConfig.getGlyphOffset(new ImVec2(0f, 9f));

        byte[] iconsFont = FileHelper.loadResourceAsBytes("/fonts/bootstrap-icons.ttf");

        short[] iconRanges = new short[] {
                (short) 0xF000,
                (short) 0xF8FF,
                0
        };

        io.getFonts().addFontFromMemoryTTF(iconsFont, 10.0f, iconConfig, iconRanges);

        iconConfig.destroy();


        io.getFonts().build();
    }
}
