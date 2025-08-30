package nl.framegengine.core.entity;

import nl.framegengine.editor.EngineSettings;
import nl.framegengine.core.components.Component;
import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.lighting.DirectionalLight;
import nl.framegengine.core.lighting.Light;
import nl.framegengine.core.lighting.PointLight;
import nl.framegengine.core.lighting.SpotLight;
import nl.framegengine.core.components.ComponentLoader;
import nl.framegengine.core.utils.JsonHelper;
import org.joml.Vector3f;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import java.io.*;

import java.net.URL;
import java.util.*;

public class SceneManager {
    private List<Scene> scenes = new ArrayList<>();
    public static Scene currentScene = null;
    public static Vector3f fogColor = new Vector3f(1);
    public static float fogDensity = 0.01f;
    public static float fogGradient = 15f;

    private static SceneManager instance = null;
    public static ComponentLoader componentLoader;

    public static synchronized SceneManager getInstance()
    {
        if (instance == null) {
            instance = new SceneManager();
        }

        return instance;
    }

    public Scene loadScene(String filePath) throws Exception {
        if(componentLoader == null){
            URL inputResourceUrl = new File(EngineSettings.currentProjectDirectory).toURI().toURL();
            URL compiledResourceUrl = new File(EngineSettings.currentProjectDirectory + File.separator + "/.compiled").toURI().toURL();

            componentLoader = new ComponentLoader(inputResourceUrl.toURI().getPath(), compiledResourceUrl.toURI().getPath());
        }

        Scene scene = new Scene();

        InputStream is = null;
        if(EngineSettings.isCompiled) {
            is = getClass().getResourceAsStream(filePath);
        }else{
            is = new FileInputStream(filePath);
        }
        JsonReader reader = Json.createReader(is);
        JsonObject sceneInfo = reader.readObject();
        scene.deserializeFromJson(sceneInfo.toString());

        scene.getGameObjects().forEach(go -> {
            for (Component component : go.components.stream().toList()) {
                component.initiate();
            }
            if(go instanceof Light light) tryAddLight(light, scene);
            go.callUpdate();
        });
        scene.updateLights();
        scene.setFogColor(scene.getFogColor());
        scene.setFogDensity(scene.getFogDensity());
        scene.setFogGradient(scene.getFogGradient());

        return scene;
    }

    private void tryAddLight(Light lightObject, Scene scene){
        switch (lightObject) {
            case DirectionalLight directionalLight -> {
                scene.setDirectionalLight(directionalLight);
                directionalLight.showProxy();
            }
            case SpotLight spotLight -> {
                spotLight.showProxy();
                scene.addSpotLight(spotLight);
            }
            case PointLight pointLight -> {
                pointLight.showProxy();
                scene.addPointLight(pointLight);
            }
            default -> throw new IllegalStateException("Unexpected value: " + lightObject);
        }
    }

    public List<Scene> getScenes() {
        return scenes;
    }

    public void setScenes(List<Scene> scenes) {
        this.scenes = scenes;
    }

    public void addScene(Scene scene){
        this.scenes.add(scene);
    }

    public Scene getCurrentScene() {
        return currentScene;
    }

    public void setCurrentScene(int sceneIndex) {
        currentScene = scenes.get(sceneIndex);
        fogColor = currentScene.getFogColor();
        fogDensity = currentScene.getFogDensity();
        fogGradient = currentScene.getFogGradient();
        Debug.log("Loading " + currentScene.getLevelName());
    }

    public void cleanUp(){
        currentScene.cleanUp();
        instance = null;
    }

    public static String sceneToJson(Scene scene){
        JsonObjectBuilder sceneInfo = Json.createObjectBuilder();
        sceneInfo.add("levelName", scene.getLevelName());
        sceneInfo.add("fogGradient", scene.getFogGradient());
        sceneInfo.add("fogDensity", scene.getFogDensity());
        sceneInfo.add("fogColor", JsonHelper.vector3ToJsonObject(scene.getFogColor()));

        JsonArrayBuilder sceneGoInfo = Json.createArrayBuilder();
        scene.getGameObjects().forEach(go -> {
            sceneGoInfo.add(go.serializeToJson());
        });
        sceneInfo.add("gameObjects", sceneGoInfo);

        Map<String, Boolean> config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory jsonWriterFactory = Json.createWriterFactory(config);

        StringWriter stringWriter = new StringWriter();
        JsonWriter jsonWriter = jsonWriterFactory.createWriter(stringWriter);
        jsonWriter.write(sceneInfo.build());

        return stringWriter.toString();
    }
}
