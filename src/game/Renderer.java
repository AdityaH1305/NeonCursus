package game;

import engine.ShaderProgram;
import engine.Window;
import objects.Track;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL30.*;

public class Renderer {

    public Renderer() {
    }

    public void render(Window window, ShaderProgram shader, Track track) {
        // Clear the screen using our dark purple background
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        shader.bind();

        // 1. Projection Matrix
        float aspectRatio = (float) window.getWidth() / window.getHeight();
        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(60.0f), aspectRatio, 0.01f, 100.0f);
        shader.setUniform("projection", projection);

        // 2. View Matrix (Move the camera up a bit so we can look down at the floor)
        Matrix4f view = new Matrix4f().lookAt(
                0.0f, 2.0f, 2.0f, // Camera is 2 units up and 2 units back
                0.0f, 0.0f, -5.0f, // Looking forward and slightly down
                0.0f, 1.0f, 0.0f);
        shader.setUniform("view", view);

        // 3. Model Matrix (Get it dynamically from the Track object!)
        shader.setUniform("model", track.getModelMatrix());

        // Colors
        shader.setUniform("neonColor", 0.0f, 1.0f, 1.0f); // Cyan
        shader.setUniform("fogColor", 0.05f, 0.0f, 0.1f); // Dark Purple

        // Tell the track to draw itself
        track.render();

        shader.unbind();
    }
}