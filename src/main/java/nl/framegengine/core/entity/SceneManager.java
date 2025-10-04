package nl.framegengine.core.entity;

import nl.framegengine.core.components.Component;
import nl.framegengine.core.components.ComponentLoader;
import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.lighting.DirectionalLight;
import nl.framegengine.core.lighting.Light;
import nl.framegengine.core.lighting.PointLight;
import nl.framegengine.core.lighting.SpotLight;
import nl.framegengine.editor.EngineSettings;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SceneManager {
    private static List<Scene> scenes = new ArrayList<>();
    public static Scene currentScene = null;

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

        return scene;
    }

    private static void initScene(Scene scene){
        Debug.log("Loading in scene game objects");
        scene.getGameObjects().forEach(go -> {
            if(go instanceof Light light) tryAddLight(light, scene);
            for (Component component : go.components.stream().toList()) {
                component.initiate();
            }
            go.callUpdate();
        });
        scene.sortedGameObjects.addAll(scene.gameObjects);
        scene.syncSortedGameObjects();
        Debug.log("Loading scene settings");
        scene.updateLights();
        scene.setFogColor(scene.getFogColor());
        scene.setFogDensity(scene.getFogDensity());
        scene.setFogGradient(scene.getFogGradient());
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
        setCurrentScene(scenes.get(sceneIndex));
    }

    public static void setCurrentScene(Scene scene){
        setCurrentScene(scene, true);
    }

    public static void setCurrentScene(Scene scene, boolean cleanup){
        if(currentScene != null && cleanup){
            Debug.log("Cleaning scene " + currentScene.getLevelName());
            currentScene.cleanUp();
        }
        currentScene = scene;
        Debug.log("Loading in " + currentScene.getLevelName());
        initScene(scene);
        Debug.log("Starting scene");
        currentScene.postStart();
        Debug.log("Updating scene lights");
        currentScene.updateLights();
        Debug.log("Scene loaded in");
    }

    public static void cleanUp(){
        currentScene.cleanUp();
    }
}
