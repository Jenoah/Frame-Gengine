package nl.framegengine.customScripts;

import nl.framegengine.core.visual.ModelManager;
import nl.framegengine.core.components.RenderComponent;
import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.entity.GameObject;
import nl.framegengine.core.visual.Material;
import nl.framegengine.core.visual.Mesh;
import nl.framegengine.core.visual.Model;
import nl.framegengine.core.visual.MeshMaterialSet;
import nl.framegengine.core.shaders.ShaderManager;
import nl.framegengine.core.utils.Constants;
import nl.framegengine.core.utils.ObjectPool;
import nl.framegengine.core.utils.Utils;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import nl.framegengine.customScripts.*;
import nl.framegengine.customScripts.utils.*;
import nl.framegengine.customScripts.utils.ChunkUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.joml.Math.lerp;

public class MarchingChunk {

    private static final int MAX_VERTICES = 4096; // Estimate or calculate max needed
    private static final int MAX_TRIANGLES = 8192;

    Vector3f[] vertices = new Vector3f[MAX_VERTICES];
    int[] triangles = new int[MAX_TRIANGLES];
    private int[] edgeVertexIndices;

    private int vertexCount = 0;
    private int triangleCount = 0;

    private final float[] terrainHeights;
    private Vector3f[] normals;

    private GameObject chunkEntity;
    private RenderComponent surfaceFeatureRenderComponent;

    public ChunkCoord chunkPosition;
    public boolean isReady = false;
    public boolean isEmpty = false;

    private final Set<Matrix4f> surfaceFeatureModelMatrices = new HashSet<>();

    public MarchingChunk(ChunkCoord chunkPosition){
        this.chunkPosition = chunkPosition;
        this.terrainHeights = new float[(ChunkUtils.CHUNK_SIZE + 1) * (ChunkUtils.CHUNK_SIZE + 1)];
        init();
    }

    private void init(){
        edgeVertexIndices = new int[(ChunkUtils.CHUNK_SIZE + 1) * (ChunkUtils.CHUNK_SIZE + 1) * (ChunkUtils.CHUNK_SIZE + 1) * 12];
        Arrays.fill(edgeVertexIndices, -1);

        populateChunk();
        if(vertexCount > 0) {
            normals = calculateNormals();
            isReady = true;
        }else{
            isEmpty = true;
            isReady = true;
        }
    }

    //REGION Generation

    private void populateChunk(){
        for (int x = 0; x <= ChunkUtils.CHUNK_SIZE; x++) {
            for (int z = 0; z <= ChunkUtils.CHUNK_SIZE; z++) {
                terrainHeights[x * (ChunkUtils.CHUNK_SIZE + 1) + z] = ChunkUtils.SampleHeight(chunkPosition.x + x, chunkPosition.z + z) + ChunkUtils.terrainSurfaceHeight;
            }
        }

        for (int x = 0; x < ChunkUtils.CHUNK_SIZE; x++) {
            for (int y = 0; y < ChunkUtils.CHUNK_SIZE; y++) {
                for (int z = 0; z < ChunkUtils.CHUNK_SIZE; z++) {
                    marchCube(x, y, z);
                }
            }
        }
    }

    private void marchCube(int x, int y, int z) {
        float[] voxelCorners = new float[8];
        for (int i = 0; i < 8; i++) {
            Vector3f corner = Constants.cornerTable[i];
            float heightSample = terrainHeights[(int) ((x + corner.x) * (ChunkUtils.CHUNK_SIZE + 1) + (z + corner.z))];
            voxelCorners[i] = y + corner.y + chunkPosition.y - heightSample + ChunkUtils.CHUNK_ISO_LEVEL;
        }

        int configIndex = ChunkUtils.GetVoxelConfiguration(voxelCorners);
        if (configIndex == 0 || configIndex == 255) return;

        int edgeIndex = 0;
        for (int t = 0; t < 5; t++) {
            for (int p = 0; p < 3; p++) {
                int edge = Constants.triangleTable[configIndex][edgeIndex];
                if (edge == -1) return;

                // Compute the interpolated position as before
                Vector3f vertex1 = ObjectPool.VECTOR3F_POOL.obtain().set(x, y, z).add(Constants.cornerTable[Constants.edgeIndexes[edge][0]]);
                Vector3f vertex2 = ObjectPool.VECTOR3F_POOL.obtain().set(x, y, z).add(Constants.cornerTable[Constants.edgeIndexes[edge][1]]);

                Vector3f vertexPosition;
                if (ChunkUtils.smoothTerrain) {
                    float difference = getVertexDifference(voxelCorners, edge);
                    vertexPosition = new Vector3f(vertex2).sub(vertex1).mul(difference).add(vertex1);
                } else {
                    vertexPosition = new Vector3f(vertex1).add(vertex2).mul(0.5f);
                }

                int vertIdx = getOrCreateVertex(x, y, z, edge, vertexPosition);
                addTriangle(vertIdx);

                edgeIndex++;
                ObjectPool.VECTOR3F_POOL.free(vertex1);
                ObjectPool.VECTOR3F_POOL.free(vertex2);
            }
        }
    }

    private Vector3f[] calculateNormals() {
        Vector3f[] normals = new Vector3f[vertexCount];

        for(int i = 0; i < vertexCount; i++) {
            Vector3f vertex = vertices[i];
            int x = (int) vertex.x;
            int y = (int) vertex.y;
            int z = (int) vertex.z;

            // Calculate the gradient using central differences
            float dx = (getScalar(x + 1, y, z) - getScalar(x - 1, y, z)) * 0.5f;
            float dy = (getScalar(x, y + 1, z) - getScalar(x, y - 1, z)) * 0.5f;
            float dz = (getScalar(x, y, z + 1) - getScalar(x, y, z - 1)) * 0.5f;

            // Normalize the gradient vector to get the normal
            float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length > 0.0f) {
                dx /= length;
                dy /= length;
                dz /= length;
            }

            normals[i] = new Vector3f(dx, dy, dz);
        }

        return normals;
    }

    //REGION Helpers

    private int getOrCreateVertex(int x, int y, int z, int edge, Vector3f position) {
        int flatIndex = getFlatEdgeVertexIndex(x, y, z, edge);
        if (edgeVertexIndices[flatIndex] != -1) {
            return edgeVertexIndices[flatIndex];
        } else {
            int idx = addVertex(position);
            edgeVertexIndices[flatIndex] = idx;
            return idx;
        }
    }

    private int getFlatEdgeVertexIndex(int x, int y, int z, int edge) {
        int size = ChunkUtils.CHUNK_SIZE + 1;
        return (((x * size + y) * size + z) * 12) + edge;
    }

    private int addVertex(Vector3f v) {
        if (vertexCount >= vertices.length) {
            Debug.logError("Marching chunk vertexCount exceeds vertex array length");
        }

        vertices[vertexCount++] = v;
        return vertexCount - 1;
    }

    private void addTriangle(int idx) {
        if (triangleCount >= triangles.length) {
            Debug.logError("Marching chunk triangleCount exceeds triangles array length");
        }
        triangles[triangleCount++] = idx;
    }

    public void publishChunk(){
        if(vertexCount == 0) return;

        Model chunkModel = ModelManager.loadModel(Arrays.copyOf(vertices, vertexCount), null, Arrays.copyOf(triangles, triangleCount), normals);
        chunkModel.getMesh().generateUVs();
        chunkEntity = new GameObject("Chunk - " + chunkPosition).setPosition(chunkPosition.toVector3());
        chunkModel.getMaterial().setShader(ShaderManager.triplanarShader);//.setDiffuseColor(new Vector4f(0.5f, 0.0f, 0.5f, 1f));//.setReflectance(64);
        chunkEntity.addComponent(new RenderComponent(chunkModel.getMesh(), chunkModel.getMaterial()));
    }

    public float getHeightAt(float x, float z) {
        // Use floor() and ceil() with grid alignment
        int x0 = (int)x;
        int z0 = (int)z;
        int x1 = x0 + 1;
        int z1 = z0 + 1;

        // Clamp using (CHUNK_SIZE + 1) for grid points
        x0 = Math.max(0, Math.min(x0, ChunkUtils.CHUNK_SIZE));
        x1 = Math.max(0, Math.min(x1, ChunkUtils.CHUNK_SIZE));
        z0 = Math.max(0, Math.min(z0, ChunkUtils.CHUNK_SIZE));
        z1 = Math.max(0, Math.min(z1, ChunkUtils.CHUNK_SIZE));

        float sx = x - x0;
        float sz = z - z0;

        // Use (CHUNK_SIZE + 1) for grid point alignment
        float h00 = terrainHeights[x0 * (ChunkUtils.CHUNK_SIZE + 1) + z0];
        float h10 = terrainHeights[x1 * (ChunkUtils.CHUNK_SIZE + 1) + z0];
        float h01 = terrainHeights[x0 * (ChunkUtils.CHUNK_SIZE + 1) + z1];
        float h11 = terrainHeights[x1 * (ChunkUtils.CHUNK_SIZE + 1) + z1];

        return lerp(lerp(h00, h10, sx), lerp(h01, h11, sx), sz);
    }

    private static float getVertexDifference(float[] voxelCorners, int index) {
        float vertex1Sample = voxelCorners[Constants.edgeIndexes[index][0]];
        float vertex2Sample = voxelCorners[Constants.edgeIndexes[index][1]];

        float difference = vertex2Sample - vertex1Sample;
        return difference == 0f ? ChunkUtils.terrainSurfaceHeight : (ChunkUtils.terrainSurfaceHeight - vertex1Sample) / difference;
    }

    public GameObject getChunkEntity(){ return chunkEntity; }

    private float getScalar(int x, int y, int z) {
        float heightSample = ChunkUtils.SampleHeight(chunkPosition.x + x, chunkPosition.z + z) - chunkPosition.y;
        return y - heightSample;
    }

    public Vector3f getNormalAt(float x, float z) {
        x = Math.max(1, Math.min(x, ChunkUtils.CHUNK_SIZE - 1));
        z = Math.max(1, Math.min(z, ChunkUtils.CHUNK_SIZE - 1));

        // Sample 4-connected neighbors
        float right = getHeightAt(x + 1, z);
        float left = getHeightAt(x - 1, z);
        float up = getHeightAt(x, z + 1);
        float down = getHeightAt(x, z - 1);

        // Sobel operator for better quality
        Vector3f tangentX = new Vector3f(
                2.0f,
                (right - left),
                0.0f
        );

        Vector3f tangentZ = new Vector3f(
                0.0f,
                (up - down),
                2.0f
        );

        return tangentZ.cross(tangentX).normalize();
    }

    public void setActive(boolean active){
        if(chunkEntity == null) return;
        chunkEntity.setEnabled(active);
        if(surfaceFeatureRenderComponent != null) surfaceFeatureRenderComponent.isEnabled = active;
    }

    //REGION Surface features

    public void addSurfaceFeatures(GameObject surfaceFeatureEntity) {
        if (surfaceFeatureEntity == null) return;

        if(surfaceFeatureRenderComponent == null) {
            Set<MeshMaterialSet> localMMS = new HashSet<>();
            surfaceFeatureEntity.getComponent(RenderComponent.class).getMeshMaterialSets().forEach(mms -> {
                localMMS.add(new MeshMaterialSet(new Mesh(mms.getMesh()), new Material(mms.material)));
            });
            this.surfaceFeatureRenderComponent = new RenderComponent(localMMS);
            this.surfaceFeatureRenderComponent.setRoot(chunkEntity);
            this.surfaceFeatureRenderComponent.initiate();
        }

        float stepSize = (float) ChunkUtils.CHUNK_SIZE / TerrainGeneration.surfaceFeatureSamples;

        for (int x = 0; x < TerrainGeneration.surfaceFeatureSamples; x++) {
            for (int z = 0; z < TerrainGeneration.surfaceFeatureSamples; z++) {
                float localPositionX = (float) x * stepSize;
                float localPositionZ = (float) z * stepSize;
                float noiseLocationX = chunkPosition.x + localPositionX;
                float noiseLocationZ = chunkPosition.z + localPositionZ;
                float spawnChance = Utils.fastNoise.GetNoise(noiseLocationX, noiseLocationZ) + 1f / 2f;

                if (spawnChance > TerrainGeneration.surfaceFeatureDensity) {
                    float sampleLocationX = noiseLocationX - chunkPosition.x;
                    float sampleLocationZ = noiseLocationZ - chunkPosition.z;
                    float spawnHeight = getHeightAt(sampleLocationX, sampleLocationZ);
                    float rotationY = (spawnChance - TerrainGeneration.surfaceFeatureDensity) * 360f / (1f - TerrainGeneration.surfaceFeatureDensity);

                    Quaternionf normalUp = new Quaternionf().rotationTo(Constants.VECTOR3_UP, getNormalAt(sampleLocationX, sampleLocationZ));
                    normalUp.rotateY(rotationY);

                    Matrix4f transform = new Matrix4f().translation(noiseLocationX, spawnHeight, noiseLocationZ)
                            .rotate(normalUp)
                            .scale(Constants.VECTOR3_ONE);

                    surfaceFeatureModelMatrices.add(transform);
                }
            }
        }

        surfaceFeatureRenderComponent.getMeshMaterialSets().forEach(mms -> {
            mms.mesh.addInstanceOffset(getSurfaceFeatureMatrices());
            mms.mesh.updateInstanceVBO();
        });

    }

    public final Set<Matrix4f> getSurfaceFeatureMatrices(){
        return surfaceFeatureModelMatrices;
    }
}
