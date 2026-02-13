package nl.framegengine.customScripts.utils;

import org.joml.Vector3f;
import nl.framegengine.customScripts.*;
import nl.framegengine.customScripts.utils.*;

public class ChunkCoord{
    public int x, y, z;
    
    public ChunkCoord(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static ChunkCoord toChunkCoord(Vector3f vector){
        int x, y, z;
        x = fastfloor((int)vector.x / (float)ChunkUtils.CHUNK_SIZE) * ChunkUtils.CHUNK_SIZE;
        y = fastfloor((int)vector.y / (float)ChunkUtils.CHUNK_SIZE) * ChunkUtils.CHUNK_SIZE;
        z = fastfloor((int)vector.z / (float)ChunkUtils.CHUNK_SIZE) * ChunkUtils.CHUNK_SIZE;

        return new ChunkCoord(x, y, z);
    }
    
    public Vector3f toVector3(){
        return new Vector3f(x, y, z);
    }

    private static int fastfloor(double x) {
        int xi = (int)x;
        return x<xi ? xi-1 : xi;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) return true;
        if(!(obj instanceof ChunkCoord b)) return false;
        return this.x == b.x && this.y == b.y && this.z == b.z;
    }

    public static boolean compareToVector(ChunkCoord a, Vector3f b){

        int x = fastfloor((int)b.x / (float)ChunkUtils.CHUNK_SIZE) * ChunkUtils.CHUNK_SIZE;
        int y = fastfloor((int)b.y / (float)ChunkUtils.CHUNK_SIZE) * ChunkUtils.CHUNK_SIZE;
        int z = fastfloor((int)b.z / (float)ChunkUtils.CHUNK_SIZE) * ChunkUtils.CHUNK_SIZE;

        return a.x == x && a.y == y && a.z == z;
    }

    @Override
    public final int hashCode() {
        return this.x ^ this.y << 2 ^ this.z >> 2;
    }

    @Override
    public String toString() {
        return "X " + x + ", Y " + y + ", Z" + z;
    }
}
