package nl.framegengine.core.rendering.renderers;

import nl.framegengine.core.components.visual.RenderComponent;
import nl.framegengine.core.debugging.RenderMetrics;
import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.shaders.Shader;
import nl.framegengine.core.shaders.SimpleLitShader;
import nl.framegengine.core.visual.MeshMaterialSet;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.*;

import java.util.*;

public class ComponentRenderer implements IRenderer {

    private final Set<RenderComponent> renderObjects = new LinkedHashSet<>();

    // Using more efficient collection for shader-based batching
    private final Map<Shader, List<MeshMaterialSet>> sortedRenderObjects = new HashMap<>();
    private final List<MeshMaterialSet> sortedTransparentRenderObjects = new ArrayList<>();

    // Caching sorted collections for better iteration performance
    private final List<Map.Entry<Shader, List<MeshMaterialSet>>> cachedSortedEntries = new ArrayList<>();
    private final List<MeshMaterialSet> cachedTransparentEntries = new ArrayList<>();

    // Flag to track if cache needs updating
    private boolean needsRebatch = true;
    private boolean needsTransparentRebatch = true;

    private Matrix4f shadowSpaceMatrix = new Matrix4f();
    private int shadowMapID = 0;
    private Camera mainCamera;

    private RenderMetrics metrics;
    private boolean recordMetrics = false;
    private boolean wireframeMode = false;
    private boolean isRenderingOnTop = false;

    // Track state changes to minimize redundant OpenGL calls
    private Shader lastBoundShader = null;
    private boolean lastWireframeState = false;
    private boolean lastCullState = true;
    private boolean lastBlendState = false;

    private final Vector3f previousCameraPosition = new Vector3f(0);

    @Override
    public void init() throws Exception {  }

    @Override
    public void render() {
        if ((sortedRenderObjects.isEmpty() && sortedTransparentRenderObjects.isEmpty()) || mainCamera == null) return;

        if(mainCamera.getPosition().distanceSquared(previousCameraPosition) > 10){
            previousCameraPosition.set(mainCamera.getPosition());
            needsTransparentRebatch = true;
        }

        // Update cached collections if needed
        if (needsRebatch) updateRenderBatches(false);
        if (needsTransparentRebatch) updateRenderBatches(true);


        //Render opaque objects
        renderPassShaderList(cachedSortedEntries);

        //Render transparent objects
        GL11.glDepthMask(false);
        renderPass(cachedTransparentEntries);
        GL11.glDepthMask(true);

        // Reset state when done
        if (isRenderingOnTop) {
            GL11.glDepthRange(0, 1.0);
            isRenderingOnTop = false;
        }

        if (lastBoundShader != null) {
            lastBoundShader.unbind();
            lastBoundShader = null;
        }
    }

    private void updateRenderBatches() {
        updateRenderBatches(false);
    }

    private void updateRenderBatches(boolean transparentOnly) {
        if(!transparentOnly) sortEntries(sortedRenderObjects, cachedSortedEntries, false);
        sortEntries(sortedTransparentRenderObjects, cachedTransparentEntries, true);

        needsRebatch = false;
        needsTransparentRebatch = false;
    }

    private List<Map.Entry<Shader, List<MeshMaterialSet>>> sortEntries(Map<Shader, List<MeshMaterialSet>> entryList, List<Map.Entry<Shader, List<MeshMaterialSet>>> outputList, boolean backToFront){
        outputList.clear();

        for (Map.Entry<Shader, List<MeshMaterialSet>> entry : entryList.entrySet()){
            List<MeshMaterialSet> list = entry.getValue();
            list.sort(Comparator.comparingDouble(mms -> mms.getRoot().getRenderCameraSquaredDistance()));
            if(backToFront) entry.setValue(list.reversed()); //TODO: This doesn't work and it allocates a new list which is not needed
            outputList.add(entry);
        }

        return outputList;
    }

    private List<MeshMaterialSet> sortEntries(List<MeshMaterialSet> entryList, List<MeshMaterialSet> outputList, boolean backToFront){
        outputList.clear();

        entryList.sort(Comparator.comparingDouble(mms -> mms.getRoot().getRenderCameraSquaredDistance()));
        if(backToFront) entryList = entryList.reversed();

        outputList.addAll(entryList);
        return outputList;
    }

    private void renderPassShaderList(List<Map.Entry<Shader, List<MeshMaterialSet>>> batchEntries) {
        for (Map.Entry<Shader, List<MeshMaterialSet>> entry : batchEntries) {
            Shader shader = entry.getKey();
            List<MeshMaterialSet> meshMaterialSetList = entry.getValue();

            if (meshMaterialSetList.isEmpty()) continue;

            // Bind shader only when different from last one
            if (shader != lastBoundShader) {
                if (lastBoundShader != null) {
                    lastBoundShader.unbind();
                }

                if (recordMetrics) metrics.recordShaderBind();
                shader.bind();
                shader.render(mainCamera);
                lastBoundShader = shader;
            }

            // Set wireframe state only when needed
            if (wireframeMode != lastWireframeState) {
                GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, wireframeMode ? GL11.GL_LINE : GL11.GL_FILL);
                lastWireframeState = wireframeMode;
            }

            // Process each mesh material set
            renderPass(shader, meshMaterialSetList);
        }
    }

    private void renderPass(List<MeshMaterialSet> batchEntries) {
        if (batchEntries.isEmpty()) return;

        for (MeshMaterialSet entry : batchEntries) {
            Shader shader = entry.material.getShader();

            // Bind shader only when different from last one
            if (shader != lastBoundShader) {
                if (lastBoundShader != null) {
                    lastBoundShader.unbind();
                }

                if (recordMetrics) metrics.recordShaderBind();
                shader.bind();
                shader.render(mainCamera);
                lastBoundShader = shader;
            }

            // Set wireframe state only when needed
            if (wireframeMode != lastWireframeState) {
                GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, wireframeMode ? GL11.GL_LINE : GL11.GL_FILL);
                lastWireframeState = wireframeMode;
            }

            // Process each mesh material set
            renderPass(entry);
        }
    }

    private void renderPass(Shader shader, List<MeshMaterialSet> meshMaterialSetList) {
        if(wireframeMode) GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);

        meshMaterialSetList.forEach(this::renderPass);

        if(wireframeMode) GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
    }

    private void renderPass(MeshMaterialSet meshMaterialSet) {
        if (!meshMaterialSet.getRoot().isEnabled() || !mainCamera.isInFrustumAABB(meshMaterialSet.getRoot())) return;
        if (recordMetrics) metrics.recordStateChange();

        bind(meshMaterialSet);
        prepareShadow(meshMaterialSet);
        meshMaterialSet.material.getShader().prepare(meshMaterialSet, mainCamera);

        if(meshMaterialSet.material.isOnTop() != isRenderingOnTop){
            isRenderingOnTop = meshMaterialSet.material.isOnTop();
            if(isRenderingOnTop) {
                GL11.glDepthRange(0, 0.01);
            }else{
                GL11.glDepthRange(0.01, 1.0);
            }
        }

        if (recordMetrics) metrics.recordDrawCall();
        if(meshMaterialSet.getMesh().isInstanced()){
            GL33.glDrawElementsInstanced(GL11.GL_TRIANGLES, meshMaterialSet.getMesh().getVertexCount(), GL11.GL_UNSIGNED_INT, 0, meshMaterialSet.getMesh().getInstanceCount());
        }else{
            if(recordMetrics) metrics.recordVertexCount(meshMaterialSet.getMesh().getVertexCount());
            GL11.glDrawElements(GL11.GL_TRIANGLES, meshMaterialSet.getMesh().getVertexCount(), GL11.GL_UNSIGNED_INT, 0);
        }

        unbind();
    }

    public void bind(MeshMaterialSet meshMaterialSet) {
        // Bind VAO only when necessary
        GL30.glBindVertexArray(meshMaterialSet.getMesh().getVaoID());
        if (recordMetrics) metrics.recordVaoBind();

        // Enable vertex attributes efficiently
        GL20.glEnableVertexAttribArray(0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glEnableVertexAttribArray(2);
        if (meshMaterialSet.getMesh().hasTangents()) {
            GL20.glEnableVertexAttribArray(3);
            GL20.glEnableVertexAttribArray(4);
        }
        if(meshMaterialSet.getMesh().isInstanced()){
            GL20.glEnableVertexAttribArray(5);
            GL20.glEnableVertexAttribArray(6);
            GL20.glEnableVertexAttribArray(7);
            GL20.glEnableVertexAttribArray(8);
        }

        // Only change blend/cull state when needed
        boolean needsBlend = meshMaterialSet.material.isDoubleSided() || meshMaterialSet.material.isTransparent();
        if (needsBlend != lastBlendState) {
            if (needsBlend) {
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            } else {
                GL11.glDisable(GL11.GL_BLEND);
            }
            lastBlendState = needsBlend;
        }

        // Only change cull state when needed
        boolean needsCull = !meshMaterialSet.material.isDoubleSided();
        if (needsCull != lastCullState) {
            if (needsCull) {
                GL11.glEnable(GL11.GL_CULL_FACE);
            } else {
                GL11.glDisable(GL11.GL_CULL_FACE);
            }
            lastCullState = needsCull;
        }
    }

    private void prepareShadow(MeshMaterialSet meshMaterialSet){
        if(meshMaterialSet.material.receiveShadows() && meshMaterialSet.material.getShader() instanceof SimpleLitShader) {
            ((SimpleLitShader) meshMaterialSet.material.getShader()).setShadowSpaceMatrix(shadowSpaceMatrix);
            GL13.glActiveTexture(GL13.GL_TEXTURE9);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, shadowMapID);
            meshMaterialSet.material.getShader().setTexture("shadowMap", 9);
        }
    }

    @Override
    public void unbind() {
        // Only disable what we actually enabled
        for (int i = 0; i <= 8; i++) {
            GL20.glDisableVertexAttribArray(i);
        }
        GL30.glBindVertexArray(0);
    }

    @Override
    public void cleanUp() {
        renderObjects.clear();
        sortedRenderObjects.clear();
        sortedTransparentRenderObjects.clear();
        mainCamera = null;
    }

    public void queue(RenderComponent renderComponent) {
        if(renderObjects.contains(renderComponent)) return;
        this.renderObjects.add(renderComponent);
        needsRebatch = true;
        renderComponent.getMeshMaterialSets().forEach(meshMaterialSet -> {
            if(meshMaterialSet.material.isTransparent()){
                if(!sortedTransparentRenderObjects.contains(meshMaterialSet)) {
                    sortedTransparentRenderObjects.add(meshMaterialSet);
                }
            }else{
                if (!sortedRenderObjects.containsKey(meshMaterialSet.material.getShader())) {
                    List<MeshMaterialSet> meshMaterialSets = new ArrayList<>();
                    meshMaterialSets.add(meshMaterialSet);
                    sortedRenderObjects.put(meshMaterialSet.material.getShader(), meshMaterialSets);
                } else {
                    sortedRenderObjects.get(meshMaterialSet.material.getShader()).add(meshMaterialSet);
                }
            }
        });
    }

    public void dequeue(RenderComponent renderComponent) {
        if(!renderObjects.contains(renderComponent)) return;
        needsRebatch = true;
        renderComponent.getMeshMaterialSets().forEach(meshMaterialSet -> {
            if(meshMaterialSet.material.isTransparent()){
                sortedTransparentRenderObjects.remove(meshMaterialSet);
            }else{
                sortedRenderObjects.get(meshMaterialSet.material.getShader()).remove(meshMaterialSet);
            }
        });

        renderObjects.remove(renderComponent);
    }

    public void setShadowSpaceMatrix(Matrix4f shadowSpaceMatrix){
        this.shadowSpaceMatrix = shadowSpaceMatrix;
    }

    public void setShadowMapID(int shadowMapID){
        this.shadowMapID = shadowMapID;
    }

    public void setMetrics(RenderMetrics metrics){
        this.metrics = metrics;
        recordMetrics = true;
    }

    public void recordMetrics(boolean recordMetrics) {
        this.recordMetrics = recordMetrics;
    }

    public void setMainCamera(Camera camera){
        this.mainCamera = camera;
    }

    public Camera getMainCamera() { return this.mainCamera; }

    public void setWireframeMode(boolean wireframeMode){
        this.wireframeMode = wireframeMode;
    }
}
