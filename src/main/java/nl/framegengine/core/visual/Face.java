package nl.framegengine.core.visual;

public class Face {
    private final int[] vertexIndices;
    private final int[] normalIndices;
    private final int[] textureCoordinateIndices;

    public Face(int[] vertexIndices, int[] textureCoordinateIndices,
                int[] normalIndices) {

        this.vertexIndices = vertexIndices;
        this.normalIndices = normalIndices;
        this.textureCoordinateIndices = textureCoordinateIndices;
    }

    public int[] getVertexIndices() { return this.vertexIndices; }

    public int[] getTextureCoords() {
        return this.textureCoordinateIndices;
    }

    public int[] getNormals() {
        return this.normalIndices;
    }
}
