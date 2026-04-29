package objects;

import static org.lwjgl.opengl.GL30.*;

/**
 * Obstacle.java — Phase 3: Dynamic transformations.
 *
 * A wireframe cube that spawns down the track. All obstacles share the same
 * static VAO/VBO (created once, drawn many times). Each instance differs by
 * position, rotation (tumbling), and scale (pulsing).
 *
 * Dynamic Rotation: Obstacles continuously tumble/spin on X and Y axes.
 * Dynamic Scaling:  Obstacles pulse in size using a sine wave to simulate
 *                   an invisible beat: scale = 1.0 + 0.2 * sin(totalTime * 5).
 */
public class Obstacle extends GameObject {

    // --- Shared GPU resources (one mesh for all cubes) ---
    private static int vaoId;
    private static int vboId;
    private static int vertexCount;
    private static boolean meshInitialized = false;

    // --- Per-instance animation state ---
    private float rotSpeedX; // Degrees per second on X-axis
    private float rotSpeedY; // Degrees per second on Y-axis
    private float phaseOffset; // Unique phase offset so cubes don't pulse in unison

    public Obstacle() {
        super();
        // Give each obstacle slightly different tumble speeds for visual variety
        rotSpeedX = 40.0f + (float)(Math.random() * 60.0f); // 40–100 deg/s
        rotSpeedY = 30.0f + (float)(Math.random() * 70.0f); // 30–100 deg/s
        // Random phase offset for the sine-wave pulsing
        phaseOffset = (float)(Math.random() * Math.PI * 2.0);
    }

    /**
     * Initialize the shared cube wireframe mesh.
     * Call this ONCE before creating any Obstacle instances.
     * 12 edges × 2 endpoints × 3 floats = 72 floats.
     */
    public static void initMesh() {
        if (meshInitialized) return;

        float s = 0.4f; // half-size of the cube

        // 12 edges of a cube, each defined by 2 endpoints (GL_LINES)
        float[] vertices = {
            // --- Bottom face edges ---
            -s, 0.0f, -s,   s, 0.0f, -s,
             s, 0.0f, -s,   s, 0.0f,  s,
             s, 0.0f,  s,  -s, 0.0f,  s,
            -s, 0.0f,  s,  -s, 0.0f, -s,

            // --- Top face edges ---
            -s, 2*s, -s,   s, 2*s, -s,
             s, 2*s, -s,   s, 2*s,  s,
             s, 2*s,  s,  -s, 2*s,  s,
            -s, 2*s,  s,  -s, 2*s, -s,

            // --- Vertical pillars connecting top and bottom ---
            -s, 0.0f, -s,  -s, 2*s, -s,
             s, 0.0f, -s,   s, 2*s, -s,
             s, 0.0f,  s,   s, 2*s,  s,
            -s, 0.0f,  s,  -s, 2*s,  s,
        };

        vertexCount = vertices.length / 3;

        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        meshInitialized = true;
    }

    /**
     * Per-frame update: apply tumbling rotation and pulsing scale.
     *
     * @param dt        delta time in seconds
     * @param totalTime total elapsed game time (drives the sine wave)
     */
    public void update(float dt, float totalTime) {
        // --- Dynamic Rotation: tumble on X and Y axes ---
        org.joml.Vector3f rot = getRotation();
        rot.x += rotSpeedX * dt;
        rot.y += rotSpeedY * dt;
        // Prevent overflow (keep angles in [0, 360))
        rot.x %= 360.0f;
        rot.y %= 360.0f;
        setRotation(rot.x, rot.y, rot.z);

        // --- Dynamic Scaling: pulse using a sine wave ---
        // scale = 1.0 + 0.2 * sin(totalTime * 5.0 + phaseOffset)
        float pulseScale = 1.0f + 0.2f * (float) Math.sin(totalTime * 5.0f + phaseOffset);
        setScale(pulseScale);
    }

    /** Bind the shared VAO and draw the wireframe cube. */
    public void render() {
        glBindVertexArray(vaoId);
        glDrawArrays(GL_LINES, 0, vertexCount);
        glBindVertexArray(0);
    }

    /** Clean up the shared mesh. Call once at shutdown. */
    public static void cleanupMesh() {
        if (!meshInitialized) return;
        glDisableVertexAttribArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDeleteBuffers(vboId);
        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);
        meshInitialized = false;
    }
}
