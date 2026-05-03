package nl.framegengine.core.audio;

import nl.framegengine.core.components.audio.AudioListener;
import nl.framegengine.core.components.audio.AudioSource;
import nl.framegengine.core.debugging.Debug;
import org.lwjgl.openal.*;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.openal.ALC11.*;
import static org.lwjgl.openal.EXTThreadLocalContext.alcSetThreadContext;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memFree;

public class AudioManager {
    private final ArrayList<SoundBuffer> soundBuffers;
    private final HashMap<String, AudioSource> audioSources;
    private long context;
    private long playbackDevice;
    private boolean useTLC;
    private AudioListener audioListener;
    private ALCapabilities caps;

    public AudioManager(){
        soundBuffers = new ArrayList<>();
        audioSources = new HashMap<>();

        playbackDevice = alcOpenDevice((ByteBuffer) null);
        if(playbackDevice == NULL) throw new IllegalStateException("Failed to open default playback device");

        ALCCapabilities playbackDeviceCapabilities = ALC.createCapabilities(playbackDevice);
        if (!playbackDeviceCapabilities.OpenALC10) {
            throw new IllegalStateException();
        }

        if (playbackDeviceCapabilities.OpenALC11) {
            List<String> devices = ALUtil.getStringList(NULL, ALC_ALL_DEVICES_SPECIFIER);
            if (devices == null) {
                checkALCError(NULL);
            }
        }

        String defaultDeviceSpecifier = Objects.requireNonNull(alcGetString(NULL, ALC_DEFAULT_DEVICE_SPECIFIER));

        this.context = alcCreateContext(playbackDevice, (IntBuffer) null);
        checkALCError(playbackDevice);
        if(context == NULL) throw new IllegalStateException("Failed to create audio context");
        alcMakeContextCurrent(context);

        useTLC = playbackDeviceCapabilities.ALC_EXT_thread_local_context && alcSetThreadContext(context);
        if (!useTLC) {
            if (!alcMakeContextCurrent(context)) {
                throw new IllegalStateException();
            }
        }
        checkALCError(playbackDevice);

        caps = AL.createCapabilities(playbackDeviceCapabilities, MemoryUtil::memCallocPointer);

        setAttenuationModel(AL11.AL_EXPONENT_DISTANCE);
        //GetAudioDevices(playbackDeviceCapabilities).forEach(device -> {Debug.log("Audio device: " + device);});
    }

    public void addSoundBuffer(SoundBuffer soundBuffer){
        this.soundBuffers.add(soundBuffer);
    }

    public void addAudioSource(String name, AudioSource audioSource){
        this.audioSources.put(name, audioSource);
    }

    public AudioListener getAudioListener() {
        return this.audioListener;
    }

    public void setAudioListener(AudioListener audioListener){
        this.audioListener = audioListener;
    }

    public AudioSource getAudioSource(String name){
        return this.audioSources.get(name);
    }

    public void playAudioSource(String name){
        AudioSource audioSource = this.audioSources.get(name);
        if(audioSource != null && !audioSource.isPlaying()) audioSource.play();
    }

    public void removeAudioSource(String name){
        this.audioSources.remove(name);
    }

    public void setAttenuationModel(int attenuationModel){
        alDistanceModel(attenuationModel);
    }

    public void cleanUp(){
        audioSources.values().forEach(AudioSource::cleanUp);
        soundBuffers.forEach(SoundBuffer::cleanUp);
        audioSources.clear();
        soundBuffers.clear();
        alcMakeContextCurrent(NULL);
        if(useTLC){
            AL.setCurrentThread(null);
        }else{
            AL.setCurrentProcess(null);
        }
        memFree(caps.getAddressBuffer());
        if(context != NULL) alcDestroyContext(context);
        if(playbackDevice != NULL) alcCloseDevice(playbackDevice);
    }

    public List<String> GetAudioDevices(ALCCapabilities playbackCapabilities){
        if (playbackCapabilities.ALC_ENUMERATE_ALL_EXT) {

            List<String> devices = ALUtil.getStringList(
                    0,
                    ALC_ALL_DEVICES_SPECIFIER
            );

            return devices;
        } else if (playbackCapabilities.ALC_ENUMERATION_EXT) {
            List<String> devices = ALUtil.getStringList(
                    0,
                    ALC10.ALC_DEVICE_SPECIFIER
            );

            return devices;

        }

        return new ArrayList<String>();
    }

    static void checkALCError(long device) {
        int err = alcGetError(device);
        if (err != ALC_NO_ERROR) {
            throw new RuntimeException(alcGetString(device, err));
        }
    }

    public static void checkALError() {
        int err = alGetError();
        if (err != AL_NO_ERROR) {
            String error = alGetString(err);
            Debug.logError(error);
            throw new RuntimeException(error);
        }
    }
}
