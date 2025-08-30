package nl.framegengine.core.rendering.renderers;

import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.entity.GameObject;
import nl.framegengine.core.visual.MeshMaterialSet;

public interface IRenderer{

    public void init() throws Exception;

    public void render();

    abstract void bind(MeshMaterialSet meshMaterialSet);

    public void unbind();

    public void prepare(GameObject entity, Camera camera);

    public void cleanUp();

}
