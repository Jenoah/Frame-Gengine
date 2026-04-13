package nl.framegengine.editor.ui;

/**
 * Toolkit-agnostic abstraction for the editor UI layer.
 *
 * <p>Implementations are responsible for managing the per-frame lifecycle of a
 * particular UI toolkit (NanoVG, ImGui, etc.).  Concrete classes should own
 * the corresponding context object and delegate frame calls to it.
 *
 * <p>Task 1.2 — Abstraction Interfaces.
 */
public interface IEditorUI {

    /**
     * Signals the start of a new UI frame.
     *
     * @param windowWidth       framebuffer width in pixels
     * @param windowHeight      framebuffer height in pixels
     * @param devicePixelRatio  ratio of framebuffer pixels to logical (CSS) pixels
     *                          (e.g. {@code 2.0} on a Retina / HiDPI display)
     */
    void beginFrame(int windowWidth, int windowHeight, float devicePixelRatio);

    /**
     * Signals the end of the current UI frame and flushes all pending draw calls.
     */
    void endFrame();

    /**
     * Returns the {@link NanoVGContext} associated with this UI instance.
     *
     * <p>Panels and widgets that need to issue NanoVG draw calls directly
     * (e.g. custom primitives) should obtain the raw handle from here rather
     * than maintaining their own context reference.
     *
     * @return the NanoVG context; never {@code null} after initialisation.
     */
    NanoVGContext getNVGContext();
}
