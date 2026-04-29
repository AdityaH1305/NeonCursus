package objects;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Player.java — Phase 3: Smooth lane shifting & dynamic banking.
 *
 * Renders as a neon wireframe pyramid. Instead of snapping instantly to a lane,
 * the X position is interpolated (lerped) each frame toward the target lane.
 * While transitioning, the ship banks (rotates on the Z-axis) to give the
 * impression of leaning into the turn.
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

    public Player() {
        super();
    }

    /**
     * Builds a pyramid mesh out of GL_LINES for the wireframe aesthetic.
     * The pyramid has a square base (4 edges) and 4 edges from the base
     * corners to the apex.
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

        // --- VAO / VBO setup (matches Track.java pattern) ---
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

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
     * Per-frame update: smoothly interpolate position toward the target lane
     * and apply dynamic banking rotation on the Z-axis.
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
    }

    /**
     * Reset the player to its initial state (used when restarting the game).
     */
    public void reset() {
        currentLane = 1;
        currentX = LANES[currentLane];
        targetX = currentX;
        currentBankAngle = 0.0f;
        setPosition(currentX, -1.0f, -3.0f);
        setRotation(0.0f, 0.0f, 0.0f);
    }

    /** Draw the wireframe pyramid. */
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
