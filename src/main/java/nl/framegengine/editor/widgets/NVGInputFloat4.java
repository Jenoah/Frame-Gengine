package nl.framegengine.editor.widgets;

/**
 * Vec4 / Quaternion float input — four side-by-side {@link NVGInputFloat} fields
 * for X, Y, Z, W.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * private final NVGInputFloat4 rotationField = new NVGInputFloat4();
 *
 * // Inside renderFrame():
 * if (rotationField.render(vg, x, y, w, h)) {
 *     float[] xyzw = rotationField.getValues();
 * }
 * }</pre>
 *
 * <p>Task 2.3 — Text Input Widgets.
 */
public class NVGInputFloat4 {

    private final NVGInputFloat fieldX;
    private final NVGInputFloat fieldY;
    private final NVGInputFloat fieldZ;
    private final NVGInputFloat fieldW;

    /** Creates a Vec4 field with all components initialised to {@code 0}. */
    public NVGInputFloat4() {
        this(0f, 0f, 0f, 0f);
    }

    /** Creates a Vec4 field with the given initial component values. */
    public NVGInputFloat4(float x, float y, float z, float w) {
        fieldX = new NVGInputFloat(x, 0.1f);
        fieldY = new NVGInputFloat(y, 0.1f);
        fieldZ = new NVGInputFloat(z, 0.1f);
        fieldW = new NVGInputFloat(w, 0.1f);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Renders four equal-width float fields side by side.
     *
     * @param vg NanoVG context handle
     * @param x  left edge of the combined widget
     * @param y  top edge
     * @param w  total width (split equally across the four sub-fields)
     * @param h  height (0 = default widget height)
     * @return {@code true} if any component was committed this frame
     */
    public boolean render(long vg, float x, float y, float w, float h) {
        if (h <= 0) h = NVGStyle.getInstance().widgetHeight;
        float gap  = 2f;
        float subW = (w - gap * 3f) / 4f;

        boolean changed = false;
        changed |= fieldX.render(vg, x,                    y, subW, h);
        changed |= fieldY.render(vg, x + (subW + gap),     y, subW, h);
        changed |= fieldZ.render(vg, x + (subW + gap) * 2, y, subW, h);
        changed |= fieldW.render(vg, x + (subW + gap) * 3, y, subW, h);
        return changed;
    }

    /** Returns the current X, Y, Z, W values as a four-element array. */
    public float[] getValues() {
        return new float[]{ fieldX.getValue(), fieldY.getValue(),
                            fieldZ.getValue(), fieldW.getValue() };
    }

    /** Programmatically sets all four component values. */
    public void setValues(float x, float y, float z, float w) {
        fieldX.setValue(x);
        fieldY.setValue(y);
        fieldZ.setValue(z);
        fieldW.setValue(w);
    }

    public float getX() { return fieldX.getValue(); }
    public float getY() { return fieldY.getValue(); }
    public float getZ() { return fieldZ.getValue(); }
    public float getW() { return fieldW.getValue(); }
}
