package nl.framegengine.core.lighting;

import nl.framegengine.core.components.visual.RenderComponent;
import nl.framegengine.core.modelLoaders.PrimitiveLoader;
import nl.framegengine.core.shaders.ShaderManager;
import nl.framegengine.core.visual.Material;
import nl.framegengine.core.visual.Mesh;
import nl.framegengine.core.visual.MeshMaterialSet;
import nl.framegengine.core.visual.Texture;
import nl.framegengine.editor.EngineSettings;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class PointLight extends Light {

    public PointLight() { super(); }

    public PointLight(Vector3f color, Vector3f position, float intensity, float constant, float linear, float exponent) {
        super(color, position, intensity, constant, linear, exponent);
    }

    public PointLight(Vector3f color, Vector3f position, float intensity, float distance) {
        super(color, position, intensity, distance);
    }

    @Override
    public Light showProxy(){
        if(!isShowingProxy && !EngineSettings.isInGame){
            Material proxyMaterial = new Material(ShaderManager.billboardShader);
            proxyMaterial.castShadow(false).receiveShadows(false);
            String texturePath = "textures/pointLight.png";
            proxyMaterial.setAlbedoTexture(new Texture(texturePath, false, false)).setTransparent(true);
            proxyMaterial.setDiffuseColor(new Vector4f(color.x, color.y, color.z, 1f));
            Mesh proxyMesh = PrimitiveLoader.getQuadMesh();

            MeshMaterialSet mms = new MeshMaterialSet(proxyMesh, proxyMaterial);
            addComponent(new RenderComponent(mms));

            isShowingProxy = true;
        }

        return this;
    }
}
