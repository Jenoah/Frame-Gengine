package nl.framegengine.core.rendering.shadow;

import nl.framegengine.core.components.visual.RenderComponent;
import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.debugging.RenderMetrics;
import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.entity.Scene;
import nl.framegengine.core.rendering.renderers.IRenderer;
import nl.framegengine.core.shaders.ShadowShader;
import nl.framegengine.core.utils.Constants;
import nl.framegengine.core.visual.MeshMaterialSet;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

import java.util.HashSet;
import java.util.Set;


public class ShadowRenderer implements IRenderer {
    private final Set<MeshMaterialSet> shadowSets = new HashSet<>();
    private ShadowShader shadowShader;
    private ShadowFrameBuffer shadowFrameBuffer;
    private ShadowFrustum shadowFrustum;
    private RenderMetrics metrics;
    private boolean recordMetrics = false;

    private final Matrix4f lightViewMatrix = new Matrix4f();
    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f projectionViewMatrix = new Matrix4f();
    private final Matrix4f offset = createOffset();
    private final Matrix4f shadowMapSpaceMatrix = new Matrix4f();

    @Override
    public void init() throws Exception {
        shadowFrameBuffer = new ShadowFrameBuffer(Constants.SHADOW_MAP_SIZE, Constants.SHADOW_MAP_SIZE);
        shadowShader = new ShadowShader();
        shadowShader.init();
        shadowFrustum = new ShadowFrustum();
    }

    @Override
    public void render() {
        Debug.logError("Using wrong render method for rendering shadows");
    }

    public void render(Scene currentScene) {
        if(shadowSets.isEmpty() || currentScene.getDirectionalLight() == null) return;

        prepare(currentScene.getDirectionalLight().getForward(), shadowFrustum);

        if (recordMetrics) metrics.recordShaderBind();
        shadowShader.bind();

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_FRONT);


        shadowSets.forEach((meshMaterialSet) -> {
            if (!meshMaterialSet.getRoot().isEnabled() || !meshMaterialSet.material.castShadow()) return;
            if (recordMetrics) {
                metrics.recordStateChange();
            }

            bind(meshMaterialSet);

            shadowShader.prepare(meshMaterialSet, projectionViewMatrix);

            if (recordMetrics) metrics.recordDrawCall();

            if(meshMaterialSet.getMesh().isInstanced()){
                GL33.glDrawElementsInstanced(GL11.GL_TRIANGLES, meshMaterialSet.getMesh().getVertexCount(), GL11.GL_UNSIGNED_INT, 0, meshMaterialSet.getMesh().getInstanceCount());
            }else{
                if(recordMetrics) metrics.recordVertexCount(meshMaterialSet.getMesh().getVertexCount());
                GL11.glDrawElements(GL11.GL_TRIANGLES, meshMaterialSet.getMesh().getVertexCount(), GL11.GL_UNSIGNED_INT, 0);
            }

            unbind();
        });

        shadowFrameBuffer.unbindFrameBuffer();
        GL11.glCullFace(GL11.GL_BACK);
        shadowShader.unbind();
    }

    @Override
    public void bind(MeshMaterialSet meshMaterialSet) {
        GL30.glBindVertexArray(meshMaterialSet.getMesh().getVaoID());
        if (recordMetrics) metrics.recordVaoBind();
        GL20.glEnableVertexAttribArray(0);
        if (meshMaterialSet.getMesh().isInstanced()) {
            for (int i = 5; i <= 8; i++) GL20.glEnableVertexAttribArray(i);
        }
    }

    @Override
    public void unbind() {
        GL20.glDisableVertexAttribArray(0);
        for (int i = 5; i <= 8; i++) GL20.glDisableVertexAttribArray(i);
        GL30.glBindVertexArray(0);
    }

    public void prepare(Vector3f lightDirection, ShadowFrustum shadowFrustum){
        updateOrthoProjectionMatrix();
        updateLightViewMatrix(lightDirection, shadowFrustum.getCenter());
        shadowFrustum.update(lightViewMatrix);

        projectionMatrix.mulOrthoAffine(lightViewMatrix, projectionViewMatrix);
        shadowFrameBuffer.bindFrameBuffer();
    }

    @Override
    public void cleanUp() {
        shadowShader.cleanUp();
    }

    public void queue(RenderComponent renderComponent) {
        shadowSets.addAll(renderComponent.getMeshMaterialSets());
    }

    public void dequeue(RenderComponent renderComponent){
        renderComponent.getMeshMaterialSets().forEach((shadowSets::remove));
    }

    public final int getShadowMapID(){
        return shadowFrameBuffer.getShadowMap();
    }

    public final Matrix4f getToShadowMapSpaceMatrix() {
        return shadowMapSpaceMatrix.set(offset).mul(projectionViewMatrix);
    }

    public void setMainCamera(Camera camera){
        if(shadowFrustum != null) shadowFrustum.setCamera(camera);
    }

    private void updateLightViewMatrix(Vector3f lightDirection, Vector3f center){
        lightViewMatrix.identity().lookAt(
                new Vector3f(center).sub(lightDirection),
                center,
                Constants.VECTOR3_UP
        );
    }


    private void updateOrthoProjectionMatrix() {
        projectionMatrix.identity();
        projectionMatrix.m00(2f / shadowFrustum.getWidth());
        projectionMatrix.m11(2f / shadowFrustum.getHeight());
        projectionMatrix.m22(-2f / shadowFrustum.getLength());
        projectionMatrix.m33(1f);
    }

    private static Matrix4f createOffset() {
        return new Matrix4f()
                .translate(0.5f, 0.5f, 0.5f)
                .scale(0.5f, 0.5f, 0.5f);
    }

    public void setMetrics(RenderMetrics metrics){
        this.metrics = metrics;
        recordMetrics = true;
    }

    public void recordMetrics(boolean recordMetrics) {
        this.recordMetrics = recordMetrics;
    }
}
