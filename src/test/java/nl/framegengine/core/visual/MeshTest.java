package nl.framegengine.core.visual;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class MeshTest {

    private static final float EPSILON = 1e-5f;

    // ------------------------------------------------------------------
    // Reflection helpers — vertices and triangles have no float[] setters.
    // ------------------------------------------------------------------

    private static void setVertices(Mesh mesh, float[] vertices) throws Exception {
        Field f = Mesh.class.getDeclaredField("vertices");
        f.setAccessible(true);
        f.set(mesh, vertices);
    }

    private static void setTriangles(Mesh mesh, int[] triangles) throws Exception {
        Field f = Mesh.class.getDeclaredField("triangles");
        f.setAccessible(true);
        f.set(mesh, triangles);
    }

    private static void setUvsRaw(Mesh mesh, float[] uvs) throws Exception {
        Field f = Mesh.class.getDeclaredField("uvs");
        f.setAccessible(true);
        f.set(mesh, uvs);
    }

    private static void setNormalsRaw(Mesh mesh, float[] normals) throws Exception {
        Field f = Mesh.class.getDeclaredField("normals");
        f.setAccessible(true);
        f.set(mesh, normals);
    }

    private static float[] getNormalsRaw(Mesh mesh) throws Exception {
        Field f = Mesh.class.getDeclaredField("normals");
        f.setAccessible(true);
        return (float[]) f.get(mesh);
    }

    private static float[] getTangentsRaw(Mesh mesh) throws Exception {
        Field f = Mesh.class.getDeclaredField("tangents");
        f.setAccessible(true);
        return (float[]) f.get(mesh);
    }

    private static float[] getBitangentsRaw(Mesh mesh) throws Exception {
        Field f = Mesh.class.getDeclaredField("bitangents");
        f.setAccessible(true);
        return (float[]) f.get(mesh);
    }

    // Convenience: build a flat-packed float[] from Vector3f varargs.
    private static float[] v3Array(Vector3f... vecs) {
        float[] out = new float[vecs.length * 3];
        for (int i = 0; i < vecs.length; i++) {
            out[i * 3]     = vecs[i].x;
            out[i * 3 + 1] = vecs[i].y;
            out[i * 3 + 2] = vecs[i].z;
        }
        return out;
    }

    // ======================================================================
    // §6.1  calculateNormals — single triangle, all-flat face
    // ======================================================================

    @Test
    void calculateNormals_singleTriangle_normalIsPerpendicularToFaceAndUnitLength() throws Exception {
        // Triangle in the XY-plane: A=(0,0,0), B=(1,0,0), C=(0,1,0)
        // Expected face normal: (0,0,1) pointing along +Z.
        Mesh mesh = new Mesh();
        setVertices(mesh,  v3Array(new Vector3f(0,0,0), new Vector3f(1,0,0), new Vector3f(0,1,0)));
        setTriangles(mesh, new int[]{0, 1, 2});

        mesh.calculateNormals();

        float[] normals = getNormalsRaw(mesh);
        assertNotNull(normals);
        assertEquals(9, normals.length, "three vertices × 3 floats");

        for (int i = 0; i < 3; i++) {
            assertEquals(0f, normals[i * 3],     EPSILON, "normal.x should be 0 for vertex " + i);
            assertEquals(0f, normals[i * 3 + 1], EPSILON, "normal.y should be 0 for vertex " + i);
            assertEquals(1f, normals[i * 3 + 2], EPSILON, "normal.z should be 1 for vertex " + i);
        }
    }

    @Test
    void calculateNormals_singleTriangle_normalIsUnitLength() throws Exception {
        Mesh mesh = new Mesh();
        setVertices(mesh,  v3Array(new Vector3f(0,0,0), new Vector3f(1,0,0), new Vector3f(0,1,0)));
        setTriangles(mesh, new int[]{0, 1, 2});

        mesh.calculateNormals();

        float[] normals = getNormalsRaw(mesh);
        for (int i = 0; i < 3; i++) {
            float nx = normals[i * 3];
            float ny = normals[i * 3 + 1];
            float nz = normals[i * 3 + 2];
            float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            assertEquals(1f, length, EPSILON, "normal for vertex " + i + " should be unit length");
        }
    }

    @Test
    void calculateNormals_sharedVertex_normalIsAveragedAndNormalized() throws Exception {
        // Two triangles sharing vertex A=(0,0,0):
        //   Triangle 1: A, B=(1,0,0), C=(0,1,0)  → face normal (0,0,1)
        //   Triangle 2: A, B=(1,0,0), D=(0,0,1)  → face normal (0,-1,0)
        //     edge1 = B-A = (1,0,0), edge2 = D-A = (0,0,1)
        //     cross = (0·1-0·0, 0·0-1·1, 1·0-0·0) = (0,-1,0)
        // Shared vertex A accumulates (0,0,1)+(0,-1,0) = (0,-1,1), normalised = (0,-1/√2,1/√2)
        Mesh mesh = new Mesh();
        setVertices(mesh,  v3Array(
                new Vector3f(0, 0, 0),  // 0 = A (shared)
                new Vector3f(1, 0, 0),  // 1 = B (shared)
                new Vector3f(0, 1, 0),  // 2 = C
                new Vector3f(0, 0, 1)   // 3 = D
        ));
        setTriangles(mesh, new int[]{0, 1, 2,   0, 1, 3});

        mesh.calculateNormals();

        float[] normals = getNormalsRaw(mesh);
        float expectedLen = (float) Math.sqrt(2.0);

        // Vertex A (index 0): averaged normal must be (0, -1, 1) normalised
        assertEquals(0f,                  normals[0], EPSILON);
        assertEquals(-1f / expectedLen,   normals[1], EPSILON);
        assertEquals( 1f / expectedLen,   normals[2], EPSILON);

        // All normals must be unit length
        for (int i = 0; i < 4; i++) {
            float nx = normals[i * 3];
            float ny = normals[i * 3 + 1];
            float nz = normals[i * 3 + 2];
            float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            assertEquals(1f, length, EPSILON, "normal for vertex " + i + " should be unit length");
        }
    }

    // ======================================================================
    // §6.2  mergeDuplicatedVerticesAndTriangles
    // ======================================================================

    @Test
    void mergeDuplicatedVertices_twoIdenticalVertices_collapseToOne() throws Exception {
        // V0=(0,0,0), V1=(1,0,0), V2=(0,0,0) — V2 is a duplicate of V0.
        // Triangle [0,1,2] references them. After merge V2 → 0.
        // The triangle becomes [0,1,0] which is degenerate (i0==i2) and is dropped.
        // Result: 2 unique vertices, 0 triangles.
        Mesh mesh = new Mesh();
        float[] verts  = v3Array(new Vector3f(0,0,0), new Vector3f(1,0,0), new Vector3f(0,0,0));
        float[] norms  = v3Array(new Vector3f(0,0,1), new Vector3f(0,0,1), new Vector3f(0,0,1));
        setVertices(mesh,   verts);
        setNormalsRaw(mesh, norms);
        setTriangles(mesh,  new int[]{0, 1, 2});

        mesh.mergeDuplicatedVerticesAndTriangles();

        Vector3f[] resultVerts = mesh.getVertices();
        assertEquals(2, resultVerts.length, "two unique vertices should remain");
        assertEquals(0, mesh.getTriangles().length, "degenerate triangle should be removed");
    }

    @Test
    void mergeDuplicatedVertices_nonDuplicateVerticesPreserved() throws Exception {
        // Three distinct vertices forming one valid triangle — nothing should change.
        Mesh mesh = new Mesh();
        float[] verts = v3Array(new Vector3f(0,0,0), new Vector3f(1,0,0), new Vector3f(0,1,0));
        float[] norms = v3Array(new Vector3f(0,0,1), new Vector3f(0,0,1), new Vector3f(0,0,1));
        setVertices(mesh,   verts);
        setNormalsRaw(mesh, norms);
        setTriangles(mesh,  new int[]{0, 1, 2});

        mesh.mergeDuplicatedVerticesAndTriangles();

        assertEquals(3, mesh.getVertices().length,  "all three distinct vertices should remain");
        assertEquals(3, mesh.getTriangles().length, "triangle should be preserved");
    }

    @Test
    void mergeDuplicatedVertices_degenerateTriangleAfterMerge_isRemoved() throws Exception {
        // Four vertices where V0==V3. Two triangles: [0,1,2] (valid) and [3,1,2] (becomes [0,1,2] after
        // merge — not degenerate by itself, but both triangles now point to same vertex set).
        // The degenerate check only removes triangles where two indices are equal.
        // A cleaner degenerate case: V0==V1==V2 as one triangle.
        // Use: V0=(1,0,0), V1=(1,0,0), V2=(2,0,0) — V0==V1, triangle [0,1,2] → [0,0,2] → degenerate.
        Mesh mesh = new Mesh();
        float[] verts = v3Array(new Vector3f(1,0,0), new Vector3f(1,0,0), new Vector3f(2,0,0));
        float[] norms = v3Array(new Vector3f(0,0,1), new Vector3f(0,0,1), new Vector3f(0,0,1));
        setVertices(mesh,   verts);
        setNormalsRaw(mesh, norms);
        setTriangles(mesh,  new int[]{0, 1, 2});

        mesh.mergeDuplicatedVerticesAndTriangles();

        assertEquals(2, mesh.getVertices().length,  "duplicate vertex collapsed to one");
        assertEquals(0, mesh.getTriangles().length, "degenerate triangle removed");
    }

    // ======================================================================
    // §6.3  removeUnusedVerticesAndTriangles
    // ======================================================================

    @Test
    void removeUnusedVertices_unreferencedVertex_isStripped() throws Exception {
        // Vertices: V0, V1, V2, V3. Triangle only references 0,1,2 — V3 is orphaned.
        Mesh mesh = new Mesh();
        setVertices(mesh,  v3Array(
                new Vector3f(0,0,0),
                new Vector3f(1,0,0),
                new Vector3f(0,1,0),
                new Vector3f(5,5,5)  // unreferenced
        ));
        setTriangles(mesh, new int[]{0, 1, 2});

        mesh.removeUnusedVerticesAndTriangles();

        Vector3f[] verts = mesh.getVertices();
        assertEquals(3, verts.length, "unreferenced vertex should be stripped");

        // Indices should still form a valid single triangle
        int[] tris = mesh.getTriangles();
        assertEquals(3, tris.length);
        // All indices must be in range [0, 2]
        for (int idx : tris) {
            assertTrue(idx >= 0 && idx <= 2, "remapped index out of range: " + idx);
        }
    }

    @Test
    void removeUnusedVertices_allVerticesUsed_meshUnchanged() throws Exception {
        Mesh mesh = new Mesh();
        setVertices(mesh,  v3Array(new Vector3f(0,0,0), new Vector3f(1,0,0), new Vector3f(0,1,0)));
        setTriangles(mesh, new int[]{0, 1, 2});

        mesh.removeUnusedVerticesAndTriangles();

        assertEquals(3, mesh.getVertices().length,  "vertex count unchanged");
        assertEquals(3, mesh.getTriangles().length, "triangle count unchanged");
    }

    // ======================================================================
    // §6.4  getVertexCount
    // ======================================================================

    @Test
    void getVertexCount_derivedFromTriangleArrayLength() throws Exception {
        // vertexCount field is -1 (no load called) — should fall back to triangles.length
        Mesh mesh = new Mesh();
        setTriangles(mesh, new int[]{0, 1, 2,  0, 2, 3});  // 6 indices

        int count = mesh.getVertexCount();

        assertEquals(6, count, "getVertexCount should return triangles.length when triangles is set");
    }

    @Test
    void getVertexCount_derivedFromVerticesWhenNoTriangles() throws Exception {
        // No triangles set — should fall back to vertices.length / dimension (3)
        Mesh mesh = new Mesh();
        setVertices(mesh, v3Array(new Vector3f(0,0,0), new Vector3f(1,0,0), new Vector3f(0,1,0)));
        // triangles stays null

        int count = mesh.getVertexCount();

        assertEquals(3, count, "getVertexCount should return vertices.length / 3 when no triangles");
    }

    // ======================================================================
    // §6.5  isBuiltin / getResourcePath
    // ======================================================================

    @Test
    void isBuiltin_pathStartingWithBuiltinPrefix_returnsTrue() {
        Mesh mesh = new Mesh();
        mesh.setMeshPath("builtin:models/cube.obj");

        assertTrue(mesh.isBuiltin());
    }

    @Test
    void isBuiltin_normalPath_returnsFalse() {
        Mesh mesh = new Mesh();
        mesh.setMeshPath("assets/models/cube.obj");

        assertFalse(mesh.isBuiltin());
    }

    @Test
    void isBuiltin_nullPath_returnsFalse() {
        Mesh mesh = new Mesh();
        mesh.setMeshPath(null);

        assertFalse(mesh.isBuiltin());
    }

    @Test
    void getResourcePath_builtinPath_stripsPrefix() {
        Mesh mesh = new Mesh();
        mesh.setMeshPath("builtin:models/cube.obj");

        assertEquals("models/cube.obj", mesh.getResourcePath());
    }

    @Test
    void getResourcePath_nonBuiltinPath_returnedAsIs() {
        Mesh mesh = new Mesh();
        mesh.setMeshPath("assets/models/sphere.obj");

        assertEquals("assets/models/sphere.obj", mesh.getResourcePath());
    }
}
