package nl.framegengine.editor.widgets;

/**
 * Stateful layout cursor that tracks an X indentation level.
 *
 * <p>Panels use this to build indented widget rows without passing indent
 * offsets through every call manually. The typical pattern is:
 *
 * <pre>{@code
 * NVGIndent indent = new NVGIndent();
 *
 * header.render(vg, indent.x(baseX), y, w - indent.depth(), h, "Transform");
 * indent.push();
 *
 * posField.render(vg, indent.x(baseX), y + rowH, w - indent.depth(), h);
 * rotField.render(vg, indent.x(baseX), y + rowH * 2, w - indent.depth(), h);
 *
 * indent.pop();
 * }</pre>
 *
 * <p>Each {@link #push()} adds one {@link NVGStyle#indentWidth} step to the
 * current depth; {@link #pop()} removes one step (clamped to zero).
 * {@link #reset()} returns to zero regardless of nesting depth.
 *
 * <p>Task 2.5 — Collapsing Header, Indent.
 */
public class NVGIndent {

    private int level = 0;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Increases the indent level by one step.
     * The step width is defined by {@link NVGStyle#indentWidth}.
     */
    public void push() {
        level++;
    }

    /**
     * Decreases the indent level by one step, clamped to zero.
     */
    public void pop() {
        if (level > 0) level--;
    }

    /**
     * Resets the indent level to zero regardless of current nesting.
     */
    public void reset() {
        level = 0;
    }

    /**
     * Returns the current indent level (number of {@link #push()} calls
     * not yet matched by {@link #pop()} calls).
     */
    public int getLevel() {
        return level;
    }

    /**
     * Returns the total pixel depth of the current indent
     * ({@code level × NVGStyle.indentWidth}).
     */
    public float depth() {
        return level * NVGStyle.getInstance().indentWidth;
    }

    /**
     * Computes the indented X position given a base X coordinate.
     *
     * <p>Equivalent to {@code baseX + depth()}.
     *
     * @param baseX the un-indented left edge of the panel content area
     * @return the X position at which indented widgets should be drawn
     */
    public float x(float baseX) {
        return baseX + depth();
    }
}
