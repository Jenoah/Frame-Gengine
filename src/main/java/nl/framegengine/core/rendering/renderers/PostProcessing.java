package nl.framegengine.core.rendering.renderers;

import nl.framegengine.core.visual.ModelManager;
import nl.framegengine.core.engine.WindowManager;
import nl.framegengine.core.visual.Model;
import nl.framegengine.core.shaders.Shader;
import nl.framegengine.core.shaders.postProcessing.*;
import nl.framegengine.core.shaders.postProcessing.effects.PPFXCombineEffect;
import nl.framegengine.core.shaders.postProcessing.effects.PPFXGenericEffect;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class PostProcessing {
    private static Model quad;

    private static PPFXGenericEffect horizontalBlurEffectPass1;
    private static PPFXGenericEffect verticalBlurEffectPass1;
    private static PPFXGenericEffect gammaCorrectEffect;
    private static PPFXGenericEffect contrastEffect;
    private static PPFXGenericEffect brightEffect;
    private static PPFXGenericEffect vignetteEffect;
    private static PPFXGenericEffect fxaaEffect;
    private static PPFXGenericEffect outputEffect;

    private static PPFXCombineEffect combineEffect;

    private static final int bloomSize = 8;
    private static final float bloomThreshold = .95f;
    private static final float bloomIntensity = 1.75f;

    public static void init(){
        quad = getQuad();
        WindowManager window = WindowManager.getInstance();

        try {
            //Blur (for Bloom)
            PPFXHorizontalBlurShader horizontalBlurShader = new PPFXHorizontalBlurShader();
            PPFXVerticalBlurShader verticalBlurShader = new PPFXVerticalBlurShader();
            verticalBlurShader.setTargetHeight(window.getHeight() / bloomSize);
            horizontalBlurShader.setTargetWidth(window.getWidth() / bloomSize);
            horizontalBlurEffectPass1 = new PPFXGenericEffect(window.getWidth() / bloomSize, window.getHeight() / bloomSize, horizontalBlurShader);
            verticalBlurEffectPass1 = new PPFXGenericEffect(window.getWidth() / bloomSize, window.getHeight() / bloomSize, verticalBlurShader);
            combineEffect = new PPFXCombineEffect(window.getWidth(), window.getHeight());
            combineEffect.setIntensity(bloomIntensity);

            //Brightness detection (for Bloom)
            PPFXBrightShader brightShader = new PPFXBrightShader();
            brightShader.setThreshold(bloomThreshold);
            brightEffect = new PPFXGenericEffect(window.getWidth() / 2, window.getHeight() / 2, brightShader); //Divided by 2 due to performance

            //Gamma and contrast adjustments
            gammaCorrectEffect = new PPFXGenericEffect(window.getWidth(), window.getHeight(), new PPFXGammaCorrectShader());
            contrastEffect = new PPFXGenericEffect(window.getWidth(), window.getHeight(), new Shader().init("/shaders/postProcessing/ppfxGeneric.vs", "/shaders/postProcessing/ppfxContrast.fs"));

            //Vignette
            vignetteEffect = new PPFXGenericEffect(window.getWidth(), window.getHeight(), new Shader().init("/shaders/postProcessing/ppfxGeneric.vs", "/shaders/postProcessing/ppfxVignette.fs"));

            //Anti Aliasing
            PPFXFxaaShader fxaaShader = new PPFXFxaaShader();
            fxaaEffect = new PPFXGenericEffect(window.getWidth(), window.getHeight(), fxaaShader);
            fxaaShader.setResolution(new Vector2f(window.getWidth(), window.getHeight()));

            //Render final
            outputEffect = new PPFXGenericEffect(new Shader().init("/shaders/postProcessing/ppfxGeneric.vs", "/shaders/postProcessing/ppfxGeneric.fs"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Model getQuad(){
        float[] vertices = new float[]{
                -1f,  1f, 0.0f, // Top-left
                -1,  -1, 0.0f, // Top-right
                1, 1, 0.0f, // Bottom-right
                1, -1, 0.0f  // Bottom-left
        };

        return ModelManager.loadModel(vertices);
    }

    public static void render(int colourTexture){
        bind();

        //Bloom
        brightEffect.render(colourTexture);
        horizontalBlurEffectPass1.render(brightEffect.getOutputTexture());
        verticalBlurEffectPass1.render(horizontalBlurEffectPass1.getOutputTexture());
        combineEffect.render(colourTexture, verticalBlurEffectPass1.getOutputTexture());

        //Color adjustment
        contrastEffect.render(combineEffect.getOutputTexture());
        gammaCorrectEffect.render(contrastEffect.getOutputTexture());
        vignetteEffect.render(gammaCorrectEffect.getOutputTexture());

        //Anti Aliasing
        fxaaEffect.render(vignetteEffect.getOutputTexture());

        //Render to screen
        outputEffect.render(fxaaEffect.getOutputTexture());

        unbind();
    }

    public static void renderOutput(){
        bind();
        outputEffect.render(fxaaEffect.getOutputTexture());
        unbind();
    }

    public int getOutputTextureID(){
        return outputEffect.getOutputTexture();
    }

    public static void cleanUp(){
        horizontalBlurEffectPass1.cleanUp();
        verticalBlurEffectPass1.cleanUp();
        brightEffect.cleanUp();
        contrastEffect.cleanUp();
        combineEffect.cleanUp();
        gammaCorrectEffect.cleanUp();
        vignetteEffect.cleanUp();
        fxaaEffect.cleanUp();
        outputEffect.cleanUp();
    }

    private static void bind(){
        GL30.glBindVertexArray(quad.getId());
        GL20.glEnableVertexAttribArray(0);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    private static void unbind(){
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL20.glDisableVertexAttribArray(0);
        GL30.glBindVertexArray(0);
    }

    public static void updateResolution(){
        cleanUp();
        init();
    }
}
