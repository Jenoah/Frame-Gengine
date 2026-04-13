package nl.framegengine.editor.widgets;

import nl.framegengine.editor.EditorWindow;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.nanovg.NVGColor;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nanovg.NanoVG.*;

/**
 * Stateful single-line text-input widget backed by NanoVG.
 *
 * <h3>Features</h3>
 * <ul>
 *   <li>Cursor positioning (left/right arrow, Home, End)</li>
 *   <li>Selection highlight (Shift + arrow / Home / End)</li>
 *   <li>Clipboard: Ctrl+C copy, Ctrl+X cut, Ctrl+V paste</li>
 *   <li>Undo: Ctrl+Z (single level)</li>
 *   <li>Backspace (delete left), Delete (delete right)</li>
 *   <li>Commit on Enter; cancel / blur on Escape</li>
 *   <li>Click-to-place-cursor</li>
 * </ul>
 *
 * <h3>Usage pattern</h3>
 * <pre>{@code
 * private final NVGInputText nameField = new NVGInputText();
 *
 * // Inside renderFrame():
 * if (nameField.render(vg, x, y, w, h, currentName)) {
 *     currentName = nameField.getText();  // value committed (Enter pressed)
 * }
 * }</pre>
 *
 * <p>Task 2.3 — Text Input Widgets.
 */
public class NVGInputText {

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private StringBuilder buffer   = new StringBuilder();
    private String         undoSnapshot = "";

    private int  cursorPos   = 0;   // insertion point (0 = before first char)
    private int  selStart    = -1;  // -1 = no selection
    private int  selEnd      = -1;

    private boolean focused  = false;
    private boolean hovered  = false;

    // GLFW callbacks installed while focused
    private GLFWCharCallback charCallback;
    private GLFWKeyCallback  keyCallback;
    // Previous callbacks to chain/restore
    private GLFWCharCallback prevCharCallback;
    private GLFWKeyCallback  prevKeyCallback;

    // Reusable mouse-position array
    private final float[] mousePos = new float[2];

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Renders the text field and processes input.
     *
     * @param vg          NanoVG context handle
     * @param x           left edge in pixels
     * @param y           top edge in pixels
     * @param w           width in pixels
     * @param h           height in pixels (0 = {@link NVGStyle#widgetHeight})
     * @param initialText value to display when this widget first receives focus;
     *                    ignored once focused
     * @return {@code true} on the frame the user commits the value (Enter key)
     */
    public boolean render(long vg, float x, float y, float w, float h, String initialText) {
        if (h <= 0) h = NVGStyle.getInstance().widgetHeight;

        EditorInput.getMousePos(mousePos);
        boolean leftClicked = EditorInput.isLeftDown();

        boolean wasHovered = hovered;
        hovered = NVGDrawHelper.isPointInRect(mousePos[0], mousePos[1], x, y, w, h);

        // Focus management
        if (hovered && leftClicked) {
            if (!focused) {
                gainFocus(initialText);
                placeCursorAtClick(vg, x, w, mousePos[0]);
            } else {
                placeCursorAtClick(vg, x, w, mousePos[0]);
            }
        } else if (!hovered && leftClicked && focused) {
            loseFocus();
        }

        boolean committed = consumeCommit();
        draw(vg, x, y, w, h);
        return committed;
    }

    /** Returns the current text in the buffer. */
    public String getText() {
        return buffer.toString();
    }

    /**
     * Programmatically sets the buffer content and resets cursor/selection.
     * Does not affect focus state.
     */
    public void setText(String text) {
        buffer = new StringBuilder(text == null ? "" : text);
        cursorPos = buffer.length();
        clearSelection();
    }

    /** Returns {@code true} if this field currently has keyboard focus. */
    public boolean isFocused() {
        return focused;
    }

    /**
     * Programmatically removes keyboard focus from this field,
     * unregistering keyboard callbacks.
     */
    public void blur() {
        if (focused) loseFocus();
    }

    /** Returns the current cursor position (0 = before first character). */
    public int getCursorPos() { return cursorPos; }

    /** Returns the selection start index, or {@code -1} if no selection. */
    public int getSelStart() { return selStart; }

    /** Returns the selection end index, or {@code -1} if no selection. */
    public int getSelEnd() { return selEnd; }

    // -------------------------------------------------------------------------
    // Package-private helpers for unit testing — drive buffer without GLFW
    // -------------------------------------------------------------------------

    /** Simulates typing a character without a GL context. */
    void injectChar(char c) { insertCharAtCursor(c); }

    /** Simulates a key press without a GL context. Uses mods = 0. */
    void injectKey(int key) { handleKeyPress(key, 0, 0L); }

    /** Simulates a key press with modifier flags without a GL context. */
    void injectKey(int key, int mods) { handleKeyPress(key, mods, 0L); }

    // -------------------------------------------------------------------------
    // Focus / callback management
    // -------------------------------------------------------------------------

    private void gainFocus(String initialText) {
        if (focused) return;
        focused = true;
        undoSnapshot = buffer.toString();

        // If buffer is empty, seed it from initialText
        if (buffer.length() == 0 && initialText != null && !initialText.isEmpty()) {
            buffer = new StringBuilder(initialText);
            cursorPos = buffer.length();
        }

        long win = windowPtr();

        charCallback = GLFWCharCallback.create((window, codepoint) -> {
            insertCharAtCursor((char) codepoint);
        });

        keyCallback = GLFWKeyCallback.create((window, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                handleKeyPress(key, mods, window);
            }
            // Chain to previous (ImGui needs key events too)
            if (prevKeyCallback != null) prevKeyCallback.invoke(window, key, scancode, action, mods);
        });

        prevCharCallback = glfwSetCharCallback(win, charCallback);
        prevKeyCallback  = glfwSetKeyCallback(win, keyCallback);
    }

    private void loseFocus() {
        if (!focused) return;
        focused = false;
        long win = windowPtr();
        glfwSetCharCallback(win, prevCharCallback);
        glfwSetKeyCallback(win, prevKeyCallback);
        charCallback.free();
        keyCallback.free();
        charCallback = null;
        keyCallback  = null;
        prevCharCallback = null;
        prevKeyCallback  = null;
        clearSelection();
    }

    // -------------------------------------------------------------------------
    // Key handling
    // -------------------------------------------------------------------------

    private boolean pendingCommit = false;

    private void handleKeyPress(int key, int mods, long window) {
        boolean ctrl  = (mods & GLFW_MOD_CONTROL) != 0;
        boolean shift = (mods & GLFW_MOD_SHIFT)   != 0;

        switch (key) {
            case GLFW_KEY_BACKSPACE -> deleteBack();
            case GLFW_KEY_DELETE    -> deleteForward();
            case GLFW_KEY_LEFT      -> moveCursor(-1, shift);
            case GLFW_KEY_RIGHT     -> moveCursor(+1, shift);
            case GLFW_KEY_HOME      -> moveCursorTo(0, shift);
            case GLFW_KEY_END       -> moveCursorTo(buffer.length(), shift);
            case GLFW_KEY_ENTER     -> { pendingCommit = true; loseFocus(); }
            case GLFW_KEY_ESCAPE    -> { buffer = new StringBuilder(undoSnapshot);
                                         cursorPos = buffer.length(); loseFocus(); }
            case GLFW_KEY_A         -> { if (ctrl) selectAll(); }
            case GLFW_KEY_C         -> { if (ctrl) copyToClipboard(window); }
            case GLFW_KEY_X         -> { if (ctrl) cutToClipboard(window); }
            case GLFW_KEY_V         -> { if (ctrl) pasteFromClipboard(window); }
            case GLFW_KEY_Z         -> { if (ctrl) undo(); }
        }
    }

    private boolean consumeCommit() {
        if (pendingCommit) { pendingCommit = false; return true; }
        return false;
    }

    // -------------------------------------------------------------------------
    // Buffer operations
    // -------------------------------------------------------------------------

    private void insertCharAtCursor(char c) {
        deleteSelection();
        buffer.insert(cursorPos, c);
        cursorPos++;
        clearSelection();
    }

    private void deleteBack() {
        if (hasSelection()) { deleteSelection(); return; }
        if (cursorPos > 0) {
            buffer.deleteCharAt(cursorPos - 1);
            cursorPos--;
        }
    }

    private void deleteForward() {
        if (hasSelection()) { deleteSelection(); return; }
        if (cursorPos < buffer.length()) {
            buffer.deleteCharAt(cursorPos);
        }
    }

    private void deleteSelection() {
        if (!hasSelection()) return;
        int lo = Math.min(selStart, selEnd);
        int hi = Math.max(selStart, selEnd);
        buffer.delete(lo, hi);
        cursorPos = lo;
        clearSelection();
    }

    // -------------------------------------------------------------------------
    // Cursor & selection
    // -------------------------------------------------------------------------

    private void moveCursor(int delta, boolean shift) {
        int next = Math.max(0, Math.min(buffer.length(), cursorPos + delta));
        if (shift) {
            if (!hasSelection()) { selStart = cursorPos; }
            selEnd = next;
        } else {
            if (hasSelection()) { cursorPos = (delta < 0) ? Math.min(selStart, selEnd)
                                                           : Math.max(selStart, selEnd);
                clearSelection(); return; }
            clearSelection();
        }
        cursorPos = next;
    }

    private void moveCursorTo(int pos, boolean shift) {
        if (shift) {
            if (!hasSelection()) selStart = cursorPos;
            selEnd = pos;
        } else {
            clearSelection();
        }
        cursorPos = pos;
    }

    private void selectAll() {
        selStart  = 0;
        selEnd    = buffer.length();
        cursorPos = buffer.length();
    }

    private boolean hasSelection() {
        return selStart != -1 && selEnd != -1 && selStart != selEnd;
    }

    private void clearSelection() {
        selStart = -1;
        selEnd   = -1;
    }

    // -------------------------------------------------------------------------
    // Clipboard
    // -------------------------------------------------------------------------

    private void copyToClipboard(long window) {
        if (!hasSelection()) return;
        int lo = Math.min(selStart, selEnd);
        int hi = Math.max(selStart, selEnd);
        glfwSetClipboardString(window, buffer.substring(lo, hi));
    }

    private void cutToClipboard(long window) {
        copyToClipboard(window);
        deleteSelection();
    }

    private void pasteFromClipboard(long window) {
        String clip = glfwGetClipboardString(window);
        if (clip == null || clip.isEmpty()) return;
        deleteSelection();
        buffer.insert(cursorPos, clip);
        cursorPos += clip.length();
    }

    private void undo() {
        buffer    = new StringBuilder(undoSnapshot);
        cursorPos = buffer.length();
        clearSelection();
    }

    // -------------------------------------------------------------------------
    // Click-to-place cursor (approximate character hit-test)
    // -------------------------------------------------------------------------

    private void placeCursorAtClick(long vg, float fieldX, float fieldW, float clickX) {
        NVGStyle style = NVGStyle.getInstance();
        float textStartX = fieldX + style.paddingX;
        float relX = clickX - textStartX;

        nvgFontSize(vg, style.fontSizeDefault);
        nvgFontFace(vg, nl.framegengine.editor.ui.NanoVGContext.DEFAULT_FONT);

        String text = buffer.toString();
        int best = text.length();
        float bestDist = Float.MAX_VALUE;

        for (int i = 0; i <= text.length(); i++) {
            float advance = nvgTextBounds(vg, 0, 0, text.substring(0, i), (java.nio.FloatBuffer) null);
            float dist = Math.abs(advance - relX);
            if (dist < bestDist) { bestDist = dist; best = i; }
        }
        cursorPos = best;
        clearSelection();
    }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    private void draw(long vg, float x, float y, float w, float h) {
        NVGStyle style = NVGStyle.getInstance();

        // Background + border
        NVGColor bg     = focused ? style.colorWidgetBgActive : style.colorWidgetBg;
        NVGColor border = focused ? style.colorBorderActive
                        : hovered ? style.colorBorderHovered
                                  : style.colorBorder;
        NVGDrawHelper.drawRoundedRectWithBorder(vg, x, y, w, h,
                style.cornerRadius, bg, style.borderWidth, border);

        // Clip text area
        float textX = x + style.paddingX;
        float textY = y + h / 2f;
        float clipW = w - style.paddingX * 2f;

        nvgSave(vg);
        nvgIntersectScissor(vg, x + style.borderWidth, y + style.borderWidth,
                w - style.borderWidth * 2f, h - style.borderWidth * 2f);

        nvgFontSize(vg, style.fontSizeDefault);
        nvgFontFace(vg, nl.framegengine.editor.ui.NanoVGContext.DEFAULT_FONT);
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);

        String text = buffer.toString();

        // Selection highlight
        if (focused && hasSelection()) {
            int lo = Math.min(selStart, selEnd);
            int hi = Math.max(selStart, selEnd);
            float selX0 = textX + nvgTextBounds(vg, 0, 0, text.substring(0, lo), (java.nio.FloatBuffer) null);
            float selX1 = textX + nvgTextBounds(vg, 0, 0, text.substring(0, hi), (java.nio.FloatBuffer) null);
            NVGDrawHelper.drawRoundedRect(vg, selX0, y + 2, selX1 - selX0, h - 4, 0f,
                    style.colorSelectionBg);
        }

        // Text
        nvgFillColor(vg, style.colorText);
        nvgText(vg, textX, textY, text);

        // Cursor
        if (focused) {
            float cursorX = textX + nvgTextBounds(vg, 0, 0, text.substring(0, cursorPos),
                    (java.nio.FloatBuffer) null);
            nvgBeginPath(vg);
            nvgMoveTo(vg, cursorX, y + 3);
            nvgLineTo(vg, cursorX, y + h - 3);
            nvgStrokeColor(vg, style.colorText);
            nvgStrokeWidth(vg, 1.5f);
            nvgStroke(vg);
        }

        nvgRestore(vg);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static long windowPtr() {
        return EditorWindow.getInstance().getWindowPtr();
    }
}
