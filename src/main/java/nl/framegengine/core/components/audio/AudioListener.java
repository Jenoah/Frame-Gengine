package nl.framegengine.core.components.audio;

import nl.framegengine.core.components.Component;
import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.utils.Constants;
import org.joml.Vector3f;

import static org.lwjgl.openal.AL10.*;

public class AudioListener extends Component {
    public static AudioListener Instance = null;

    //TODO: Continue implementation of OpenAL: https://ahbejarano.gitbook.io/lwjglgamedev/chapter-16

    @Override
    public void initiate() {
        super.initiate();
        if (Instance != this && Instance != null){
            Debug.logError("An audio listener has already registered on " + Instance.root);
            root.removeComponent(this);
            return;
        }

        Instance = this;
        alListener3f(AL_POSITION, 0f, 0f, 0f);
        alListener3f(AL_VELOCITY, 0f, 0f, 0f);
    }

    @Override
    public void update() {
        super.update();
        if(root.hasUpdated()){
            Vector3f rootPosition = root.getPosition();
            alListener3f(AL_POSITION, rootPosition.x, rootPosition.y, rootPosition.z);
            setOrientation(root.getForward(), Constants.VECTOR3_UP);
        }
    }

    public void setOrientation(Vector3f at, Vector3f up) {
        float[] data = new float[6];
        data[0] = at.x;
        data[1] = at.y;
        data[2] = at.z;
        data[3] = up.x;
        data[4] = up.y;
        data[5] = up.z;
        alListenerfv(AL_ORIENTATION, data);
    }

    @Override
    public void cleanUp() {
        super.cleanUp();
        Instance = null;
    }
}
