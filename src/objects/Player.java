package objects;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_POINTS;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.glfw.GLFW.*;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;

/**
 * Player.java — Phase 3: Smooth lane shifting, dynamic banking, and speed trails.
 *
 * Renders as a neon wireframe pyramid. Instead of snapping instantly to a lane,
 * the X position is interpolated (lerped) each frame toward the target lane.
 * While transitioning, the ship banks (rotates on the Z-axis) to give the
 * impression of leaning into the turn.
 *
 * NEW — Speed Trails: Maintains a rolling history of the last 15 positions.
 * These are rendered as GL_POINTS with fading transparency, creating a
 * sweeping neon light trail whenever the player changes lanes.
 *
 * Lane positions on X-axis: LEFT = -2.0, CENTER = 0.0, RIGHT = 2.0
 */
public class Player extends GameObject {
    private int vaoId;
    private int vboId;
    private int vertexCount;

    // --- Lane System ---
    private static final float[] LANES = { -2.0f, 0.0f, 2.0f }; // Left, Center, Right
    private int currentLane = 1; // Start in the center lane (index 1)

    // --- Smooth Movement (Lerp) ---
    private float targetX;           // The X-coordinate of the desired lane
    private float currentX;          // The actual, smoothly-interpolated X position
    private float smoothingSpeed = 8.0f; // Higher = snappier transitions

    // --- Dynamic Banking ---
    private float currentBankAngle = 0.0f;   // Current Z-rotation in degrees
    private float maxBankAngle    = 25.0f;   // Maximum tilt angle during a lane change
    private float bankReturnSpeed = 6.0f;    // How fast the ship levels out

    // Input debounce: prevents multiple lane switches per key press
    private boolean leftPressed = false;
    private boolean rightPressed = false;

    // ===== SPEED TRAIL SYSTEM =====
    // We store the player's last N world positions. Each frame during update(),
    // a new snapshot is pushed and the oldest is discarded if we exceed the cap.
    private static final int MAX_TRAIL_POSITIONS = 15;
    private List<Vector3f> trailPositions = new ArrayList<>();

    // GPU resources for drawing the trail as GL_POINTS
    private int trailVaoId;
    private int trailVboId;

    // ===== INVINCIBILITY BLINK =====
    // When true, Renderer draws the player. When false, drawing is skipped
    // to create a rapid on/off blink effect during the grace period.
    private boolean visible = true;

    public Player() {
        super();
    }

    /**
     * Builds a pyramid mesh out of GL_LINES for the wireframe aesthetic.
     * The pyramid has a square base (4 edges) and 4 edges from the base
     * corners to the apex.
     * Also sets up the trail VAO/VBO for speed trail rendering.
     */
    public void init() {
        // Pyramid dimensions
        float halfBase = 0.3f;
        float height = 0.6f;

        // 8 lines (16 endpoints) for a wireframe pyramid:
        // 4 base edges + 4 edges from base corners to apex
        float[] vertices = {
                // --- Base edges (square at Y=0) ---
                -halfBase, 0.0f, -halfBase, halfBase, 0.0f, -halfBase, // front edge
                halfBase, 0.0f, -halfBase, halfBase, 0.0f, halfBase, // right edge
                halfBase, 0.0f, halfBase, -halfBase, 0.0f, halfBase, // back edge
                -halfBase, 0.0f, halfBase, -halfBase, 0.0f, -halfBase, // left edge

                // --- Edges from base to apex ---
                -halfBase, 0.0f, -halfBase, 0.0f, height, 0.0f, // front-left to apex
                halfBase, 0.0f, -halfBase, 0.0f, height, 0.0f, // front-right to apex
                halfBase, 0.0f, halfBase, 0.0f, height, 0.0f, // back-right to apex
                -halfBase, 0.0f, halfBase, 0.0f, height, 0.0f, // back-left to apex
        };

        vertexCount = vertices.length / 3; // 16 vertices (8 lines)

        // --- VAO / VBO setup for the pyramid mesh ---
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        // --- Trail VAO / VBO setup (dynamic buffer, re-uploaded each frame) ---
        trailVaoId = glGenVertexArrays();
        glBindVertexArray(trailVaoId);

        trailVboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, trailVboId);
        // Pre-allocate buffer for MAX_TRAIL_POSITIONS points (3 floats each)
        glBufferData(GL_ARRAY_BUFFER, MAX_TRAIL_POSITIONS * 3 * Float.BYTES, GL_DYNAMIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        // Place the player at the center lane, sitting on the track floor
        currentX = LANES[currentLane];
        targetX  = currentX;
        setPosition(currentX, -1.0f, -3.0f);
    }

    /**
     * Poll GLFW keys and SET the target lane.
     * The actual position is NOT changed here; that happens in update().
     *
     * @param windowHandle the GLFW window handle (needed by glfwGetKey)
     */
    public void handleInput(long windowHandle) {
        // --- LEFT (A or LEFT_ARROW) ---
        if (glfwGetKey(windowHandle, GLFW_KEY_A) == GLFW_PRESS
                || glfwGetKey(windowHandle, GLFW_KEY_LEFT) == GLFW_PRESS) {
            if (!leftPressed && currentLane > 0) {
                currentLane--;
                targetX = LANES[currentLane];
            }
            leftPressed = true;
        } else {
            leftPressed = false;
        }

        // --- RIGHT (D or RIGHT_ARROW) ---
        if (glfwGetKey(windowHandle, GLFW_KEY_D) == GLFW_PRESS
                || glfwGetKey(windowHandle, GLFW_KEY_RIGHT) == GLFW_PRESS) {
            if (!rightPressed && currentLane < 2) {
                currentLane++;
                targetX = LANES[currentLane];
            }
            rightPressed = true;
        } else {
            rightPressed = false;
        }
    }

    /**
     * Per-frame update: smoothly interpolate position toward the target lane,
     * apply dynamic banking rotation on the Z-axis, and record position for
     * the speed trail.
     *
     * @param dt delta time in seconds
     */
    public void update(float dt) {
        // --- 1. Smooth Lane Shifting (Lerp) ---
        // currentX = currentX + (targetX - currentX) * speed * dt
        currentX = currentX + (targetX - currentX) * smoothingSpeed * dt;

        // Snap precisely once we are close enough to avoid endless micro-drift
        if (Math.abs(targetX - currentX) < 0.005f) {
            currentX = targetX;
        }

        // Apply the smoothed X position (keep Y and Z untouched)
        setPosition(currentX, getPosition().y, getPosition().z);

        // --- 2. Dynamic Z-axis Banking ---
        float diff = targetX - currentX;

        if (Math.abs(diff) > 0.01f) {
            // We are still transitioning — bank in the direction of travel.
            // diff > 0 means moving RIGHT -> bank RIGHT (negative Z rotation)
            // diff < 0 means moving LEFT  -> bank LEFT  (positive Z rotation)
            float desiredBank = -Math.signum(diff) * maxBankAngle;
            currentBankAngle = currentBankAngle + (desiredBank - currentBankAngle) * bankReturnSpeed * dt;
        } else {
            // Centered in lane — smoothly return to level flight
            currentBankAngle = currentBankAngle + (0.0f - currentBankAngle) * bankReturnSpeed * dt;
            if (Math.abs(currentBankAngle) < 0.5f) {
                currentBankAngle = 0.0f;
            }
        }

        // Apply the bank rotation (keep X/Y rotation at 0)
        setRotation(0.0f, 0.0f, currentBankAngle);

        // --- 3. Record position snapshot for Speed Trail ---
        // Store the world-space position of the player (center of the pyramid base)
        Vector3f pos = getPosition();
        trailPositions.add(new Vector3f(pos.x, pos.y + 0.3f, pos.z)); // Slightly above base

        // Trim the list to the last MAX_TRAIL_POSITIONS entries
        while (trailPositions.size() > MAX_TRAIL_POSITIONS) {
            trailPositions.remove(0);
        }
    }

    /**
     * Reset the player to its initial state (used when restarting the game).
     */
    public void reset() {
        currentLane = 1;
        currentX = LANES[currentLane];
        targetX = currentX;
        currentBankAngle = 0.0f;
        visible = true;
        setPosition(currentX, -1.0f, -3.0f);
        setRotation(0.0f, 0.0f, 0.0f);

        // Clear the speed trail history
        trailPositions.clear();
    }

    /**
     * Update the blink state based on the invincibility timer.
     * During the grace period the player rapidly toggles visible/invisible
     * (approximately every 0.1 seconds) to signal invulnerability.
     *
     * @param invincibilityTimer remaining invincibility time (>0 = active)
     */
    public void updateInvincibility(float invincibilityTimer) {
        if (invincibilityTimer > 0.0f) {
            // Fast sine oscillation: sin(timer * 30) toggles ~5 times/sec
            visible = Math.sin(invincibilityTimer * 30.0f) > 0.0f;
        } else {
            visible = true;
        }
    }

    /** Returns whether the player should be drawn this frame. */
    public boolean isVisible() {
        return visible;
    }

    /** Draw the wireframe pyramid. */
    public void render() {
        glBindVertexArray(vaoId);
        glDrawArrays(GL_LINES, 0, vertexCount);
        glBindVertexArray(0);
    }

    /**
     * Returns the current trail position history.
     * The oldest position is at index 0, the newest at the end.
     * Used by Renderer to draw fading trail points.
     */
    public List<Vector3f> getTrailPositions() {
        return trailPositions;
    }

    /**
     * Returns the trail VAO for binding during trail rendering.
     */
    public int getTrailVaoId() {
        return trailVaoId;
    }

    /**
     * Returns the trail VBO for uploading updated position data.
     */
    public int getTrailVboId() {
        return trailVboId;
    }

    public void cleanup() {
        glDisableVertexAttribArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDeleteBuffers(vboId);
        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);

        // Clean up trail GPU resources
        glDeleteBuffers(trailVboId);
        glDeleteVertexArrays(trailVaoId);
    }
}
