package nl.framegengine.core.entity;

import nl.framegengine.core.components.Component;
import nl.framegengine.core.components.visual.RenderComponent;
import nl.framegengine.core.engine.WindowManager;
import nl.framegengine.core.modelLoaders.PrimitiveLoader;
import nl.framegengine.core.rendering.utils.FrustumPlane;
import nl.framegengine.core.shaders.ShaderManager;
import nl.framegengine.core.utils.AABB;
import nl.framegengine.core.utils.Constants;
import nl.framegengine.core.utils.ObjectPool;
import nl.framegengine.core.visual.Material;
import nl.framegengine.core.visual.Mesh;
import nl.framegengine.core.visual.MeshMaterialSet;
import nl.framegengine.core.visual.Texture;
import nl.framegengine.editor.EngineSettings;
import org.joml.*;

import java.lang.Math;

public class Camera extends Component {

    private static Camera mainCamera = null;

    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f viewProjectionMatrix = new Matrix4f();
    private final Matrix4f viewMatrix = new Matrix4f();
    private final WindowManager windowManager;
    private FrustumPlane[] frustumPlanes = new FrustumPlane[6];

    private final AABB worldFrustumTestingAABB = new AABB();
    private final Vector3f frustumPlaneNormal = new Vector3f();
    private boolean isShowingProxy = false;
    private float aspectRatio = 1.777f;
    public float FOV = 60f;

    public Camera() {
        super();

        if(mainCamera == null) mainCamera = this;
        windowManager = WindowManager.getInstance();
        runInEditor = true;

        for (int i = 0; i < frustumPlanes.length; i++) {
            frustumPlanes[i] = new FrustumPlane();
        }

        updateViewFrustum();
        updateAspectRatio();
        updateProjectionMatrix();
        //root.callUpdate();
    }

    @Override
    public void initiate() {
        super.initiate();
        root.addUpdateTransformActions(this::updateViewMatrix);
        root.addUpdateTransformActions(this::updateViewProjectionMatrix);
        root.addUpdateTransformActions(this::updateViewFrustum);
    }

    public void updateViewFrustum(){

        frustumPlaneNormal.set(viewProjectionMatrix.m03() + viewProjectionMatrix.m00(),
                viewProjectionMatrix.m13() + viewProjectionMatrix.m10(),
                viewProjectionMatrix.m23() + viewProjectionMatrix.m20());
        float d = viewProjectionMatrix.m33() + viewProjectionMatrix.m30();
        frustumPlanes[0].set(frustumPlaneNormal, d);
        frustumPlanes[0].normalize();

        // Right plane
        frustumPlaneNormal.set(viewProjectionMatrix.m03() - viewProjectionMatrix.m00(),
                viewProjectionMatrix.m13() - viewProjectionMatrix.m10(),
                viewProjectionMatrix.m23() - viewProjectionMatrix.m20());
        d = viewProjectionMatrix.m33() - viewProjectionMatrix.m30();
        frustumPlanes[1].set(frustumPlaneNormal, d);
        frustumPlanes[1].normalize();

        // Bottom plane
        frustumPlaneNormal.set(viewProjectionMatrix.m03() + viewProjectionMatrix.m01(),
                viewProjectionMatrix.m13() + viewProjectionMatrix.m11(),
                viewProjectionMatrix.m23() + viewProjectionMatrix.m21());
        d = viewProjectionMatrix.m33() + viewProjectionMatrix.m31();
        frustumPlanes[2].set(frustumPlaneNormal, d);
        frustumPlanes[2].normalize();

        // Top plane
        frustumPlaneNormal.set(viewProjectionMatrix.m03() - viewProjectionMatrix.m01(),
                viewProjectionMatrix.m13() - viewProjectionMatrix.m11(),
                viewProjectionMatrix.m23() - viewProjectionMatrix.m21());
        d = viewProjectionMatrix.m33() - viewProjectionMatrix.m31();
        frustumPlanes[3].set(frustumPlaneNormal, d);
        frustumPlanes[3].normalize();

        // Near plane
        frustumPlaneNormal.set(viewProjectionMatrix.m03() + viewProjectionMatrix.m02(),
                viewProjectionMatrix.m13() + viewProjectionMatrix.m12(),
                viewProjectionMatrix.m23() + viewProjectionMatrix.m22());
        d = viewProjectionMatrix.m33() + viewProjectionMatrix.m32();
        frustumPlanes[4].set(frustumPlaneNormal, d);
        frustumPlanes[4].normalize();

        // Far plane
        frustumPlaneNormal.set(viewProjectionMatrix.m03() - viewProjectionMatrix.m02(),
                viewProjectionMatrix.m13() - viewProjectionMatrix.m12(),
                viewProjectionMatrix.m23() - viewProjectionMatrix.m22());
        d = viewProjectionMatrix.m33() - viewProjectionMatrix.m32();
        frustumPlanes[5].set(frustumPlaneNormal, d);
        frustumPlanes[5].normalize();
    }

    public boolean isInFrustumSphere(GameObject object){
        boolean isInFrustum = true;
        for (FrustumPlane plane : frustumPlanes) {
            if (plane.isSphereOutside(object.getPosition(), object.getRadius())) {
                isInFrustum = false;
                break;
            }
        }

        return isInFrustum;
    }

    public boolean isInFrustumAABB(GameObject object) {
        worldFrustumTestingAABB.set(object.getAabb().toWorld());

        Vector3f positiveCorner = ObjectPool.VECTOR3F_POOL.obtain().set(0,0,0);

        for (FrustumPlane plane : frustumPlanes) {
            positiveCorner.set(
                    plane.normal.x > 0 ? worldFrustumTestingAABB.max.x : worldFrustumTestingAABB.min.x,
                    plane.normal.y > 0 ? worldFrustumTestingAABB.max.y : worldFrustumTestingAABB.min.y,
                    plane.normal.z > 0 ? worldFrustumTestingAABB.max.z : worldFrustumTestingAABB.min.z
            );

            if (plane.getDistanceTo(positiveCorner) < 0) {
                ObjectPool.VECTOR3F_POOL.free(positiveCorner);
                return false;
            }
        }

        ObjectPool.VECTOR3F_POOL.free(positiveCorner);
        return true;
    }

    public void updateProjectionMatrix(){
        projectionMatrix.setPerspective((float) Math.toRadians(FOV), aspectRatio, Constants.Z_NEAR, Constants.Z_FAR);
        ShaderManager.updateGenericUniforms();
    }

    public void updateAspectRatio(){
        aspectRatio = windowManager == null ? 1.777f : ((float) windowManager.getWidth() / windowManager.getHeight());
    }

    public void updateAspectRatio(float aspect){
        aspectRatio = aspect;
    }

    public Matrix4f getProjectionMatrix(){
        return projectionMatrix;
    }

    public final Matrix4f getViewProjectionMatrix(){
        return viewProjectionMatrix;
    }

    public final Matrix4f updateViewProjectionMatrix(){
        this.viewProjectionMatrix.set(projectionMatrix).mul(getViewMatrix());
        return viewProjectionMatrix;
    }

    public final Matrix4f getViewMatrix(){
        return this.viewMatrix;
    }

    public final Matrix4f updateViewMatrix(){
        Vector3f currentPosition = root.getPosition();

        this.viewMatrix.identity()
                .rotate(root.getRotation())
                .translate(-currentPosition.x, -currentPosition.y, -currentPosition.z);

        return this.viewMatrix;
    }

    public static Camera getMainCamera(){
        return SceneManager.currentScene == null ? null : SceneManager.currentScene.getMainCamera();
    }

    public void sortGameObjectsInScene(){
        if(SceneManager.currentScene == null) return; //TODO: Implement hadUpdated() to check to increase performance
        Vector3f currentCameraPosition = ObjectPool.VECTOR3F_POOL.obtain().set(root.getPosition());
        SceneManager.currentScene.getSortedGameObjects().forEach(go -> go.setRenderCameraSquaredDistance(root.getPosition()));
        ObjectPool.VECTOR3F_POOL.free(currentCameraPosition);
    }

    @Override
    public void cleanUp() {
        super.cleanUp();
        root.removeUpdateTransformActions(this::updateViewMatrix);
        root.removeUpdateTransformActions(this::updateViewProjectionMatrix);
        root.removeUpdateTransformActions(this::updateViewFrustum);
        mainCamera = null;
    }

    public Camera showProxy(){
        if(!isShowingProxy && !EngineSettings.isInGame){
            Material proxyMaterial = new Material(ShaderManager.billboardShader);
            proxyMaterial.castShadow(false).receiveShadows(false);
            String texturePath = "textures/cameraGizmo.png";
            proxyMaterial.setAlbedoTexture(new Texture(texturePath, false, false)).setDoubleSided(true).setTransparent(true);
            Mesh proxyMesh = PrimitiveLoader.getQuadMesh();

            MeshMaterialSet mms = new MeshMaterialSet(proxyMesh, proxyMaterial);
            root.addComponent(new RenderComponent(mms));

            isShowingProxy = true;
        }

        return this;
    }

    public Vector2f projectToScreen(Vector3f worldPos) {
        Vector4f clipSpace = ObjectPool.VECTOR4F_POOL.obtain().set(worldPos, 1.0f).mul(getViewProjectionMatrix());
        clipSpace.div(clipSpace.w);

        float x = (clipSpace.x * 0.5f + 0.5f) * WindowManager.getInstance().getWidth();
        float y = (1.0f - (clipSpace.y * 0.5f + 0.5f)) * WindowManager.getInstance().getHeight(); // flip Y if needed

        ObjectPool.VECTOR4F_POOL.free(clipSpace);

        return new Vector2f(x, y);
    }
}
