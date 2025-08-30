package nl.framegengine.core.rendering.renderers;

import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.entity.GameObject;
import nl.framegengine.core.visual.Mesh;
import nl.framegengine.core.modelLoaders.PrimitiveLoader;
import nl.framegengine.core.shaders.DebugShader;
import nl.framegengine.core.utils.DebugEntity;
import nl.framegengine.core.visual.MeshMaterialSet;
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
    public void prepare(GameObject entity, Camera camera) {

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

    public void drawCube(Vector3f position, Quaternionf rotation, Vector3f size){
        debugEntities.add(new DebugEntity(position, rotation, size, DebugEntity.DebugShape.CUBE));
    }

    public void setMainCamera(Camera camera){
        this.mainCamera = camera;
    }
}
