package nl.framegengine.core.debugging;

import nl.framegengine.core.engine.EngineManager;
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
    private int queryStartID;
    private int queryEndID;
    private long gpuFrameDurationNs;
    private boolean queryResultPending;

    public void init() {
        queryStartID = GL15.glGenQueries();
        queryEndID = GL15.glGenQueries();
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

        if(queryResultPending) {
            long startNs = GL33.glGetQueryObjectui64(queryStartID, GL33.GL_QUERY_RESULT);
            long endNs = GL33.glGetQueryObjectui64(queryEndID, GL33.GL_QUERY_RESULT);
            gpuFrameDurationNs = endNs - startNs;
            queryResultPending = false;
        }

        GL33.glQueryCounter(queryStartID, GL33.GL_TIMESTAMP);
    }

    public void frameEnd() {
        // CPU frame duration
        lastFrameDuration = System.nanoTime() - frameStartTime;

        GL33.glQueryCounter(queryEndID, GL33.GL_TIMESTAMP);
        queryResultPending = true;
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
                "CPU: %.2fms | GPU: %.2fms | Draws: %d | Shaders: %d | VAOs: %d | Vertex count: %d | DeltaTime: %.2fms",
                lastFrameDuration / 1e6,
                gpuFrameDurationNs / 1e6,
                drawCalls,
                shaderBinds,
                vaoBinds,
                vertexCount,
                EngineManager.getDeltaTimeMS()
        );
    }
}

