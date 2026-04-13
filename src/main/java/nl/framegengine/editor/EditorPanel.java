package nl.framegengine.editor;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import nl.framegengine.editor.ui.IPanel;

public abstract class EditorPanel implements IPanel {

    protected int posX;
    protected int posY;
    protected int sizeX;
    protected int sizeY;
    protected String windowName;
    protected int windowFlags;
    protected boolean inFocus;


    public EditorPanel(int posX, int posY, int sizeX, int sizeY){
        this.posX = posX;
        this.posY = posY;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.windowName = getClass().getSimpleName();
        this.windowFlags = ImGuiWindowFlags.NoMove |
                ImGuiWindowFlags.NoResize |
                ImGuiWindowFlags.NoCollapse;
    }

    public void prepareFrame(){
        ImGui.setNextWindowPos(posX, posY);
        ImGui.setNextWindowSize(sizeX, sizeY);
        ImGui.begin(windowName, windowFlags);
    }

    public abstract void renderFrame();

    public void endFrame(){
        ImGui.end();
    }

    public void addWindowFlag(int windowFlags){
        this.windowFlags = this.windowFlags | windowFlags;
    }

    public void setSizeAndPosition(int posX, int posY, int sizeX, int sizeY){
        this.posX = posX;
        this.posY = posY;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link #setSizeAndPosition(int, int, int, int)} by
     * truncating the float values to int.  This preserves compatibility with
     * the existing ImGui-backed panels during the migration period.
     */
    @Override
    public void setPosition(float x, float y, float width, float height) {
        setSizeAndPosition((int) x, (int) y, (int) width, (int) height);
    }
}
