package nl.framegengine.editor.widgets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.lwjgl.glfw.GLFW.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NVGInputText} buffer and cursor logic.
 *
 * <p>All tests drive the widget through its package-private
 * {@code injectChar} / {@code injectKey} helpers so no GLFW window or
 * NanoVG context is required.
 */
class NVGInputTextTest {

    private NVGInputText field;

    @BeforeEach
    void setUp() {
        field = new NVGInputText();
    }

    // -------------------------------------------------------------------------
    // Basic typing
    // -------------------------------------------------------------------------

    @Test
    void type_singleCharacter_appearsInBuffer() {
        field.injectChar('A');
        assertEquals("A", field.getText());
        assertEquals(1, field.getCursorPos());
    }

    @Test
    void type_multipleCharacters_allAppearInOrder() {
        "Hello".chars().forEach(c -> field.injectChar((char) c));
        assertEquals("Hello", field.getText());
        assertEquals(5, field.getCursorPos());
    }

    // -------------------------------------------------------------------------
    // Backspace / Delete
    // -------------------------------------------------------------------------

    @Test
    void backspace_atEnd_removesLastChar() {
        field.setText("abc");
        field.injectKey(GLFW_KEY_BACKSPACE);
        assertEquals("ab", field.getText());
        assertEquals(2, field.getCursorPos());
    }

    @Test
    void backspace_atStart_doesNothing() {
        field.setText("abc");
        field.injectKey(GLFW_KEY_HOME);     // cursor to 0
        field.injectKey(GLFW_KEY_BACKSPACE);
        assertEquals("abc", field.getText());
        assertEquals(0, field.getCursorPos());
    }

    @Test
    void delete_atStart_removesFirstChar() {
        field.setText("abc");
        field.injectKey(GLFW_KEY_HOME);
        field.injectKey(GLFW_KEY_DELETE);
        assertEquals("bc", field.getText());
        assertEquals(0, field.getCursorPos());
    }

    @Test
    void delete_atEnd_doesNothing() {
        field.setText("abc");
        field.injectKey(GLFW_KEY_DELETE);
        assertEquals("abc", field.getText());
    }

    // -------------------------------------------------------------------------
    // Cursor navigation
    // -------------------------------------------------------------------------

    @Test
    void arrowLeft_movesOnePosBack() {
        field.setText("abc");
        field.injectKey(GLFW_KEY_LEFT);
        assertEquals(2, field.getCursorPos());
    }

    @Test
    void arrowRight_atEnd_doesNotExceedLength() {
        field.setText("abc");
        field.injectKey(GLFW_KEY_RIGHT);
        assertEquals(3, field.getCursorPos());
    }

    @Test
    void home_movesCursorToZero() {
        field.setText("abc");
        field.injectKey(GLFW_KEY_HOME);
        assertEquals(0, field.getCursorPos());
    }

    @Test
    void end_movesCursorToEnd() {
        field.setText("abc");
        field.injectKey(GLFW_KEY_HOME);
        field.injectKey(GLFW_KEY_END);
        assertEquals(3, field.getCursorPos());
    }

    // -------------------------------------------------------------------------
    // Selection with Shift
    // -------------------------------------------------------------------------

    @Test
    void shiftLeft_createsSelectionToLeft() {
        field.setText("abc");
        // cursor at 3, shift+left selects char 'c'
        field.injectKey(GLFW_KEY_LEFT, GLFW_MOD_SHIFT);
        assertEquals(3, field.getSelStart());
        assertEquals(2, field.getSelEnd());
        assertEquals(2, field.getCursorPos());
    }

    @Test
    void shiftHome_selectsFromEndToStart() {
        field.setText("abc");
        field.injectKey(GLFW_KEY_HOME, GLFW_MOD_SHIFT);
        assertEquals(3, field.getSelStart());
        assertEquals(0, field.getSelEnd());
        assertEquals(0, field.getCursorPos());
    }

    @Test
    void selectAll_ctrlA_selectsEntireBuffer() {
        field.setText("hello");
        field.injectKey(GLFW_KEY_A, GLFW_MOD_CONTROL);
        assertEquals(0, field.getSelStart());
        assertEquals(5, field.getSelEnd());
    }

    // -------------------------------------------------------------------------
    // Backspace / Delete with active selection
    // -------------------------------------------------------------------------

    @Test
    void backspace_withSelection_deletesSelection() {
        field.setText("hello");
        field.injectKey(GLFW_KEY_A, GLFW_MOD_CONTROL);   // select all
        field.injectKey(GLFW_KEY_BACKSPACE);
        assertEquals("", field.getText());
        assertEquals(0, field.getCursorPos());
    }

    @Test
    void delete_withSelection_deletesSelection() {
        field.setText("hello");
        field.injectKey(GLFW_KEY_HOME);
        // shift+end selects all
        field.injectKey(GLFW_KEY_END, GLFW_MOD_SHIFT);
        field.injectKey(GLFW_KEY_DELETE);
        assertEquals("", field.getText());
    }

    // -------------------------------------------------------------------------
    // Undo (Ctrl+Z)
    // -------------------------------------------------------------------------

    @Test
    void undo_revertsToBufferAtFocusGain() {
        // setText acts as the "pre-focus" state for undo; we then type, then undo
        field.setText("original");
        // snapshot is set on focus-gain which we can't trigger without GLFW;
        // but we can snapshot via setText then mutate and ctrl+z
        // The undo snapshot is set in gainFocus, so we simulate it via setText
        // which sets undoSnapshot only if we peek at the private field.
        // Instead, test the simpler flow: type chars, then ctrl+z reverts to
        // the state the field had when it was constructed (empty).
        NVGInputText fresh = new NVGInputText();
        fresh.injectChar('a');
        fresh.injectChar('b');
        fresh.injectChar('c');
        // undoSnapshot for a never-focused field is ""; ctrl+z reverts to ""
        fresh.injectKey(GLFW_KEY_Z, GLFW_MOD_CONTROL);
        assertEquals("", fresh.getText());
        assertEquals(0, fresh.getCursorPos());
    }

    // -------------------------------------------------------------------------
    // Type over selection
    // -------------------------------------------------------------------------

    @Test
    void typeOverSelection_replacesSelectedText() {
        field.setText("world");
        field.injectKey(GLFW_KEY_A, GLFW_MOD_CONTROL);  // select all
        "new".chars().forEach(c -> field.injectChar((char) c));
        assertEquals("new", field.getText());
        assertEquals(3, field.getCursorPos());
    }

    // -------------------------------------------------------------------------
    // setText
    // -------------------------------------------------------------------------

    @Test
    void setText_updatesCursorToEnd() {
        field.setText("hello");
        assertEquals(5, field.getCursorPos());
        assertEquals(-1, field.getSelStart());
        assertEquals(-1, field.getSelEnd());
    }

    @Test
    void setText_null_treatsAsEmpty() {
        field.setText(null);
        assertEquals("", field.getText());
        assertEquals(0, field.getCursorPos());
    }

    // -------------------------------------------------------------------------
    // Insert in middle
    // -------------------------------------------------------------------------

    @Test
    void insertInMiddle_insertedAtCorrectPosition() {
        field.setText("ac");
        field.injectKey(GLFW_KEY_HOME);   // cursor at 0
        field.injectKey(GLFW_KEY_RIGHT);  // cursor at 1
        field.injectChar('b');
        assertEquals("abc", field.getText());
        assertEquals(2, field.getCursorPos());
    }

    // -------------------------------------------------------------------------
    // Arrow key clears selection
    // -------------------------------------------------------------------------

    @Test
    void arrowLeft_withSelection_collapsesCursorToLowerBound() {
        field.setText("abc");
        field.injectKey(GLFW_KEY_A, GLFW_MOD_CONTROL);  // select all (cursor=3, sel=0..3)
        field.injectKey(GLFW_KEY_LEFT);                  // collapse to left
        assertEquals(-1, field.getSelStart());
        assertEquals(-1, field.getSelEnd());
        assertEquals(0, field.getCursorPos());
    }

    @Test
    void arrowRight_withSelection_collapsesCursorToUpperBound() {
        field.setText("abc");
        field.injectKey(GLFW_KEY_A, GLFW_MOD_CONTROL);  // select all (cursor=3, sel=0..3)
        field.injectKey(GLFW_KEY_RIGHT);                 // collapse to right
        assertEquals(-1, field.getSelStart());
        assertEquals(-1, field.getSelEnd());
        assertEquals(3, field.getCursorPos());
    }
}
