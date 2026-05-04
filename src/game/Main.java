package game;

import engine.ShaderProgram;
import engine.Utils;
import engine.Window;
import objects.Track;
import objects.Player;
import objects.Obstacle;
import objects.Stars;
import objects.LaneIndicators;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Main.java — Phase 4: QOL & HUD.
 *
 * Features (Phase 3 carried forward):
 * 1. State Machine: WAITING_TO_START → PLAYING → GAME_OVER → (restart) PLAYING
 * 2. Smooth lane shifting & banking (via Player.update(dt))
 * 3. Dynamic obstacle transforms: tumbling rotation + sine-wave pulsing scale
 * 4. Speed-warp FOV, camera shake, background stars, thicker wireframes
 * 5. "Fake Bloom" glow (two-pass rendering in Renderer)
 * 6. Player speed trails (fading GL_POINTS trail history)
 * 7. CRT arcade overlay (scanlines + vignette post-process)
 * 8. Dynamic resolution support (Window framebuffer callback)
 * 9. Press SPACE to start / restart; full state reset on restarts
 *
 * New in Phase 4:
 * 10. High Score Persistence — reads/writes highscore.txt
 * 11. Pause Menu — ESC toggles pause during gameplay
 * 12. Invincibility Grace Period — 1.5s of blinking invulnerability on
 * start/restart
 * 13. Visual Lane Indicators — faint guide lines at lane positions
 * 14. On-Screen HUD — NanoVG score, best, controls, state overlays
 */
public class Main {

    // ===== Game State Enum =====
    enum GameState {
        WAITING_TO_START,
        PLAYING,
        GAME_OVER
    }

    // ===== High Score File =====
    private static final String HIGH_SCORE_FILE = "highscore.txt";

    /**
     * Load the high score from file. Returns 0.0 if the file doesn't exist
     * or contains invalid data.
     */
    private static float loadHighScore() {
        try {
            if (Files.exists(Paths.get(HIGH_SCORE_FILE))) {
                String content = Files.readString(Paths.get(HIGH_SCORE_FILE)).trim();
                return Float.parseFloat(content);
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("Could not load high score, starting fresh.");
        }
        return 0.0f;
    }

    /**
     * Save the high score to file.
     */
    private static void saveHighScore(float score) {
        try {
            Files.writeString(Paths.get(HIGH_SCORE_FILE), String.format("%.1f", score));
        } catch (IOException e) {
            System.out.println("Warning: Could not save high score.");
        }
    }

    public static void main(String[] args) {
        // Create a window that is 800x600 with our retro-future name
        Window window = new Window(800, 600, "Neon Cursus");

        try {
            // Initialize GLFW and OpenGL
            window.init();
            System.out.println("Engine started successfully! Welcome to Neon Cursus.");

            // Setup Neon Shaders
            ShaderProgram shaderProgram = new ShaderProgram();

            // Read the GLSL files and compile them
            shaderProgram.createVertexShader(Utils.loadResource("resources/shaders/neon.vert"));
            shaderProgram.createFragmentShader(Utils.loadResource("resources/shaders/neon.frag"));
            shaderProgram.link();

            // Create our uniform variables for the matrices
            shaderProgram.createUniform("projection");
            shaderProgram.createUniform("model");
            shaderProgram.createUniform("view");
            shaderProgram.createUniform("neonColor"); // Now vec4 (RGBA) in the shader
            shaderProgram.createUniform("fogColor");

            // ===== Setup Renderer, Track, Player, Stars, and Obstacle mesh =====
            Renderer renderer = new Renderer();
            renderer.initCRT(); // Initialize CRT overlay (shader + full-screen quad)

            Track trackTile = new Track();
            trackTile.init();
            trackTile.setPosition(0.0f, -1.0f, 0.0f);

            Player player = new Player();
            player.init();

            Stars stars = new Stars();
            stars.init();

            LaneIndicators laneIndicators = new LaneIndicators();
            laneIndicators.init();

            HUD hud = new HUD();
            hud.init();

            Obstacle.initMesh(); // One-time shared cube VAO/VBOs

            // ===== Game State Variables =====
            double lastFrameTime = glfwGetTime();
            float totalTime = 0.0f; // Total elapsed time (for sine wave animations)

            float speed = 15.0f; // Units/second (increases over time)
            float speedIncreaseRate = 0.3f; // Speed gained per second of survival
            float initialSpeed = 15.0f; // Starting speed (for resets)

            // Obstacle spawning
            List<Obstacle> obstacles = new ArrayList<>();
            Random random = new Random();
            float spawnTimer = 0.0f; // Accumulator for spawn cooldown
            float spawnInterval = 1.2f; // Seconds between obstacle spawns
            float initialSpawnInterval = 1.2f; // For resets
            float spawnZ = -40.0f; // How far down -Z obstacles appear
            float despawnZ = 3.0f; // +Z threshold to remove obstacles
            float[] laneXPositions = { -2.0f, 0.0f, 2.0f };

            // Score & game state
            float score = 0.0f;
            int obstaclesPassed = 0;
            GameState gameState = GameState.WAITING_TO_START;

            // --- High Score ---
            float highScore = loadHighScore();
            System.out.println("High score loaded: " + String.format("%.1f", highScore));

            // --- AABB half-extents for collision ---
            // Player pyramid: roughly 0.3 wide, 0.6 tall, 0.3 deep
            float playerHalfW = 0.3f, playerHalfH = 0.6f, playerHalfD = 0.3f;
            // Obstacle cube: 0.4 half-size, 0.8 tall
            float obsHalfW = 0.4f, obsHalfH = 0.4f, obsHalfD = 0.4f;

            // --- Input debounce for SPACE ---
            boolean spacePressed = false;

            // --- Pause State ---
            boolean isPaused = false;
            boolean escPressed = false;

            // --- Invincibility Grace Period ---
            float invincibilityTimer = 0.0f;
            float INVINCIBILITY_DURATION = 1.5f;

            // Print welcome message
            System.out.println("╔══════════════════════════════════════╗");
            System.out.println("║         N E O N   C U R S U S       ║");
            System.out.println("║                                     ║");
            System.out.println("║   Press SPACE to begin your run...  ║");
            System.out.println("║   A/D or ←/→ to change lanes        ║");
            System.out.println("╚══════════════════════════════════════╝");

            // ===============================================================
            // THE CORE GAME LOOP
            // ===============================================================
            while (!window.shouldClose()) {
                double currentFrameTime = glfwGetTime();
                float deltaTime = (float) (currentFrameTime - lastFrameTime);
                lastFrameTime = currentFrameTime;

                // --------- STATE MACHINE ----------

                switch (gameState) {

                    // ==========================================================
                    // STATE: WAITING_TO_START — Scene is visible, nothing moves
                    // ==========================================================
                    case WAITING_TO_START: {
                        // Poll SPACE to transition to PLAYING
                        if (glfwGetKey(window.getWindowHandle(), GLFW_KEY_SPACE) == GLFW_PRESS) {
                            if (!spacePressed) {
                                gameState = GameState.PLAYING;
                                invincibilityTimer = INVINCIBILITY_DURATION;
                                System.out.println(">>> GO! <<<");
                            }
                            spacePressed = true;
                        } else {
                            spacePressed = false;
                        }

                        // The track still scrolls slowly for visual effect
                        org.joml.Vector3f trackPos = trackTile.getPosition();
                        trackPos.z += 5.0f * deltaTime; // Slow idle scroll
                        if (trackPos.z >= 1.0f) {
                            trackPos.z -= 1.0f;
                        }
                        trackTile.setPosition(trackPos.x, trackPos.y, trackPos.z);

                        // Update totalTime so obstacles can still animate if any exist
                        totalTime += deltaTime;
                        break;
                    }

                    // ==========================================================
                    // STATE: PLAYING — Full gameplay
                    // ==========================================================
                    case PLAYING: {
                        // --- Pause Toggle (ESC with debounce) ---
                        if (glfwGetKey(window.getWindowHandle(), GLFW_KEY_ESCAPE) == GLFW_PRESS) {
                            if (!escPressed) {
                                isPaused = !isPaused;
                                if (isPaused) {
                                    System.out.println("=== PAUSED ===");
                                } else {
                                    System.out.println("=== RESUMED ===");
                                }
                            }
                            escPressed = true;
                        } else {
                            escPressed = false;
                        }

                        // When paused, skip all game logic but keep rendering
                        if (isPaused) {
                            break;
                        }

                        totalTime += deltaTime;

                        // 1. Player input (sets target lane)
                        player.handleInput(window.getWindowHandle());

                        // 2. Smooth player position update (lerp + banking + trail recording)
                        player.update(deltaTime);

                        // 3. Update invincibility timer & player blink
                        if (invincibilityTimer > 0.0f) {
                            invincibilityTimer -= deltaTime;
                            if (invincibilityTimer < 0.0f)
                                invincibilityTimer = 0.0f;
                        }
                        player.updateInvincibility(invincibilityTimer);

                        // 4. Move the track towards the camera (+Z) for infinite scroll
                        org.joml.Vector3f trackPos = trackTile.getPosition();
                        trackPos.z += speed * deltaTime;
                        // Translational loop: snap back by 1 unit for seamless tiling
                        if (trackPos.z >= 1.0f) {
                            trackPos.z -= 1.0f;
                        }
                        trackTile.setPosition(trackPos.x, trackPos.y, trackPos.z);

                        // 5. Spawn obstacles on a timer
                        spawnTimer += deltaTime;
                        if (spawnTimer >= spawnInterval) {
                            spawnTimer = 0.0f;

                            // Pick a random lane
                            int lane = random.nextInt(3);
                            Obstacle obs = new Obstacle();
                            // Place on the track floor (Y = -1.0) far down the -Z axis
                            obs.setPosition(laneXPositions[lane], -1.0f, spawnZ);
                            obstacles.add(obs);
                        }

                        // 6. Move obstacles towards the camera, update transforms, & despawn
                        Iterator<Obstacle> it = obstacles.iterator();
                        while (it.hasNext()) {
                            Obstacle obs = it.next();
                            org.joml.Vector3f p = obs.getPosition();
                            p.z += speed * deltaTime;
                            obs.setPosition(p.x, p.y, p.z);

                            // Dynamic rotation & pulsing scale
                            obs.update(deltaTime, totalTime);

                            if (p.z > despawnZ) {
                                it.remove();
                                obstaclesPassed++;
                            }
                        }

                        // 7. AABB Collision Detection (Player vs every Obstacle)
                        // Skipped during the invincibility grace period.
                        if (invincibilityTimer <= 0.0f) {
                            org.joml.Vector3f pp = player.getPosition();
                            for (Obstacle obs : obstacles) {
                                org.joml.Vector3f op = obs.getPosition();

                                // Player AABB: center at (pp.x, pp.y + halfH, pp.z)
                                // Obstacle AABB: center at (op.x, op.y + obsHalfH, op.z)
                                boolean overlapX = Math.abs(pp.x - op.x) < (playerHalfW + obsHalfW);
                                boolean overlapY = Math
                                        .abs((pp.y + playerHalfH) - (op.y + obsHalfH)) < (playerHalfH + obsHalfH);
                                boolean overlapZ = Math.abs(pp.z - op.z) < (playerHalfD + obsHalfD);

                                if (overlapX && overlapY && overlapZ) {
                                    gameState = GameState.GAME_OVER;
                                    renderer.triggerShake(); // Camera shake on crash!

                                    // Update high score if beaten
                                    if (score > highScore) {
                                        highScore = score;
                                        saveHighScore(highScore);
                                        System.out.println("  ★ NEW HIGH SCORE! ★");
                                    }

                                    System.out.println("========================================");
                                    System.out.println("           *** GAME OVER ***");
                                    System.out
                                            .println("  Score (time survived): " + String.format("%.1f", score) + "s");
                                    System.out.println("  Obstacles dodged:      " + obstaclesPassed);
                                    System.out.println("  Final speed:           " + String.format("%.1f", speed));
                                    System.out.println(
                                            "  Best:                  " + String.format("%.1f", highScore) + "s");
                                    System.out.println("========================================");
                                    System.out.println("  Press SPACE to restart...");
                                    break;
                                }
                            }
                        }

                        // 8. Increase difficulty over time
                        speed += speedIncreaseRate * deltaTime;
                        // Also tighten spawn interval slightly (minimum 0.5s gap)
                        spawnInterval = Math.max(0.5f, spawnInterval - 0.02f * deltaTime);

                        // 9. Update score
                        score += deltaTime;
                        break;
                    }

                    // ==========================================================
                    // STATE: GAME_OVER — Scene freezes, SPACE restarts
                    // ==========================================================
                    case GAME_OVER: {
                        // Poll SPACE to restart
                        if (glfwGetKey(window.getWindowHandle(), GLFW_KEY_SPACE) == GLFW_PRESS) {
                            if (!spacePressed) {
                                // --- Full Reset ---
                                speed = initialSpeed;
                                spawnInterval = initialSpawnInterval;
                                spawnTimer = 0.0f;
                                score = 0.0f;
                                obstaclesPassed = 0;
                                totalTime = 0.0f;
                                obstacles.clear();
                                player.reset();
                                renderer.reset();
                                trackTile.setPosition(0.0f, -1.0f, 0.0f);
                                isPaused = false;
                                invincibilityTimer = INVINCIBILITY_DURATION;

                                gameState = GameState.PLAYING;
                                System.out.println(">>> RESTARTED — GO! <<<");
                            }
                            spacePressed = true;
                        } else {
                            spacePressed = false;
                        }
                        break;
                    }
                }

                // --------- RENDER (always — so the scene is visible in every state) ----------
                renderer.render(window, shaderProgram, trackTile, player, obstacles, stars,
                        laneIndicators, hud, gameState, score, highScore, isPaused,
                        speed, deltaTime);

                // Swap buffers and poll events
                window.update();
            }

            // ===== Cleanup =====
            trackTile.cleanup();
            player.cleanup();
            stars.cleanup();
            laneIndicators.cleanup();
            hud.cleanup();
            Obstacle.cleanupMesh();
            renderer.cleanup(); // Clean up CRT overlay resources

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Clean up memory and destroy the window when the loop breaks
            window.cleanup();
            System.out.println("Engine shut down safely.");
        }
    }
}