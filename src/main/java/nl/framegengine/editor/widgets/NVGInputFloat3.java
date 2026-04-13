package nl.framegengine.editor.widgets;

/**
 * Vec3 float input — three side-by-side {@link NVGInputFloat} fields for X, Y, Z.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * private final NVGInputFloat3 positionField = new NVGInputFloat3();
 *
 * // Inside renderFrame():
 * if (positionField.render(vg, x, y, w, h)) {
 *     float[] xyz = positionField.getValues();
 * }
 * }</pre>
 *
 * <p>Task 2.3 — Text Input Widgets.
 */
public class NVGInputFloat3 {

    private final NVGInputFloat fieldX;
    private final NVGInputFloat fieldY;
    private final NVGInputFloat fieldZ;

    /** Creates a Vec3 field with all components initialised to {@code 0}. */
    public NVGInputFloat3() {
        this(0f, 0f, 0f);
    }

    /** Creates a Vec3 field with the given initial component values. */
    public NVGInputFloat3(float x, float y, float z) {
        fieldX = new NVGInputFloat(x, 0.1f);
        fieldY = new NVGInputFloat(y, 0.1f);
        fieldZ = new NVGInputFloat(z, 0.1f);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Renders three equal-width float fields side by side.
     *
     * @param vg NanoVG context handle
     * @param x  left edge of the combined widget
     * @param y  top edge
     * @param w  total width (split equally across the three sub-fields)
     * @param h  height (0 = default widget height)
     * @return {@code true} if any component was committed this frame
     */
    public boolean render(long vg, float x, float y, float w, float h) {
        if (h <= 0) h = NVGStyle.getInstance().widgetHeight;
        float gap      = 2f;
        float subW     = (w - gap * 2f) / 3f;

        boolean changed = false;
        changed |= fieldX.render(vg, x,                   y, subW, h);
        changed |= fieldY.render(vg, x + subW + gap,      y, subW, h);
        changed |= fieldZ.render(vg, x + (subW + gap) * 2, y, subW, h);
        return changed;
    }

    /** Returns the current X, Y, Z values as a three-element array. */
    public float[] getValues() {
        return new float[]{ fieldX.getValue(), fieldY.getValue(), fieldZ.getValue() };
    }

    /** Programmatically sets all three component values. */
    public void setValues(float x, float y, float z) {
        fieldX.setValue(x);
        fieldY.setValue(y);
        fieldZ.setValue(z);
    }

    public float getX() { return fieldX.getValue(); }
    public float getY() { return fieldY.getValue(); }
    public float getZ() { return fieldZ.getValue(); }
}
