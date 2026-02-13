package nl.framegengine.customScripts.utils;

import nl.framegengine.core.utils.SimplexNoise;
import nl.framegengine.core.utils.Utils;

public class ChunkUtils {

    //Terrain settings (I KNOW I SHOULD MOVE THIS TO A SEPARATE SETTINGS THING)
    public static final float terrainSurfaceHeight = .5f;
    public static final boolean smoothTerrain = true;
    public static final float maxTerrainHeight = 8f;
    private static final float continentalScale = .025f;
    private static final float erosionScale = .1f;
    private static final float peakAndValleyScale = .5f;
    private static final float continentalAmplitude = 1f;
    private static final float erosionAmplitude = 1f;
    private static final float peakAndValleyAmplitude = 1f;
    private static final float continetalFrequency = 0.5f;
    private static final float erosionFrequency = .5f;
    private static final float peakAndValleyFrequency = 0.5f;
    public static final int CHUNK_SIZE = 16;
    public static final float CHUNK_ISO_LEVEL = 0.5f;

    public static float SampleHeight(float x, float z){
        //INTERESTING ARTICLE: https://www.redblobgames.com/maps/terrain-from-noise/
        final int noiseSeed = SimplexNoise.RANDOMSEED;

        Utils.fastNoise.SetSeed(noiseSeed);
        Utils.fastNoise.SetFrequency(continetalFrequency);
        float continentalNoise = (Utils.fastNoise.GetNoise(x * continentalScale, z * continentalScale) + 1f) / 2f * continentalAmplitude;

        Utils.fastNoise.SetSeed(noiseSeed + noiseSeed);
        Utils.fastNoise.SetFrequency(erosionFrequency);
        float erosionNoise = (Utils.fastNoise.GetNoise(x * erosionScale, z * erosionScale) + 1f) / 2f * erosionAmplitude;

        //Utils.fastNoise.SetSeed(noiseSeed + noiseSeed + noiseSeed);
        //Utils.fastNoise.SetFrequency(peakAndValleyFrequency);
        //float peakAndValleyNoise = (Utils.fastNoise.GetNoise(x * peakAndValleyScale, z * peakAndValleyScale) + 1f) / 2f * peakAndValleyAmplitude;

        float heightSample = continentalNoise + erosionNoise;// * peakAndValleyNoise;
        heightSample /= continentalAmplitude + erosionAmplitude;// + peakAndValleyAmplitude;
        heightSample = (float)Math.pow(heightSample * 1.2, 2.0);
        heightSample *= maxTerrainHeight;

        return heightSample;
    }

    public static int GetVoxelConfiguration(float[] voxelCorners)
    {
        int configIndex = 0;
        for (int i = 0; i < 8; i++)
        {
            if (voxelCorners[i] > terrainSurfaceHeight) configIndex |= 1 << i;
        }

        return configIndex;
    }

}

