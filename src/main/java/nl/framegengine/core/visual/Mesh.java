package nl.framegengine.core.visual;

import nl.framegengine.core.entity.SceneManager;
import nl.framegengine.core.utils.IJsonSerializable;
import nl.framegengine.core.utils.Constants;
import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.utils.Conversion;
import nl.framegengine.core.utils.JsonHelper;
import nl.framegengine.core.utils.Utils;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

public class Mesh implements IJsonSerializable {
    private float[] vertices;
    private float[] normals;
    private float[] tangents;
    private float[] bitangents;
    private float[] uvs;
    private int[] triangles;
    private int dimension = 3;
    private String meshPath = "";

    private final Set<Integer> vbos = new HashSet<>();

    private int vaoID = -1;
    private int vertexVBOID = -1, normalVBOID = -1, tangentsVBOID = -1, bitangentsVBOID = -1, triangleVBOID = -1, uvVBOID = -1, instanceVBOID = -1;

    private int vertexCount = -1;

    private boolean isVisible = true;
    private boolean isStatic = true;
    protected float uvScale = 1f;

    //Instancing
    private boolean isInstanced = false;
    private Set<Matrix4f> instanceOffsets = new HashSet<>();
    private int previousInstanceCount = 0;
    private FloatBuffer instanceBuffer = null;

    public Mesh(){ }

    public Mesh(Mesh mesh){
        this.vertices = mesh.vertices;
        this.uvs = mesh.uvs;
        this.normals = mesh.normals;
        this.triangles = mesh.triangles;
        this.meshPath = mesh.meshPath;

        load(this.vertices, this.uvs, this.triangles, this.normals);
    }

    public Mesh(Vector3f[] vertices){
        this.vertices = Conversion.toFloatArray(vertices);

        load(this.vertices, null, null, null);
    }
    public Mesh(Vector2f[] vertices){
        Vector3f[] vertices3D = new Vector3f[vertices.length];
        for (int i = 0; i < vertices.length; i++) {
            vertices3D[i] = new Vector3f(vertices[i].x, vertices[i].y, 0);
        }
        this.vertices = Conversion.toFloatArray(vertices3D);

        load(this.vertices, null, null, null);
    }
    public Mesh(Vector3f[] vertices, int[] triangles){
        this.triangles = triangles;
        this.vertices = Conversion.toFloatArray(vertices);

        load(this.vertices, null, this.triangles, null);
    }
    public Mesh(Vector3f[] vertices, Vector2f[] uvs, int[] triangles){
        this.uvs = Conversion.toFloatArray(uvs);
        this.triangles = triangles;
        this.vertices = Conversion.toFloatArray(vertices);

        load(this.vertices, this.uvs, this.triangles, null);
    }
    public Mesh(Vector3f[] vertices, Vector3f[] normals, int[] triangles){
        this.normals = Conversion.toFloatArray(normals);
        this.triangles = triangles;
        this.vertices = Conversion.toFloatArray(vertices);

        load(this.vertices, this.normals, this.triangles, null);
    }
    public Mesh(Vector3f[] vertices, Vector2f[] uvs, int[] triangles, Vector3f[] normals){
        this.vertices = Conversion.toFloatArray(vertices);
        this.uvs = uvs != null ? Conversion.toFloatArray(uvs) : null;
        this.normals = normals != null ? Conversion.toFloatArray(normals) : null;
        this.triangles = triangles;

        load(this.vertices, this.uvs, this.triangles, this.normals);
    }

    public Mesh(float[] vertices, float[] uvs, int dimensions){
        this.dimension = dimensions;
        this.vertices = vertices;
        this.uvs = uvs;

        load(this.vertices, this.uvs, null, null);
    }
    public Mesh(float[] vertices, float[] uvs, int[] triangles, float[] normals){
        this.triangles = triangles;
        this.vertices = vertices;
        this.normals = normals;
        this.uvs = uvs;

        load(this.vertices, this.uvs, this.triangles, this.normals);
    }

    private void load(float[] vertexFloatArray, float[] uvFloatArray, int[] triangleArray, float[] normals){
        vaoID = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoID);

        if(triangleArray != null) {
            triangleVBOID = storeIndicesBuffer(triangleArray);
            if(this.triangles == null){
                this.triangles = triangleArray;
            }
        }
        if(vertexFloatArray != null && vertexFloatArray.length > 0){
            vertexVBOID = storeDataInAttributeList(0, this.dimension, vertexFloatArray);
            vertexCount = vertexFloatArray.length;
            this.vertices = vertexFloatArray;
        }

        if(uvFloatArray != null && uvFloatArray.length > 0){
            uvVBOID = storeDataInAttributeList(1, 2, uvFloatArray);
            this.uvs = uvFloatArray;
        }

        if(normals != null && normals.length > 0) {
            normalVBOID = storeDataInAttributeList(2, 3, normals);
            this.normals = normals;
        }

        if(uvs != null && uvs.length > 0 && triangles != null && vertices != null){
            calculateTangents();
        }

        ModelManager.addMesh(this);
        unbind();
    }

    private int storeIndicesBuffer(int[] triangles){
        if (triangles == null || triangles.length == 0) {
            throw new IllegalArgumentException("Index array is empty or null!");
        }

        int vbo = GL15.glGenBuffers();
        vbos.add(vbo);
        GL30.glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, vbo);
        IntBuffer buffer = MemoryUtil.memAllocInt(triangles.length);
        buffer.put(triangles);
        buffer.flip();
        GL30.glBufferData(GL30.GL_ELEMENT_ARRAY_BUFFER, buffer, GL30.GL_STATIC_DRAW);

        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) {
            Debug.logConsoleError("OpenGL error (storeIndicesBuffer) (data length: " + triangles.length + "): " + error);
        }

        MemoryUtil.memFree(buffer);

        return vbo;
    }

    private int storeDataInAttributeList(int attributeNumber, int vertexCount, float[] data){
        int vbo = GL15.glGenBuffers();

        vbos.add(vbo);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        FloatBuffer buffer = MemoryUtil.memAllocFloat(data.length);
        buffer.put(data);
        buffer.flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(attributeNumber, vertexCount, GL11.GL_FLOAT, false, 0, 0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) {
            Debug.logConsoleError("OpenGL error (storeDataInAttributeList) (data length: " + attributeNumber + "/" + data.length + "): " + error);
        }

        MemoryUtil.memFree(buffer);
        return vbo;
    }

    public void unbind(){
        GL30.glBindVertexArray(0);
    }

//    Recalculate

    public Mesh calculateNormals(){
        int triangleCount = triangles.length / 3;
        normals = new float[vertices.length];
        Arrays.fill(normals, 0f);

        for (int i = 0; i < triangleCount; i++) {
            int ia = triangles[i * 3];
            int ib = triangles[i * 3 + 1];
            int ic = triangles[i * 3 + 2];

            float ax = vertices[ia * 3];
            float ay = vertices[ia * 3 + 1];
            float az = vertices[ia * 3 + 2];

            float bx = vertices[ib * 3];
            float by = vertices[ib * 3 + 1];
            float bz = vertices[ib * 3 + 2];

            float cx = vertices[ic * 3];
            float cy = vertices[ic * 3 + 1];
            float cz = vertices[ic * 3 + 2];

            // Edge vectors
            float edge1x = bx - ax, edge1y = by - ay, edge1z = bz - az;
            float edge2x = cx - ax, edge2y = cy - ay, edge2z = cz - az;

// Cross product (edge1 x edge2)
            float nx = edge1y * edge2z - edge1z * edge2y;
            float ny = edge1z * edge2x - edge1x * edge2z;
            float nz = edge1x * edge2y - edge1y * edge2x;

            // Accumulate normals for each vertex
            normals[ia * 3]     += nx;
            normals[ia * 3 + 1] += ny;
            normals[ia * 3 + 2] += nz;

            normals[ib * 3]     += nx;
            normals[ib * 3 + 1] += ny;
            normals[ib * 3 + 2] += nz;

            normals[ic * 3]     += nx;
            normals[ic * 3 + 1] += ny;
            normals[ic * 3 + 2] += nz;
        }

        int vertexCount = vertices.length / 3;
        for (int i = 0; i < vertexCount; i++) {
            float nx = normals[i * 3];
            float ny = normals[i * 3 + 1];
            float nz = normals[i * 3 + 2];
            float length = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (length > 0f) {
                normals[i * 3]     = nx / length;
                normals[i * 3 + 1] = ny / length;
                normals[i * 3 + 2] = nz / length;
            }
        }

        return this;
    }

    public Mesh calculateTangents() {
        // Initialize tangent and bitangent arrays
        float[] tangents = new float[vertices.length];
        float[] bitangents = new float[vertices.length];

        int triangleCount = triangles.length / 3;

        for (int i = 0; i < triangleCount; i++) {
            int i0 = triangles[i * 3];
            int i1 = triangles[i * 3 + 1];
            int i2 = triangles[i * 3 + 2];

            float x0 = vertices[i0 * 3], y0 = vertices[i0 * 3 + 1], z0 = vertices[i0 * 3 + 2];
            float x1 = vertices[i1 * 3], y1 = vertices[i1 * 3 + 1], z1 = vertices[i1 * 3 + 2];
            float x2 = vertices[i2 * 3], y2 = vertices[i2 * 3 + 1], z2 = vertices[i2 * 3 + 2];

            float u0 = uvs[i0 * 2], v0 = uvs[i0 * 2 + 1];
            float u1 = uvs[i1 * 2], v1 = uvs[i1 * 2 + 1];
            float u2 = uvs[i2 * 2], v2 = uvs[i2 * 2 + 1];

            float edge1x = x1 - x0, edge1y = y1 - y0, edge1z = z1 - z0;
            float edge2x = x2 - x0, edge2y = y2 - y0, edge2z = z2 - z0;

            float deltaUV1x = u1 - u0, deltaUV1y = v1 - v0;
            float deltaUV2x = u2 - u0, deltaUV2y = v2 - v0;

            float r = deltaUV1x * deltaUV2y - deltaUV1y * deltaUV2x;
            if (r == 0.0f) r = 1.0f; // Prevent division by zero
            float invR = 1.0f / r;

            float tx = (edge1x * deltaUV2y - edge2x * deltaUV1y) * invR;
            float ty = (edge1y * deltaUV2y - edge2y * deltaUV1y) * invR;
            float tz = (edge1z * deltaUV2y - edge2z * deltaUV1y) * invR;

            float bx = (edge1x * deltaUV2x - edge2x * deltaUV1x) * invR;
            float by = (edge1y * deltaUV2x - edge2y * deltaUV1x) * invR;
            float bz = (edge1z * deltaUV2x - edge2z * deltaUV1x) * invR;

            // Accumulate per vertex
            tangents[i0 * 3]     += tx; tangents[i0 * 3 + 1] += ty; tangents[i0 * 3 + 2] += tz;
            tangents[i1 * 3]     += tx; tangents[i1 * 3 + 1] += ty; tangents[i1 * 3 + 2] += tz;
            tangents[i2 * 3]     += tx; tangents[i2 * 3 + 1] += ty; tangents[i2 * 3 + 2] += tz;

            bitangents[i0 * 3]   += bx; bitangents[i0 * 3 + 1] += by; bitangents[i0 * 3 + 2] += bz;
            bitangents[i1 * 3]   += bx; bitangents[i1 * 3 + 1] += by; bitangents[i1 * 3 + 2] += bz;
            bitangents[i2 * 3]   += bx; bitangents[i2 * 3 + 1] += by; bitangents[i2 * 3 + 2] += bz;
        }

        // Normalize tangents and bitangents
        int vertexCount = vertices.length / 3;
        for (int i = 0; i < vertexCount; i++) {
            // Tangent
            float tx = tangents[i * 3], ty = tangents[i * 3 + 1], tz = tangents[i * 3 + 2];
            float tLen = (float)Math.sqrt(tx * tx + ty * ty + tz * tz);
            if (tLen > 0.0f) {
                tangents[i * 3]     = tx / tLen;
                tangents[i * 3 + 1] = ty / tLen;
                tangents[i * 3 + 2] = tz / tLen;
            }

            // Bitangent
            float bx = bitangents[i * 3], by = bitangents[i * 3 + 1], bz = bitangents[i * 3 + 2];
            float bLen = (float)Math.sqrt(bx * bx + by * by + bz * bz);
            if (bLen > 0.0f) {
                bitangents[i * 3]     = bx / bLen;
                bitangents[i * 3 + 1] = by / bLen;
                bitangents[i * 3 + 2] = bz / bLen;
            }
        }

        // Store or return as needed
        this.tangents = tangents;
        this.bitangents = bitangents;

// Tangents
        if (tangentsVBOID == -1) {
            GL30.glBindVertexArray(vaoID);
            tangentsVBOID = storeDataInAttributeList(3, 3, this.tangents);
            unbind();
        } else {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tangentsVBOID);
            FloatBuffer buffer = Utils.storeDataInFloatBuffer(this.tangents);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
            GL20.glVertexAttribPointer(3, 3, GL11.GL_FLOAT, false, 0, 0); // location 3, size 3
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        }

// Bitangents
        if (bitangentsVBOID == -1) {
            GL30.glBindVertexArray(vaoID);
            bitangentsVBOID = storeDataInAttributeList(4, 3, this.bitangents); // location 4
            unbind();
        } else {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bitangentsVBOID);
            FloatBuffer buffer = Utils.storeDataInFloatBuffer(this.bitangents);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
            GL20.glVertexAttribPointer(4, 3, GL11.GL_FLOAT, false, 0, 0); // location 4, size 3
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        }

        return this;
    }

    public void generateUVs() {
        int vertexCount = vertices.length / 3;
        uvs = new float[vertexCount * 2];

        for (int i = 0; i < triangles.length; i += 3) {
            int ni = triangles[i];
            Vector3f norm = new Vector3f(
                    normals[ni * 3],
                    normals[ni * 3 + 1],
                    normals[ni * 3 + 2]
            );

            float dotX = Math.abs(norm.dot(Constants.VECTOR3_RIGHT));
            float dotY = Math.abs(norm.dot(Constants.VECTOR3_UP));
            float dotZ = Math.abs(norm.dot(Constants.VECTOR3_BACK));

            for (int j = 0; j < 3; j++) {
                int triangleIndex = triangles[i + j];
                if (triangleIndex < 0 || triangleIndex >= vertexCount) continue; // Safety check

                if (dotX > dotY && dotX > dotZ) {
                    uvs[triangleIndex * 2]     = vertices[triangleIndex * 3 + 2];
                    uvs[triangleIndex * 2 + 1] = vertices[triangleIndex * 3 + 1];
                } else if (dotY > dotX && dotY > dotZ) {
                    uvs[triangleIndex * 2]     = vertices[triangleIndex * 3];
                    uvs[triangleIndex * 2 + 1] = vertices[triangleIndex * 3 + 2];
                } else {
                    uvs[triangleIndex * 2]     = vertices[triangleIndex * 3];
                    uvs[triangleIndex * 2 + 1] = vertices[triangleIndex * 3 + 1];
                }
            }
        }

        if (uvVBOID == -1) {
            GL30.glBindVertexArray(vaoID);
            uvVBOID = storeDataInAttributeList(1, 2, uvs);
            unbind();
        } else {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, uvVBOID);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(uvs.length);
                buffer.put(uvs);
                buffer.flip();
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
            }
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 0, 0); // 2 components per UV
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        }
    }

//    Setters

    public void setUvs(Vector2f[] uvs){ this.uvs = Conversion.toFloatArray(uvs); }
    public void setUVScale(float uvScale){
        if(uvVBOID == -1){
            Debug.log("Texture coordinates not set");
            return;
        }

        float[] updatedUVs = uvs;
        for (int i = 0; i < updatedUVs.length; i++) {
            updatedUVs[i] *= uvScale;
        }

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, uvVBOID);
        FloatBuffer buffer = Utils.storeDataInFloatBuffer(updatedUVs);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(1, vertexCount, GL11.GL_FLOAT, false, 0, 0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        this.uvs = updatedUVs;
        this.uvScale = uvScale;
    }

    public void setNormals(Vector3f[] normals){
        this.normals = Conversion.toFloatArray(normals);
    }
    public void setNormals(Vector2f[] normals){ this.normals = Conversion.toFloatArray(normals); }

//     Getters

    public final int getVertexCount(){
        if(vertexCount == -1){
            vertexCount = vertices.length;
        }

        return vertexCount;
    }

    public final int getVaoID(){
        return vaoID;
    }
    public final int[] getTriangles() {
        return triangles;
    }
    public final Vector2f[] getUVs() { return Conversion.floatArrayToVector2Array(uvs); }
    public final Vector3f[] getNormals() { return Conversion.floatArrayToVector3Array(normals); }
    public final Vector3f[] getVertices() { return Conversion.floatArrayToVector3Array(vertices); }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    public void cleanUp(){
        if(SceneManager.currentScene == null || !SceneManager.currentScene.hasVaoId(vaoID)) return;
        GL30.glDeleteVertexArrays(vaoID);

        vertices = null;
        uvs = null;
        normals = null;
        tangents = null;
        bitangents = null;
        triangles = null;
        instanceOffsets = null;

        for(int vbo: vbos){
            GL30.glDeleteBuffers(vbo);
        }
        GL30.glDeleteBuffers(instanceVBOID);
        GL30.glDeleteBuffers(tangentsVBOID);
        GL30.glDeleteBuffers(bitangentsVBOID);
        GL30.glDeleteBuffers(normalVBOID);
        GL30.glDeleteBuffers(vertexVBOID);
        GL30.glDeleteBuffers(triangleVBOID);
        GL30.glDeleteBuffers(uvVBOID);
        ModelManager.unloadMeshId(vaoID);
    }

    public boolean hasTangents(){
        return tangentsVBOID != -1 && bitangentsVBOID != -1;
    }

    public void setMeshPath(String meshPath) {
        this.meshPath = meshPath;
    }

    public String getMeshPath() {
        return meshPath;
    }

    public final float getUvScale() { return uvScale; }

    public final int getInstanceVBOID(){
        return instanceVBOID;
    }

    public Mesh setInstanced(boolean instanced) {
        isInstanced = instanced;
        return this;
    }

    public boolean isInstanced() {
        return isInstanced;
    }

    public int getInstanceCount() {
        return instanceOffsets.size();
    }

    public void addInstanceOffset(Matrix4f offset){
        isInstanced = true;
        instanceOffsets.add(offset);
    }

    public void addInstanceOffset(Set<Matrix4f> offset){
        isInstanced = true;
        instanceOffsets.addAll(offset);
    }

    public void setInstanceOffsets(Set<Matrix4f> offset){
        isInstanced = true;
        instanceOffsets = offset;
    }

    public void updateInstanceVBO() {
        int instanceCount = instanceOffsets.size();

        if (instanceBuffer == null || instanceBuffer.capacity() < instanceCount * 16) {
            if (instanceBuffer != null) MemoryUtil.memFree(instanceBuffer);
            // Grow by 1.5x or a fixed margin to reduce realloc frequency
            int newCapacity = Math.max(instanceCount * 16, instanceBuffer != null ? (int)(instanceBuffer.capacity() * 1.5f) : 0);
            instanceBuffer = MemoryUtil.memAllocFloat(newCapacity);
        } else {
            instanceBuffer.clear();
        }

        for (Matrix4f m : instanceOffsets) {
            m.get(instanceBuffer);
            instanceBuffer.position(instanceBuffer.position() + 16);
        }
        instanceBuffer.flip();

        GL30.glBindVertexArray(vaoID);

        if (instanceVBOID == -1) {
            instanceVBOID = GL15.glGenBuffers();
            vbos.add(instanceVBOID);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceVBOID);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) instanceCount * 64, isStatic ? GL15.GL_STATIC_DRAW : GL15.GL_DYNAMIC_DRAW);

            int start = 5;
            for (int i = 0; i < 4; i++) {
                GL20.glEnableVertexAttribArray(start + i);
                GL20.glVertexAttribPointer(start + i, 4, GL11.GL_FLOAT, false, 64, i * 16L);
                GL33.glVertexAttribDivisor(start + i, 1);
            }

        } else {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceVBOID);
            if (previousInstanceCount != instanceCount) {
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) instanceCount * 64, isStatic ? GL15.GL_STATIC_DRAW : GL15.GL_DYNAMIC_DRAW);
            }
        }

        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, instanceBuffer);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);

        previousInstanceCount = instanceCount;
    }

    public void optimize(){
        removeUnusedVerticesAndTriangles();
        mergeDuplicatedVerticesAndTriangles();
    }

    public void mergeDuplicatedVerticesAndTriangles() {
        int vertexCount = this.vertices.length / dimension;

        Map<VertexKey, Integer> uniqueMap = new HashMap<>();
        List<Float> newPositions = new ArrayList<>();
        List<Float> newNormals = new ArrayList<>();
        List<Float> newUVs = new ArrayList<>();
        int[] remap = new int[vertexCount];
        int newIndex = 0;

        for (int i = 0; i < vertexCount; i++) {
            VertexKey key = new VertexKey(
                    this.vertices, i * dimension, dimension,
                    this.normals, i * dimension, dimension,
                    this.uvs, i * 2, 2,
                    1e-6f
            );
            Integer existing = uniqueMap.get(key);
            if (existing == null) {
                uniqueMap.put(key, newIndex);
                remap[i] = newIndex++;
                for (int d = 0; d < dimension; d++) newPositions.add(this.vertices[i * dimension + d]);
                for (int d = 0; d < dimension; d++) newNormals.add(this.normals[i * dimension + d]);
                if(this.uvs != null && this.uvs.length > 0) for (int d = 0; d < 2; d++) newUVs.add(this.uvs[i * 2 + d]);
            } else {
                remap[i] = existing;
            }
        }

        List<Integer> newTriangles = new ArrayList<>();
        for (int i = 0; i < this.triangles.length; i += 3) {
            int i0 = remap[this.triangles[i]];
            int i1 = remap[this.triangles[i + 1]];
            int i2 = remap[this.triangles[i + 2]];
            if (i0 != i1 && i1 != i2 && i2 != i0) { // Not degenerate
                newTriangles.add(i0);
                newTriangles.add(i1);
                newTriangles.add(i2);
            }
        }

        this.vertices = new float[newPositions.size()];
        for (int i = 0; i < newPositions.size(); i++) this.vertices[i] = newPositions.get(i);

        if (this.normals != null) {
            this.normals = new float[newNormals.size()];
            for (int i = 0; i < newNormals.size(); i++) this.normals[i] = newNormals.get(i);
        }

        if (this.uvs != null) {
            this.uvs = new float[newUVs.size()];
            for (int i = 0; i < newUVs.size(); i++) this.uvs[i] = newUVs.get(i);
        }

        this.triangles = new int[newTriangles.size()];
        for (int i = 0; i < newTriangles.size(); i++) this.triangles[i] = newTriangles.get(i);
    }

    public void removeUnusedVerticesAndTriangles(){
        if(this.vertices == null || this.triangles == null) return;

        int vertexCount = this.vertices.length / dimension;

        boolean[] usedVertices = new boolean[vertexCount];
        for (int idx : this.triangles) {
            usedVertices[idx] = true;
        }

        int[] oldToNewIndex = new int[vertexCount];
        int newIndex = 0;
        for (int i = 0; i < vertexCount; i++) {
            if (usedVertices[i]) {
                oldToNewIndex[i] = newIndex++;
            } else {
                oldToNewIndex[i] = -1;
            }
        }

        float[] optimizedVertices = new float[newIndex * dimension];
        int dest = 0;
        for (int i = 0; i < vertexCount; i++) {
            if (usedVertices[i]) {
                System.arraycopy(this.vertices, i * dimension, optimizedVertices, dest * dimension, dimension);
                dest++;
            }
        }

        int[] optimizedTriangles = new int[this.triangles.length];
        for (int i = 0; i < this.triangles.length; i++) {
            optimizedTriangles[i] = oldToNewIndex[this.triangles[i]];
        }

        this.vertices = optimizedVertices;
        this.triangles = optimizedTriangles;
    }

    public Set<Matrix4f> getInstanceOffsets(){
        return instanceOffsets;
    }

    public void setStatic(boolean isStatic){
        this.isStatic = isStatic;
    }

    public boolean isStatic(){
        return isStatic;
    }

    @Override
    public JsonObject serializeToJson() {
        return JsonHelper.objectToJson(this, new String[]{"vbos", "vaoID", "vertexVBOID", "normalVBOID", "tangentsVBOID",
                "bitangentsVBOID", "triangleVBOID", "uvVBOID", "instanceVBOID", "vertexCount", "previousInstanceCount"});
    }

    @Override
    public IJsonSerializable deserializeFromJson(String json) {
        JsonReader jsonReader = Json.createReader(new StringReader(json));
        JsonObject jsonInfo = jsonReader.readObject();
        try{
            JsonHelper.loadVariableIntoObject(this, jsonInfo);
        } catch (Exception e) {
            Debug.logError("Error loading in data: " + e.getMessage());
        }

        return this;
    }

    static class VertexKey {
        int[] quantized;

        VertexKey(float[] positions, int posOffset, int posDim,
                  float[] normals, int normOffset, int normDim,
                  float[] uvs, int uvOffset, int uvDim,
                  float epsilon) {
            int totalDim = posDim + normDim + uvDim;
            quantized = new int[totalDim];
            int idx = 0;
            for (int i = 0; i < posDim; i++)
                quantized[idx++] = Math.round(positions[posOffset + i] / epsilon);
            if(normals != null) {
                for (int i = 0; i < normDim; i++)
                    quantized[idx++] = Math.round(normals[normOffset + i] / epsilon);
            }
            if(uvs != null && uvs.length > 0) {
                for (int i = 0; i < uvDim; i++)
                    quantized[idx++] = Math.round(uvs[uvOffset + i] / epsilon);
            }
        }
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof VertexKey)) return false;
            return Arrays.equals(quantized, ((VertexKey) o).quantized);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(quantized);
        }
    }
}
