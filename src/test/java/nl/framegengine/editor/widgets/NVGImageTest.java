package nl.framegengine.editor.widgets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NVGImage} handle-lifecycle logic.
 *
 * <p>A {@link TestableNVGImage} subclass overrides the two protected GL hooks
 * ({@code createImageFromHandle} and {@code deleteImage}) so no OpenGL context
 * is required.  The stub assigns incrementing fake handles so tests can verify
 * that the correct handle is tracked and released.
 */
class NVGImageTest {

    // -------------------------------------------------------------------------
    // Test double — stubs out GL calls
    // -------------------------------------------------------------------------

    private static class TestableNVGImage extends NVGImage {

        int nextHandle      = 1;   // fake handle counter
        int deleteCallCount = 0;
        int lastDeletedHandle = -1;

        @Override
        protected int createImageFromHandle(long vg, int glTexId, int texW, int texH) {
            return nextHandle++;
        }

        @Override
        protected void deleteImage(long vg, int handle) {
            deleteCallCount++;
            lastDeletedHandle = handle;
        }
    }

    private TestableNVGImage img;
    private static final long FAKE_VG = 0L; // vg is not used by the stubs

    @BeforeEach
    void setUp() {
        img = new TestableNVGImage();
    }

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    void initialState_notReady() {
        assertFalse(img.isReady());
        assertEquals(-1, img.getNvgImageHandle());
        assertEquals(-1, img.getRegisteredGlTexId());
    }

    // -------------------------------------------------------------------------
    // updateTexture
    // -------------------------------------------------------------------------

    @Test
    void updateTexture_firstCall_registersHandleAndIsReady() {
        img.updateTexture(FAKE_VG, 42, 800, 600);
        assertTrue(img.isReady());
        assertEquals(1, img.getNvgImageHandle());
        assertEquals(42, img.getRegisteredGlTexId());
    }

    @Test
    void updateTexture_sameTexId_doesNotReRegister() {
        img.updateTexture(FAKE_VG, 42, 800, 600);
        img.updateTexture(FAKE_VG, 42, 800, 600); // same — no-op
        assertEquals(1, img.getNvgImageHandle(), "Handle must not change on identical re-call");
        assertEquals(0, img.deleteCallCount, "No delete should occur");
    }

    @Test
    void updateTexture_differentTexId_releasesOldHandleAndRegistersNew() {
        img.updateTexture(FAKE_VG, 42, 800, 600);
        int firstHandle = img.getNvgImageHandle();

        img.updateTexture(FAKE_VG, 99, 800, 600); // different texId
        assertEquals(1, img.deleteCallCount, "Old handle should be released");
        assertEquals(firstHandle, img.lastDeletedHandle);
        assertNotEquals(firstHandle, img.getNvgImageHandle(), "New handle should differ");
        assertEquals(99, img.getRegisteredGlTexId());
    }

    @Test
    void updateTexture_differentSize_releasesOldHandleAndRegistersNew() {
        img.updateTexture(FAKE_VG, 42, 800, 600);
        img.updateTexture(FAKE_VG, 42, 1280, 720); // same texId, new size
        assertEquals(1, img.deleteCallCount);
    }

    // -------------------------------------------------------------------------
    // destroy
    // -------------------------------------------------------------------------

    @Test
    void destroy_afterUpdate_releasesHandleAndBecomesNotReady() {
        img.updateTexture(FAKE_VG, 42, 800, 600);
        img.destroy(FAKE_VG);
        assertFalse(img.isReady());
        assertEquals(-1, img.getNvgImageHandle());
        assertEquals(1, img.deleteCallCount);
    }

    @Test
    void destroy_calledTwice_isIdempotent() {
        img.updateTexture(FAKE_VG, 42, 800, 600);
        img.destroy(FAKE_VG);
        img.destroy(FAKE_VG); // second call — no-op
        assertEquals(1, img.deleteCallCount, "deleteImage must only be called once");
    }

    @Test
    void destroy_withoutPriorUpdate_doesNothing() {
        img.destroy(FAKE_VG);
        assertEquals(0, img.deleteCallCount);
    }

    // -------------------------------------------------------------------------
    // Re-use after destroy
    // -------------------------------------------------------------------------

    @Test
    void updateTexture_afterDestroy_registersNewHandle() {
        img.updateTexture(FAKE_VG, 42, 800, 600);
        img.destroy(FAKE_VG);
        img.updateTexture(FAKE_VG, 42, 800, 600);
        assertTrue(img.isReady());
        assertEquals(2, img.getNvgImageHandle(), "Should have obtained a new handle");
    }
}
