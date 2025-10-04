package nl.framegengine.core.rendering.renderers;

import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.modelLoaders.PrimitiveLoader;
import nl.framegengine.core.physics.Raycast;
import nl.framegengine.core.shaders.DebugShader;
import nl.framegengine.core.utils.Calculus;
import nl.framegengine.core.utils.Constants;
import nl.framegengine.core.utils.DebugEntity;
import nl.framegengine.core.utils.ObjectPool;
import nl.framegengine.core.visual.Mesh;
import nl.framegengine.core.visual.MeshMaterialSet;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.util.HashSet;
import java.util.Set;

public class DebugRenderer implements IRenderer {

    private final Set<DebugEntity> debugEntities = new HashSet<>();
    private DebugShader debugShader;
    private Camera mainCamera;

    private Mesh cubeMesh;

    @Override
    public void init() throws Exception {
        debugShader = new DebugShader();
        debugShader.init();

        cubeMesh = PrimitiveLoader.getCube().getMesh();
    }

    @Override
    public void render() {
        if(debugEntities.isEmpty() || mainCamera == null) return;

        debugShader.bind();
        debugShader.render(mainCamera);

        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);

        debugEntities.forEach(debugEntity -> {
            bind(null);
            debugShader.prepare(debugEntity.getPosition(), debugEntity.getRotation(), debugEntity.getScale(), mainCamera);

            if(debugEntity.getShape() == DebugEntity.DebugShape.CUBE) {
                GL11.glDrawElements(GL11.GL_TRIANGLES, cubeMesh.getVertexCount(), GL11.GL_UNSIGNED_INT, 0);
            }
            unbind();
        });
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
        debugEntities.clear();

        debugShader.unbind();
    }

    @Override
    public void bind(MeshMaterialSet meshMaterialSet) {
        GL30.glBindVertexArray(cubeMesh.getVaoID());

        GL20.glEnableVertexAttribArray(0);
    }

    @Override
    public void unbind() {
        GL20.glDisableVertexAttribArray(0);
        GL30.glBindVertexArray(0);
    }

    @Override
    public void cleanUp() {

    }

    public void drawCube(Vector3f position){
        debugEntities.add(new DebugEntity(position, DebugEntity.DebugShape.CUBE));
    }

    public void drawCube(Vector3f position, Vector3f size){
        debugEntities.add(new DebugEntity(position, size, DebugEntity.DebugShape.CUBE));
    }

    public void drawCube(Vector3f position, float size){
        debugEntities.add(new DebugEntity(position, size, DebugEntity.DebugShape.CUBE));
    }

    public void drawCube(Vector3f position, Quaternionf rotation, Vector3f size){
        debugEntities.add(new DebugEntity(position, rotation, size, DebugEntity.DebugShape.CUBE));
    }

    public void drawLine(Vector3f startPoint, Vector3f endPoint){
        Vector3f centerPoint = new Vector3f();
        centerPoint.x = Math.lerp(startPoint.x, endPoint.x, 0.5f);
        centerPoint.y = Math.lerp(startPoint.y, endPoint.y, 0.5f);
        centerPoint.z = Math.lerp(startPoint.z, endPoint.z, 0.5f);

        Vector3f difference = Calculus.subtractVectors(startPoint, endPoint);
        float length = difference.length();
        Vector3f direction = difference.normalize();

        if(direction.lengthSquared() <= 0.01) direction.set(0, 0, -1);
        Quaternionf lookDirection = new Quaternionf().rotateTo(new Vector3f(Constants.VECTOR3_FORWARD), direction);

        debugEntities.add(new DebugEntity(centerPoint, lookDirection, new Vector3f(0.025f, 0.025f, length)));
    }

    public void drawRay(Raycast.Ray ray, float length){
        Vector3f endPoint = ObjectPool.VECTOR3F_POOL.obtain();
        endPoint.set(ray.direction.normalize()).mul(length);
        endPoint.set(Calculus.addVectors(ray.origin, endPoint));

        Vector3f centerPoint = ObjectPool.VECTOR3F_POOL.obtain();
        centerPoint.x = Math.lerp(ray.origin.x, endPoint.x, 0.5f);
        centerPoint.y = Math.lerp(ray.origin.y, endPoint.y, 0.5f);
        centerPoint.z = Math.lerp(ray.origin.z, endPoint.z, 0.5f);

        Quaternionf lookDirection = new Quaternionf().rotateTo(new Vector3f(Constants.VECTOR3_FORWARD), ray.direction);

        debugEntities.add(new DebugEntity(centerPoint, lookDirection, new Vector3f(0.0125f, 0.0125f, length)));
        ObjectPool.VECTOR3F_POOL.free(centerPoint);
        ObjectPool.VECTOR3F_POOL.free(endPoint);
    }

    public void setMainCamera(Camera camera){
        this.mainCamera = camera;
    }
}
