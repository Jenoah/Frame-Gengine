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
    private static List<Scene> scenes = new ArrayList<>();
    public static Scene currentScene = null;
    public static Vector3f fogColor = new Vector3f(1);
    public static float fogDensity = 0.01f;
    public static float fogGradient = 15f;

    public static ComponentLoader componentLoader;

    public static Scene loadScene(String filePath) throws Exception {
        Debug.log("Loading in scene at " + filePath);
        if(componentLoader == null){
            Debug.log("Loading in components");
            URL inputResourceUrl = new File(EngineSettings.currentProjectDirectory).toURI().toURL();
            URL compiledResourceUrl = new File(EngineSettings.currentProjectDirectory + File.separator + "/.compiled").toURI().toURL();

            componentLoader = new ComponentLoader(inputResourceUrl.toURI().getPath(), compiledResourceUrl.toURI().getPath());
            Debug.log("Components loaded");
        }

        Scene scene = new Scene();

        InputStream is = null;
        if(EngineSettings.isCompiled) {
            is = SceneManager.class.getResourceAsStream(filePath);
        }else{
            is = new FileInputStream(filePath);
        }
        JsonReader reader = Json.createReader(is);
        JsonObject sceneInfo = reader.readObject();
        scene.deserializeFromJson(sceneInfo.toString());

        Debug.log("Loading in scene game objects");
        scene.getGameObjects().forEach(go -> {
            for (Component component : go.components.stream().toList()) {
                component.initiate();
            }
            if(go instanceof Light light) tryAddLight(light, scene);
            go.callUpdate();
        });
        Debug.log("Loading scene settings");
        scene.updateLights();
        scene.setFogColor(scene.getFogColor());
        scene.setFogDensity(scene.getFogDensity());
        scene.setFogGradient(scene.getFogGradient());

        return scene;
    }

    private static void tryAddLight(Light lightObject, Scene scene){
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

    public static List<Scene> getScenes() {
        return scenes;
    }

    public static void setScenes(List<Scene> scenes) {
        SceneManager.scenes = scenes;
    }

    public static void addScene(Scene scene){
        if(!scenes.contains(scene)) scenes.add(scene);
    }

    public static void setCurrentScene(int sceneIndex) {
        currentScene = scenes.get(sceneIndex);
        fogColor = currentScene.getFogColor();
        fogDensity = currentScene.getFogDensity();
        fogGradient = currentScene.getFogGradient();
        Debug.log("Loading " + currentScene.getLevelName());
    }

    public static void cleanUp(){
        currentScene.cleanUp();
    }

    //TODO: Move underlying method to Scenes SerializeToJson function
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
