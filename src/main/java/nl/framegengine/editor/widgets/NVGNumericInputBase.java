package nl.framegengine.editor.widgets;

/**
 * Base class for numeric input widgets ({@link NVGInputInt}, {@link NVGInputFloat},
 * {@link NVGInputFloat3}, {@link NVGInputFloat4}).
 *
 * <p>Wraps an {@link NVGInputText} and adds:
 * <ul>
 *   <li><b>Drag-scrub</b>: holding the left button and dragging horizontally
 *       while hovering the field increments/decrements the value.</li>
 *   <li><b>Parse-on-commit</b>: the text is parsed when Enter is pressed;
 *       invalid input reverts to the previous valid value.</li>
 * </ul>
 *
 * <p>Subclasses implement {@link #valueToString()} and
 * {@link #tryParseAndApply(String)} to define the value type and formatting.
 *
 * <p>Task 2.3 — Text Input Widgets.
 */
abstract class NVGNumericInputBase {

    protected final NVGInputText textField = new NVGInputText();

    // Drag-scrub state
    private boolean  dragging       = false;
    private float    dragStartX     = 0f;
    private float    dragAccum      = 0f;

    /** Pixels of horizontal drag required to change the value by one step. */
    protected float dragPixelsPerStep = 5f;

    private final float[] mousePos = new float[2];

    // -------------------------------------------------------------------------
    // Template method — subclasses implement these
    // -------------------------------------------------------------------------

    /** Formats the current value as a string for display/editing. */
    protected abstract String valueToString();

    /**
     * Attempts to parse {@code text} and apply it as the new value.
     *
     * @param text the committed text from the text field
     * @return {@code true} if parsing succeeded and the value was updated
     */
    protected abstract boolean tryParseAndApply(String text);

    /**
     * Applies one drag step in the given direction.
     *
     * @param direction positive = increment, negative = decrement
     */
    protected abstract void applyDragStep(float direction);

    // -------------------------------------------------------------------------
    // Shared render logic
    // -------------------------------------------------------------------------

    /**
     * Renders the numeric field. Called by each concrete subclass's own
     * {@code render} method after it has set up display text.
     *
     * @param vg NanoVG context handle
     * @param x  left edge
     * @param y  top edge
     * @param w  width
     * @param h  height (0 = default widget height)
     * @return {@code true} if the value was committed this frame
     */
    protected boolean renderField(long vg, float x, float y, float w, float h) {
        if (h <= 0) h = NVGStyle.getInstance().widgetHeight;

        EditorInput.getMousePos(mousePos);
        float mx = mousePos[0];
        boolean leftDown = EditorInput.isLeftDown();

        boolean hovered = NVGDrawHelper.isPointInRect(mx, mousePos[1], x, y, w, h);

        // --- Drag-scrub ---
        if (hovered && leftDown && !textField.isFocused()) {
            if (!dragging) {
                dragging    = true;
                dragStartX  = mx;
                dragAccum   = 0f;
            }
        }
        if (dragging) {
            if (leftDown) {
                dragAccum += mx - dragStartX;
                dragStartX = mx;
                while (dragAccum >= dragPixelsPerStep) {
                    applyDragStep(+1f);
                    dragAccum -= dragPixelsPerStep;
                    textField.setText(valueToString());
                }
                while (dragAccum <= -dragPixelsPerStep) {
                    applyDragStep(-1f);
                    dragAccum += dragPixelsPerStep;
                    textField.setText(valueToString());
                }
            } else {
                dragging  = false;
                dragAccum = 0f;
            }
        }

        // --- Text field ---
        boolean committed = textField.render(vg, x, y, w, h, valueToString());
        if (committed) {
            if (!tryParseAndApply(textField.getText())) {
                // Revert display to last valid value
                textField.setText(valueToString());
            }
        }
        return committed;
    }
}
