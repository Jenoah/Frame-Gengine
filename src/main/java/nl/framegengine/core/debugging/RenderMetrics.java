package nl.framegengine.core.debugging;

import nl.framegengine.core.EngineManager;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

public class RenderMetrics {
    // Frame timing
    private long frameStartTime;
    private long lastFrameDuration;

    // Counters (reset each frame)
    private int drawCalls;
    private int shaderBinds;
    private int stateChanges;
    private int vaoBinds;
    private int vertexCount;

    // GPU Timing
    private int queryID;
    private boolean queryActive;
    private long gpuTimeNs;

    public void init() {
        // Create GPU timestamp query
        queryID = GL15.glGenQueries();
    }

    public void frameStart() {
        // CPU timing
        frameStartTime = System.nanoTime();

        // Reset counters
        drawCalls = 0;
        shaderBinds = 0;
        stateChanges = 0;
        vaoBinds = 0;
        vertexCount = 0;

        // Begin GPU timing (alternating queries)
        if(!queryActive) {
            GL33.glQueryCounter(queryID, GL33.GL_TIMESTAMP);
            queryActive = true;
        }
    }

    public void frameEnd() {
        // CPU frame duration
        lastFrameDuration = System.nanoTime() - frameStartTime;

        // Retrieve GPU time
        if(queryActive) {
            gpuTimeNs = GL33.glGetQueryObjectui64(queryID, GL33.GL_QUERY_RESULT);
            queryActive = false;
        }
    }

    // Instrumentation methods
    public void recordDrawCall() { drawCalls++; }
    public void recordShaderBind() { shaderBinds++; }
    public void recordStateChange() { stateChanges++; }
    public void recordVaoBind() { vaoBinds++; }
    public void recordVertexCount(int vertexCount){ this.vertexCount += vertexCount; }

    // Reporting
    public String getMetrics() {
        return String.format(
                "CPU: %.2fs | GPU: %.2fs | Draws: %d | Shaders: %d | VAOs: %d | Vertex count: %d | DeltaTime: %fs",
                lastFrameDuration / 1e6,
                gpuTimeNs / 1e6,
                drawCalls,
                shaderBinds,
                vaoBinds,
                vertexCount,
                EngineManager.getDeltaTimeMS()
        );
    }
}

