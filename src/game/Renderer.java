package game;

import engine.ShaderProgram;
import engine.Utils;
import engine.Window;
import objects.Track;
import objects.Player;
import objects.Obstacle;
import objects.Stars;
import objects.LaneIndicators;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.Random;

import static org.lwjgl.opengl.GL30.*;

/**
 * Renderer.java — Phase 4: Full Visual Polish + HUD + Lane Indicators.
 *
 * Visual upgrades implemented:
 *
 * 1. "Warp Speed" FOV: The projection FOV dynamically increases with game
 *    speed, creating a tunnel-vision rush effect. Capped at 100 degrees.
 *
 * 2. "Fake Bloom" Glow: Every object is drawn TWICE — once thick and
 *    semi-transparent (the glow halo), then again thin and fully opaque
 *    (the bright core). Combined with GL_BLEND, this simulates bloom
 *    without a multi-pass framebuffer pipeline.
 *
 * 3. Player Speed Trails: The player's recent positions are drawn as
 *    fading GL_POINTS with progressively decreasing alpha, leaving a
 *    sweeping neon light trail during lane changes.
 *
 * 4. CRT Arcade Overlay: A full-screen quad drawn with a dedicated CRT
 *    shader pair applies horizontal scanlines and a vignette effect as
 *    the absolute final render step.
 *
 * 5. Camera Shake: On game-over, the view matrix is randomly jittered for
 *    a few frames to simulate a crash impact.
 *
 * 6. Background Stars: A field of white GL_POINTS rendered behind everything.
 *
 * 7. Visual Lane Indicators: 3 faint guide lines at lane positions (Phase 4).
 *
 * 8. HUD: NanoVG text overlay rendered before the CRT pass (Phase 4).
 */
public class Renderer {

    // Camera shake state
    private boolean shakeActive = false;
    private float shakeTimer = 0.0f;
    private float shakeDuration = 0.4f;  // Total shake time in seconds
    private float shakeIntensity = 0.15f; // Max pixel offset
    private Random shakeRng = new Random();

    // ===== CRT Overlay Resources =====
    private ShaderProgram crtShader;
    private int crtVaoId;
    private int crtVboId;
    private boolean crtInitialized = false;

    public Renderer() {
    }

    /**
     * Initialize the CRT overlay shader and full-screen quad.
     * Must be called after GL context is current (after Window.init()).
     */
    public void initCRT() throws Exception {
        // --- Load and compile the CRT shader pair ---
        crtShader = new ShaderProgram();
        crtShader.createVertexShader(Utils.loadResource("resources/shaders/crt.vert"));
        crtShader.createFragmentShader(Utils.loadResource("resources/shaders/crt.frag"));
        crtShader.link();
        crtShader.createUniform("resolution");

        // --- Full-screen quad in NDC: two triangles covering [-1,+1] ---
        float[] quadVertices = {
            // Triangle 1
            -1.0f, -1.0f,
             1.0f, -1.0f,
             1.0f,  1.0f,
            // Triangle 2
            -1.0f, -1.0f,
             1.0f,  1.0f,
            -1.0f,  1.0f,
        };

        crtVaoId = glGenVertexArrays();
        glBindVertexArray(crtVaoId);

        crtVboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, crtVboId);
        glBufferData(GL_ARRAY_BUFFER, quadVertices, GL_STATIC_DRAW);

        // location = 0, vec2 (2 floats per vertex)
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        crtInitialized = true;
    }

    /**
     * Trigger the camera shake effect (called once on collision).
     */
    public void triggerShake() {
        shakeActive = true;
        shakeTimer = 0.0f;
    }

    /**
     * Full-scene render with fake bloom, speed trails, lane guides, HUD, and CRT overlay.
     *
     * @param window         the game window (for aspect ratio / resolution)
     * @param shader         the neon shader program
     * @param track          the infinite grid floor
     * @param player         the player pyramid
     * @param obstacles      live obstacle list
     * @param stars          background star field
     * @param laneIndicators visual lane guide lines
     * @param hud            NanoVG HUD overlay
     * @param gameState      current game state (for HUD rendering)
     * @param score          current score (for HUD rendering)
     * @param highScore      best score (for HUD rendering)
     * @param isPaused       pause state (for HUD rendering)
     * @param speed          current game speed (drives FOV warp)
     * @param dt             delta time (for shake timer)
     */
    public void render(Window window, ShaderProgram shader, Track track,
                       Player player, List<Obstacle> obstacles, Stars stars,
                       LaneIndicators laneIndicators, HUD hud,
                       Main.GameState gameState, float score, float highScore,
                       boolean isPaused, float speed, float dt) {
        // Clear the screen using our dark purple background
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

        shader.bind();

        // === 1. Projection Matrix with "Warp Speed" FOV ===
        // FOV = 60° + currentSpeed * 0.5 (dynamically widens with speed)
        float currentFov = Math.min(60.0f + (speed * 0.5f), 100.0f);

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

        // === 3. Enable Alpha Blending for Fake Bloom ===
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Point size for stars
        glPointSize(2.0f);

        // ========== STARS (White points, drawn first / behind everything) ==========
        if (stars != null) {
            shader.setUniform("model", stars.getModelMatrix());
            shader.setUniform("neonColor", 1.0f, 1.0f, 1.0f, 1.0f); // White, full alpha
            shader.setUniform("fogColor", 0.02f, 0.01f, 0.06f);
            stars.render();
        }

        // ============================================================
        //              FAKE BLOOM: Two-pass rendering
        // ============================================================
        // Pass 1 (GLOW): Thick lines, darker, semi-transparent color.
        // Pass 2 (CORE): Thin lines, bright, fully opaque color.
        // Combined, this creates the illusion of a neon bloom effect.

        // ========== TRACK (Cyan wireframe grid) ==========
        shader.setUniform("model", track.getModelMatrix());
        shader.setUniform("fogColor", 0.02f, 0.01f, 0.06f);
        // Pass 1: Glow
        glLineWidth(6.0f);
        shader.setUniform("neonColor", 0.0f, 0.5f, 0.5f, 0.3f); // Dark cyan, 30% alpha
        track.render();
        // Pass 2: Core
        glLineWidth(2.0f);
        shader.setUniform("neonColor", 0.0f, 1.0f, 1.0f, 1.0f); // Bright cyan, full alpha
        track.render();

        // ========== LANE INDICATORS (Faint guide lines at lane X positions) ==========
        if (laneIndicators != null) {
            shader.setUniform("model", track.getModelMatrix()); // Same transform as track
            shader.setUniform("fogColor", 0.02f, 0.01f, 0.06f);
            // Pass 1: Subtle glow
            glLineWidth(3.0f);
            shader.setUniform("neonColor", 1.0f, 1.0f, 0.3f, 0.12f); // Faint yellow glow
            laneIndicators.render();
            // Pass 2: Thin core
            glLineWidth(1.0f);
            shader.setUniform("neonColor", 1.0f, 1.0f, 0.3f, 0.25f); // Slightly brighter yellow
            laneIndicators.render();
        }

        // ========== PLAYER (Magenta pyramid) ==========
        if (player != null && player.isVisible()) {
            shader.setUniform("model", player.getModelMatrix());
            // Pass 1: Glow
            glLineWidth(6.0f);
            shader.setUniform("neonColor", 0.5f, 0.0f, 0.5f, 0.3f); // Dark magenta, 30% alpha
            player.render();
            // Pass 2: Core
            glLineWidth(2.0f);
            shader.setUniform("neonColor", 1.0f, 0.0f, 1.0f, 1.0f); // Bright magenta, full alpha
            player.render();

            // ========== PLAYER SPEED TRAILS ==========
            renderSpeedTrails(shader, player);
        }

        // ========== OBSTACLES (Hot-pink cubes) ==========
        if (obstacles != null) {
            shader.setUniform("fogColor", 0.02f, 0.01f, 0.06f);
            for (Obstacle obs : obstacles) {
                shader.setUniform("model", obs.getModelMatrix());
                // Pass 1: Glow
                glLineWidth(6.0f);
                shader.setUniform("neonColor", 1.0f, 0.0f, 0.5f, 0.6f); // Bright magenta glow, 60% alpha
                obs.render();
                // Pass 2: Core
                glLineWidth(2.0f);
                shader.setUniform("neonColor", 1.0f, 0.0f, 0.5f, 1.0f); // Pure hot-pink, full alpha
                obs.render();
            }
        }

        // Reset line width to default
        glLineWidth(1.0f);
        glPointSize(1.0f);

        shader.unbind();

        // ========== HUD (NanoVG text — rendered BEFORE CRT so it gets scanlines) ==========
        if (hud != null) {
            hud.render(window, gameState, score, highScore, isPaused);
        }

        // ========== CRT ARCADE OVERLAY (final post-process) ==========
        renderCRTOverlay(window);

        // Disable blending after all draws are complete
        glDisable(GL_BLEND);
    }

    /**
     * Render the player's speed trail as fading GL_POINTS.
     * Each point corresponds to a past frame's position, with older points
     * drawn progressively more transparent to create a fade-out trail.
     */
    private void renderSpeedTrails(ShaderProgram shader, Player player) {
        List<Vector3f> trail = player.getTrailPositions();
        if (trail.isEmpty()) return;

        int count = trail.size();

        // Upload trail positions into the player's trail VBO
        float[] trailData = new float[count * 3];
        for (int i = 0; i < count; i++) {
            Vector3f p = trail.get(i);
            trailData[i * 3]     = p.x;
            trailData[i * 3 + 1] = p.y;
            trailData[i * 3 + 2] = p.z;
        }

        glBindBuffer(GL_ARRAY_BUFFER, player.getTrailVboId());
        glBufferSubData(GL_ARRAY_BUFFER, 0, trailData);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        // Use an identity model matrix (trail positions are already in world space)
        shader.setUniform("model", new Matrix4f().identity());

        // Draw each trail point with decreasing alpha (oldest = most faded)
        glPointSize(4.0f);
        glBindVertexArray(player.getTrailVaoId());

        for (int i = 0; i < count; i++) {
            // Alpha fades from nearly 0 (oldest) to ~0.8 (newest)
            float alpha = ((float)(i + 1) / count) * 0.8f;
            shader.setUniform("neonColor", 1.0f, 0.0f, 1.0f, alpha); // Magenta trail
            glDrawArrays(GL_POINTS, i, 1);
        }

        glBindVertexArray(0);
        glPointSize(1.0f);
    }

    /**
     * Draw the CRT overlay quad over the entire screen.
     * This applies scanlines and vignette as a post-process effect.
     */
    private void renderCRTOverlay(Window window) {
        if (!crtInitialized) return;

        // Disable depth testing — the overlay is always on top
        glDisable(GL_DEPTH_TEST);

        crtShader.bind();
        crtShader.setUniform("resolution",
                (float) window.getWidth(), (float) window.getHeight());

        glBindVertexArray(crtVaoId);
        glDrawArrays(GL_TRIANGLES, 0, 6); // 6 vertices = 2 triangles = full-screen quad
        glBindVertexArray(0);

        crtShader.unbind();

        // Re-enable depth testing for the next frame
        glEnable(GL_DEPTH_TEST);
    }

    /**
     * Reset the renderer state (e.g., clear shake).
     */
    public void reset() {
        shakeActive = false;
        shakeTimer = 0.0f;
    }

    /**
     * Clean up CRT overlay GPU resources.
     */
    public void cleanup() {
        if (crtInitialized) {
            glDeleteBuffers(crtVboId);
            glDeleteVertexArrays(crtVaoId);
            crtShader.cleanup();
        }
    }
}