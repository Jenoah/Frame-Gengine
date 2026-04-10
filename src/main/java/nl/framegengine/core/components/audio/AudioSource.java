package nl.framegengine.core.components.audio;

import nl.framegengine.core.components.Component;
import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.utils.Calculus;
import nl.framegengine.core.utils.ObjectPool;
import org.joml.Math;
import org.joml.Vector3f;

import static org.lwjgl.openal.AL10.*;

public class AudioSource extends Component {
    private int sourceId;
    private Camera mainCamera = null;
    private boolean is3d = true;

    public AudioSource(){}

    @Override
    public void initiate() {
        super.initiate();
        if (hasInitiated) return;
        this.sourceId = alGenSources();
        if(Camera.getMainCamera() != null) mainCamera = Camera.getMainCamera();
        set3d(true);
    }

    public final boolean isPlaying() {
        return alGetSourcei(sourceId, AL_SOURCE_STATE) == AL_PLAYING;
    }

    public void play() {
        alSourcePlay(sourceId);
    }

    public void stop() {
        alSourceStop(sourceId);
    }

    public void pause() {
        alSourcePause(sourceId);
    }

    public void setBuffer(int bufferId) {
        stop();
        alSourcei(sourceId, AL_BUFFER, bufferId);
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
        super.cleanUp();
        stop();
        alDeleteSources(sourceId);
    }
}
