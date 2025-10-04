package nl.framegengine.core.rendering.renderers;

import nl.framegengine.core.visual.MeshMaterialSet;

public interface IRenderer{

    public void init() throws Exception;

    public void render();

    abstract void bind(MeshMaterialSet meshMaterialSet);

    public void unbind();


    public void cleanUp();

}
