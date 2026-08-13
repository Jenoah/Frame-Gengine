package nl.framegengine.core.lighting;

import nl.framegengine.core.components.visual.RenderComponent;
import nl.framegengine.core.modelLoaders.PrimitiveLoader;
import nl.framegengine.core.shaders.ShaderManager;
import nl.framegengine.core.utils.Constants;
import nl.framegengine.core.utils.ObjectPool;
import nl.framegengine.core.visual.Material;
import nl.framegengine.core.visual.Mesh;
import nl.framegengine.core.visual.MeshMaterialSet;
import nl.framegengine.core.visual.Texture;
import nl.framegengine.editor.EngineSettings;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class DirectionalLight extends Light{

    private final Vector3f lookAtDirection = new Vector3f(0);

    public DirectionalLight(){ super(); }

    @Override
    public void initiate() {
        super.initiate();
        root.lookAtDirection(lookAtDirection);
    }

    public DirectionalLight(Vector3f color, Vector3f direction, float intensity) {
        super(color, intensity, 0);
        lookAtDirection.set(direction);

    }

    public DirectionalLight(Vector3f color, Quaternionf direction, float intensity) {
        super(color, intensity, 0);

        direction.normalize();
        direction.getEulerAnglesXYZ(lookAtDirection);
        if(lookAtDirection.lengthSquared() <= 0.01) lookAtDirection.set(0, 0, -1);
    }

    @Override
    public Light showProxy(){
        if(!isShowingProxy && !EngineSettings.isInGame){
            Material proxyMaterial = new Material(ShaderManager.unlitShader);
            proxyMaterial.castShadow(false).receiveShadows(false);
            String texturePath = "textures/directionalLight.png";
            proxyMaterial.setAlbedoTexture(new Texture(texturePath, false, true)).setDoubleSided(true).setTransparent(true);
            proxyMaterial.setDiffuseColor(new Vector4f(color.x, color.y, color.z, 1f));
            Mesh proxyMesh = PrimitiveLoader.getQuadRotatedMesh();

            MeshMaterialSet mms = new MeshMaterialSet(proxyMesh, proxyMaterial);
            root.addComponent(new RenderComponent(mms));

            isShowingProxy = true;
        }

        return this;
    }
}
