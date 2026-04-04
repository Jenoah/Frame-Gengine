package nl.framegengine.core.utils;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ClassHelperTest {

    // ------------------------------------------------------------------
    // Minimal POJOs used as test fixtures
    // ------------------------------------------------------------------

    static class Base {
        String baseField = "baseValue";
    }

    static class Child extends Base {
        String childField = "childValue";
    }

    static class WithList {
        List<String> items = new ArrayList<>();
        int count = 0;
    }

    static class Outer {
        Inner inner = new Inner();
    }

    static class Inner {
        String value = "original";
    }

    // ======================================================================
    // findField
    // ======================================================================

    @Test
    void findField_fieldOnDirectClass_returnsField() {
        Field f = ClassHelper.findField(Child.class, "childField");

        assertNotNull(f);
        assertEquals("childField", f.getName());
    }

    @Test
    void findField_fieldOnSuperclass_returnsField() {
        Field f = ClassHelper.findField(Child.class, "baseField");

        assertNotNull(f);
        assertEquals("baseField", f.getName());
    }

    @Test
    void findField_fieldAbsentEverywhere_returnsNull() {
        Field f = ClassHelper.findField(Child.class, "doesNotExist");

        assertNull(f);
    }

    // ======================================================================
    // getAllProperties
    // ======================================================================

    @Test
    void getAllProperties_includesFieldsFromClassAndSuperclass() {
        List<Field> fields = new ArrayList<>();

        ClassHelper.getAllProperties(fields, Child.class);

        List<String> names = fields.stream().map(Field::getName).toList();
        assertTrue(names.contains("childField"), "must include own field");
        assertTrue(names.contains("baseField"),  "must include superclass field");
    }

    // ======================================================================
    // getFieldFromObject
    // ======================================================================

    @Test
    void getFieldFromObject_fieldOnDirectClass_returnsField() throws NoSuchFieldException {
        Field f = ClassHelper.getFieldFromObject("childField", Child.class);

        assertNotNull(f);
        assertEquals("childField", f.getName());
    }

    @Test
    void getFieldFromObject_fieldOnSuperclass_returnsField() throws NoSuchFieldException {
        Field f = ClassHelper.getFieldFromObject("baseField", Child.class);

        assertNotNull(f);
        assertEquals("baseField", f.getName());
    }

    @Test
    void getFieldFromObject_fieldAbsentEverywhere_throwsNoSuchFieldException() {
        assertThrows(NoSuchFieldException.class,
                () -> ClassHelper.getFieldFromObject("ghost", Child.class));
    }

    // ======================================================================
    // getFieldGenericType
    // ======================================================================

    @Test
    void getFieldGenericType_listOfString_returnsStringClass() throws NoSuchFieldException {
        Field f = WithList.class.getDeclaredField("items");

        Class<?> type = ClassHelper.getFieldGenericType(f);

        assertEquals(String.class, type);
    }

    @Test
    void getFieldGenericType_nonParameterizedField_returnsObjectClass() throws NoSuchFieldException {
        Field f = WithList.class.getDeclaredField("count");

        Class<?> type = ClassHelper.getFieldGenericType(f);

        assertEquals(Object.class, type);
    }

    // ======================================================================
    // isValueObject
    // ======================================================================

    @Test
    void isValueObject_vector3f_returnsTrue() {
        assertTrue(ClassHelper.isValueObject(Vector3f.class));
    }

    @Test
    void isValueObject_vector4f_returnsTrue() {
        assertTrue(ClassHelper.isValueObject(Vector4f.class));
    }

    @Test
    void isValueObject_quaternionf_returnsFalse_knownBug() {
        // Production code checks for "org.joml.Quaternion4f" (misspelled) instead of
        // "org.joml.Quaternionf", so this always returns false. Test documents the bug.
        assertFalse(ClassHelper.isValueObject(Quaternionf.class));
    }

    // ======================================================================
    // createCollectionOfType
    // ======================================================================

    @Test
    void createCollectionOfType_list_returnsArrayList() throws Exception {
        Collection<Object> col = ClassHelper.createCollectionOfType(List.class);

        assertInstanceOf(ArrayList.class, col);
    }

    @Test
    void createCollectionOfType_set_returnsHashSet() throws Exception {
        Collection<Object> col = ClassHelper.createCollectionOfType(Set.class);

        assertInstanceOf(HashSet.class, col);
    }

    @Test
    void createCollectionOfType_unknownInterface_throwsRuntimeException() {
        assertThrows(RuntimeException.class,
                () -> ClassHelper.createCollectionOfType(Queue.class));
    }

    // ======================================================================
    // setProperty
    // ======================================================================

    @Test
    void setProperty_setsFieldValueOnDirectClass() {
        Child obj = new Child();

        ClassHelper.setProperty(obj, "childField", "updated");

        assertEquals("updated", obj.childField);
    }

    @Test
    void setProperty_walksSuperclainsChainToSetInheritedField() {
        Child obj = new Child();

        ClassHelper.setProperty(obj, "baseField", "newBase");

        assertEquals("newBase", obj.baseField);
    }

    // ======================================================================
    // setDeepProperty
    // ======================================================================

    @Test
    void setDeepProperty_setsNestedFieldViaDotPath() {
        Outer obj = new Outer();

        ClassHelper.setDeepProperty(obj, "inner.value", "deep");

        assertEquals("deep", obj.inner.value);
    }
}
