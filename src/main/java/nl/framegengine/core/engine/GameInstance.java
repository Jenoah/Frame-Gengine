package nl.framegengine.core.engine;

import nl.framegengine.core.input.MouseInput;
import nl.framegengine.core.shaders.ShaderManager;
import nl.framegengine.editor.EngineSettings;
import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.entity.Scene;
import nl.framegengine.core.entity.SceneManager;
import nl.framegengine.core.rendering.RenderManager;

import java.io.File;

public class GameInstance implements ILogic{
    private final RenderManager renderer;
    private final WindowManager window;

    public GameInstance(){
        RenderManager.createInstance();
        renderer = RenderManager.getInstance();
        window = WindowManager.getInstance();
        SceneManager.getInstance();
    }

    @Override
    public void init() throws Exception {
        Debug.log("Initiating game...");
        renderer.init();
        EngineSettings.loadSettings();

        Scene level = SceneManager.loadScene(EngineSettings.currentProjectDirectory + File.separator + EngineSettings.currentLevelPath);

        window.setClearColor(0, 0, 0, 0);
        window.setWindowIcon("textures/FrameGengine_icon.png");

        SceneManager.addScene(level);
        SceneManager.setCurrentScene(0);

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
        renderer.render(SceneManager.currentScene);
    }

    @Override
    public void cleanUp() {
        renderer.cleanUp();
        SceneManager.cleanUp();
    }

    public RenderManager getRenderer(){
        return renderer;
    }
}
