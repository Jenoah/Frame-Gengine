package nl.framegengine.core.visual;

import nl.framegengine.core.debugging.Debug;
import nl.framegengine.core.utils.Constants;
import nl.framegengine.editor.EngineSettings;
import nl.framegengine.editor.ManifestHelper;
import org.joml.Math;
import org.lwjgl.opengl.*;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.lwjgl.BufferUtils;

import static org.lwjgl.stb.STBImage.stbi_set_flip_vertically_on_load;

public class TextureLoader {

    private static final HashMap<String, Integer> textures = new HashMap<>();
    private static boolean flipTexture = true;
    private static boolean pointFilter = false;
    private static boolean repeatTexture = true;
    private static boolean isNormalMap = false;
    private static boolean isDataTexture = false;
    private static int defaultTextureID = 0;

    public static int loadTexture(String fileName){
        String textureGUID = ManifestHelper.getGuidByPath(ManifestHelper.manifestFileType.TEXTURE, fileName);
        if(textures.containsKey(textureGUID)){
            //Debug.log("Loading existing texture for " + fileName);
            return textures.get(textureGUID);
        }

        ByteBuffer imageBuffer;
        int width = 0, height = 0, alphaFormat, components = 0;


        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer comp = stack.mallocInt(1);

            stbi_set_flip_vertically_on_load(flipTexture);
            File file = new File(fileName);
            if (file.exists()) {
                imageBuffer = STBImage.stbi_load(fileName, w, h, comp, 0);
            } else {
                if (!fileName.startsWith(EngineSettings.currentProjectDirectory)) {
                    fileName = Paths.get(EngineSettings.currentProjectDirectory, fileName).toString();
                }
                file = new File(fileName);
                if (file.exists()) {
                    imageBuffer = STBImage.stbi_load(fileName, w, h, comp, 0);
                } else if (EngineSettings.isCompiled) {
                    String resourcePath = fileName.startsWith("/") ? fileName : ("/" + fileName);
                    try (InputStream is = TextureLoader.class.getResourceAsStream(resourcePath)) {
                        if (is == null) {
                            Debug.logConsoleError("Image resource " + resourcePath + " not found in classpath");
                            return defaultTextureID;
                        }
                        byte[] bytes = is.readAllBytes();
                        ByteBuffer resourceBuffer = BufferUtils.createByteBuffer(bytes.length);
                        resourceBuffer.put(bytes).flip();
                        imageBuffer = STBImage.stbi_load_from_memory(resourceBuffer, w, h, comp, 0);
                        if (imageBuffer == null) {
                            Debug.logConsoleError("Failed to decode image resource " + resourcePath + ": " + STBImage.stbi_failure_reason());
                            return defaultTextureID;
                        }
                    } catch (IOException e) {
                        Debug.logConsoleError("Failed to read image resource: " + e.getMessage());
                        return defaultTextureID;
                    }
                } else {
                    Debug.logConsoleError("Image file " + fileName + " could not be located in filesystem or resource folder: " + STBImage.stbi_failure_reason());
                    return defaultTextureID;
                }
            }

            if(imageBuffer == null){
                Debug.logConsoleError("Image file " + fileName + " could not be loaded because " + STBImage.stbi_failure_reason());
                return defaultTextureID;
            }

            width = w.get();
            height = h.get();
            components = comp.get(); // Store component count before stack closes
        } catch (Exception e) {
            Debug.logConsoleError(e.getMessage());
            return defaultTextureID;
        }

        int id = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

        int format;
        if (components == 1) {
            // Single-channel grayscale texture (e.g., roughness, AO, metallic maps)
            // Use linear color space (GL_R8) for data textures, not sRGB
            format = GL30.GL_R8;
            alphaFormat = GL11.GL_RED;
            
            // Use texture swizzling to make single-channel textures appear as grayscale
            // This maps R -> RGB so previews show as black-and-white instead of red
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL33.GL_TEXTURE_SWIZZLE_R, GL11.GL_RED);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL33.GL_TEXTURE_SWIZZLE_G, GL11.GL_RED);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL33.GL_TEXTURE_SWIZZLE_B, GL11.GL_RED);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL33.GL_TEXTURE_SWIZZLE_A, GL11.GL_RED);
        } else if (components == 3) {
            if ((width & 3) != 0) {
                GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 2 - (width & 1));
            }
            // Use linear color space for normal maps and data textures (roughness, metallic, AO)
            // Use sRGB for color textures (albedo)
            format = (isNormalMap || isDataTexture) ? GL11.GL_RGB8 : GL21.GL_SRGB8;
            alphaFormat = GL11.GL_RGB;
        } else {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            format = (isNormalMap || isDataTexture) ? GL11.GL_RGBA8 : GL21.GL_SRGB8_ALPHA8;
            alphaFormat = GL11.GL_RGBA;
        }

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, format, width, height, 0, alphaFormat, GL11.GL_UNSIGNED_BYTE, imageBuffer);
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);

        if(repeatTexture) {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_REPEAT);
        }else{
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        }

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST_MIPMAP_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, pointFilter ? GL11.GL_NEAREST : GL11.GL_LINEAR);

        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, 0f);

        if(Constants.USE_ANISOTROPIC && GL.createCapabilities().GL_EXT_texture_filter_anisotropic){
            float anisotropicAmount = Math.min(4f, GL11.glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT));
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, anisotropicAmount);
        }else{
            System.out.println("Anisotropic filtering not supported");
        }

        STBImage.stbi_image_free(imageBuffer);

        TextureLoader.flipTexture = true;
        TextureLoader.pointFilter = false;
        TextureLoader.repeatTexture = true;
        TextureLoader.isNormalMap = false;
        TextureLoader.isDataTexture = false;

        if(textureGUID != null){
            textures.put(textureGUID, id);
        }else{
            textureGUID = ManifestHelper.getGuidByPath(ManifestHelper.manifestFileType.TEXTURE, fileName);
            if(textureGUID != null) textures.put(textureGUID, id);
        }

        return id;
    }


    public static int loadTexture(String fileName, boolean pointFilter){
        TextureLoader.pointFilter = pointFilter;
        return loadTexture(fileName);
    }


    public static int loadTexture(String fileName, boolean pointFilter, boolean flipTexture){
        TextureLoader.pointFilter = pointFilter;
        TextureLoader.flipTexture = flipTexture;
        return loadTexture(fileName);
    }

    public static int loadTexture(String fileName, boolean pointFilter, boolean flipTexture, boolean repeatTexture){
        TextureLoader.pointFilter = pointFilter;
        TextureLoader.repeatTexture = repeatTexture;
        TextureLoader.flipTexture = flipTexture;
        return loadTexture(fileName);
    }

    public static int loadTexture(String fileName, boolean pointFilter, boolean flipTexture, boolean repeatTexture, boolean isNormalMap){
        TextureLoader.pointFilter = pointFilter;
        TextureLoader.repeatTexture = repeatTexture;
        TextureLoader.flipTexture = flipTexture;
        TextureLoader.isNormalMap = isNormalMap;
        return loadTexture(fileName);
    }

    public static int loadTexture(String fileName, boolean pointFilter, boolean flipTexture, boolean repeatTexture, boolean isNormalMap, boolean isDataTexture){
        TextureLoader.pointFilter = pointFilter;
        TextureLoader.repeatTexture = repeatTexture;
        TextureLoader.flipTexture = flipTexture;
        TextureLoader.isNormalMap = isNormalMap;
        TextureLoader.isDataTexture = isDataTexture;
        return loadTexture(fileName);
    }

    public static int loadCubeMapTexture(String[] fileNames){
        int textureID = GL11.glGenTextures();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, textureID);

        for(int i = 0; i < fileNames.length; i++){
            TextureData data = TextureLoader.getTextureData(fileNames[i]);
            GL11.glTexImage2D(GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL11.GL_RGBA, data.getWidth(), data.getHeight(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, data.buffer);
        }
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

        textures.put(java.util.UUID.randomUUID().toString(), textureID);
        return textureID;
    }

    private static TextureData getTextureData(String fileName){
        ByteBuffer imageBuffer;
        int width, height;
        IntBuffer comp;

        try(MemoryStack stack = MemoryStack.stackPush()){
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            comp = stack.mallocInt(1);

            stbi_set_flip_vertically_on_load(false);
            imageBuffer = STBImage.stbi_load(fileName, w, h, comp, 4);
            if(imageBuffer == null){
                throw new Exception("Image file " + fileName + " could not be loaded because " + STBImage.stbi_failure_reason());
            }

            width = w.get();
            height = h.get();

            return new TextureData(imageBuffer, width, height);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static void cleanUp(){
        textures.forEach((guid, id) -> GL11.glDeleteTextures(id));
    }

    private static class TextureData{
        private int width, height;
        private ByteBuffer buffer;

        public TextureData(ByteBuffer buffer, int width, int height){
            this.height = height;
            this.width = width;
            this.buffer = buffer;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public ByteBuffer getBuffer() {
            return buffer;
        }
    }

    public static int getTextureByGUID(String guid){
        if(textures.containsKey(guid)) return textures.get(guid);
        return -1;
    }

    public static String getGuidById(int textureId) {
        AtomicReference<String> guid = new AtomicReference<>();

        textures.forEach((textureGuid, id) -> {
            if (id == textureId) guid.set(textureGuid);
        });
        return guid.get();
    }

    public static void setDefaultTextureId(int id){
        defaultTextureID = id;
    }

    public static int getDefaultTextureId(){ return defaultTextureID; }
}
