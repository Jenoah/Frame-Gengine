package nl.framegengine.core.debugging;

import java.util.*;
import java.util.regex.*;

public class ConsoleColors {
    public final String text;
    public final float[] color; // ImGui uses float[4] RGBA

    public ConsoleColors(String text, float[] color) {
        this.text = text;
        this.color = color;
    }

    // Map ANSI color codes to ImGui colors
    private static final Map<String, float[]> ANSI_TO_IMGUI = Map.of(
            "\u001B[31m", new float[]{1.0f, 0.2f, 0.2f, 1.0f}, // gray
            "\u001B[35m", new float[]{0.58f, 0.0f, 0.83f, 1.0f}, // purple
            "\u001B[90m", new float[]{0.56f, 0.56f, 0.56f, 1.0f}, // gray
            "\u001B[0m",  new float[]{1.0f, 1.0f, 1.0f, 1.0f} // reset to white
    );

    private static final Pattern ANSI_PATTERN = Pattern.compile("(\u001B\\[[;\\d]*m)");

    public static List<ConsoleColors> parseAnsi(String input) {
        List<ConsoleColors> segments = new ArrayList<>();
        Matcher matcher = ANSI_PATTERN.matcher(input);
        int lastEnd = 0;
        float[] currentColor = ANSI_TO_IMGUI.getOrDefault("\u001B[0m", new float[]{1,1,1,1});

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                segments.add(new ConsoleColors(input.substring(lastEnd, matcher.start()), currentColor));
            }
            String ansi = matcher.group(1);
            currentColor = ANSI_TO_IMGUI.getOrDefault(ansi, currentColor);
            lastEnd = matcher.end();
        }
        if (lastEnd < input.length()) {
            segments.add(new ConsoleColors(input.substring(lastEnd), currentColor));
        }
        return segments;
    }
}
