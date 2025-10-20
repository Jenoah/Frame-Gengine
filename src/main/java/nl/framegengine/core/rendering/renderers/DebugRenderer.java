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

import java.util.HashSet;
import java.util.Set;

public class DebugRenderer implements IRenderer {

    public class DebugMesh {
        public Mesh mesh;
        public Vector3f worldPosition = new Vector3f(0);
        public Quaternionf worldRotation = new Quaternionf().identity();
        public Vector3f color;
        public Vector3f worldScale = new Vector3f(1);
        public boolean persistent;

        public DebugMesh(Vector3f startPoint, Vector3f endPoint) {
            this.mesh = new Mesh(new Vector3f[]{startPoint, endPoint});
            this.color = Constants.COLOR_RED;
            this.persistent = false;
            this.worldPosition.set(worldPosition);
        }

        public DebugMesh(Mesh mesh, Vector3f worldPosition) {
            this.mesh = mesh;
            this.color = Constants.COLOR_RED;
            this.persistent = false;
            this.worldPosition.set(worldPosition);
        }

        public DebugMesh(Vector3f startPoint, Vector3f endPoint, Vector3f color) {
            this.mesh = new Mesh(new Vector3f[]{startPoint, endPoint});
            this.color = color;
            this.persistent = false;
            this.worldPosition.set(worldPosition);
        }

        public DebugMesh(Mesh mesh, Vector3f worldPosition, Vector3f color) {
            this.mesh = mesh;
            this.color = color;
            this.persistent = false;
            this.worldPosition.set(worldPosition);
        }

        public DebugMesh(Vector3f startPoint, Vector3f endPoint, Vector3f color, boolean persistent) {
            this.mesh = new Mesh(new Vector3f[]{startPoint, endPoint});
            this.color = color;
            this.persistent = persistent;
            this.worldPosition.set(worldPosition);
        }

        public DebugMesh(Mesh mesh, Vector3f worldPosition, Vector3f color, boolean persistent) {
            this.mesh = mesh;
            this.color = color;
            this.persistent = persistent;
            this.worldPosition.set(worldPosition);
        }

        public void cleanUp(){
            this.mesh.cleanUp();
        }
    }

    private final Set<DebugEntity> debugEntities = new HashSet<>();
    private final Set<DebugMesh> debugMeshes = new HashSet<>();
    private final Set<DebugMesh> debugLines = new HashSet<>();
    private DebugShader debugShader;
    private Camera mainCamera;

    private Mesh cubeMesh;

    @Override
    public void init() throws Exception {
        debugShader = new DebugShader();
        debugShader.init();

        cubeMesh = PrimitiveLoader.getCube().getMesh();
        GL30.glEnable(GL30.GL_LINE_SMOOTH);
        GL30.glLineWidth(100);
    }

    @Override
    public void render() {
        if(mainCamera == null || debugMeshes.isEmpty() && debugEntities.isEmpty() && debugLines.isEmpty()) return;

        debugShader.bind();
        debugShader.render(mainCamera);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        renderDebugShape();
        renderMeshes();
        renderLines();

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        debugShader.unbind();
    }

    private void renderLines(){
        if(debugLines.isEmpty()) return;

        debugLines.forEach(debugMesh -> {
        debugShader.prepare(debugMesh.worldPosition, debugMesh.worldRotation, Constants.VECTOR3_ONE, mainCamera);
            bind(debugMesh.mesh);
            debugShader.setColor(debugMesh.color);
            GL11.glDrawArrays(GL11.GL_LINES, 0, debugMesh.mesh.getVertices().length);
            unbind();
        });

        debugLines.removeIf(debugMesh -> {
            if(!debugMesh.persistent){
                debugMesh.cleanUp();
                return true;
            }
            return false;
        });
    }

    private void renderMeshes(){
        if(debugMeshes.isEmpty()) return;

        debugMeshes.forEach(debugMesh -> {
            debugShader.prepare(debugMesh.worldPosition, debugMesh.worldRotation, debugMesh.worldScale, mainCamera);
            bind(debugMesh.mesh);
            debugShader.setColor(debugMesh.color);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, debugMesh.mesh.getVertices().length);
            unbind();
        });

        debugMeshes.removeIf(debugMesh -> {
            if(!debugMesh.persistent){
                debugMesh.cleanUp();
                return true;
            }
            return false;
        });
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

    public DebugMesh drawLine(Vector3f startPoint, Vector3f endPoint){
        return drawLine(startPoint, endPoint, Constants.COLOR_RED, false);
    }

    public DebugMesh drawLine(Vector3f startPoint, Vector3f endPoint, boolean persistent){
        return drawLine(startPoint, endPoint, Constants.COLOR_RED, persistent);
    }

    public DebugMesh drawLine(Vector3f startPoint, Vector3f endPoint, Vector3f color, boolean persistent){
        DebugMesh debugMesh = new DebugMesh(startPoint, endPoint, color, persistent);
        debugLines.add(debugMesh);
        return debugMesh;
    }

    public DebugMesh drawMesh(Vector3f worldPosition, Mesh mesh){
        return drawMesh(worldPosition, mesh, Constants.COLOR_RED, false);
    }

    public DebugMesh drawMesh(Vector3f worldPosition, Mesh mesh, boolean persistent){
        return drawMesh(worldPosition, mesh, Constants.COLOR_RED, persistent);
    }

    public DebugMesh drawMesh(Vector3f worldPosition, Mesh mesh, Vector3f color){
        return drawMesh(worldPosition, mesh, color, false);
    }

    public DebugMesh drawMesh(Vector3f worldPosition, Mesh mesh, Vector3f color, boolean persistent){
        DebugMesh debugMesh = new DebugMesh(mesh, worldPosition, color, persistent);
        debugMeshes.add(debugMesh);
        return debugMesh;
    }

    public void drawRay(Raycast.Ray ray, float length){
        drawRay(ray, length, Constants.COLOR_RED);
    }

    public void drawRay(Raycast.Ray ray, float length, Vector3f color){
        Vector3f endPoint = new Vector3f();
        endPoint.set(ray.direction.normalize()).mul(length);
        endPoint.set(Calculus.addVectors(ray.origin, endPoint));

        drawLine(ray.origin, endPoint, color, false);
    }

    public void setMainCamera(Camera camera){
        this.mainCamera = camera;
    }
}
