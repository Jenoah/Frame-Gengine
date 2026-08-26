package nl.framegengine.editor.editorComponents;

import imgui.ImGui;
import org.joml.Vector4f;

public class InputField {

    public static boolean vector4(String id, Vector4f vector) {
        float[] vector4FloatValues = new float[]{
                vector.x,
                vector.y,
                vector.z,
                vector.w
        };

        boolean changed = ImGui.inputFloat4(id, vector4FloatValues);

        if (changed) {
            vector.set(vector4FloatValues[0], vector4FloatValues[1], vector4FloatValues[2], vector4FloatValues[3]);
        }

        return changed;
    }
}
