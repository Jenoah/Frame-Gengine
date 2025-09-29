package nl.framegengine.core.utils;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public class DebugEntity {
    private final Vector3f position;
    private Vector3f scale = new Vector3f(1);
    private Quaternionf rotation = new Quaternionf().identity();
    private DebugShape shape = DebugShape.CUBE;

    public DebugEntity(Vector3f position){
        this.position = position;
    }

    public DebugEntity(Vector3f position, Vector3f scale){
        this.position = position;
        this.scale.set(scale);
    }

    public DebugEntity(Vector3f position, float scale){
        this.position = position;
        this.scale.set(scale);
    }

    public DebugEntity(Vector3f position, Quaternionf rotation){
        this.position = position;
        this.rotation.set(rotation);
    }

    public DebugEntity(Vector3f position, Quaternionf rotation, Vector3f scale){
        this.position = position;
        this.rotation.set(rotation);
        this.scale.set(scale);
    }

    public DebugEntity(Vector3f position, Quaternionf rotation, float scale){
        this.position = position;
        this.rotation.set(rotation);
        this.scale.set(scale);
    }

    public DebugEntity(Vector3f position, Vector3f scale, DebugShape shape){
        this.position = position;
        this.scale.set(scale);
        this.shape = shape;
    }

    public DebugEntity(Vector3f position, float scale, DebugShape shape){
        this.position = position;
        this.scale.set(scale);
        this.shape = shape;
    }

    public DebugEntity(Vector3f position, Quaternionf rotation, Vector3f scale, DebugShape shape){
        this.position = position;
        this.rotation.set(rotation);
        this.scale.set(scale);
        this.shape = shape;
    }

    public DebugEntity(Vector3f position, Quaternionf rotation, float scale, DebugShape shape){
        this.position = position;
        this.rotation.set(rotation);
        this.scale.set(scale);
        this.shape = shape;
    }

    public DebugEntity(Vector3f position, DebugShape shape){
        this.position = position;
        this.shape = shape;
    }

    public DebugEntity(Vector3f position, Quaternionf rotation, DebugShape shape){
        this.position = position;
        this.rotation.set(rotation);
        this.shape = shape;
    }

    public enum DebugShape{
        CUBE,
        //SPHERE
    }

    public Vector3f getPosition() { return position; }
    public Vector3f getScale() { return scale; }
    public Quaternionf getRotation() { return rotation; }
    public DebugShape getShape() { return shape; }
}
