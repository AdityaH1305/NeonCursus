package objects;

import static org.lwjgl.opengl.GL30.*;
import java.util.Random;

/**
 * Stars.java — Phase 3: Background star field.
 *
 * Generates a static array of random GL_POINTS scattered in the background
 * to simulate a starry night sky. The points are positioned far down the
 * -Z axis (Z = -40 to -80) so they peek through the distance fog, creating
 * a subtle vaporwave ambiance.
 */
public class Stars extends GameObject {
    private int vaoId;
    private int vboId;
    private int pointCount;

    // Number of stars to generate
    private static final int NUM_STARS = 200;

    public Stars() {
        super();
    }

    /**
     * Generate random star positions and upload them to the GPU.
     */
    public void init() {
        Random rng = new Random(42); // Fixed seed for consistent star placement
        float[] vertices = new float[NUM_STARS * 3];

        for (int i = 0; i < NUM_STARS; i++) {
            // Spread stars across a wide area
            vertices[i * 3]     = (rng.nextFloat() - 0.5f) * 40.0f; // X: -20 to +20
            vertices[i * 3 + 1] = rng.nextFloat() * 15.0f + 2.0f;   // Y: 2 to 17 (above horizon)
            vertices[i * 3 + 2] = -40.0f - rng.nextFloat() * 40.0f; // Z: -40 to -80 (far back)
        }

        pointCount = NUM_STARS;

        // --- VAO / VBO setup ---
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    /** Draw the star field as GL_POINTS. */
    public void render() {
        glBindVertexArray(vaoId);
        glDrawArrays(GL_POINTS, 0, pointCount);
        glBindVertexArray(0);
    }

    public void cleanup() {
        glDisableVertexAttribArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDeleteBuffers(vboId);
        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);
    }
}
