package nl.framegengine.core.utils;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class Calculus {

//    Vector3f

    public static Vector3f addVectors(Vector3f a, Vector3f b){
        return new Vector3f(a.x + b.x, a.y + b.y, a.z + b.z);
    }

    public static Vector3f subtractVectors(Vector3f a, Vector3f b){
        return new Vector3f(a.x - b.x, a.y - b.y, a.z - b.z);
    }

    public static Vector3f multiplyVector(Vector3f a, float b){
        return new Vector3f(a.x * b, a.y * b, a.z * b);
    }

//    Vector3i

    public static Vector3i addVectors(Vector3i a, Vector3i b){
        return new Vector3i(a.x + b.x, a.y + b.y, a.z + b.z);
    }

    public static Vector3f addVectorsF(Vector3i a, Vector3i b){
        return new Vector3f(a.x + b.x, a.y + b.y, a.z + b.z);
    }

    public static Vector3f addVectors(Vector3f a, Vector3i b){
        return new Vector3f(a.x + b.x, a.y + b.y, a.z + b.z);
    }

    public static Vector3f addVectors(Vector3i a, Vector3f b){
        return new Vector3f(a.x + b.x, a.y + b.y, a.z + b.z);
    }

    public static Vector3i subtractVectors(Vector3i a, Vector3i b){
        return new Vector3i(a.x - b.x, a.y - b.y, a.z - b.z);
    }

    public static Vector3f subtractVectors(Vector3f a, Vector3i b){
        return new Vector3f(a.x - b.x, a.y - b.y, a.z - b.z);
    }

    public static Vector3f subtractVectors(Vector3i a, Vector3f b){
        return new Vector3f(a.x - b.x, a.y - b.y, a.z - b.z);
    }

    public static Vector3i divideVectors(Vector3i a, Vector3i b){
        return new Vector3i(a.x / b.x, a.y / b.y, a.z / b.z);
    }

    public static Vector3f divideVectors(Vector3f a, Vector3i b){
        return new Vector3f(a.x / b.x, a.y / b.y, a.z / b.z);
    }

    public static Vector3f divideVectors(Vector3f a, Vector3f b){
        return new Vector3f(a.x / b.x, a.y / b.y, a.z / b.z);
    }

    public static Vector3f divideVectors(Vector3i a, Vector3f b){
        return new Vector3f(a.x / b.x, a.y / b.y, a.z / b.z);
    }

    public static Vector3i multiplyVector(Vector3i a, float b){
        return new Vector3i((int)(a.x * b), (int)(a.y * b), (int)(a.z * b));
    }

    public static Vector3i multiplyVector(Vector3i a, Vector3i b){
        return new Vector3i(a.x * b.x, a.y * b.y, a.z * b.z);
    }

    public static Vector3f multiplyVector(Vector3f a, Vector3f b){
        return new Vector3f(a.x * b.x, a.y * b.y, a.z * b.z);
    }

    public static Vector3f multiplyVector(Vector3f a, Vector3i b){
        return new Vector3f(a.x * b.x, a.y * b.y, a.z * b.z);
    }

    public static Vector3f multiplyVector(Vector3i a, Vector3f b){
        return new Vector3f(a.x * b.x, a.y * b.y, a.z * b.z);
    }

//    Vector2f

    public static Vector2f addVectors(Vector2f a, Vector2f b){
        return new Vector2f(a.x + b.x, a.y + b.y);
    }

    public static Vector2f subtractVectors(Vector2f a, Vector2f b){
        return new Vector2f(a.x - b.x, a.y - b.y);
    }

    public static Vector2f multiplyVector(Vector2f a, float b){
        return new Vector2f(a.x * b, a.y * b);
    }

    public static Vector3f cross(Vector3f a, Vector3f b) {
        return a.cross(b);
    }
}
