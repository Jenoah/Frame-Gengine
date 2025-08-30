package nl.framegengine.core.entity;

import nl.framegengine.core.IJsonSerializable;
import nl.framegengine.core.ModelManager;
import nl.framegengine.core.MouseInput;
import nl.framegengine.core.WindowManager;
import nl.framegengine.core.components.Component;
import nl.framegengine.core.fonts.fontMeshCreator.FontType;
import nl.framegengine.core.fonts.fontMeshCreator.GUIText;
import nl.framegengine.core.fonts.fontMeshCreator.TextMeshData;
import nl.framegengine.core.gui.GuiObject;
import nl.framegengine.core.lighting.DirectionalLight;
import nl.framegengine.core.lighting.PointLight;
import nl.framegengine.core.lighting.SpotLight;
import nl.framegengine.core.shaders.ShaderManager;
import nl.framegengine.core.shaders.SimpleLitShader;
import nl.framegengine.core.utils.JsonHelper;
import org.joml.Vector3f;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class Scene implements IJsonSerializable {
    private final List<GameObject> gameObjects;
    private final List<GameObject> rootGameObjects;
    private final List<GuiObject> guiObjects;
    private final Map<FontType, List<GUIText>> textObjects;
    private Vector3f fogColor = new Vector3f(1);
    private float fogDensity = 0.01f;
    private float fogGradient = 15f;

    //Lighting
    private Vector3f ambientLight;
    private PointLight[] pointLights = new PointLight[0];
    private SpotLight[] spotLights = new SpotLight[0];
    private DirectionalLight directionalLight;

    protected final WindowManager windowManager;

    protected String levelName = "Undefined Scene";

    public Scene() {
        this.gameObjects = new ArrayList<>();
        this.rootGameObjects = new ArrayList<>();
        this.guiObjects = new ArrayList<>();
        this.windowManager = WindowManager.getInstance();
        this.textObjects = new HashMap<>();
        init();
    }

    public void init() { }

    public void postStart() { }

    public void update(MouseInput mouseInput) {
        for (GameObject rootGameObject : rootGameObjects.stream().toList()) {
            rootGameObject.OnUpdateTransform();
        }

        for (GameObject gameObject : gameObjects.stream().toList()) {
            gameObject.update(mouseInput);
        }
    }

    public void handleInput() { }

    public void cleanUp() {
        ModelManager.cleanUp();
        getGameObjects().forEach(GameObject::cleanUp);
        gameObjects.clear();
        rootGameObjects.clear();
        guiObjects.clear();
        rootGameObjects.clear();
    }

    public void addEntity(GameObject entity, boolean intitiateComponents){
        if (entity == null) {
            return;
        }

        if (entity.getChildren() != null) {
            for (GameObject child : entity.getChildren()) {
                addGameObject(child);
            }
        }

        addGameObject(entity);
        if(intitiateComponents && !entity.getComponents().isEmpty()) entity.getComponents().forEach(Component::initiate);
    }

    public void addEntity(GameObject entity) {
        if (entity == null) {
            return;
        }

        if (entity.getChildren() != null) {
            for (GameObject child : entity.getChildren()) {
                addGameObject(child);
            }
        }

        addGameObject(entity);
        if (!entity.getComponents().isEmpty()) entity.getComponents().forEach(Component::initiate);
    }


    public void addGameObject(GameObject gameObject) {
        if (gameObjects.contains(gameObject)) return;

        gameObjects.add(gameObject);
        if(gameObject.getParent() == null) this.rootGameObjects.add(gameObject);

        if (gameObject.getChildren() != null) {
            for (GameObject child : gameObject.getChildren()) {
                addGameObject(child);
            }
        }
    }

    public void removeFromRoot(GameObject gameObject){
        this.rootGameObjects.remove(gameObject);
    }

    public void removeGameObject(GameObject gameObject){
        this.rootGameObjects.remove(gameObject);
        gameObjects.remove(gameObject);
        gameObject.remove();
    }

    public void addGUI(GuiObject guiObject) {
        if (!guiObjects.contains(guiObject)) {
            guiObjects.add(guiObject);

            if (guiObject.getChildren() != null) {
                for (GameObject child : guiObject.getChildren()) {
                    if (child instanceof GuiObject) {
                        addGUI((GuiObject) child);
                    }
                }
            }
        }
    }

    public void updateLights(){
        for (SimpleLitShader s : Arrays.asList(ShaderManager.litShader, ShaderManager.triplanarShader, ShaderManager.pbrShader))
            s.setLights(getDirectionalLight(), getPointLights(), getSpotLights());

        ShaderManager.updateGenericUniforms();
    }

    public List<GuiObject> getGuiObjects() {
        return guiObjects;
    }

    public Vector3f getAmbientLight() {
        return ambientLight;
    }

    public void setAmbientLight(Vector3f ambientLight) {
        this.ambientLight = ambientLight;
        ShaderManager.updateGenericUniforms();
    }

    public void setAmbientLight(float r, float g, float b) {
        this.ambientLight = new Vector3f(r, g, b);
        ShaderManager.updateGenericUniforms();
    }

    public PointLight[] getPointLights() {
        return pointLights;
    }

    public void setPointLights(PointLight[] pointLights) {
        this.pointLights = pointLights;
    }

    public void addPointLight(PointLight pointLight) {
        List<PointLight> pointLightList = new ArrayList<>(Arrays.stream(pointLights).toList());
        pointLightList.add(pointLight);

        this.pointLights = pointLightList.toArray(pointLights);
    }

    public SpotLight[] getSpotLights() {
        return spotLights;
    }

    public void setSpotLights(SpotLight[] spotLights) {
        this.spotLights = spotLights;
    }

    public void addSpotLight(SpotLight spotLight) {
        List<SpotLight> spotLightList = new ArrayList<>(Arrays.stream(spotLights).toList());
        spotLightList.add(spotLight);

        this.spotLights = spotLightList.toArray(spotLights);
    }

    public DirectionalLight getDirectionalLight() {
        return directionalLight;
    }

    public void setDirectionalLight(DirectionalLight directionalLight) {
        this.directionalLight = directionalLight;
    }

    public String getLevelName() {
        return levelName;
    }

    public void addText(GUIText textObject) {
        FontType font = textObject.getFont();
        TextMeshData data = font.loadText(textObject);
        int id = ModelManager.loadModelID(data.getVertexPositions(), data.getTextureCoords(), 2);
        textObject.setMeshInfo(id, data.getVertexCount());
        List<GUIText> textBatch = textObjects.computeIfAbsent(font, k -> new ArrayList<GUIText>());
        textBatch.add(textObject);
    }

    public void updateText(GUIText textObject){
        ModelManager.unloadModel(textObject.getMesh());
        FontType font = textObject.getFont();
        TextMeshData data = font.loadText(textObject);
        int id = ModelManager.loadModelID(data.getVertexPositions(), data.getTextureCoords(), 2);
        textObject.setMeshInfo(id, data.getVertexCount());

        if(!textObjects.get(textObject.getFont()).contains(textObject)){
            List<GUIText> textBatch = textObjects.computeIfAbsent(font, k -> new ArrayList<GUIText>());
            textBatch.add(textObject);
        }
    }

    public void removeText(GUIText textObject) {
        List<GUIText> textBatch = textObjects.get(textObject.getFont());
        textBatch.remove(textObject);
        if (textBatch.isEmpty()) {
            textObjects.remove(textObject.getFont());
            ModelManager.unloadModel(textObject.getMesh());
        }
    }

    public Map<FontType, List<GUIText>> getTextObjects() {
        return textObjects;
    }

    public Vector3f getFogColor() {
        return fogColor;
    }

    public void setFogColor(Vector3f fogColor) {
        this.fogColor = fogColor;
        ShaderManager.updateGenericUniforms();
    }

    public float getFogDensity() {
        return fogDensity;
    }

    public float getFogGradient() {
        return fogGradient;
    }

    public void setFogDensity(float fogDensity) {
        this.fogDensity = fogDensity;
        ShaderManager.updateGenericUniforms();
    }

    public void setFogGradient(float fogGradient) {
        this.fogGradient = fogGradient;
        ShaderManager.updateGenericUniforms();
    }

    public void disableFog(){
        this.fogGradient = 100000;
        this.fogDensity = 0;
        ShaderManager.updateGenericUniforms();
    }

    public List<GameObject> getGameObjects() {
        return gameObjects;
    }

    public List<GameObject> getRootGameObjects() {
        return gameObjects;
    }

    public void setLevelName(String levelName) {
        this.levelName = levelName;
    }

    private void processGameObjects(){
        rootGameObjects.clear();

        gameObjects.forEach(go -> {
            if(go.getParent() == null) rootGameObjects.add(go);
        });
    }

    @Override
    public JsonObject serializeToJson() {
        return null;
    }

    @Override
    public IJsonSerializable deserializeFromJson(String json) {
        JsonReader jsonReader = Json.createReader(new StringReader(json));
        JsonObject jsonInfo = jsonReader.readObject();
        try {
            JsonHelper.loadVariableIntoObject(this, jsonInfo);
        } catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        processGameObjects();
        return this;
    }
}
