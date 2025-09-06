package nl.framegengine.core.engine;

public interface ILogic {
    void init() throws Exception;
    void input();
    void update(float interval);
    void render();
    void cleanUp();
}
