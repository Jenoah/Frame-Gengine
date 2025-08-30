package nl.framegengine.core.utils;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.List;

public class Conversion {

    public static Vector3f[] floatArrayToVector3Array(float[] floatArray){
        Vector3f[] vectors = new Vector3f[floatArray.length / 3];
        for (int i = 0; i < vectors.length; i++) {
            Vector3f vector = new Vector3f();
            vector.x = floatArray[i * 3];
            vector.y = floatArray[i * 3 + 1];
            vector.z = floatArray[i * 3 + 2];
            vectors[i] = vector;
        }
        return vectors;
    }
    public static Vector2f[] floatArrayToVector2Array(float[] floatArray){
        Vector2f[] vectors = new Vector2f[floatArray.length / 2];
        for (int i = 0; i < vectors.length; i++) {
            Vector2f vector = new Vector2f();
            vector.x = floatArray[i * 2];
            vector.y = floatArray[i * 2 + 1];
            vectors[i] = vector;
        }
        return vectors;
    }

    public static float[] v2ToFloatArray(List<Vector2f> vectors){
        float[] vectorsStripped = new float[vectors.size() * 2];
        for (int i = 0; i < vectors.size(); i++) {
            vectorsStripped[i * 2] = vectors.get(i).x;
            vectorsStripped[i * 2 + 1] = vectors.get(i).y;
        }

        return vectorsStripped;
    }
    public static float[] toFloatArray(Vector2f[] vectors){
        float[] vectorsStripped = new float[vectors.length * 2];
        for (int i = 0; i < vectors.length; i++) {
            vectorsStripped[i * 2] = vectors[i].x;
            vectorsStripped[i * 2 + 1] = vectors[i].y;
        }

        return vectorsStripped;
    }

    public static float[] v3ToFloatArray(List<Vector3f> vectors){
        float[] vectorsStripped = new float[vectors.size() * 3];
        int i = 0;
        for(Vector3f vector : vectors){
            vectorsStripped[i * 3] = vector.x;
            vectorsStripped[i * 3 + 1] = vector.y;
            vectorsStripped[i * 3 + 2] = vector.z;
            i++;
        }

        return vectorsStripped;
    }
    public static float[] toFloatArray(Vector3f[] vectors){
        float[] vectorsStripped = new float[vectors.length * 3];
        for (int i = 0; i < vectors.length; i++) {
            vectorsStripped[i * 3] = vectors[i].x;
            vectorsStripped[i * 3 + 1] = vectors[i].y;
            vectorsStripped[i * 3 + 2] = vectors[i].z;
        }

        return vectorsStripped;
    }

    public static int[] toIntArray(List<Integer> integerArray){
        return integerArray.stream().mapToInt(i->i).toArray();
    }

    public static float angleTo360degrees(float angle) {
        // Convert radians to degrees
        angle = (float) Math.toDegrees(angle);
        // Normalize to [0, 360]
        angle = angle % 360;
        if (angle < 0) angle += 360;
        return angle;
    }

    public static float[] toFloatArray(List<Float> floatList){
        float[] floatArray = new float[floatList.size()];
        int i = 0;

        for (Float f : floatList) {
            floatArray[i++] = (f != null ? f : Float.NaN); // Or whatever default you want.
        }

        return floatArray;
    }

    public static String v3ToString(Vector3f vector){
        return "X " + floatTwoDecimals(vector.x) + ", Y " + floatTwoDecimals(vector.y) + ", Z " + floatTwoDecimals(vector.z);
    }

    public static String v2ToString(Vector2f vector){
        return "X " + floatTwoDecimals(vector.x) + ", Y " + floatTwoDecimals(vector.y);
    }

    public static String floatTwoDecimals(float number){
        boolean isNegative = number < 0;

        int intPart = (int) Math.abs(number);  // Get absolute value for the integer part
        int decimalPart = (int) ((Math.abs(number) - intPart) * 100);  // Get the decimal part (absolute value)

        // Build the string result
        return (isNegative ? "-" : "") + intPart + "." + (decimalPart < 10 ? "0" : "") + decimalPart;
    }

    public static Vector3i toVector3I(Vector3f a){
        return new Vector3i((int)a.x, (int)a.y, (int)a.z);
    }

    public static Vector3f toVector3F(Vector3i a){
        return new Vector3f(a.x, a.y, a.z);
    }


}
