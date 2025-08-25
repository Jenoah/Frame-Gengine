package nl.framegengine.core;

import nl.framegengine.editor.EngineSettings;
import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.entity.Scene;
import nl.framegengine.core.entity.SceneManager;
import nl.framegengine.core.rendering.RenderManager;

import java.io.File;

public class GameInstance implements ILogic{
    private final RenderManager renderer;
    private final WindowManager window;
    private final SceneManager sceneManager;

    public GameInstance(){
        RenderManager.createInstance();
        renderer = RenderManager.getInstance();
        window = WindowManager.getInstance();
        sceneManager = SceneManager.getInstance();
    }

    @Override
    public void init() throws Exception {
        Debug.Log("Initiating game...");
        renderer.init();
        EngineSettings.loadSettings();

        Scene level = sceneManager.loadScene(EngineSettings.currentProjectDirectory + File.separator + EngineSettings.currentLevelPath);

        window.setClearColor(0, 0, 0, 0);
        window.setWindowIcon("textures/FrameGengine_icon.png");

        sceneManager.addScene(level);
        sceneManager.setCurrentScene(0);
    }

    @Override
    public void input() {
        sceneManager.getCurrentScene().handleInput();
    }

    @Override
    public void update(float interval, MouseInput mouseInput) {
        sceneManager.getCurrentScene().update(mouseInput);
    }

    @Override
    public void render() {
        renderer.render(sceneManager.getCurrentScene());
    }

    @Override
    public void cleanUp() {
        renderer.cleanUp();
        sceneManager.cleanUp();
    }

    public RenderManager getRenderer(){
        return renderer;
    }
}
