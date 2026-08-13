package nl.framegengine.core.lighting;

import nl.framegengine.core.components.Component;
import nl.framegengine.core.components.visual.RenderComponent;
import nl.framegengine.core.entity.GameObject;
import nl.framegengine.core.modelLoaders.PrimitiveLoader;
import nl.framegengine.core.shaders.ShaderManager;
import nl.framegengine.core.utils.IJsonSerializable;
import nl.framegengine.core.utils.JsonHelper;
import nl.framegengine.core.visual.Material;
import nl.framegengine.core.visual.Mesh;
import nl.framegengine.core.visual.MeshMaterialSet;
import nl.framegengine.core.visual.Texture;
import nl.framegengine.editor.EngineSettings;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.json.JsonObject;
import java.util.Set;

public class Light extends Component {

    protected Vector3f color;
    protected float intensity;
    protected float constant;
    protected float linear;
    protected float exponent;
    protected boolean isShowingProxy = false;
    public static final Set<String> fieldsToIgnore = Set.of("intensity", "color", "constant", "linear", "exponent", "isShowingProxy");


    public Light(){}

    public Light(Vector3f color, float intensity, float constant, float linear, float exponent) {
        this.color = color;
        this.intensity = intensity;
        this.constant = constant;
        this.linear = linear;
        this.exponent = exponent;
    }

    public Light(Vector3f color, float intensity, float distance) {
        this(color, intensity, 1, 0, 0);
        this.color = color;
        this.intensity = intensity;
        setValuesByDistance(distance);
    }

    public void setValuesByDistance(float distance){
        float distancePlusOne = distance + 1;

        this.constant = 1.0f; // Always constant
        this.linear = 1f / distancePlusOne;
        this.exponent = 1f / (distancePlusOne * distancePlusOne);
    }

    public Vector3f getColor() {
        return color;
    }

    public void setColor(Vector3f color) {
        this.color.set(color);
    }

    public void setColor(float r, float g, float b) {
        this.color.set(r, g, b);
    }

    public float getIntensity() {
        return intensity;
    }

    public void setIntensity(float intensity) {
        this.intensity = intensity;
    }

    public float getConstant() {
        return constant;
    }

    public void setConstant(float constant) {
        this.constant = constant;
    }

    public float getLinear() {
        return linear;
    }

    public void setLinear(float linear) {
        this.linear = linear;
    }

    public float getExponent() {
        return exponent;
    }

    public void setExponent(float exponent) {
        this.exponent = exponent;
    }

    public Light showProxy(){
        if(!isShowingProxy && !EngineSettings.isInGame){
            boolean isPointLight = this instanceof PointLight;
            Material proxyMaterial = new Material(isPointLight ? ShaderManager.billboardShader : ShaderManager.unlitShader);
            proxyMaterial.castShadow(false).receiveShadows(false);
            String texturePath = this instanceof PointLight ? "textures/light.png": "textures/lightDirection.png";
            proxyMaterial.setAlbedoTexture(new Texture(texturePath, false, !isPointLight)).setDoubleSided(true).setTransparent(true);
            proxyMaterial.setDiffuseColor(new Vector4f(color.x, color.y, color.z, 1f));
            Mesh proxyMesh = isPointLight ? PrimitiveLoader.getQuadMesh() : PrimitiveLoader.getQuadRotatedMesh();

            MeshMaterialSet mms = new MeshMaterialSet(proxyMesh, proxyMaterial);
            root.addComponent(new RenderComponent(mms));

            isShowingProxy = true;
        }

        return this;
    }

    @Override
    public JsonObject serializeToJson() {
        return JsonHelper.objectToJson(this, new String[]{"isShowingProxy"});
    }

    public IJsonSerializable deserializeFromJson(String json) {
        super.deserializeFromJson(json);
        if(root != null && root.getComponent(RenderComponent.class) != null) root.removeComponent(root.getComponent(RenderComponent.class));
        return this;
    }
}
