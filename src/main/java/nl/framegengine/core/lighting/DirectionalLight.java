package nl.framegengine.core.lighting;

import nl.framegengine.core.components.visual.RenderComponent;
import nl.framegengine.core.modelLoaders.PrimitiveLoader;
import nl.framegengine.core.shaders.ShaderManager;
import nl.framegengine.core.visual.Material;
import nl.framegengine.core.visual.Mesh;
import nl.framegengine.core.visual.MeshMaterialSet;
import nl.framegengine.core.visual.Texture;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class DirectionalLight extends Light{

    public DirectionalLight(){ super(); }

    public DirectionalLight(Vector3f color, Vector3f direction, float intensity) {
        super(color, new Vector3f(0), intensity, 0);
        lookAtDirection(direction);
    }

    public DirectionalLight(Vector3f color, Quaternionf direction, float intensity) {
        super(color, new Vector3f(0), intensity, 0);
        lookAtDirection(direction);
    }

    @Override
    public Light showProxy(){
        if(!isShowingProxy){
            Material proxyMaterial = new Material(ShaderManager.unlitShader);
            proxyMaterial.castShadow(false).receiveShadows(false);
            String texturePath = "textures/directionalLight.png";
            proxyMaterial.setAlbedoTexture(new Texture(texturePath, false, true)).setDoubleSided(true).setTransparent(true);
            proxyMaterial.setDiffuseColor(new Vector4f(color.x, color.y, color.z, 1f));
            Mesh proxyMesh = PrimitiveLoader.getQuadRotatedMesh();

            MeshMaterialSet mms = new MeshMaterialSet(proxyMesh, proxyMaterial);
            addComponent(new RenderComponent(mms));

            isShowingProxy = true;
        }

        return this;
    }
}
