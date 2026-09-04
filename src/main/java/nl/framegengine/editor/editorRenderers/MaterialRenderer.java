package nl.framegengine.editor.editorRenderers;

import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.entity.GameObject;
import nl.framegengine.core.entity.Scene;
import nl.framegengine.core.entity.SceneManager;
import nl.framegengine.core.lighting.DirectionalLight;
import nl.framegengine.core.modelLoaders.PrimitiveLoader;
import nl.framegengine.core.rendering.renderers.IRenderer;
import nl.framegengine.core.rendering.shadow.ShadowRenderer;
import nl.framegengine.core.rendering.utils.FrameBuffer;
import nl.framegengine.core.shaders.Shader;
import nl.framegengine.core.shaders.SimpleLitShader;
import nl.framegengine.core.visual.Material;
import nl.framegengine.core.visual.Mesh;
import nl.framegengine.core.visual.MeshMaterialSet;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class MaterialRenderer implements IRenderer {

    private Camera previewCamera;
    private MeshMaterialSet previewMMS = new MeshMaterialSet();
    private FrameBuffer frameBuffer;
    private GameObject previewObject;
    private static int previewSize = 256;
    private Shader previewShader = null;
    private Material previewMaterial = null;
    private static Scene previewScene;
    private static SimpleLitShader litShader = null;
    private ShadowRenderer shadowRenderer = null;

    @Override
    public void init() throws Exception {

    }

    public void postStartInit() throws Exception {
        previewScene = new Scene();
        previewScene.init();
        GameObject directionalLightObject = new GameObject("Directional light");
        DirectionalLight directionalLight = (DirectionalLight) directionalLightObject.addComponent(new DirectionalLight(new Vector3f(1f, 1f, 1f), new Vector3f(0f, 0f, -1f).normalize(), 4f));
        directionalLightObject.setRotation(45f, 0f, 0f);

        previewScene.setDirectionalLight(directionalLight);

        previewObject = new GameObject("PreviewObject");
        previewObject.setPosition(0, 0, 0f);
        previewObject.setRotation(-25, -35f, 0f);

        previewCamera = new Camera();
        Mesh previewMesh = PrimitiveLoader.getCube().getMesh();
        previewMMS = new MeshMaterialSet(previewMesh);
        previewMMS.setRoot(previewObject);
        previewObject.callUpdate();


        GameObject cameraObject = new GameObject("PreviewCamera");
        cameraObject.setPosition(0, 0, 2f);
        cameraObject.setRotation(0, 0f, 0);
        cameraObject.addComponent(previewCamera);

        previewScene.addEntity(previewObject);
        previewScene.addEntity(directionalLightObject);
        previewScene.addEntity(cameraObject);

        cameraObject.callUpdate();
        previewCamera.updateViewMatrix();
        previewCamera.updateAspectRatio((float)previewSize / (float)previewSize);
        previewCamera.updateProjectionMatrix();
        previewCamera.updateViewFrustum();

        previewScene.processGameObjects();
        previewScene.updateRootGameObjectTransforms();

        frameBuffer = new FrameBuffer(previewSize, previewSize, FrameBuffer.NONE);

        shadowRenderer = new ShadowRenderer();
        shadowRenderer.init();
    }

    @Override
    public void render() {

    }

    @Override
    public void bind(MeshMaterialSet meshMaterialSet) {
        GL30.glBindVertexArray(meshMaterialSet.getMesh().getVaoID());

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

        boolean needsBlend = meshMaterialSet.material.isDoubleSided() || meshMaterialSet.material.isTransparent();
        if (needsBlend) {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        } else {
            GL11.glDisable(GL11.GL_BLEND);
        }

        // Only change cull state when needed
        boolean needsCull = !meshMaterialSet.material.isDoubleSided();
        if (needsCull) {
            GL11.glEnable(GL11.GL_CULL_FACE);
        } else {
            GL11.glDisable(GL11.GL_CULL_FACE);
        }
    }

    @Override
    public void unbind() {
        for (int i = 0; i <= 8; i++) {
            GL20.glDisableVertexAttribArray(i);
        }
        GL30.glBindVertexArray(0);
    }

    @Override
    public void cleanUp() {

    }

    public void renderPreview(Material material){
        if(previewCamera == null || material == null) return;

        int previousDrawMode = GL11.glGetInteger(GL11.GL_POLYGON_MODE);
        int previousDoubleSided = GL11.glGetInteger(GL11.GL_CULL_FACE_MODE);


        if(material != previewMaterial){
            previewMaterial = material;
            previewShader = material.getShader();
        }

        if(previewShader instanceof SimpleLitShader simpleLitShader)
        {
            litShader = simpleLitShader;
            litShader.setLights(previewScene.getDirectionalLight(), null, null);
            litShader.updateGenericUniforms();
            shadowRenderer.render(previewScene);

            GL13.glActiveTexture(GL13.GL_TEXTURE9);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, shadowRenderer.getShadowMapID());
            previewShader.setTexture("shadowMap", 9);
            litShader.setShadowSpaceMatrix(shadowRenderer.getToShadowMapSpaceMatrix());
        }

        previewMMS.material = material;


        frameBuffer.bindFrameBuffer();

        try {
            previewShader.bind();
            previewShader.prepare(previewMMS, previewCamera);
            previewShader.render(previewCamera);
            bind(previewMMS);

            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
            GL11.glDrawElements(GL11.GL_TRIANGLES, previewMMS.getMesh().getVertexCount(), GL11.GL_UNSIGNED_INT, 0);

            GL11.glPolygonMode(previousDoubleSided, previousDrawMode);

        }catch (Exception e) {
            Debug.logError("Error: " + e);
        }
        finally {

            previewShader.unbind();
            unbind();
            GL11.glPolygonMode(previousDoubleSided, previousDrawMode);
            frameBuffer.unbindFrameBuffer();
        }




        if(litShader != null && SceneManager.currentScene != null){
            Scene currentScene = SceneManager.currentScene;
            litShader.setLights(currentScene.getDirectionalLight(), currentScene.getPointLights(), currentScene.getSpotLights());
            litShader = null;
        }
    }

    public int getPreviewFBOID(){
        return frameBuffer.getColourTexture();
    }
}
