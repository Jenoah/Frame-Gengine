package nl.framegengine.core.lighting;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.type.ImFloat;
import nl.framegengine.core.components.Component;
import nl.framegengine.core.components.visual.RenderComponent;
import nl.framegengine.core.modelLoaders.PrimitiveLoader;
import nl.framegengine.core.shaders.ShaderManager;
import nl.framegengine.core.utils.IJsonSerializable;
import nl.framegengine.core.utils.JsonHelper;
import nl.framegengine.core.utils.ObjectPool;
import nl.framegengine.core.visual.Material;
import nl.framegengine.core.visual.Mesh;
import nl.framegengine.core.visual.MeshMaterialSet;
import nl.framegengine.core.visual.Texture;
import nl.framegengine.editor.EngineSettings;
import nl.framegengine.editor.editorComponents.Panel;
import nl.framegengine.editor.editorComponents.Text;
import nl.framegengine.editor.panels.ICustomEditorPanel;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.json.JsonObject;
import java.util.Set;

public class Light extends Component implements ICustomEditorPanel {

    protected Vector3f color;
    protected float intensity;
    protected float constant;
    protected float linear;
    protected float exponent;
    protected boolean isShowingProxy = false;

    protected Set<String> fieldsToIgnore = Set.of("fieldsToIgnore", "runInEditor", "guid", "root", "hasCleanedUp", "addedDuringPlaymode", "hasInitiated", "isShowingProxy");

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
        return JsonHelper.objectToJson(this, new String[]{"hasInitiated", "isShowingProxy", "fieldsToIgnore"});
    }

    public IJsonSerializable deserializeFromJson(String json) {
        super.deserializeFromJson(json);
        if(root != null && root.getComponent(RenderComponent.class) != null) root.removeComponent(root.getComponent(RenderComponent.class));
        return this;
    }

    @Override
    public void renderPanel() {
        float tableWidth = Panel.getPanelWidth() - Panel.getPaddingX() * 2.0f;
        ImGui.setCursorPosX(ImGui.getCursorPosX() + Panel.getPaddingY());

        if (ImGui.beginTable("lightSettings##" + getGuid(), 2, ImGuiTableFlags.SizingStretchProp, new ImVec2(tableWidth, 0))) {

            ImGui.tableSetupColumn("Label", ImGuiTableColumnFlags.WidthStretch, 1.0f);
            ImGui.tableSetupColumn("Value", ImGuiTableColumnFlags.WidthStretch, 3.0f);

            ImGui.tableNextRow();
            ImGui.tableNextColumn();
            ImGui.tableNextColumn();

            if (ImGui.beginTable(
                    "lightSettingsRGBHeader##" + getGuid(), 3, ImGuiTableFlags.SizingStretchSame)) {

                ImGui.tableNextColumn();
                Text.ColoredAndCentered("R", 0.85f, 0.25f, 0.25f);

                ImGui.tableNextColumn();
                Text.ColoredAndCentered("G", 0.30f, 0.85f, 0.30f);

                ImGui.tableNextColumn();
                Text.ColoredAndCentered("B", 0.35f, 0.50f, 1.00f);

                ImGui.endTable();
            }

            ImGui.tableNextRow();
            ImGui.tableNextColumn();

            ImGui.text("Color");

            ImGui.tableNextColumn();
            Vector3f colorVector = ObjectPool.VECTOR3F_POOL.obtain()
                    .set(getColor());

            float[] lightColor = {
                    colorVector.x,
                    colorVector.y,
                    colorVector.z
            };


            ImGui.setNextItemWidth(-1);
            if (ImGui.inputFloat3("##lightColor", lightColor)) {
                setColor(lightColor[0], lightColor[1], lightColor[2]);
            }

            ImGui.endTable();

            ImGui.setCursorPosX(ImGui.getCursorPosX() + Panel.getPaddingY());
            ImFloat intensity = new ImFloat(getIntensity());
            if (ImGui.inputFloat("intensity##" + getGuid(), intensity)) {
                setIntensity(intensity.get());
            }

            ImGui.setCursorPosX(ImGui.getCursorPosX() + Panel.getPaddingY());
            ImFloat linear = new ImFloat(getIntensity());
            if (ImGui.inputFloat("linear##" + getGuid(), linear)) {
                setLinear(linear.get());
            }

            ImGui.setCursorPosX(ImGui.getCursorPosX() + Panel.getPaddingY());
            ImFloat exponent = new ImFloat(getExponent());
            if (ImGui.inputFloat("exponent##" + getGuid(), exponent)) {
                setExponent(exponent.get());
            }

            ImGui.setCursorPosX(ImGui.getCursorPosX() + Panel.getPaddingY());
            ImFloat constant = new ImFloat(getConstant());
            if (ImGui.inputFloat("constant##" + getGuid(), constant)) {
                setConstant(constant.get());
            }
        }
    }
}
