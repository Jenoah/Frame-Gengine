package nl.framegengine.core.audio;

import nl.framegengine.core.components.audio.AudioListener;
import nl.framegengine.core.components.audio.AudioSource;
import nl.framegengine.core.debugging.Debug;
import org.lwjgl.openal.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.openal.AL10.alDistanceModel;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class AudioManager {
    private final ArrayList<SoundBuffer> soundBuffers;
    private final HashMap<String, AudioSource> audioSources;
    private long context;
    private long playbackDevice;
    private AudioListener audioListener;

    public AudioManager(){
        soundBuffers = new ArrayList<>();
        audioSources = new HashMap<>();

        playbackDevice = alcOpenDevice((ByteBuffer) null);
        if(playbackDevice == NULL) throw new IllegalStateException("Failed to open default playback device");

        ALCCapabilities playbackDeviceCapabilities = ALC.createCapabilities(playbackDevice);
        this.context = alcCreateContext(playbackDevice, (IntBuffer) null);
        if(context == NULL) throw new IllegalStateException("Failed to create audio context");
        alcMakeContextCurrent(context);
        AL.createCapabilities(playbackDeviceCapabilities);
        setAttenuationModel(AL11.AL_EXPONENT_DISTANCE);
        GetAudioDevices(playbackDeviceCapabilities).forEach(device -> {Debug.log("Audio device: " + device);});

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
        audioSources.clear();
        soundBuffers.forEach(SoundBuffer::cleanUp);
        soundBuffers.clear();
        if(context != NULL) alcDestroyContext(context);
        if(playbackDevice != NULL) alcCloseDevice(playbackDevice);
    }

    public List<String> GetAudioDevices(ALCCapabilities playbackCapabilities){
        if (playbackCapabilities.ALC_ENUMERATE_ALL_EXT) {

            List<String> devices = ALUtil.getStringList(
                    0,
                    ALC11.ALC_ALL_DEVICES_SPECIFIER
            );

            return devices;
        } else if (playbackCapabilities.ALC_ENUMERATION_EXT) {
            Debug.log("Using ALC_ENUMERATION_EXT");

            List<String> devices = ALUtil.getStringList(
                    0,
                    ALC10.ALC_DEVICE_SPECIFIER
            );

            return devices;

        }

        return new ArrayList<String>();
    }
}
