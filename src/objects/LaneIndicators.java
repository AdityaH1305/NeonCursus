package objects;

import static org.lwjgl.opengl.GL30.*;

/**
 * LaneIndicators.java — Phase 4: Visual Lane Guides.
 *
 * Draws 3 faint vertical guide lines running down the Z-axis at the
 * three lane positions (X = -2.0, 0.0, 2.0). These sit just above the
 * track floor (Y = 0.01) to avoid z-fighting with the grid, and extend
 * from Z = 0 to Z = -40 to match the obstacle spawn distance.
 *
 * Rendered with a low-alpha color and optional two-pass glow so they
 * subtly guide the player's eyes without overpowering the main grid.
 */
public class LaneIndicators extends GameObject {
    private int vaoId;
    private int vboId;
    private int vertexCount;

    public LaneIndicators() {
        super();
    }

    /**
     * Build 3 GL_LINES at the lane X positions.
     * Each line runs from Z=0 to Z=-40 at Y=0.01 (just above floor).
     */
    public void init() {
        float y = 0.01f;       // Slightly above track floor
        float zNear = 0.0f;    // Front end of the line
        float zFar = -40.0f;   // Back end (matches obstacle spawn Z)

        float[] lanes = { -2.0f, 0.0f, 2.0f };

        // 3 lines × 2 endpoints × 3 floats = 18 floats
        float[] vertices = new float[3 * 2 * 3];
        int idx = 0;

        for (float laneX : lanes) {
            // Near endpoint
            vertices[idx++] = laneX;
            vertices[idx++] = y;
            vertices[idx++] = zNear;

            // Far endpoint
            vertices[idx++] = laneX;
            vertices[idx++] = y;
            vertices[idx++] = zFar;
        }

        vertexCount = vertices.length / 3; // 6 vertices (3 lines)

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

    /** Draw the 3 lane guide lines. */
    public void render() {
        glBindVertexArray(vaoId);
        glDrawArrays(GL_LINES, 0, vertexCount);
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
