package nl.framegengine.core.engine;

import nl.framegengine.core.rendering.RenderManager;

public interface ILogic {
    void init() throws Exception;
    void input();
    void update(float interval);
    void render();
    void cleanUp();
    RenderManager getRenderer();
}
