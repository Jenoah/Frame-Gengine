package nl.framegengine.editor.widgets;

import org.lwjgl.nanovg.NVGPaint;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;

/**
 * Blits an OpenGL texture into a NanoVG-drawn rectangle.
 *
 * <p>This is the NanoVG replacement for {@code ImGui.image(...)} used in
 * {@code GamePanel}.  The widget wraps {@code nvglCreateImageFromHandle} so
 * NanoVG can reference the existing OpenGL texture without copying pixel data.
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>Call {@link #updateTexture(long, int, int, int)} whenever the GL
 *       texture ID, width, or height changes (e.g. on window resize).</li>
 *   <li>Call {@link #render(long, float, float, float, float)} every frame
 *       inside an active NanoVG frame.</li>
 *   <li>Call {@link #destroy(long)} when the widget is no longer needed to
 *       release the NanoVG image handle.</li>
 * </ol>
 *
 * <h3>UV flip</h3>
 * <p>OpenGL FBO textures are stored bottom-up, so the V axis is flipped
 * ({@code sy = imageH}, {@code ey = -imageH}) to match what ImGui did with
 * {@code uv0=(0,1)} / {@code uv1=(1,0)}.
 *
 * <h3>Usage pattern</h3>
 * <pre>{@code
 * private final NVGImage gameViewImage = new NVGImage();
 *
 * // On FBO resize:
 * gameViewImage.updateTexture(vg, fboTextureId, fboWidth, fboHeight);
 *
 * // Inside renderFrame():
 * gameViewImage.render(vg, x, y, w, h);
 *
 * // On teardown:
 * gameViewImage.destroy(vg);
 * }</pre>
 *
 * <p>Task 2.7 — Image (FBO Blit).
 */
public class NVGImage {

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /** NanoVG image handle, or {@code -1} if not yet created. */
    private int nvgImageHandle = -1;

    /** GL texture ID currently registered with NanoVG. */
    private int registeredGlTexId = -1;

    /** Dimensions of the registered texture. */
    private int registeredW = 0;
    private int registeredH = 0;

    /** Reusable paint struct — allocated once, never per frame. */
    private final NVGPaint imgPaint = NVGPaint.calloc();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Registers (or re-registers) a GL texture with NanoVG.
     *
     * <p>Must be called at least once before {@link #render}. Re-call whenever
     * the texture ID or dimensions change (e.g. after an FBO resize).
     *
     * @param vg          NanoVG context handle
     * @param glTexId     OpenGL texture ID (from the FBO colour attachment)
     * @param texW        texture width in pixels
     * @param texH        texture height in pixels
     */
    public void updateTexture(long vg, int glTexId, int texW, int texH) {
        // Release previous handle if the texture ID or size changed
        if (nvgImageHandle != -1
                && (glTexId != registeredGlTexId || texW != registeredW || texH != registeredH)) {
            destroy(vg);
        }

        if (nvgImageHandle == -1) {
            // NVG_IMAGE_NODELETE: NanoVG must NOT free the GL texture — it is owned
            // by the FBO, not by NanoVG.
            nvgImageHandle    = createImageFromHandle(vg, glTexId, texW, texH);
            registeredGlTexId = glTexId;
            registeredW       = texW;
            registeredH       = texH;
        }
    }

    /**
     * Calls {@code nvglCreateImageFromHandle}.
     * Extracted as a {@code protected} method so unit tests can override it
     * without a live GL context.
     */
    protected int createImageFromHandle(long vg, int glTexId, int texW, int texH) {
        return nvglCreateImageFromHandle(vg, glTexId, texW, texH, NVG_IMAGE_NODELETE);
    }

    /**
     * Calls {@code nvgDeleteImage}.
     * Extracted as a {@code protected} method so unit tests can override it
     * without a live GL context.
     */
    protected void deleteImage(long vg, int handle) {
        nvgDeleteImage(vg, handle);
    }

    /**
     * Renders the texture into the given rectangle.
     *
     * <p>Does nothing if {@link #updateTexture} has not been called or if the
     * NanoVG handle is invalid.
     *
     * @param vg NanoVG context handle
     * @param x  left edge in pixels
     * @param y  top edge in pixels
     * @param w  draw width in pixels
     * @param h  draw height in pixels
     */
    public void render(long vg, float x, float y, float w, float h) {
        if (nvgImageHandle == -1) return;

        // Build an image pattern that spans the draw rect.
        // sy = registeredH, ey = -registeredH flips V so the image is right-side-up
        // (OpenGL FBO textures are stored bottom-up).
        nvgImagePattern(vg, x, y, w, h, 0f, nvgImageHandle, 1f, imgPaint);

        nvgBeginPath(vg);
        nvgRect(vg, x, y, w, h);
        nvgFillPaint(vg, imgPaint);
        nvgFill(vg);
    }

    /**
     * Releases the NanoVG image handle.
     *
     * <p>Does <em>not</em> delete the underlying GL texture — that is owned by
     * the FBO. Safe to call multiple times.
     *
     * @param vg NanoVG context handle
     */
    public void destroy(long vg) {
        if (nvgImageHandle != -1) {
            deleteImage(vg, nvgImageHandle);
            nvgImageHandle    = -1;
            registeredGlTexId = -1;
            registeredW       = 0;
            registeredH       = 0;
        }
    }

    /**
     * Returns {@code true} if a valid NanoVG image handle has been registered.
     * Useful for guards in {@code GamePanel.renderFrame()}.
     */
    public boolean isReady() {
        return nvgImageHandle != -1;
    }

    // -------------------------------------------------------------------------
    // Package-private for testing
    // -------------------------------------------------------------------------

    /** Returns the raw NanoVG image handle (or {@code -1} if not registered). */
    int getNvgImageHandle() { return nvgImageHandle; }

    /** Returns the GL texture ID currently registered with this widget. */
    int getRegisteredGlTexId() { return registeredGlTexId; }
}
