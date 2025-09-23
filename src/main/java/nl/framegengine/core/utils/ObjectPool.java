package nl.framegengine.core.utils;

import org.joml.*;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

public class ObjectPool<T> {
    private final Queue<T> pool = new ConcurrentLinkedQueue<>();
    private final Supplier<T> creator;

    public ObjectPool(Supplier<T> creator) {
        this.creator = creator;
    }

    public T obtain() {
        T obj = pool.poll();
        return obj != null ? obj : creator.get();
    }

    public void free(T obj) {
        pool.offer(obj);
    }

    public static final ObjectPool<Vector4f> VECTOR4F_POOL = new ObjectPool<>(Vector4f::new);
    public static final ObjectPool<Vector3f> VECTOR3F_POOL = new ObjectPool<>(Vector3f::new);
    public static final ObjectPool<Vector2f> VECTOR2F_POOL = new ObjectPool<>(Vector2f::new);
    public static final ObjectPool<Quaternionf> QUATERNIONF_OBJECT_POOL = new ObjectPool<>(Quaternionf::new);
    public static final ObjectPool<Matrix4f> MATRIX4F_OBJECT_POOL = new ObjectPool<>(Matrix4f::new);
}