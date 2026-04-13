package nl.framegengine.editor.widgets;

/**
 * Scalar float input field with drag-scrub support.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * private final NVGInputFloat speedField = new NVGInputFloat(1.0f);
 *
 * // Inside renderFrame():
 * if (speedField.render(vg, x, y, w, h)) {
 *     float value = speedField.getValue();
 * }
 * }</pre>
 *
 * <p>Task 2.3 — Text Input Widgets.
 */
public class NVGInputFloat extends NVGNumericInputBase {

    private float value;
    private float step;

    /** Creates a float field with an initial value of {@code 0.0} and step of {@code 0.1}. */
    public NVGInputFloat() {
        this(0f, 0.1f);
    }

    /**
     * Creates a float field with the given initial value and drag step.
     *
     * @param initialValue starting value
     * @param step         amount added/subtracted per drag pixel-group
     */
    public NVGInputFloat(float initialValue, float step) {
        this.value = initialValue;
        this.step  = step;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Renders the field and processes input.
     *
     * @return {@code true} if the value was committed this frame
     */
    public boolean render(long vg, float x, float y, float w, float h) {
        return renderField(vg, x, y, w, h);
    }

    /** Returns the current float value. */
    public float getValue() {
        return value;
    }

    /** Programmatically sets the value. */
    public void setValue(float value) {
        this.value = value;
        textField.setText(valueToString());
    }

    // -------------------------------------------------------------------------
    // NVGNumericInputBase
    // -------------------------------------------------------------------------

    @Override
    protected String valueToString() {
        // Trim unnecessary trailing zeros while keeping at least one decimal place
        String s = String.format("%.4f", value);
        s = s.replaceAll("0+$", "");
        if (s.endsWith(".")) s += "0";
        return s;
    }

    @Override
    protected boolean tryParseAndApply(String text) {
        try {
            value = Float.parseFloat(text.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    protected void applyDragStep(float direction) {
        value += Math.signum(direction) * step;
    }
}
