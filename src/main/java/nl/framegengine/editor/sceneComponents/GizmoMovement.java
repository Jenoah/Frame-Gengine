package nl.framegengine.editor.sceneComponents;

import nl.framegengine.core.components.Component;
import nl.framegengine.core.components.constraint.MoveOnAxis;
import nl.framegengine.core.components.visual.RenderComponent;
import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.entity.GameObject;
import nl.framegengine.core.input.MouseInput;
import nl.framegengine.core.modelLoaders.OBJLoader.OBJLoader;
import nl.framegengine.core.physics.Raycast;
import nl.framegengine.core.rendering.RenderManager;
import nl.framegengine.core.rendering.renderers.DebugRenderer;
import nl.framegengine.core.shaders.ShaderManager;
import nl.framegengine.core.utils.Calculus;
import nl.framegengine.core.utils.Constants;
import nl.framegengine.core.utils.ObjectPool;
import nl.framegengine.core.visual.Material;
import nl.framegengine.core.visual.Mesh;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import static nl.framegengine.core.physics.Raycast.fromCameraByMouse;

public class GizmoMovement extends Component {

    private final MoveOnAxis moveOnAxis;

    private final GameObject xAxis, yAxis, zAxis;
    private DebugRenderer.DebugMesh xAxisLine, yAxisLine, zAxisLine = null;
    private Camera camera;
    private boolean isDragging = false;

    public GizmoMovement(MoveOnAxis moveOnAxis){
        this.moveOnAxis = moveOnAxis;
        runInEditor = true;
        Mesh coneMesh = OBJLoader.loadOBJModel("/models/cone.obj").stream().findFirst().get().getMesh();

        this.xAxis = new GameObject("x-axis");
        this.yAxis = new GameObject("y-axis");
        this.zAxis = new GameObject("z-axis");

        this.xAxis.setPosition(Calculus.multiplyVector(Constants.VECTOR3_RIGHT, 0.8f));
        this.yAxis.setPosition(Calculus.multiplyVector(Constants.VECTOR3_UP, 0.8f));
        this.zAxis.setPosition(Calculus.multiplyVector(Constants.VECTOR3_FORWARD, 0.8f));

        this.xAxis.setRotation(new Quaternionf().fromAxisAngleRad(Constants.VECTOR3_BACK, Math.toRadians(270f)));
        this.yAxis.setRotation(Constants.QUATERNION_LEFT);
        this.zAxis.setRotation(Constants.QUATERNION_DOWN);

        this.xAxis.setScale(0.2f);
        this.yAxis.setScale(0.2f);
        this.zAxis.setScale(0.2f);

        Material axisMaterial = new Material(ShaderManager.unlitShader).setDiffuseColor(new Vector4f(Constants.COLOR_RED, 1f)).
                setOnTop(true).castShadow(false).setDoubleSided(true);
        RenderComponent xAxisConeRenderComponent = new RenderComponent(new Mesh(coneMesh), axisMaterial);
        RenderComponent yAxisConeRenderComponent = new RenderComponent(new Mesh(coneMesh), new Material(axisMaterial).setDiffuseColor(new Vector4f(Constants.COLOR_GREEN, 1)));
        RenderComponent zAxisConeRenderComponent = new RenderComponent(new Mesh(coneMesh), new Material(axisMaterial).setDiffuseColor(new Vector4f(Constants.COLOR_BLUE, 1)));

        xAxis.addComponent(xAxisConeRenderComponent);
        yAxis.addComponent(yAxisConeRenderComponent);
        zAxis.addComponent(zAxisConeRenderComponent);

        xAxis.initiate();
        yAxis.initiate();
        zAxis.initiate();
    }

    @Override
    public void initiate() {
        super.initiate();
        this.xAxis.setParent(root);
        this.yAxis.setParent(root);
        this.zAxis.setParent(root);

        updateAxisLines();
    }

    @Override
    public void enable() {
        super.enable();
        updateAxisLines();
    }

    private void updateAxisLines(){
        if(xAxisLine == null) xAxisLine = RenderManager.debugLine(Constants.VECTOR3_ZERO, Constants.VECTOR3_RIGHT, Constants.COLOR_RED, true);
        if(yAxisLine == null) yAxisLine = RenderManager.debugLine(Constants.VECTOR3_ZERO, Constants.VECTOR3_UP, Constants.COLOR_GREEN, true);
        if(zAxisLine == null) zAxisLine = RenderManager.debugLine(Constants.VECTOR3_ZERO, Constants.VECTOR3_FORWARD, Constants.COLOR_BLUE, true);

        Vector3f rootPosition = ObjectPool.VECTOR3F_POOL.obtain().set(root.getPosition());

        xAxisLine.worldPosition.set(rootPosition);
        yAxisLine.worldPosition.set(rootPosition);
        zAxisLine.worldPosition.set(rootPosition);

        ObjectPool.VECTOR3F_POOL.free(rootPosition);
    }

    @Override
    public void disable() {
        super.disable();
        if(xAxisLine != null){
            xAxisLine.persistent = false;
            xAxisLine = null;
        }
        if(yAxisLine != null){
            yAxisLine.persistent = false;
            yAxisLine = null;
        }
        if(zAxisLine != null){
            zAxisLine.persistent = false;
            zAxisLine = null;
        }
    }

    @Override
    public void update() {
        super.update();
        if(camera == null) camera = RenderManager.getRenderCamera();

        if(MouseInput.isLbReleased()) isDragging = false;
        if(!MouseInput.isLbDown()) return;

        Raycast.Ray mouseRay = fromCameraByMouse(camera);

        if(MouseInput.isLbClicked()) {
            if (Raycast.intersectRay(mouseRay, xAxis)) {
                isDragging = true;
                moveOnAxis.setConstraintAxis(Constants.VECTOR3_RIGHT);
                moveOnAxis.setOffset(Calculus.subtractVectors(root.getPosition(), Raycast.closestPointOnLine(root.getPosition(), Constants.VECTOR3_RIGHT, mouseRay)));
                updateAxisLines();
            } else if (Raycast.intersectRay(mouseRay, yAxis)) {
                isDragging = true;
                moveOnAxis.setConstraintAxis(Constants.VECTOR3_UP);
                moveOnAxis.setOffset(Calculus.subtractVectors(root.getPosition(), Raycast.closestPointOnLine(root.getPosition(), Constants.VECTOR3_UP, mouseRay)));
                updateAxisLines();
            } else if (Raycast.intersectRay(mouseRay, zAxis)) {
                isDragging = true;
                moveOnAxis.setConstraintAxis(Constants.VECTOR3_FORWARD);
                moveOnAxis.setOffset(Calculus.subtractVectors(root.getPosition(), Raycast.closestPointOnLine(root.getPosition(), Constants.VECTOR3_FORWARD, mouseRay)));
                updateAxisLines();
            }
        }

        if(isDragging) move(mouseRay);
    }

    private void move(Raycast.Ray mouseRay){
        moveOnAxis.move(mouseRay.origin, mouseRay.direction);
        updateAxisLines();
    }

    public final boolean isCurrentlyMoving(){
        return isDragging;
    }
}
