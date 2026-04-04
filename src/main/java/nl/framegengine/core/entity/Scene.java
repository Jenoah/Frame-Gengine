package nl.framegengine.core.entity;

import nl.framegengine.core.engine.WindowManager;
import nl.framegengine.core.fonts.fontMeshCreator.FontType;
import nl.framegengine.core.fonts.fontMeshCreator.GUIText;
import nl.framegengine.core.fonts.fontMeshCreator.TextMeshData;
import nl.framegengine.core.gui.GuiObject;
import nl.framegengine.core.lighting.DirectionalLight;
import nl.framegengine.core.lighting.PointLight;
import nl.framegengine.core.lighting.SpotLight;
import nl.framegengine.core.rendering.RenderManager;
import nl.framegengine.core.shaders.ShaderManager;
import nl.framegengine.core.shaders.SimpleLitShader;
import nl.framegengine.core.utils.FileHelper;
import nl.framegengine.core.utils.IJsonSerializable;
import nl.framegengine.core.utils.JsonHelper;
import nl.framegengine.core.visual.ModelManager;
import nl.framegengine.editor.EngineSettings;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class Scene implements IJsonSerializable {
    protected final List<GameObject> gameObjects;
    protected final List<GameObject> sortedGameObjects;
    protected final List<GameObject> rootGameObjects;
    protected final List<GuiObject> guiObjects;
    protected final Map<FontType, List<GUIText>> textObjects;
    protected final Set<Integer> vaoIds = new HashSet<>();
    protected Vector3f fogColor = new Vector3f(1);
    protected float fogDensity = 0.01f;
    protected float fogGradient = 15f;
    protected Camera mainCamera = null;

    //Editor camera
    protected Vector3f editorCameraPosition = new Vector3f(0, 1, 0);
    protected Quaternionf editorCameraRotation = new Quaternionf();

    //Lighting
    protected Vector3f ambientLight;
    protected PointLight[] pointLights = new PointLight[0];
    protected SpotLight[] spotLights = new SpotLight[0];
    protected DirectionalLight directionalLight;

    protected final WindowManager windowManager;

    protected String levelName = "Undefined Scene";

    public Scene() {
        this.gameObjects = new ArrayList<>();
        this.sortedGameObjects = new ArrayList<>();
        this.rootGameObjects = new ArrayList<>();
        this.guiObjects = new ArrayList<>();
        this.windowManager = WindowManager.getInstance();
        this.textObjects = new HashMap<>();
        init();
    }

    public Scene(WindowManager windowManager) {
        this.gameObjects = new ArrayList<>();
        this.sortedGameObjects = new ArrayList<>();
        this.rootGameObjects = new ArrayList<>();
        this.guiObjects = new ArrayList<>();
        this.windowManager = windowManager;
        this.textObjects = new HashMap<>();
        init();
    }

    public void init() { }

    public void postStart() {
        RenderManager.setRenderCamera(mainCamera);
        mainCamera.callUpdate();
        mainCamera.onUpdateTransform();
    }

    public void update() {
        updateRootGameObjectTransforms();

        RenderManager.getRenderCamera().sortGameObjectsInScene();
        syncSortedGameObjects();

        for (GameObject gameObject : gameObjects.stream().toList()) {
            gameObject.update();
        }

        updateRootGameObjectTransforms();
    }

    public void updateRootGameObjectTransforms(){
        for (GameObject rootGameObject : rootGameObjects.stream().toList()) {
            rootGameObject.onUpdateTransform();
        }
    }

    public void handleInput() { }

    public void cleanComponents(){ getGameObjects().forEach(GameObject::cleanUp); }

    public void cleanUp() {
        ModelManager.cleanUp();
        cleanComponents();
        gameObjects.clear();
        sortedGameObjects.clear();
        rootGameObjects.clear();
        guiObjects.clear();
    }

    public void addEntity(GameObject entity, boolean intitiateComponents){
        if (entity == null) { return; }

        if (entity.getChildren() != null) {
            for (GameObject child : entity.getChildren()) {
                addEntity(child);
            }
        }

        addGameObject(entity);
        if(intitiateComponents && !entity.getComponents().isEmpty()){
            entity.getComponents().forEach(component -> {
                if(component.getRoot() == null) component.setRoot(entity);
                component.initiate();
            });
        }
    }

    public void addEntity(GameObject entity) {
        addEntity(entity, true);
    }

    public void addGameObject(GameObject gameObject) {
        if (gameObjects.contains(gameObject)) return;

        gameObjects.add(gameObject);
        sortedGameObjects.add(gameObject);
        if(gameObject.getParent() == null) this.rootGameObjects.add(gameObject);

        if (gameObject.getChildren() != null) {
            for (GameObject child : gameObject.getChildren()) {
                addGameObject(child);
            }
        }
    }

    public void syncSortedGameObjects(){
        sortedGameObjects.sort(Comparator.comparingDouble(GameObject::getRenderCameraSquaredDistance));
    }

    public List<GameObject> getSortedGameObjects() {
        return sortedGameObjects;
    }

    public void removeFromRoot(GameObject gameObject){
        this.rootGameObjects.remove(gameObject);
    }

    public void removeGameObject(GameObject gameObject){
        this.rootGameObjects.remove(gameObject);
        gameObjects.remove(gameObject);
        sortedGameObjects.remove(gameObject);
        gameObject.remove();
    }

    public GameObject removeGameObject(String name){
        for (GameObject gameObject : gameObjects) {
            if(gameObject.getName().equals(name)){
                removeGameObject(gameObject);
                return gameObject;
            }
        }

        return null;
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

    public void setMainCamera(Camera camera){ mainCamera = camera; }

    public Camera getMainCamera(){ return mainCamera; }

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

    public GameObject getGameObjectByName(String name){
        for (GameObject gameObject : gameObjects) {
            if(gameObject.getName().equals(name)) return gameObject;
        }

        return null;
    }

    public List<GameObject> getRootGameObjects() {
        return rootGameObjects;
    }

    public void setLevelName(String levelName) {
        this.levelName = levelName;
    }

    public void processGameObjects(){
        rootGameObjects.clear();

        gameObjects.forEach(go -> {
            if(go.getParent() == null) rootGameObjects.add(go);
            if(mainCamera == null && go instanceof Camera camera) setMainCamera(camera);
        });
    }

    public void saveScene(){
        removeGameObject(EngineSettings.editorCameraName);

        Map<String, Boolean> config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory jsonWriterFactory = Json.createWriterFactory(config);

        StringWriter stringWriter = new StringWriter();
        JsonWriter jsonWriter = jsonWriterFactory.createWriter(stringWriter);
        jsonWriter.write(this.serializeToJson());

        FileHelper.writeToFile(stringWriter.toString(), EngineSettings.currentProjectDirectory + File.separator + EngineSettings.currentLevelPath);
    }

    public void addVaoId(int vaoId){
        vaoIds.add(vaoId);
    }

    public boolean hasVaoId(int vaoId){
        return vaoIds.contains(vaoId);
    }

    public void removeVaoId(int vaoId){
        vaoIds.remove(vaoId);
    }

    public final Vector3f getEditorCameraPosition() {
        return editorCameraPosition;
    }

    public void setEditorCameraPosition(Vector3f editorCameraPosition) {
        this.editorCameraPosition = editorCameraPosition;
    }

    public final Quaternionf getEditorCameraRotation() {
        return editorCameraRotation;
    }

    public void setEditorCameraRotation(Quaternionf editorCameraRotation) {
        this.editorCameraRotation = editorCameraRotation;
    }

    @Override
    public String getGuid() {
        return "NoGuid";
    }

    @Override
    public IJsonSerializable setGuid(String guid) {
        return null;
    }

    @Override
    public JsonObject serializeToJson() {
        JsonObjectBuilder sceneInfo = Json.createObjectBuilder();
        sceneInfo.add("levelName", this.getLevelName());
        sceneInfo.add("fogGradient", this.getFogGradient());
        sceneInfo.add("fogDensity", this.getFogDensity());
        sceneInfo.add("fogColor", JsonHelper.vector3ToJsonObject(this.getFogColor()));
        sceneInfo.add("editorCameraPosition", JsonHelper.vector3ToJsonObject(this.editorCameraPosition));
        sceneInfo.add("editorCameraRotation", JsonHelper.quaternionToJsonObject(this.editorCameraRotation));

        JsonArrayBuilder sceneGoInfo = Json.createArrayBuilder();
        this.getGameObjects().forEach(go -> {
            if(go.canBeSaved()) sceneGoInfo.add(go.serializeToJson());
        });
        sceneInfo.add("gameObjects", sceneGoInfo);

        return sceneInfo.build();
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
