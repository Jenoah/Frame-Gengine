package nl.framegengine.customScripts;

import nl.framegengine.core.engine.EngineManager;
import nl.framegengine.core.components.Component;
import nl.framegengine.core.components.RenderComponent;
import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.entity.GameObject;
import nl.framegengine.core.entity.SceneManager;
import nl.framegengine.core.modelLoaders.OBJLoader.OBJLoader;
import nl.framegengine.core.visual.TextureLoader;
import nl.framegengine.core.visual.MeshMaterialSet;
import nl.framegengine.core.shaders.ShaderManager;
import nl.framegengine.core.utils.Utils;

import nl.framegengine.customScripts.TerrainGeneration;
import nl.framegengine.customScripts.MarchingChunk;
import nl.framegengine.customScripts.utils.*;

import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class TerrainManager extends Component {
    private int renderDistance = 2;
    private int maxChunksPerFrame = 10;
    private GameObject treeSurfaceFeature;
    private float terrainUpdateTime = 0;

    private TerrainGeneration terrainGeneration;
    private Thread terrainGenerationThread;
    Set<MarchingChunk> marchingQueue = new HashSet<>();
    private GameObject playerCamera = null;

    @Override
    public void initiate(){
        terrainGeneration = new TerrainGeneration(renderDistance);
        terrainGenerationThread = new Thread(terrainGeneration);
        terrainGenerationThread.setDaemon(true);
        terrainGeneration.setSurfaceFeatureDensity(0.7f);
        terrainGeneration.setSurfaceFeatureSamples(8);

        Utils.setNoiseSeed(123);
        playerCamera = Camera.mainCamera;

        Set<MeshMaterialSet> treeMeshMaterialSet = OBJLoader.loadOBJModel("/models/birch.obj");
        treeMeshMaterialSet.forEach((meshMaterialSet -> meshMaterialSet.mesh.generateUVs()));
        treeSurfaceFeature = new GameObject("Tree").setPosition(5, 5f, -2);
        treeSurfaceFeature.setStatic(true);
        treeSurfaceFeature.addComponent(new RenderComponent(treeMeshMaterialSet));

        int grassTextureID = TextureLoader.loadTexture("textures/grass.jpg");
        int rockTextureID = TextureLoader.loadTexture("textures/rock/rock_albedo.jpg");
        ShaderManager.triplanarShader.setTextureIDs(grassTextureID, rockTextureID);
        ShaderManager.triplanarShader.setBlendFactor(32);

        terrainGenerationThread.start();
        terrainGeneration.setUpdatePosition(playerCamera.getPosition());
    }

    @Override
    public void update() {
        terrainUpdateTime += EngineManager.getDeltaTime();
        if(terrainUpdateTime < 0.2f) return;
        terrainUpdateTime = 0;

        updateTerrain();
    }

    private void updateTerrain() {
        terrainGeneration.setUpdatePosition(playerCamera.getPosition());
        if (marchingQueue.isEmpty()) {
            marchingQueue.addAll(terrainGeneration.getMarchingChunksQueue());
        }

        int processed = 0;

        Iterator<MarchingChunk> it = marchingQueue.iterator();
        while (it.hasNext() && processed < maxChunksPerFrame) {
            MarchingChunk chunk = it.next();
            if (!chunk.isReady) {
                terrainGeneration.requeueChunk(chunk);
                it.remove(); // Remove from marchingQueue
                continue;
            }

            chunk.publishChunk();
            if (!ChunkCoord.compareToVector(chunk.chunkPosition, new Vector3f(0, 0, -10)) &&
                    !ChunkCoord.compareToVector(chunk.chunkPosition, new Vector3f(-1, 0, -10))) {
                chunk.addSurfaceFeatures(treeSurfaceFeature);
            }

            GameObject chunkEntity = chunk.getChunkEntity();
            if (chunkEntity != null) {
                chunkEntity.getComponent(RenderComponent.class)
                        .getMeshMaterialSets().iterator().next()
                        .material.setShader(ShaderManager.triplanarShader)
                        .setReflectance(64);
                SceneManager.currentScene.addEntity(chunk.getChunkEntity());
            }
            processed++;
            it.remove(); // Remove from marchingQueue
            if(!it.hasNext()) terrainGeneration.restockQueue();
        }
    }

    @Override
    public void cleanUp() {
        terrainGeneration.end();
        super.cleanUp();
    }
}