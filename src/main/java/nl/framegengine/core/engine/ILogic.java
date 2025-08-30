package nl.framegengine.core.engine;

import nl.framegengine.core.input.MouseInput;
import nl.framegengine.core.rendering.RenderManager;

public interface ILogic {
    void init() throws Exception;
    void input();
    void update(float interval, MouseInput mouseInput);
    void render();
    void cleanUp();
    RenderManager getRenderer();
}
