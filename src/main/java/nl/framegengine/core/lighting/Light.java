package nl.framegengine.core.lighting;

import nl.framegengine.core.components.RenderComponent;
import nl.framegengine.core.entity.GameObject;
import nl.framegengine.core.visual.Material;
import nl.framegengine.core.visual.Texture;
import nl.framegengine.core.modelLoaders.PrimitiveLoader;
import nl.framegengine.core.shaders.ShaderManager;
import nl.framegengine.core.utils.JsonHelper;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.json.JsonObject;

public class Light extends GameObject {

    protected Vector3f color;
    protected float intensity;
    protected float constant;
    protected float linear;
    protected float exponent;
    private boolean isShowingProxy = false;

    public Light(){}

    public Light(Vector3f color, Vector3f position, float intensity, float constant, float linear, float exponent) {
        this.color = color;
        this.intensity = intensity;
        this.constant = constant;
        this.linear = linear;
        this.exponent = exponent;

        setPosition(position);
    }

    public Light(Vector3f color, Vector3f position, float intensity, float distance) {
        this(color, position, intensity, 1, 0, 0);
        this.color = color;
        this.intensity = intensity;
        setValuesByDistance(distance);
        this.setPosition(position);
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
        this.color = color;
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
        if(!isShowingProxy){
            boolean isPointLight = this instanceof PointLight;
            Material proxyMaterial = new Material(isPointLight ? ShaderManager.billboardShader : ShaderManager.unlitShader);
            String texturePath = this instanceof PointLight ? "textures/light.png": "textures/lightDirection.png";
            proxyMaterial.setAlbedoTexture(new Texture(texturePath, false, !isPointLight)).setDoubleSided(true).setTransparent(true);
            proxyMaterial.setDiffuseColor(new Vector4f(color.x, color.y, color.z, 1f));
            if(getComponent(RenderComponent.class) != null){
                RenderComponent renderComponent = getComponent(RenderComponent.class);
                renderComponent.addMesh(isPointLight ? PrimitiveLoader.getQuad().getMesh() : PrimitiveLoader.getQuadRotated().getMesh(), proxyMaterial);
            }else {
                RenderComponent renderComponent = new RenderComponent(isPointLight ? PrimitiveLoader.getQuad().getMesh() : PrimitiveLoader.getQuadRotated().getMesh(), proxyMaterial);
                this.addComponent(renderComponent);
                renderComponent.initiate();
            }
            isShowingProxy = true;
        }

        return this;
    }

    @Override
    public JsonObject serializeToJson() {
        return JsonHelper.objectToJson(this, new String[]{"isShowingProxy"});
    }
}
