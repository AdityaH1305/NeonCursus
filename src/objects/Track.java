package objects;

import static org.lwjgl.opengl.GL30.*;

public class Track extends GameObject {
    private int vaoId;
    private int vboId;
    private int vertexCount;

    public Track() {
        super(); // Calls the GameObject constructor to set up our position and rotation
    }

    public void init() {
        // Let's create a grid tile that is 20 units wide and 20 units deep.
        int width = 20;
        int depth = 20;

        // Calculate the array size: (Horizontal Lines + Vertical Lines) * 2 points per
        // line * 3 coordinates (x,y,z)
        int numLines = (depth + 1) + (width + 1);
        float[] vertices = new float[numLines * 2 * 3];
        int index = 0;

        // 1. Generate Horizontal lines (running left to right)
        for (int z = 0; z <= depth; z++) {
            vertices[index++] = -width / 2.0f; // Left X
            vertices[index++] = 0.0f; // Y
            vertices[index++] = -z; // Z

            vertices[index++] = width / 2.0f; // Right X
            vertices[index++] = 0.0f; // Y
            vertices[index++] = -z; // Z
        }

        // 2. Generate Vertical lines (running front to back)
        for (int x = -width / 2; x <= width / 2; x++) {
            vertices[index++] = x; // X
            vertices[index++] = 0.0f; // Y
            vertices[index++] = 0.0f; // Front Z

            vertices[index++] = x; // X
            vertices[index++] = 0.0f; // Y
            vertices[index++] = -depth; // Back Z
        }

        vertexCount = vertices.length / 3;

        // --- VAO and VBO Setup (Just like our triangle!) ---
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

    public void render() {
        glBindVertexArray(vaoId);
        // CRITICAL: We use GL_LINES instead of GL_TRIANGLES to get the wireframe look
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