package nl.framegengine.editor.ui;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NanoVGGL3;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;

/**
 * Owns the NanoVG context handle for the editor.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Create and destroy the {@code NVGContext} (via {@link NanoVGGL3#nvgCreate}).</li>
 *   <li>Load the default TTF font via {@link NanoVG#nvgCreateFontMem}.</li>
 *   <li>Expose {@link #beginFrame}, {@link #endFrame}, and {@link #cancelFrame} for the render loop.</li>
 * </ul>
 *
 * <p>Must be initialised <em>after</em> an OpenGL context is current on the calling thread.
 *
 * <p>Task 1.1 — NanoVG Context Setup.
 */
public class NanoVGContext {

    /** Name registered with NanoVG for the default font. */
    public static final String DEFAULT_FONT = "default";

    /** Path to the default TTF font relative to the working directory (project root). */
    private static final String DEFAULT_FONT_PATH = "fonts/jetbrains/JetBrainsMono-Regular.ttf";

    /**
     * The raw NanoVG context handle.  A value of {@code 0L} indicates the context
     * has not been created or has already been destroyed.
     */
    private long nvgHandle = 0L;

    /**
     * Keep a strong reference to the font data buffer so it is not GC'd while
     * NanoVG still holds a pointer to it (NanoVG does not copy font data by
     * default when {@code freeData = false}).
     */
    @SuppressWarnings("FieldCanBeLocal")
    private ByteBuffer fontDataBuffer;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Creates the NanoVG GL3 context and loads the default font.
     *
     * @throws IllegalStateException if the context cannot be created or the font cannot be loaded.
     */
    public void init() {
        nvgHandle = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        if (nvgHandle == 0L) {
            throw new IllegalStateException("Failed to create NanoVG context.");
        }

        loadDefaultFont();
    }

    /**
     * Destroys the NanoVG context.  Safe to call even if {@link #init()} was never called.
     */
    public void destroy() {
        if (nvgHandle != 0L) {
            nvgDelete(nvgHandle);
            nvgHandle = 0L;
        }
    }

    // -------------------------------------------------------------------------
    // Frame lifecycle
    // -------------------------------------------------------------------------

    /**
     * Begins a new NanoVG frame.
     *
     * <p>Must be called every frame before any NanoVG draw calls.
     * {@link #endFrame()} or {@link #cancelFrame()} must be called before the next
     * invocation.
     *
     * @param windowWidth       framebuffer width in pixels
     * @param windowHeight      framebuffer height in pixels
     * @param devicePixelRatio  ratio of framebuffer pixels to logical pixels (e.g. 2.0 on Retina)
     */
    public void beginFrame(int windowWidth, int windowHeight, float devicePixelRatio) {
        nvgBeginFrame(nvgHandle, windowWidth, windowHeight, devicePixelRatio);
    }

    /**
     * Ends the current NanoVG frame and flushes all pending draw calls to OpenGL.
     */
    public void endFrame() {
        nvgEndFrame(nvgHandle);
    }

    /**
     * Cancels the current NanoVG frame without flushing draw calls.
     * Use when the frame must be aborted (e.g., during resize).
     */
    public void cancelFrame() {
        nvgCancelFrame(nvgHandle);
    }

    // -------------------------------------------------------------------------
    // Smoke-test drawing primitive
    // -------------------------------------------------------------------------

    /**
     * Draws a filled rectangle using NanoVG draw calls.
     *
     * <p>Intended as a smoke-test to verify the NanoVG context is operational.
     * This method is called from {@code EditorWindow} until Task 1.2 introduces
     * proper panel rendering.
     *
     * @param x      left edge in pixels
     * @param y      top edge in pixels
     * @param width  rectangle width in pixels
     * @param height rectangle height in pixels
     * @param r      red   component [0, 1]
     * @param g      green component [0, 1]
     * @param b      blue  component [0, 1]
     * @param a      alpha component [0, 1]
     */
    public void drawFilledRect(float x, float y, float width, float height,
                               float r, float g, float b, float a) {
        try (NVGColor color = NVGColor.calloc()) {
            nvgRGBAf(r, g, b, a, color);
            nvgBeginPath(nvgHandle);
            nvgRect(nvgHandle, x, y, width, height);
            nvgFillColor(nvgHandle, color);
            nvgFill(nvgHandle);
        }
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the raw NanoVG context handle for use by widgets and panels that
     * call {@link NanoVG} / {@link NanoVGGL3} methods directly.
     *
     * @return the context handle; never {@code 0L} after a successful {@link #init()}.
     */
    public long getHandle() {
        return nvgHandle;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Reads the default TTF font from the filesystem and registers it with NanoVG.
     *
     * <p>The font file is resolved relative to the working directory (project root
     * when running from the IDE or via Gradle).
     *
     * @throws IllegalStateException if the font file cannot be found or loaded.
     */
    private void loadDefaultFont() {
        File fontFile = new File(DEFAULT_FONT_PATH);
        if (!fontFile.exists()) {
            throw new IllegalStateException(
                    "Default font not found at: " + fontFile.getAbsolutePath());
        }

        try (FileInputStream fontStream = new FileInputStream(fontFile)) {
            byte[] fontBytes = fontStream.readAllBytes();
            fontDataBuffer = ByteBuffer.allocateDirect(fontBytes.length);
            fontDataBuffer.put(fontBytes).flip();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to read default font: " + fontFile.getAbsolutePath(), e);
        }

        int fontId = nvgCreateFontMem(nvgHandle, DEFAULT_FONT, fontDataBuffer, false);
        if (fontId == -1) {
            throw new IllegalStateException(
                    "NanoVG failed to parse default font: " + fontFile.getAbsolutePath());
        }
    }
}
