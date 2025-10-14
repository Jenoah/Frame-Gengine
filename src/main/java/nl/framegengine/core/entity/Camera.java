package nl.framegengine.core.entity;

import nl.framegengine.core.components.visual.RenderComponent;
import nl.framegengine.core.engine.WindowManager;
import nl.framegengine.core.modelLoaders.PrimitiveLoader;
import nl.framegengine.core.rendering.utils.FrustumPlane;
import nl.framegengine.core.shaders.ShaderManager;
import nl.framegengine.core.utils.AABB;
import nl.framegengine.core.utils.ObjectPool;
import nl.framegengine.core.visual.Material;
import nl.framegengine.core.visual.Mesh;
import nl.framegengine.core.visual.MeshMaterialSet;
import nl.framegengine.core.visual.Texture;
import nl.framegengine.editor.EngineSettings;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Camera extends GameObject {

    private static Camera mainCamera = null;

    private final Matrix4f viewProjectionMatrix = new Matrix4f();
    private final Matrix4f viewMatrix = new Matrix4f();
    private final WindowManager windowManager;
    private FrustumPlane[] frustumPlanes = new FrustumPlane[6];

    private final AABB worldFrustumTestingAABB = new AABB();
    private final Vector3f frustumPlaneNormal = new Vector3f();
    private boolean isShowingProxy = false;

    public Camera() {
        super();

        if(mainCamera == null) mainCamera = this;
        windowManager = WindowManager.getInstance();

        setPosition(new Vector3f(0, 0, 0));

        for (int i = 0; i < frustumPlanes.length; i++) {
            frustumPlanes[i] = new FrustumPlane();
        }

        updateViewFrustum();
        callUpdate();
    }

    public Camera(Vector3f position, Vector3f rotation) {
        super();

        windowManager = WindowManager.getInstance();

        setPosition(position);
        setRotation(rotation);

        for (int i = 0; i < frustumPlanes.length; i++) {
            frustumPlanes[i] = new FrustumPlane();
        }

        updateViewFrustum();
        callUpdate();
    }

    public Camera(Vector3f position, Quaternionf rotation) {
        super();

        windowManager = WindowManager.getInstance();

        setPosition(position);
        setRotation(rotation);

        for (int i = 0; i < frustumPlanes.length; i++) {
            frustumPlanes[i] = new FrustumPlane();
        }

        updateViewFrustum();
        callUpdate();
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
        worldFrustumTestingAABB.set(object.getAabb()).offset(object.getPosition());

        Vector3f positiveCorner = ObjectPool.VECTOR3F_POOL.obtain().set(0,0,0);

        for (FrustumPlane plane : frustumPlanes) {
            positiveCorner = ObjectPool.VECTOR3F_POOL.obtain().set(
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

    public final Matrix4f getViewProjectionMatrix(){
        return viewProjectionMatrix;
    }

    public final Matrix4f updateViewProjectionMatrix(){
        this.viewProjectionMatrix.set(windowManager.getProjectionMatrix()).mul(getViewMatrix());
        return viewProjectionMatrix;
    }

    public final Matrix4f getViewMatrix(){
        return this.viewMatrix;
    }

    public final Matrix4f updateViewMatrix(){
        Vector3f currentPosition = getPosition();

        this.viewMatrix.identity()
                .rotate(getRotation())
                .translate(-currentPosition.x, -currentPosition.y, -currentPosition.z);

        return this.viewMatrix;
    }

    public static Camera getMainCamera(){
        return SceneManager.currentScene == null ? null : SceneManager.currentScene.getMainCamera();
    }

    public void sortGameObjectsInScene(){
        if(SceneManager.currentScene == null) return; //TODO: Implement hadUpdated() to check to increase performance
        Vector3f currentCameraPosition = ObjectPool.VECTOR3F_POOL.obtain().set(getPosition());
        SceneManager.currentScene.getSortedGameObjects().forEach(go -> go.setRenderCameraSquaredDistance(getPosition()));
        ObjectPool.VECTOR3F_POOL.free(currentCameraPosition);
    }

    @Override
    protected void onUpdateTransform() {
        super.onUpdateTransform();
        updateViewMatrix();
        updateViewProjectionMatrix();
        updateViewFrustum();
    }

    @Override
    public void cleanUp() {
        super.cleanUp();
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
            addComponent(new RenderComponent(mms));

            isShowingProxy = true;
        }

        return this;
    }
}
