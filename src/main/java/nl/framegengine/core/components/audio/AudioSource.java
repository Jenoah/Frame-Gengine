package nl.framegengine.core.components.audio;

import nl.framegengine.core.audio.AudioManager;
import nl.framegengine.core.audio.SoundBuffer;
import nl.framegengine.core.components.Component;
import nl.framegengine.core.entity.SceneManager;
import org.joml.Math;
import org.joml.Vector3f;

import static nl.framegengine.core.audio.AudioManager.checkALError;
import static org.lwjgl.openal.AL10.*;

public class AudioSource extends Component {
    private int sourceId;
    private AudioManager audioManager;
    private boolean is3d = true;

    public AudioSource(){
        if(SceneManager.currentScene != null && SceneManager.currentScene.getAudioManager() != null) {
            this.audioManager = SceneManager.currentScene.getAudioManager();
        }
    }

    @Override
    public void initiate() {
        if (hasInitiated) return;
        super.initiate();
        this.sourceId = alGenSources();
        checkALError();
        set3d(true);
        if(SceneManager.currentScene != null && SceneManager.currentScene.getAudioManager() != null){
            audioManager.addAudioSource(getGuid(), this);
        }
    }

    public final boolean isPlaying() {
        return alGetSourcei(sourceId, AL_SOURCE_STATE) == AL_PLAYING;
    }

    public void play() {
        int currentBuffer = alGetSourcei(sourceId, AL_BUFFER);
        alSourcePlay(sourceId);
    }

    public void stop() {
        if(isPlaying()) alSourceStop(sourceId);
    }

    public void pause() {
        if(isPlaying()) alSourcePause(sourceId);
    }

    public void setBuffer(SoundBuffer soundBuffer) {
        stop();
        alSourcei(sourceId, AL_BUFFER, soundBuffer.getBufferId());
        audioManager.addSoundBuffer(soundBuffer);
    }

    public void setVolume(float gain) {
        gain = Math.clamp(0f, 1f, gain);
        alSourcef(sourceId, AL_GAIN, gain);
    }

    public void setLoop(boolean isLooping){
        alSourcei(sourceId, AL_LOOPING, isLooping ? AL_TRUE : AL_FALSE);
    }

    public void set3d(boolean is3d){
        alSourcei(sourceId, AL_SOURCE_RELATIVE, is3d ? AL_TRUE : AL_FALSE);
        if(!is3d) alSource3f(sourceId, AL_POSITION, 0, 0, 0);

        this.is3d = is3d;
    }

    @Override
    public void update() {
        super.update();
        if(is3d && (root.hasUpdated())){
            Vector3f rootPosition = root.getPosition();
            alSource3f(sourceId, AL_POSITION, rootPosition.x, rootPosition.y, rootPosition.z);
        }
    }

    @Override
    public void cleanUp() {
        if(hasCleanedUp) return;
        super.cleanUp();
        alSourceStop(sourceId);
        alSourcei(sourceId, AL_BUFFER, 0);
        alDeleteSources(sourceId);
    }
}
