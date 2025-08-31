package nl.framegengine.core.engine;

import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.utils.Constants;
import nl.framegengine.editor.EngineSettings;
import org.lwjgl.Version;

public class Launcher {
    private static GameInstance game;

    public static void main(String[] args){
        System.out.println(Version.getVersion());

        WindowManager.createInstance(Constants.TITLE, 1280, 720, true, true);
        game = new GameInstance();
        EngineManager engine = new EngineManager();
        EngineSettings.loadEngineConfig();

        try{
            engine.start(game, true);
        } catch (Exception e) {
            Debug.logError("Error: " + e.getMessage());
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static GameInstance getGame() {
        return game;
    }
}
