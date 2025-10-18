package nl.framegengine.core.rendering.renderers;

import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.modelLoaders.PrimitiveLoader;
import nl.framegengine.core.physics.Raycast;
import nl.framegengine.core.shaders.DebugShader;
import nl.framegengine.core.utils.Calculus;
import nl.framegengine.core.utils.Constants;
import nl.framegengine.core.utils.DebugEntity;
import nl.framegengine.core.visual.Mesh;
import nl.framegengine.core.visual.MeshMaterialSet;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class DebugRenderer implements IRenderer {

    private final Set<DebugEntity> debugEntities = new HashSet<>();
    private final HashMap<Mesh, Vector3f> debugLines = new HashMap<>();
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
        if(mainCamera == null || debugLines.isEmpty() && debugEntities.isEmpty()) return;

        debugShader.bind();
        debugShader.render(mainCamera);

        renderDebugShape();
        renderLines();

        debugShader.unbind();
    }

    private void renderLines(){
        if(debugLines.isEmpty()) return;

        debugShader.prepare(Constants.VECTOR3_ZERO, Constants.QUATERNION_IDENTITY, Constants.VECTOR3_ONE, mainCamera);
        debugLines.forEach((mesh, color) -> {
            bind(mesh);
            debugShader.setColor(color);
            GL11.glDrawArrays(GL11.GL_LINES, 0, mesh.getVertices().length);
            unbind();
            mesh.cleanUp();
        });
        debugLines.clear();
    }

    private void renderDebugShape(){
        if(debugEntities.isEmpty()) {

            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);

            debugEntities.forEach(debugEntity -> {
                bind(cubeMesh);
                debugShader.prepare(debugEntity.getPosition(), debugEntity.getRotation(), debugEntity.getScale(), mainCamera);

                if (debugEntity.getShape() == DebugEntity.DebugShape.CUBE) {
                    GL11.glDrawElements(GL11.GL_TRIANGLES, cubeMesh.getVertexCount(), GL11.GL_UNSIGNED_INT, 0);
                }
                unbind();
            });
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
            debugEntities.clear();
        }
    }

    @Override
    public void bind(MeshMaterialSet meshMaterialSet) {
        GL30.glBindVertexArray(cubeMesh.getVaoID());
        GL20.glEnableVertexAttribArray(0);
    }

    public void bind(Mesh mesh) {
        GL30.glBindVertexArray(mesh.getVaoID());
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
        drawLine(startPoint, endPoint, Constants.VECTOR3_RIGHT);
    }

    public void drawLine(Vector3f startPoint, Vector3f endPoint, Vector3f color){
        Mesh lineMesh = new Mesh(new Vector3f[]{startPoint, endPoint});
        debugLines.put(lineMesh, color);
    }

    public void drawRay(Raycast.Ray ray, float length){
        drawRay(ray, length, Constants.VECTOR3_RIGHT);
    }

    public void drawRay(Raycast.Ray ray, float length, Vector3f color){
        Vector3f endPoint = new Vector3f();
        endPoint.set(ray.direction.normalize()).mul(length);
        endPoint.set(Calculus.addVectors(ray.origin, endPoint));

        drawLine(ray.origin, endPoint, color);
    }

    public void setMainCamera(Camera camera){
        this.mainCamera = camera;
    }
}
