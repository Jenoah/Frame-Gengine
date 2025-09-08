package nl.framegengine.core.engine;

import nl.framegengine.core.shaders.ShaderManager;
import nl.framegengine.editor.EngineSettings;
import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.entity.Scene;
import nl.framegengine.core.entity.SceneManager;
import nl.framegengine.core.rendering.RenderManager;

import java.io.File;

public class GameInstance implements ILogic{
    private final WindowManager window;

    public GameInstance(){
        window = WindowManager.getInstance();
    }

    @Override
    public void init() throws Exception {
        Debug.log("Initiating game...");
        RenderManager.init();
        EngineSettings.loadSettings();

        Debug.log("Loading window settings");
        window.setClearColor(0, 0, 0, 0);
        window.setWindowIcon("textures/FrameGengine_icon.png");

        Scene level = SceneManager.loadScene(EngineSettings.currentProjectDirectory + File.separator + EngineSettings.currentLevelPath);

        Debug.log("Adding scene to SceneManager");
        SceneManager.addScene(level);
        SceneManager.setCurrentScene(0);

        Debug.log("Updating shaders with scene settings");
        ShaderManager.updateGenericUniforms();
    }

    @Override
    public void input() {
        SceneManager.currentScene.handleInput();
    }

    @Override
    public void update(float interval) {
        SceneManager.currentScene.update();
    }

    @Override
    public void render() {
        RenderManager.render(SceneManager.currentScene);
    }

    @Override
    public void cleanUp() {
        RenderManager.cleanUp();
        SceneManager.cleanUp();
    }
}
