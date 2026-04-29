package game;

import engine.ShaderProgram;
import engine.Utils;
import engine.Window;
import objects.Track;

public class Main {
    public static void main(String[] args) {
        // Create a window that is 800x600 with our retro-future name
        Window window = new Window(800, 600, "Neon Cursus");

        try {
            // Initialize GLFW and OpenGL
            window.init();
            System.out.println("Engine started successfully! Welcome to Neon Cursus.");

            // Setup Shaders
            ShaderProgram shaderProgram = new ShaderProgram();

            // Read the GLSL files and compile them
            shaderProgram.createVertexShader(Utils.loadResource("resources/shaders/neon.vert"));
            shaderProgram.createFragmentShader(Utils.loadResource("resources/shaders/neon.frag"));
            shaderProgram.link();

            // Create our uniform variables for the matrices
            shaderProgram.createUniform("projection");
            shaderProgram.createUniform("model");
            shaderProgram.createUniform("view");
            shaderProgram.createUniform("neonColor");
            shaderProgram.createUniform("fogColor");

            // Setup Renderer and Track
            Renderer renderer = new Renderer();
            Track trackTile = new Track();
            trackTile.init();

            // Move the tile slightly down so it's a "floor"
            trackTile.setPosition(0.0f, -1.0f, 0.0f);

            // THE CORE GAME LOOP
            while (!window.shouldClose()) {

                // Pass the track to the renderer
                renderer.render(window, shaderProgram, trackTile);

                // Swap the color buffers and poll for input events
                window.update();
            }

            // Clean up the track when done
            trackTile.cleanup();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Clean up memory and destroy the window when the loop breaks
            window.cleanup();
            System.out.println("Engine shut down safely.");
        }
    }
}