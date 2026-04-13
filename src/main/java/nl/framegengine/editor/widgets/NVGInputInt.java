package nl.framegengine.editor.widgets;

/**
 * Integer input field with drag-scrub support.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * private final NVGInputInt countField = new NVGInputInt(0);
 *
 * // Inside renderFrame():
 * if (countField.render(vg, x, y, w, h)) {
 *     int value = countField.getValue();
 * }
 * }</pre>
 *
 * <p>Task 2.3 — Text Input Widgets.
 */
public class NVGInputInt extends NVGNumericInputBase {

    private int value;

    /** Creates an integer field with an initial value of {@code 0}. */
    public NVGInputInt() {
        this(0);
    }

    /** Creates an integer field with the given initial value. */
    public NVGInputInt(int initialValue) {
        this.value = initialValue;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Renders the field and processes input.
     *
     * @param vg NanoVG context handle
     * @param x  left edge
     * @param y  top edge
     * @param w  width
     * @param h  height (0 = default widget height)
     * @return {@code true} if the value was committed this frame
     */
    public boolean render(long vg, float x, float y, float w, float h) {
        return renderField(vg, x, y, w, h);
    }

    /** Returns the current integer value. */
    public int getValue() {
        return value;
    }

    /** Programmatically sets the value. */
    public void setValue(int value) {
        this.value = value;
        textField.setText(valueToString());
    }

    // -------------------------------------------------------------------------
    // NVGNumericInputBase
    // -------------------------------------------------------------------------

    @Override
    protected String valueToString() {
        return Integer.toString(value);
    }

    @Override
    protected boolean tryParseAndApply(String text) {
        try {
            value = Integer.parseInt(text.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    protected void applyDragStep(float direction) {
        value += (int) Math.signum(direction);
    }
}
