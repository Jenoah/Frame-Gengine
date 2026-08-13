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

public class SpotLight extends Light {

    private float cutOff;
    private float outerCutOff;

    public SpotLight(){ super(); }

    public SpotLight(Vector3f color, Vector3f position, float intensity, float constant, float linear, float exponent, float cutOff, float outerCutOff) {
        super(color, intensity, constant, linear, exponent);

        this.cutOff = cutOff;
        this.outerCutOff = outerCutOff;
    }

    public SpotLight(Vector3f color, float intensity, float distance, float cutOff, float outerCutOff) {
        super(color, intensity, distance);
        this.cutOff = cutOff;
        this.outerCutOff = outerCutOff;
    }

    public float getCutOff() {
        return this.cutOff;
    }

    public void setCutOff(float cutOff) {
        this.cutOff = cutOff;
    }

    public float getOuterCutOff() {
        return this.outerCutOff;
    }

    public void setOuterCutOff(float outerCutOff) {
        this.outerCutOff = outerCutOff;
    }

    @Override
    public Light showProxy(){
        if(!isShowingProxy && !EngineSettings.isInGame){
            Material proxyMaterial = new Material(ShaderManager.unlitShader);
            proxyMaterial.castShadow(false).receiveShadows(false);
            String texturePath = "textures/spotLight.png";
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
