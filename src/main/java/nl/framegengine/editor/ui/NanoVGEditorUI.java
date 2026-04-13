package nl.framegengine.editor.ui;

/**
 * NanoVG implementation of {@link IEditorUI}.
 *
 * <p>Owns a {@link NanoVGContext} and delegates {@link #beginFrame} /
 * {@link #endFrame} to it.  This is the concrete UI layer that will drive all
 * NanoVG panel rendering once the migration is complete.
 *
 * <p>During the migration period this class co-exists with ImGui.  It is
 * wired into {@link nl.framegengine.editor.EditorWindow} alongside the existing
 * ImGui infrastructure; the smoke-test draw call (Task 1.1) runs through it.
 *
 * <p>Task 1.2 — Abstraction Interfaces.
 */
public class NanoVGEditorUI implements IEditorUI {

    private final NanoVGContext nvgContext;

    /**
     * Creates a {@code NanoVGEditorUI} that wraps an existing, already-initialised
     * {@link NanoVGContext}.
     *
     * <p>{@link NanoVGContext#init()} must have been called before this constructor
     * is invoked.
     *
     * @param nvgContext the NanoVG context owned by {@link nl.framegengine.editor.EditorWindow}
     */
    public NanoVGEditorUI(NanoVGContext nvgContext) {
        this.nvgContext = nvgContext;
    }

    // -------------------------------------------------------------------------
    // IEditorUI
    // -------------------------------------------------------------------------

    @Override
    public void beginFrame(int windowWidth, int windowHeight, float devicePixelRatio) {
        nvgContext.beginFrame(windowWidth, windowHeight, devicePixelRatio);
    }

    @Override
    public void endFrame() {
        nvgContext.endFrame();
    }

    @Override
    public NanoVGContext getNVGContext() {
        return nvgContext;
    }
}
