package nl.framegengine.core.audio;

import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.modelLoaders.StaticMeshLoader;
import nl.framegengine.core.utils.FileHelper;
import nl.framegengine.core.visual.TextureLoader;
import nl.framegengine.editor.EngineSettings;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class SoundBuffer {
    private int bufferId;
    private ShortBuffer pcm;

    public SoundBuffer(String fileName){
        Path filePath = Path.of(EngineSettings.currentProjectDirectory, fileName);
        if (filePath.toFile().exists()) {
            fileName = filePath.toString();
        } else if (EngineSettings.isCompiled) {
            fileName = EngineSettings.currentProjectDirectory + File.separator + fileName;
        } else {
            try {
                URL resource = SoundBuffer.class.getResource(fileName);
                if (resource != null) fileName = Paths.get(resource.toURI()).toAbsolutePath().toString();
            } catch (URISyntaxException e) {
                Debug.logError("Error loading model at " + fileName + ". " + e.getMessage());
                bufferId = -1;
                return;
            }
        }

        try {
            bufferId = loadBuffer(fileName);
            return;
        } catch (Exception e) {
            Debug.logError("Error loading sound buffer: " + e.getMessage());
        }
    }

    private int loadBuffer(String filePath){
        int bufferId = alGenBuffers();
        try (STBVorbisInfo info = STBVorbisInfo.malloc()) {
            pcm = readVorbis(filePath, info);
            alBufferData(bufferId, info.channels() == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, pcm, info.sample_rate());
            Debug.log("Loaded sound buffer from " + filePath);
        }

        return bufferId;
    }

    private ShortBuffer readVorbis(String filePath, STBVorbisInfo info){
        try (MemoryStack stack = MemoryStack.stackPush()){
            IntBuffer error = stack.mallocInt(1);
            long decoder = STBVorbis.stb_vorbis_open_filename(filePath, error, null);
            if(decoder == NULL){
                throw new RuntimeException("Failed to open Ogg Vorbis file. Error: " + error.get(0));
            }

            STBVorbis.stb_vorbis_get_info(decoder, info);

            int channels = info.channels();
            int lengthSamples = STBVorbis.stb_vorbis_stream_length_in_samples(decoder);

            ShortBuffer result = MemoryUtil.memAllocShort(lengthSamples * channels);
            result.limit(STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channels, result) * channels);
            STBVorbis.stb_vorbis_close(decoder);

            return result;
        }
    }

    public int getBufferId(){
        return this.bufferId;
    }

    public void cleanUp(){
        alDeleteBuffers(this.bufferId);
        if(pcm != null){
            MemoryUtil.memFree(pcm);
        }
    }
}
