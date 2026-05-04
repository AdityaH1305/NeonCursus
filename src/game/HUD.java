package game;

import engine.Window;
import org.lwjgl.nanovg.NVGColor;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * HUD.java — Phase 4: On-Screen UI via NanoVG.
 *
 * Renders all 2D text overlays using LWJGL's NanoVG bindings:
 *   - Score display (top-center)
 *   - Best/high score (top-right)
 *   - Controls hint (bottom-left)
 *   - State overlays: "PRESS SPACE TO START", "PAUSED", "GAME OVER"
 *
 * Each text element is drawn with a two-pass "fake bloom" technique:
 * first a larger, blurred, semi-transparent pass for glow, then a crisp
 * solid pass on top. This mirrors the wireframe bloom aesthetic.
 *
 * The HUD renders AFTER the 3D scene but BEFORE the CRT post-process
 * overlay, so the text also receives the scanline/vignette treatment.
 */
public class HUD {

    private long nvgContext;
    private int fontId;
    private boolean initialized = false;

    // Reusable NVGColor instances to avoid allocations per frame
    private NVGColor colorA = NVGColor.create();
    private NVGColor colorB = NVGColor.create();

    public HUD() {
    }

    /**
     * Initialize NanoVG context and load the font.
     * Must be called after the GL context is current.
     */
    public void init() throws Exception {
        // Create NanoVG context with antialiasing and stencil strokes
        nvgContext = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        if (nvgContext == 0) {
            throw new Exception("Could not create NanoVG context");
        }

        // Load Consolas (monospaced, available on all Windows installs)
        fontId = nvgCreateFont(nvgContext, "mono", "C:/Windows/Fonts/consola.ttf");
        if (fontId == -1) {
            throw new Exception("Could not load font: C:/Windows/Fonts/consola.ttf");
        }

        initialized = true;
    }

    /**
     * Render all HUD elements for the current frame.
     *
     * @param window    game window (for dimensions)
     * @param gameState current game state enum ordinal name
     * @param score     current run score
     * @param highScore all-time high score
     * @param isPaused  whether the game is currently paused
     */
    public void render(Window window, Main.GameState gameState,
                       float score, float highScore, boolean isPaused) {
        if (!initialized) return;

        int w = window.getWidth();
        int h = window.getHeight();

        // NanoVG requires specific GL state
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_STENCIL_TEST);
        glDisable(GL_DEPTH_TEST);

        nvgBeginFrame(nvgContext, w, h, 1.0f);

        // --- Score Display (top-center) ---
        if (gameState == Main.GameState.PLAYING || gameState == Main.GameState.GAME_OVER) {
            String scoreText = String.format("SCORE: %.1f", score);

            // Glow pass
            nvgFontFaceId(nvgContext, fontId);
            nvgFontSize(nvgContext, 38.0f);
            nvgTextAlign(nvgContext, NVG_ALIGN_CENTER | NVG_ALIGN_TOP);
            nvgFontBlur(nvgContext, 4.0f);
            setColor(colorA, 0.0f, 1.0f, 1.0f, 0.5f); // Cyan glow
            nvgFillColor(nvgContext, colorA);
            nvgText(nvgContext, w / 2.0f, 20.0f, scoreText);

            // Solid pass
            nvgFontBlur(nvgContext, 0.0f);
            setColor(colorA, 0.0f, 1.0f, 1.0f, 1.0f); // Bright cyan
            nvgFillColor(nvgContext, colorA);
            nvgText(nvgContext, w / 2.0f, 20.0f, scoreText);
        }

        // --- Best Score (top-right) ---
        if (gameState == Main.GameState.PLAYING || gameState == Main.GameState.GAME_OVER) {
            String bestText = String.format("BEST: %.1f", highScore);

            nvgFontSize(nvgContext, 24.0f);
            nvgTextAlign(nvgContext, NVG_ALIGN_RIGHT | NVG_ALIGN_TOP);

            // Glow pass
            nvgFontBlur(nvgContext, 3.0f);
            setColor(colorA, 1.0f, 1.0f, 0.3f, 0.4f); // Yellow glow
            nvgFillColor(nvgContext, colorA);
            nvgText(nvgContext, w - 20.0f, 25.0f, bestText);

            // Solid pass
            nvgFontBlur(nvgContext, 0.0f);
            setColor(colorA, 1.0f, 1.0f, 0.3f, 1.0f); // Bright yellow
            nvgFillColor(nvgContext, colorA);
            nvgText(nvgContext, w - 20.0f, 25.0f, bestText);
        }

        // --- Controls Hint (bottom-left) ---
        if (gameState == Main.GameState.WAITING_TO_START || gameState == Main.GameState.PLAYING) {
            String controls = "[A] \u25C0   \u25B6 [D]";

            nvgFontSize(nvgContext, 20.0f);
            nvgTextAlign(nvgContext, NVG_ALIGN_LEFT | NVG_ALIGN_BOTTOM);
            nvgFontBlur(nvgContext, 0.0f);
            setColor(colorA, 1.0f, 1.0f, 1.0f, 0.35f); // Semi-transparent white
            nvgFillColor(nvgContext, colorA);
            nvgText(nvgContext, 20.0f, h - 20.0f, controls);
        }

        // --- State Overlays (center of screen) ---

        // WAITING_TO_START overlay
        if (gameState == Main.GameState.WAITING_TO_START) {
            drawCenteredOverlay(w, h, "PRESS SPACE TO START",
                    48.0f, 1.0f, 0.0f, 1.0f); // Magenta
        }

        // PAUSED overlay
        if (isPaused && gameState == Main.GameState.PLAYING) {
            drawCenteredOverlay(w, h, "PAUSED",
                    64.0f, 1.0f, 0.0f, 1.0f); // Magenta
        }

        // GAME_OVER overlay
        if (gameState == Main.GameState.GAME_OVER) {
            drawCenteredOverlay(w, h, "GAME OVER",
                    64.0f, 1.0f, 0.2f, 0.4f); // Hot pink

            // Sub-text
            nvgFontSize(nvgContext, 28.0f);
            nvgTextAlign(nvgContext, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);

            // Glow
            nvgFontBlur(nvgContext, 3.0f);
            setColor(colorA, 1.0f, 1.0f, 1.0f, 0.3f);
            nvgFillColor(nvgContext, colorA);
            nvgText(nvgContext, w / 2.0f, h / 2.0f + 50.0f, "PRESS SPACE TO RESTART");

            // Solid
            nvgFontBlur(nvgContext, 0.0f);
            setColor(colorA, 1.0f, 1.0f, 1.0f, 0.9f);
            nvgFillColor(nvgContext, colorA);
            nvgText(nvgContext, w / 2.0f, h / 2.0f + 50.0f, "PRESS SPACE TO RESTART");
        }

        nvgEndFrame(nvgContext);

        // Restore GL state for subsequent rendering (CRT overlay)
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_STENCIL_TEST);
    }

    /**
     * Draw a large centered text with a two-pass glow effect.
     */
    private void drawCenteredOverlay(int w, int h, String text,
                                     float fontSize, float r, float g, float b) {
        nvgFontFaceId(nvgContext, fontId);
        nvgFontSize(nvgContext, fontSize);
        nvgTextAlign(nvgContext, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);

        // Glow pass
        nvgFontBlur(nvgContext, 6.0f);
        setColor(colorA, r, g, b, 0.5f);
        nvgFillColor(nvgContext, colorA);
        nvgText(nvgContext, w / 2.0f, h / 2.0f, text);

        // Solid pass
        nvgFontBlur(nvgContext, 0.0f);
        setColor(colorA, r, g, b, 1.0f);
        nvgFillColor(nvgContext, colorA);
        nvgText(nvgContext, w / 2.0f, h / 2.0f, text);
    }

    /**
     * Helper to set NVGColor RGBA values in-place.
     */
    private void setColor(NVGColor color, float r, float g, float b, float a) {
        color.r(r);
        color.g(g);
        color.b(b);
        color.a(a);
    }

    /**
     * Clean up NanoVG context and resources.
     */
    public void cleanup() {
        if (initialized) {
            colorA.free();
            colorB.free();
            nvgDelete(nvgContext);
        }
    }
}
