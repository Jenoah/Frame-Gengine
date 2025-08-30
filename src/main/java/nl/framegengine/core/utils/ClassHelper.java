package nl.framegengine.core.utils;

import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.entity.GameObject;

import java.lang.reflect.*;
import java.util.*;

public class ClassHelper {
    public static void setProperty(Object instance, String fieldName, Object value) {
        Class<?> clazz = instance.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                Debug.log("Found a " + clazz.getSimpleName() + " for " + fieldName + " with value of " + value.toString());

                if(field.getType() == GameObject.class && value instanceof String guid) {
                    GameObject gameObject = GameObject.getByGUID(guid);
                    if (gameObject != null) field.set(instance, gameObject);
                }else if(clazz.isAssignableFrom(IJsonSerializable.class)){
                    IJsonSerializable serializable = (IJsonSerializable)value;
                }else {
                    field.set(instance, value);
                }
                return; // success, exit method
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass(); // try superclass
            } catch (IllegalAccessException e) {
                Debug.logError("Access denied to field: " + fieldName);
                return;
            }
        }
        Debug.logError("Field '" + fieldName + "' not found in class " + instance.getClass().getName());
    }

    public static void setDeepProperty(Object instance, String propertyPath, Object value) {
        String[] fields = propertyPath.split("\\.");
        Object currentObj = instance;
        try {
            for (int i = 0; i < fields.length - 1; i++) {
                Field field = ClassHelper.findField(currentObj.getClass(), fields[i]);
                if (field == null) {
                    Debug.logError("Field '" + fields[i] + "' not found while traversing " + propertyPath);
                    return;
                }
                field.setAccessible(true);
                Object nestedObj = field.get(currentObj);
                if (nestedObj == null) {
                    nestedObj = field.getType().getDeclaredConstructor().newInstance();
                    field.set(currentObj, nestedObj);
                }
                currentObj = nestedObj;
            }
            Field lastField = ClassHelper.findField(currentObj.getClass(), fields[fields.length - 1]);
            if (lastField == null) {
                Debug.logError("Field '" + fields[fields.length - 1] + "' not found in " + currentObj.getClass().getName());
                return;
            }
            lastField.setAccessible(true);
            lastField.set(currentObj, value);
        } catch (Exception e) {
            Debug.logError("Failed to set property '" + propertyPath + "': " + e.getMessage());
        }
    }

    public static void getAllProperties(List<Field> fields, Class<?> type) {
        fields.addAll(Arrays.asList(type.getDeclaredFields()));

        if (type.getSuperclass() != null) {
            getAllProperties(fields, type.getSuperclass());
        }
    }

    public static void getAllPublicAndProtectedProperties(List<Field> fields, Class<?> type) {
        fields.addAll(Arrays.stream(type.getDeclaredFields()).filter(f -> Modifier.isPublic(f.getModifiers())  || Modifier.isProtected(f.getModifiers())).toList());

        if (type.getSuperclass() != null) {
            getAllPublicAndProtectedProperties(fields, type.getSuperclass());
        }
    }

    public static Field getFieldFromObject(String fieldName, Class<?> type) throws NoSuchFieldException {
        try {
            return type.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class<?> superClass = type.getSuperclass();
            if (superClass != null) {
                return getFieldFromObject(fieldName, superClass);
            } else {
                throw e;
            }
        }
    }

    public static void getAllPublicProperties(List<Field> fields, Class<?> type) {
        fields.addAll(Arrays.stream(type.getDeclaredFields()).filter(f -> Modifier.isPublic(f.getModifiers())).toList());

        if (type.getSuperclass() != null) {
            getAllPublicProperties(fields, type.getSuperclass());
        }
    }

    public static Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    public static Class<?> getFieldGenericType(Field field) {
        Type type = field.getGenericType();
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Type[] args = pType.getActualTypeArguments();
            if (args.length > 0) return (Class<?>) args[0];
        }
        return Object.class; // fallback
    }

    public static boolean isValueObject(Class<?> type) {
        return type.getName().equals("org.joml.Vector3f") ||
                type.getName().equals("org.joml.Vector4f") ||
                type.getName().equals("org.joml.Quaternion4f");
    }

    @SuppressWarnings("unchecked")
    public static Collection<Object> createCollectionOfType(Class<?> type) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Collection<Object> collection;
        if (Set.class.isAssignableFrom(type)) {
            collection = new HashSet<>();
        } else if (List.class.isAssignableFrom(type)) {
            collection = new ArrayList<>();
        } else if (!type.isInterface()) {
            collection = (Collection<Object>) type.getDeclaredConstructor().newInstance();
        } else {
            throw new RuntimeException("Cannot instantiate collection type: " + type.getName());
        }

        return collection;
    }
}
