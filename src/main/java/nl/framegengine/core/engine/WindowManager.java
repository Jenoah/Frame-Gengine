package nl.framegengine.core.engine;

import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.entity.SceneManager;
import nl.framegengine.core.shaders.ShaderManager;
import nl.framegengine.editor.EditorWindow;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.glfwSetWindowIcon;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryUtil.memAllocInt;

public class WindowManager {
    private final String title;
    private int width, height;
    private long window;
    private boolean resize = false;
    private final boolean vSync;
    private boolean standalone = true;
    private boolean isInFocus = true;

    public WindowManager(String title, int width, int height, boolean vSync, boolean standalone) {
        this.title = title;
        this.width = width;
        this.height = height;
        this.vSync = vSync;
        this.standalone = standalone;
        if(!this.standalone) window = GLFW.glfwGetCurrentContext();
        instance = this;
    }

    private static WindowManager instance = null;

    public static synchronized void createInstance(String title, int width, int height, boolean vSync, boolean standalone) {
        if (instance != null) {
            throw new IllegalStateException("WindowManager already initialized");
        }
        instance = new WindowManager(title, width, height, vSync, standalone);
    }

    public static WindowManager getInstance() {
//        if (instance == null) {
//            throw new IllegalStateException("WindowManager has not been initialized. Call createInstance() first.");
//        }
        return instance;
    }

    public void init(){
        Debug.logConsole("Initializing window");
        boolean maximized = (width == 0 || height == 0);

        if(standalone) {
            GLFWErrorCallback.createPrint(System.err).set();
            if (!GLFW.glfwInit())
                throw new IllegalStateException("Unable to initialize GLFW");

            GLFW.glfwDefaultWindowHints();
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GL_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GL_TRUE);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);

            if (maximized) {
                width = 100;
                height = 100;
                GLFW.glfwWindowHint(GLFW.GLFW_MAXIMIZED, GLFW.GLFW_TRUE);
            }

            window = GLFW.glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL);
            if (window == MemoryUtil.NULL)
                throw new RuntimeException("Failed to create GLFW window");
        }else{
            window = GLFW.glfwGetCurrentContext();
        }


        GLFW.glfwSetFramebufferSizeCallback(window, (window, width, height) -> {
            setWindowSize(width, height);
            if(SceneManager.currentScene != null && SceneManager.currentScene.getMainCamera() != null){
                SceneManager.currentScene.getMainCamera().updateAspectRatio();
                SceneManager.currentScene.getMainCamera().updateProjectionMatrix();
            }
            ShaderManager.updateGenericUniforms();
            if(!standalone){
                EditorWindow.windowWidth = this.width;
                EditorWindow.windowHeight = this.height;
                if(EditorWindow.editorLayout != null){
                    EditorWindow.editorLayout.recalculatePanels();
                }
            }
        });

        if(standalone) {
            GLFW.glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
                if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE)
                    GLFW.glfwSetWindowShouldClose(window, true);
            });


            if (maximized) {
                GLFW.glfwMaximizeWindow(window);
            } else {
                GLFWVidMode vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
                if (vidMode != null) {
                    GLFW.glfwSetWindowPos(window, (vidMode.width() - width) / 2, (vidMode.height() - height) / 2);
                }
            }

            GLFW.glfwMakeContextCurrent(window);
            GLFW.glfwShowWindow(window);
            GL.createCapabilities();
        }

        GLFW.glfwSwapInterval(isvSync() ? 1 : 0);

        glClearColor(0,0,0, 0);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        glEnable(GL_STENCIL_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
    }

    public void update(){
        if(!standalone) return;
        GLFW.glfwSwapBuffers(window);
        GLFW.glfwPollEvents();
    }

    public boolean isKeyPressed(int keyCode){
        if(isInFocus) return GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
        return false;
    }

    public boolean isResize(){
        return resize;
    }

    public void setResize(boolean resize){
        this.resize = resize;
    }

    public boolean windowShouldClose() {
        return GLFW.glfwWindowShouldClose(window);
    }

    public boolean isvSync(){
        return vSync;
    }

    public void cleanUp() {
        instance = null;
        if(!standalone) return;
        GLFW.glfwDestroyWindow(GLFW.glfwGetCurrentContext());
        GLFW.glfwTerminate();
    }

    public String getTitle() {
        return title;
    }

    public void setWindowTitle(String title){
        GLFW.glfwSetWindowTitle(window, title);
    }

    public void setWindowIcon(String path){
        if (System.getProperty("os.name").toLowerCase().contains("mac")) return;

        IntBuffer w = memAllocInt(1);
        IntBuffer h = memAllocInt(1);
        IntBuffer comp = memAllocInt(1);

        ByteBuffer pixels = stbi_load(path, w, h, comp, 4);
        if (pixels == null) {
            Debug.log("Failed to load icon: " + stbi_failure_reason());
            return;
        }
        try (GLFWImage.Buffer icons = GLFWImage.malloc(1)) {
            icons.position(0)
                    .width(w.get(0))
                    .height(h.get(0))
                    .pixels(pixels);
            icons.position(0);
            glfwSetWindowIcon(window, icons);
        }
        stbi_image_free(pixels);
    }

    public long getWindow() { return window; }

    public int getHeight() { return height; }

    public int getWidth() { return width; }

    public void setClearColor(float r, float g, float b, float a){ glClearColor(r,g,b,a); }


    public boolean isStandalone() {
        return standalone;
    }

    public void setFocus(boolean focus){
        this.isInFocus = focus;
    }

    public final boolean getFocus(){
        return this.isInFocus;
    }

    public void setWindowSize(int width, int height){
        this.width = width;
        this.height = height;
        setResize(true);
    }
}
