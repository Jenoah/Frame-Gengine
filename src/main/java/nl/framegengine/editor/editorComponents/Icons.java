package nl.framegengine.editor.editorComponents;

import nl.framegengine.core.entity.Camera;
import nl.framegengine.core.entity.GameObject;
import nl.framegengine.core.lighting.Light;

public class Icons {
    public static final String PLAY         = "\uF4F4";
    public static final String PAUSE        = "\uF4C3";
    public static final String STOP         = "\uF592";
    public static final String SETTINGS     = "\uF3E3";
    public static final String TRANSFORM    = "\uF14E";
    public static final String ROTATE       = "\uF130";
    public static final String SCALE        = "\uF14A";
    public static final String LIGHT        = "\uF468";
    public static final String CAMERA       = "\uF21A";
    public static final String BOX          = "\uF1C8";
    public static final String CARET_DOWN   = "\uF229";
    public static final String CARET_RIGHT  = "\uF231";
    public static final String DIAMOND      = "\uF2F1";
    public static final String FILTER       = "\uF3CA";
    public static final String DISPLAY      = "\uF302";
    public static final String FOLDER       = "\uF3D7";
    public static final String TERMINAL     = "\uF5C3";

    public static String GetIcon(GameObject object){
        if(object instanceof Camera) return CAMERA;
        if(object instanceof Light) return LIGHT;
        return BOX;
    }
}
