package nl.framegengine.editor;

import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;
import java.nio.FloatBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class EditorWindow {

    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiImplGl3 = new ImGuiImplGl3();

    private String glslVersion = null;
    private long windowPtr;
    public static EditorLayout editorLayout;
    private int gameFBOID = -1;

    public static int referenceWidth = 1920;
    public static int referenceHeight = 1080;
    public static int windowWidth;
    public static int windowHeight;
    public static float windowScaleX = 1;
    public static float windowScaleY = 1;

    private static EditorWindow instance = null;

    public EditorWindow(){
        instance = this;
    }

    public static EditorWindow getInstance() {
        return instance;
    }

    public void init(){
        initWindow();
        initImGui();
        imGuiGlfw.init(windowPtr, true);
        imGuiImplGl3.init(glslVersion);
    }

    public void cleanUp(){
        imGuiImplGl3.shutdown();
        imGuiGlfw.shutdown();
        ImGui.destroyContext();
        Callbacks.glfwFreeCallbacks(windowPtr);
        glfwDestroyWindow(windowPtr);
        glfwTerminate();
    }

    private void initWindow(){
        GLFWErrorCallback.createPrint(System.err).set();
        if(!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");

        glslVersion = "#version 140";
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GL_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GL_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);

        // Get content scale for primary monitor
        FloatBuffer xScale = BufferUtils.createFloatBuffer(1);
        FloatBuffer yScale = BufferUtils.createFloatBuffer(1);
        glfwGetMonitorContentScale(glfwGetPrimaryMonitor(), xScale, yScale);
        windowScaleX = xScale.get(0);
        windowScaleY = yScale.get(0);

        // Calculate window size in logical pixels
        int width = (int)(referenceWidth / windowScaleX);
        int height = (int)(referenceHeight / windowScaleY);

        windowWidth = referenceWidth;
        windowHeight = referenceHeight;

        if(editorLayout != null) editorLayout.recalculatePanels();

        // Set GLFW hint to scale to monitor
        glfwWindowHint(GLFW_SCALE_TO_MONITOR, GLFW_TRUE);

        windowPtr = glfwCreateWindow(width, height, "FrameGengine Editor", MemoryUtil.NULL, MemoryUtil.NULL);

        if(windowPtr == MemoryUtil.NULL) throw new RuntimeException("Failed to create GLFW window");
        glfwMakeContextCurrent(windowPtr);
        glfwSwapInterval(1);
        glfwShowWindow(windowPtr);

        GLFW.glfwSetFramebufferSizeCallback(windowPtr, (window, windowWidth, windowHeight) -> {
            this.windowWidth = windowWidth;
            this.windowHeight = windowHeight;

            if(editorLayout != null) editorLayout.recalculatePanels();
        });

        GL.createCapabilities();
    }

    private void initImGui(){ ImGui.createContext(); }

    public void run(){
        while (!glfwWindowShouldClose(windowPtr)){
            prepareFrame();
            renderFrame();
            endFrame();
        }
    }

    private void prepareFrame(){
        glClearColor(.2f, 0.2f, 0.2f, 1);
        glClear(GL_COLOR_BUFFER_BIT);

        imGuiGlfw.newFrame();
        imGuiImplGl3.newFrame();

        ImGui.newFrame();
    }

    private void renderFrame(){
        editorLayout.renderLayout();
        ImGui.render();
        imGuiImplGl3.renderDrawData(ImGui.getDrawData());
    }

    private void endFrame(){
        if(ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)){
            final long backupWindowPtr = glfwGetCurrentContext();
            ImGui.updatePlatformWindows();
            ImGui.renderPlatformWindowsDefault();
            glfwMakeContextCurrent(backupWindowPtr);
        }

        glfwSwapBuffers(windowPtr);
        glfwPollEvents();
    }

    public int getGameFBOID() {
        return gameFBOID;
    }

    public void setGameFBOID(int gameFBOID) {
        this.gameFBOID = gameFBOID;
    }

    public void resetGameFBOID(){
        this.gameFBOID = -1;
    }

    public void setEditorLayout(EditorLayout editorLayout){
        EditorWindow.editorLayout = editorLayout;
    }

    public static EditorLayout getEditorLayout() {
        return editorLayout;
    }
}
