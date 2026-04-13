package nl.framegengine.editor.ui;

/**
 * Toolkit-agnostic abstraction for a single editor panel.
 *
 * <p>Every panel in the editor (Hierarchy, Game, Console, etc.) must implement
 * this interface.  During the migration period both the existing ImGui-backed
 * {@link nl.framegengine.editor.EditorPanel} subclasses and the new NanoVG
 * implementations satisfy this contract, allowing {@link nl.framegengine.editor.EditorLayout}
 * to drive them uniformly.
 *
 * <p>The three-method frame protocol mirrors the existing ImGui panel pattern:
 * <ol>
 *   <li>{@link #prepareFrame()} — set up window/clip state before any draw calls.</li>
 *   <li>{@link #renderFrame()} — issue all draw calls for this panel.</li>
 *   <li>{@link #endFrame()}    — tear down window/clip state after draw calls.</li>
 * </ol>
 *
 * <p>Task 1.2 — Abstraction Interfaces.
 */
public interface IPanel {

    /**
     * Sets the position and dimensions of this panel in framebuffer pixels.
     *
     * <p>Called by {@link nl.framegengine.editor.EditorLayout} whenever the
     * window is resized.  Implementations must store these values and apply
     * them in {@link #prepareFrame()}.
     *
     * @param x      left edge in pixels
     * @param y      top edge in pixels
     * @param width  panel width in pixels
     * @param height panel height in pixels
     */
    void setPosition(float x, float y, float width, float height);

    /**
     * Prepares the UI context for this panel's draw calls.
     *
     * <p>For ImGui panels this opens the window ({@code ImGui.begin(...)}).
     * For NanoVG panels this will set up the scissor region.
     */
    void prepareFrame();

    /**
     * Issues all draw calls for this panel's content.
     *
     * <p>Must be called after {@link #prepareFrame()} and before {@link #endFrame()}.
     */
    void renderFrame();

    /**
     * Finalises this panel's draw state for the current frame.
     *
     * <p>For ImGui panels this calls {@code ImGui.end()}.
     * For NanoVG panels this will restore the scissor region.
     */
    void endFrame();
}
