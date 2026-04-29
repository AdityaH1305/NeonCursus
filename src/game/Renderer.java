package game;

import engine.ShaderProgram;
import engine.Window;
import objects.Track;
import objects.Player;
import objects.Obstacle;
import objects.Stars;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Random;

import static org.lwjgl.opengl.GL30.*;

/**
 * Renderer.java — Phase 3: Visual upgrades.
 *
 * New features:
 * - "Speed Warp" FOV: The projection FOV dynamically increases with game speed,
 *   creating a tunnel-vision rush effect. Capped at 100 degrees.
 * - Camera Shake: On game-over, the view matrix is randomly jittered for a few
 *   frames to simulate a crash impact.
 * - Line Thickness: Thicker wireframe lines for a bolder neon aesthetic.
 * - Background Stars: A field of white GL_POINTS rendered behind everything.
 * - Point Size: Stars rendered at 2.0 pixel size for visibility through fog.
 */
public class Renderer {

    // Camera shake state
    private boolean shakeActive = false;
    private float shakeTimer = 0.0f;
    private float shakeDuration = 0.4f;  // Total shake time in seconds
    private float shakeIntensity = 0.15f; // Max pixel offset
    private Random shakeRng = new Random();

    public Renderer() {
    }

    /**
     * Trigger the camera shake effect (called once on collision).
     */
    public void triggerShake() {
        shakeActive = true;
        shakeTimer = 0.0f;
    }

    /**
     * Full-scene render: stars, track (cyan), player (magenta), obstacles (hot pink).
     *
     * @param window    the game window (for aspect ratio)
     * @param shader    the bound shader program
     * @param track     the infinite grid floor
     * @param player    the player pyramid
     * @param obstacles live obstacle list
     * @param stars     background star field
     * @param speed     current game speed (drives FOV warp)
     * @param dt        delta time (for shake timer)
     */
    public void render(Window window, ShaderProgram shader, Track track,
                       Player player, List<Obstacle> obstacles, Stars stars,
                       float speed, float dt) {
        // Clear the screen using our dark purple background
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        shader.bind();

        // === 1. Projection Matrix with "Speed Warp" FOV ===
        // Base FOV of 60° increases with speed, capped at 100°
        float baseFov = 60.0f;
        float fovBoost = (speed - 15.0f) * 0.5f; // 0 at start speed, grows over time
        float currentFov = Math.min(baseFov + Math.max(fovBoost, 0.0f), 100.0f);

        float aspectRatio = (float) window.getWidth() / window.getHeight();
        Matrix4f projection = new Matrix4f().perspective(
                (float) Math.toRadians(currentFov), aspectRatio, 0.01f, 100.0f);
        shader.setUniform("projection", projection);

        // === 2. View Matrix with Camera Shake ===
        float eyeX = 0.0f, eyeY = 2.0f;

        if (shakeActive) {
            shakeTimer += dt;
            if (shakeTimer < shakeDuration) {
                // Decaying shake: intensity reduces as the timer progresses
                float decay = 1.0f - (shakeTimer / shakeDuration);
                eyeX += (shakeRng.nextFloat() - 0.5f) * 2.0f * shakeIntensity * decay;
                eyeY += (shakeRng.nextFloat() - 0.5f) * 2.0f * shakeIntensity * decay;
            } else {
                shakeActive = false; // Shake is over
            }
        }

        Matrix4f view = new Matrix4f().lookAt(
                eyeX, eyeY, 2.0f,    // eye (with optional shake offset)
                0.0f, 0.0f, -5.0f,   // center
                0.0f, 1.0f, 0.0f);   // up
        shader.setUniform("view", view);

        // === 3. Line Thickness (platform-dependent, may be clamped to 1.0) ===
        glLineWidth(2.0f);

        // === 4. Point Size for stars ===
        glPointSize(2.0f);

        // ========== STARS (White points, drawn first / behind everything) ==========
        if (stars != null) {
            shader.setUniform("model", stars.getModelMatrix());
            shader.setUniform("neonColor", 1.0f, 1.0f, 1.0f); // White
            shader.setUniform("fogColor", 0.05f, 0.0f, 0.1f);
            stars.render();
        }

        // ========== TRACK (Cyan wireframe grid) ==========
        shader.setUniform("model", track.getModelMatrix());
        shader.setUniform("neonColor", 0.0f, 1.0f, 1.0f); // Cyan
        shader.setUniform("fogColor", 0.05f, 0.0f, 0.1f);
        track.render();

        // ========== PLAYER (Magenta pyramid) ==========
        if (player != null) {
            shader.setUniform("model", player.getModelMatrix());
            shader.setUniform("neonColor", 1.0f, 0.0f, 1.0f); // Magenta
            player.render();
        }

        // ========== OBSTACLES (Hot-pink cubes) ==========
        if (obstacles != null) {
            shader.setUniform("neonColor", 1.0f, 0.2f, 0.6f); // Hot pink
            for (Obstacle obs : obstacles) {
                shader.setUniform("model", obs.getModelMatrix());
                obs.render();
            }
        }

        // Reset line width to default
        glLineWidth(1.0f);
        glPointSize(1.0f);

        shader.unbind();
    }

    /**
     * Reset the renderer state (e.g., clear shake).
     */
    public void reset() {
        shakeActive = false;
        shakeTimer = 0.0f;
    }
}