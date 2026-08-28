package nl.framegengine.core.visual;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.type.ImBoolean;
import imgui.type.ImFloat;
import nl.framegengine.core.utils.IJsonSerializable;
import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.shaders.Shader;
import nl.framegengine.core.shaders.ShaderManager;
import nl.framegengine.core.utils.Constants;
import nl.framegengine.core.utils.JsonHelper;
import nl.framegengine.editor.ManifestHelper;
import nl.framegengine.editor.editorComponents.Icons;
import nl.framegengine.editor.editorComponents.InputField;
import nl.framegengine.editor.editorComponents.Panel;
import nl.framegengine.editor.editorComponents.Text;
import nl.framegengine.editor.panels.ICustomEditorPanel;
import nl.framegengine.editor.panels.InfoPanel;
import org.joml.Math;
import org.joml.Vector4f;

import javax.json.*;
import java.io.StringReader;
import java.lang.reflect.Field;

public class Material implements IJsonSerializable, ICustomEditorPanel {

    protected Vector4f ambientColor = new Vector4f(Constants.DEFAULT_COLOR);
    protected Vector4f diffuseColor = new Vector4f(Constants.DEFAULT_COLOR);
    protected Vector4f specularColor = new Vector4f(Constants.DEFAULT_COLOR);

    protected float reflectance = 0.04f;
    protected float roughness = 0.1f;
    protected float metallic = 0.1f;
    protected float tilingScale = 1;
    protected Texture albedoTexture = null;
    protected Texture normalMap = null;
    protected Texture roughnessMap = null;
    protected Texture metallicMap = null;
    protected Texture aoMap = null;

    protected boolean isDoubleSided = false;
    protected boolean castShadow = true;
    protected boolean receiveShadows = true;
    protected boolean isTransparent;
    protected boolean isOnTop = false;

    protected Shader shader;
    protected String guid;

    public Material(){
        this.shader = ShaderManager.pbrShader;
        setGuid();
        MaterialManager.addMaterial(this);
    }

    public Material(Shader shader){
        this.shader = shader;
        setGuid();
        MaterialManager.addMaterial(this);
    }

    public Material(Material material){
        this.shader = material.getShader();
        this.ambientColor = new Vector4f(material.getAmbientColor());
        this.diffuseColor = new Vector4f(material.getDiffuseColor());
        this.specularColor = new Vector4f(material.getSpecularColor());
        this.reflectance = material.getReflectance();
        this.albedoTexture = material.getAlbedoTexture();
        this.normalMap = material.getNormalMap();
        this.isDoubleSided = material.isDoubleSided();
        this.roughnessMap = material.getRoughnessMap();
        this.metallicMap = material.getMetallicMap();
        this.aoMap = material.getAoMap();
        this.roughness = material.getRoughness();
        this.metallic = material.getMetallic();
        this.tilingScale = material.getTilingScale();
        this.isOnTop = material.isOnTop();
        this.castShadow = material.castShadow();
        this.receiveShadows = material.receiveShadows();
        this.isTransparent = material.isTransparent();

        setGuid();
        MaterialManager.addMaterial(this);
    }

    public Material(Shader shader, Texture albedoTexture){
        this(shader);
        this.setAlbedoTexture(albedoTexture);
        setGuid();
        MaterialManager.addMaterial(this);
    }

//      Setters

    public Material setAmbientColor(Vector4f ambientColor) {
        this.ambientColor.set(ambientColor);
        return this;
    }

    public Material setAmbientColor(float r, float g, float b, float a) {
        this.ambientColor.set(r, g, b, a);
        return this;
    }

    public Material setDiffuseColor(Vector4f diffuseColor) {
        this.diffuseColor.set(diffuseColor);
        return this;
    }

    public Material setDiffuseColor(float r, float g, float b, float a) {
        this.diffuseColor.set(r, g, b, a);
        return this;
    }

    public Material setSpecularColor(Vector4f specularColor) {
        this.specularColor.set(specularColor);
        return this;
    }

    public Material setSpecularColor(float r, float g, float b, float a) {
        this.specularColor.set(r, g, b, a);
        return this;
    }

    public Material setReflectance(float reflectance) {
        this.reflectance = reflectance;
        return this;
    }

    public Material setRoughness(float roughness){
        this.roughness = Math.clamp(0.01f, 1f, roughness);
        return this;
    }

    public Material setMetallic(float metallic){
        this.metallic = Math.clamp(0.01f, 1f, metallic);
        return this;
    }

    public Material setTilingScale(float tilingScale){
        this.tilingScale = Math.max(0.001f, tilingScale);
        return this;
    }

    public Material setAlbedoTexture(Texture texture) {
        this.albedoTexture = texture;
        return this;
    }

    public Material setNormalMap(Texture texture) {
        this.normalMap = texture;
        return this;
    }

    public Material setRoughnessMap(Texture texture) {
        this.roughnessMap = texture;
        return this;
    }

    public Material setMetallicMap(Texture texture) {
        this.metallicMap = texture;
        return this;
    }

    public Material setAOMap(Texture texture) {
        this.aoMap = texture;
        return this;
    }

    public Material setShader(Shader shader){
        this.shader = shader;
        return this;
    }

    public Material setDoubleSided(boolean isDoubleSided){
        this.isDoubleSided = isDoubleSided;
        return this;
    }

    public Material castShadow(boolean canCast){
        castShadow = canCast;
        return this;
    }

    public Material receiveShadows(boolean canReceive){
        receiveShadows = canReceive;
        return this;
    }

    public Material setTransparent(boolean isTransparent){
        this.isTransparent = isTransparent;
        return this;
    }

    public Material setOnTop(boolean isOnTop){
        this.isOnTop = isOnTop;
        return this;
    }

    public Material setGuid(){
        if(guid == null || guid.isBlank()) return setGuid(String.valueOf(java.util.UUID.randomUUID()));
        return this;
    }

    public Material setGuid(String guid){
        this.guid = guid;
        return this;
    }

    //    Getters

    public final Vector4f getAmbientColor() {
        return ambientColor;
    }

    public final Vector4f getDiffuseColor() {
        return diffuseColor;
    }

    public final Vector4f getSpecularColor() {
        return specularColor;
    }

    public final float getReflectance() {
        return reflectance;
    }

    public final float getRoughness(){
        return roughness;
    }

    public final float getMetallic(){ return metallic; }

    public final float getTilingScale() { return tilingScale; }

    public final Texture getAlbedoTexture() {
        return albedoTexture;
    }

    public final Texture getNormalMap() {
        return normalMap;
    }

    public final Texture getRoughnessMap() {
        return roughnessMap;
    }

    public final Texture getMetallicMap() {
        return metallicMap;
    }

    public final Texture getAoMap() {
        return aoMap;
    }

    public final Shader getShader(){
        return shader;
    }

    public final boolean isDoubleSided(){
        return isDoubleSided;
    }

    public final boolean castShadow(){ return castShadow; }

    public final boolean receiveShadows(){ return receiveShadows; }

    public final boolean isTransparent() { return isTransparent; }

    public final boolean isOnTop() { return isOnTop; }

    @Override
    public final String getGuid(){ return guid; }

//  Has Getters

    public final boolean hasAlbedoTexture(){
        return albedoTexture != null;
    }

    public final boolean hasNormalMap(){
        return normalMap != null;
    }

    public final boolean hasRoughnessMap(){
        return roughnessMap != null;
    }

    public final boolean hasMetallicMap(){
        return metallicMap != null;
    }

    public final boolean hasAOMap(){
        return aoMap != null;
    }

    @Override
    public JsonObject serializeToJson() {
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        JsonObject jsonObject = JsonHelper.objectToJson(this, new String[]{"shader"});
        jsonObject.forEach(jsonObjectBuilder::add);
        jsonObjectBuilder.remove("class");
        if(shader != null) jsonObjectBuilder.add("shader", shader.getClass().getName());
        return jsonObjectBuilder.build();
    }

    @Override
    public IJsonSerializable deserializeFromJson(String json) {
        JsonReader jsonReader = Json.createReader(new StringReader(json));
        JsonObject jsonInfo = jsonReader.readObject();
        try{
            JsonHelper.loadVariableIntoObject(this, jsonInfo, new String[]{"shader"});
        } catch (Exception e) {
            Debug.logError("Error loading in data: " + e.getMessage());
        }

        if(JsonHelper.hasJsonKey(jsonInfo, "shader")){
            this.shader = ShaderManager.getShaderByQualifiedClassName(jsonInfo.get("shader").toString());
        }

        if(guid == null || guid.isBlank()) setGuid();
        if(shader == null) shader = ShaderManager.getDefaultShader();
        return this;
    }

    @Override
    public void renderPanel() {
        ImGui.text(Icons.HIGHLIGHT + " Material");
        ImGui.text("Shader: " + getShader().getClass().getSimpleName());
        ImGui.spacing();

        float tableWidth = Panel.getPanelWidth() - Panel.getPaddingX() * 2.0f;

        if (ImGui.beginTable("materialSettings##" + getGuid(), 2, ImGuiTableFlags.SizingStretchProp, new ImVec2(tableWidth, 0))) {

            ImGui.tableSetupColumn("Label", ImGuiTableColumnFlags.WidthStretch, 1.0f);
            ImGui.tableSetupColumn("Value", ImGuiTableColumnFlags.WidthStretch, 4.0f);

            ImGui.tableNextRow();
            ImGui.tableNextColumn();
            ImGui.tableNextColumn();

            if (ImGui.beginTable(
                    "materialRGBAHeader##" + getGuid(), 4, ImGuiTableFlags.SizingStretchSame)) {

                ImGui.tableNextColumn();
                Text.ColoredAndCentered("R", 0.85f, 0.25f, 0.25f);

                ImGui.tableNextColumn();
                Text.ColoredAndCentered("G", 0.30f, 0.85f, 0.30f);

                ImGui.tableNextColumn();
                Text.ColoredAndCentered("B", 0.35f, 0.50f, 1.00f);

                ImGui.tableNextColumn();
                Text.ColoredAndCentered("A", 1.00f, 1.00f, 1.00f);

                ImGui.endTable();
            }

            ImGui.tableNextRow();
            ImGui.tableNextColumn();
            ImGui.text("Diffuse");
            ImGui.tableNextColumn();
            ImGui.setNextItemWidth(-1);
            InputField.vector4("##materialDiffuse" + getGuid(), getDiffuseColor());

            ImGui.tableNextRow();
            ImGui.tableNextColumn();
            ImGui.text("Ambient");
            ImGui.tableNextColumn();
            ImGui.setNextItemWidth(-1);
            InputField.vector4("##materialAmbient" + getGuid(), getAmbientColor());

            ImGui.tableNextRow();
            ImGui.tableNextColumn();
            ImGui.text("Specular");
            ImGui.tableNextColumn();
            ImGui.setNextItemWidth(-1);
            InputField.vector4("##materialSpecular" + getGuid(), getSpecularColor());

            ImGui.endTable();
        }

        ImGui.spacing();

        ImFloat reflectance = new ImFloat(getReflectance());
        if (ImGui.inputFloat("reflectance##reflectance" + getGuid(), reflectance)) {
            setReflectance(reflectance.get());
        }

        ImFloat metallic = new ImFloat(getMetallic());
        if (ImGui.inputFloat("metallic##metallic" + getGuid(), metallic)) {
            setMetallic(metallic.get());
        }

        ImFloat roughness = new ImFloat(getRoughness());
        if (ImGui.inputFloat("roughness##roughness" + getGuid(), roughness)) {
            setRoughness(roughness.get());
        }

        ImFloat tiling = new ImFloat(getTilingScale());
        if (ImGui.inputFloat("tiling##tiling" + getGuid(), tiling)) {
            setTilingScale(tiling.get());
        }

        ImGui.spacing();

        if (ImGui.beginTable("materialTextures##" + getGuid(), 2, ImGuiTableFlags.SizingStretchProp, new ImVec2(tableWidth, 0))) {

            ImGui.tableSetupColumn("Label", ImGuiTableColumnFlags.WidthStretch, 1.0f);
            ImGui.tableSetupColumn("Value", ImGuiTableColumnFlags.WidthStretch, 2.0f);

            ImGui.tableNextRow();
            ImGui.tableNextColumn();

            ImGui.text("Albedo");
            ImGui.tableNextColumn();
            try {
                Field albedoField = getClass().getDeclaredField("albedoTexture");
                albedoField.setAccessible(true);
                InfoPanel.drawManifestType(ManifestHelper.manifestFileType.TEXTURE, getAlbedoTexture(), albedoField, this);
            } catch (Exception e) {
                Debug.logError("Error loading texture: " + e);
            }

            ImGui.tableNextRow();
            ImGui.tableNextColumn();

            ImGui.text("Normal");
            ImGui.tableNextColumn();

            try {
                Field normalField = getClass().getDeclaredField("normalMap");
                normalField.setAccessible(true);
                InfoPanel.drawManifestType(ManifestHelper.manifestFileType.TEXTURE, getNormalMap(), normalField, this);
            } catch (Exception e) {
                Debug.logError("Error loading texture: " + e);
            }

            ImGui.tableNextRow();
            ImGui.tableNextColumn();

            ImGui.text("Roughness");
            ImGui.tableNextColumn();

            try {
                Field roughnessField = getClass().getDeclaredField("roughnessMap");
                roughnessField.setAccessible(true);
                InfoPanel.drawManifestType(ManifestHelper.manifestFileType.TEXTURE, getRoughnessMap(), roughnessField, this);
            } catch (Exception e) {
                Debug.logError("Error loading texture: " + e);
            }

            ImGui.tableNextRow();
            ImGui.tableNextColumn();

            ImGui.text("Metallic");
            ImGui.tableNextColumn();

            try {
                Field metallicField = getClass().getDeclaredField("metallicMap");
                metallicField.setAccessible(true);
                InfoPanel.drawManifestType(ManifestHelper.manifestFileType.TEXTURE, getMetallicMap(), metallicField, this);
            } catch (Exception e) {
                Debug.logError("Error loading texture: " + e);
            }

            ImGui.tableNextRow();
            ImGui.tableNextColumn();

            ImGui.text("Ambient Occlusion");
            ImGui.tableNextColumn();

            try {
                Field aoField = getClass().getDeclaredField("aoMap");
                aoField.setAccessible(true);
                InfoPanel.drawManifestType(ManifestHelper.manifestFileType.TEXTURE, getAoMap(), aoField, this);
            } catch (Exception e) {
                Debug.logError("Error loading texture: " + e);
            }

            ImGui.endTable();
        }

        ImGui.spacing();

        ImBoolean doubleSided = new ImBoolean(isDoubleSided());
        if (ImGui.checkbox("doubleSided##doubleSided" + getGuid(), doubleSided)) {
            setDoubleSided(doubleSided.get());
        }

        ImBoolean castShadow = new ImBoolean(castShadow());
        if (ImGui.checkbox("castShadow##castShadow" + getGuid(), castShadow)) {
            castShadow(castShadow.get());
        }

        ImBoolean receiveShadow = new ImBoolean(receiveShadows());
        if (ImGui.checkbox("receiveShadow##receiveShadow" + getGuid(), receiveShadow)) {
            receiveShadows(receiveShadow.get());
        }

        ImBoolean transparent = new ImBoolean(isTransparent());
        if (ImGui.checkbox("transparent##transparent" + getGuid(), transparent)) {
            setTransparent(transparent.get());
        }

        ImGui.spacing();

        InfoPanel.materialRenderer.renderPreview(this);
        ImGui.image(InfoPanel.materialPreviewFBOID, new ImVec2(tableWidth, tableWidth));
    }
}
