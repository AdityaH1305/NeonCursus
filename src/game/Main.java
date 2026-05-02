package game;

import engine.ShaderProgram;
import engine.Utils;
import engine.Window;
import objects.Track;
import objects.Player;
import objects.Obstacle;
import objects.Stars;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Main.java — Phase 3: Full Polish, UX, and Advanced CG Transformations.
 *
 * New features over Phase 2::
 * 1. State Machine: WAITING_TO_START → PLAYING → GAME_OVER → (restart) PLAYING
 * 2. Smooth lane shifting & banking (via Player.update(dt))
 * 3. Dynamic obstacle transforms: tumbling rotation + sine-wave pulsing scale
 * 4. Speed-warp FOV, camera shake, background stars, thicker wireframes
 * 5. Press SPACE to start / restart; full state reset on restarts
 */
public class Main {

    // ===== Game State Enum =====
    enum GameState {
        WAITING_TO_START,
        PLAYING,
        GAME_OVER
    }

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

            // ===== Setup Renderer, Track, Player, Stars, and Obstacle mesh =====
            Renderer renderer = new Renderer();

            Track trackTile = new Track();
            trackTile.init();
            trackTile.setPosition(0.0f, -1.0f, 0.0f);

            Player player = new Player();
            player.init();

            Stars stars = new Stars();
            stars.init();

            Obstacle.initMesh(); // One-time shared cube VAO/VBO

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

            // --- AABB half-extents for collision ---
            // Player pyramid: roughly 0.3 wide, 0.6 tall, 0.3 deep
            float playerHalfW = 0.3f, playerHalfH = 0.6f, playerHalfD = 0.3f;
            // Obstacle cube: 0.4 half-size, 0.8 tall
            float obsHalfW = 0.4f, obsHalfH = 0.4f, obsHalfD = 0.4f;

            // --- Input debounce for SPACE ---
            boolean spacePressed = false;

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
                        totalTime += deltaTime;

                        // 1. Player input (sets target lane)
                        player.handleInput(window.getWindowHandle());

                        // 2. Smooth player position update (lerp + banking)
                        player.update(deltaTime);

                        // 3. Move the track towards the camera (+Z) for infinite scroll
                        org.joml.Vector3f trackPos = trackTile.getPosition();
                        trackPos.z += speed * deltaTime;
                        // Translational loop: snap back by 1 unit for seamless tiling
                        if (trackPos.z >= 1.0f) {
                            trackPos.z -= 1.0f;
                        }
                        trackTile.setPosition(trackPos.x, trackPos.y, trackPos.z);

                        // 4. Spawn obstacles on a timer
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

                        // 5. Move obstacles towards the camera, update transforms, & despawn
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

                        // 6. AABB Collision Detection (Player vs every Obstacle)
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

                                System.out.println("========================================");
                                System.out.println("           *** GAME OVER ***");
                                System.out.println("  Score (time survived): " + String.format("%.1f", score) + "s");
                                System.out.println("  Obstacles dodged:      " + obstaclesPassed);
                                System.out.println("  Final speed:           " + String.format("%.1f", speed));
                                System.out.println("========================================");
                                System.out.println("  Press SPACE to restart...");
                                break;
                            }
                        }

                        // 7. Increase difficulty over time
                        speed += speedIncreaseRate * deltaTime;
                        // Also tighten spawn interval slightly (minimum 0.5s gap)
                        spawnInterval = Math.max(0.5f, spawnInterval - 0.02f * deltaTime);

                        // 8. Update score
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
                        speed, deltaTime);

                // Swap buffers and poll events
                window.update();
            }

            // ===== Cleanup =====
            trackTile.cleanup();
            player.cleanup();
            stars.cleanup();
            Obstacle.cleanupMesh();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Clean up memory and destroy the window when the loop breaks
            window.cleanup();
            System.out.println("Engine shut down safely.");
        }
    }
}