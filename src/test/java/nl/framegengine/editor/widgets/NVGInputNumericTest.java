package nl.framegengine.editor.widgets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for numeric input widget parse, revert, and drag-step logic.
 *
 * <p>All tests call template methods directly ({@code tryParseAndApply},
 * {@code applyDragStep}, {@code valueToString}) — no GLFW window or NanoVG
 * context is required.
 */
class NVGInputNumericTest {

    // =========================================================================
    // NVGInputInt
    // =========================================================================

    @Nested
    class InputIntTests {

        private NVGInputInt field;

        @BeforeEach
        void setUp() {
            field = new NVGInputInt(10);
        }

        // --- valueToString ---

        @Test
        void valueToString_returnsDecimalString() {
            assertEquals("10", field.valueToString());
        }

        @Test
        void valueToString_negativeValue_includesSign() {
            field.setValue(-5);
            assertEquals("-5", field.valueToString());
        }

        // --- tryParseAndApply ---

        @Test
        void parse_validInteger_updatesValueAndReturnsTrue() {
            boolean ok = field.tryParseAndApply("42");
            assertTrue(ok);
            assertEquals(42, field.getValue());
        }

        @Test
        void parse_validIntegerWithWhitespace_stripsAndParses() {
            boolean ok = field.tryParseAndApply("  7  ");
            assertTrue(ok);
            assertEquals(7, field.getValue());
        }

        @Test
        void parse_emptyString_returnsFalseAndDoesNotChangeValue() {
            boolean ok = field.tryParseAndApply("");
            assertFalse(ok);
            assertEquals(10, field.getValue());
        }

        @Test
        void parse_floatString_returnsFalseAndDoesNotChangeValue() {
            boolean ok = field.tryParseAndApply("3.14");
            assertFalse(ok);
            assertEquals(10, field.getValue());
        }

        @Test
        void parse_alphabeticString_returnsFalseAndDoesNotChangeValue() {
            boolean ok = field.tryParseAndApply("abc");
            assertFalse(ok);
            assertEquals(10, field.getValue());
        }

        // --- applyDragStep ---

        @Test
        void dragStep_positive_incrementsByOne() {
            field.applyDragStep(+1f);
            assertEquals(11, field.getValue());
        }

        @Test
        void dragStep_negative_decrementsByOne() {
            field.applyDragStep(-1f);
            assertEquals(9, field.getValue());
        }

        @Test
        void dragStep_multipleSteps_accumulatesCorrectly() {
            for (int i = 0; i < 5; i++) field.applyDragStep(+1f);
            assertEquals(15, field.getValue());
        }

        // --- revert pattern (parse failure leaves value unchanged) ---

        @Test
        void revertOnInvalidInput_valueRemainsAtLastValid() {
            field.tryParseAndApply("99");
            assertEquals(99, field.getValue());
            field.tryParseAndApply("notanumber");
            assertEquals(99, field.getValue());  // reverted
        }
    }

    // =========================================================================
    // NVGInputFloat
    // =========================================================================

    @Nested
    class InputFloatTests {

        private NVGInputFloat field;

        @BeforeEach
        void setUp() {
            field = new NVGInputFloat(1.0f, 0.1f);
        }

        // --- valueToString ---

        @Test
        void valueToString_hasAtLeastOneDecimalPlace() {
            field.setValue(2.0f);
            String s = field.valueToString();
            assertTrue(s.contains("."), "Must contain a decimal point");
            assertFalse(s.endsWith("."), "Must not end with bare decimal");
        }

        @Test
        void valueToString_trailingZerosTrimmed() {
            field.setValue(1.5f);
            String s = field.valueToString();
            // "1.5000" -> "1.5" or "1.50" -- must not end with unnecessary zeros
            assertFalse(s.matches(".*\\.\\d*0$") && !s.endsWith(".0"),
                    "Trailing zeros beyond first decimal should be trimmed, got: " + s);
        }

        // --- tryParseAndApply ---

        @Test
        void parse_validFloat_updatesValueAndReturnsTrue() {
            boolean ok = field.tryParseAndApply("3.14");
            assertTrue(ok);
            assertEquals(3.14f, field.getValue(), 0.0001f);
        }

        @Test
        void parse_integerString_parsedAsFloat() {
            boolean ok = field.tryParseAndApply("5");
            assertTrue(ok);
            assertEquals(5.0f, field.getValue(), 0.0001f);
        }

        @Test
        void parse_floatWithWhitespace_stripsAndParses() {
            boolean ok = field.tryParseAndApply("  2.5  ");
            assertTrue(ok);
            assertEquals(2.5f, field.getValue(), 0.0001f);
        }

        @Test
        void parse_invalidString_returnsFalseAndDoesNotChangeValue() {
            boolean ok = field.tryParseAndApply("bad");
            assertFalse(ok);
            assertEquals(1.0f, field.getValue(), 0.0001f);
        }

        @Test
        void parse_emptyString_returnsFalseAndDoesNotChangeValue() {
            boolean ok = field.tryParseAndApply("");
            assertFalse(ok);
            assertEquals(1.0f, field.getValue(), 0.0001f);
        }

        // --- applyDragStep ---

        @Test
        void dragStep_positive_addsSingleStep() {
            field.applyDragStep(+1f);
            assertEquals(1.1f, field.getValue(), 0.0001f);
        }

        @Test
        void dragStep_negative_subtractsSingleStep() {
            field.applyDragStep(-1f);
            assertEquals(0.9f, field.getValue(), 0.0001f);
        }

        @Test
        void dragStep_multiplePositiveSteps_accumulatesCorrectly() {
            for (int i = 0; i < 3; i++) field.applyDragStep(+1f);
            assertEquals(1.3f, field.getValue(), 0.001f);
        }

        // --- revert pattern ---

        @Test
        void revertOnInvalidInput_valueRemainsAtLastValid() {
            field.tryParseAndApply("9.9");
            assertEquals(9.9f, field.getValue(), 0.0001f);
            field.tryParseAndApply("!!!");
            assertEquals(9.9f, field.getValue(), 0.0001f);
        }

        // --- setValue ---

        @Test
        void setValue_updatesTextField() {
            field.setValue(7.77f);
            assertEquals(7.77f, field.getValue(), 0.0001f);
            // textField should reflect the formatted value
            assertFalse(field.textField.getText().isEmpty());
        }
    }
}
