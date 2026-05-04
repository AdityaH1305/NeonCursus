package engine;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Window.java — Phase 3: Dynamic Resolution Support.
 *
 * Handles GLFW window creation, input polling, and buffer swapping.
 * Now includes a framebuffer size callback so that resizing the window
 * automatically updates the OpenGL viewport and the stored dimensions.
 */
public class Window {
    private int width, height;
    private String title;
    private long glfwWindow;

    public Window(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.title = title;
    }

    public void init() {
        // 1. Setup an error callback to print errors to the console
        GLFWErrorCallback.createPrint(System.err).set();

        // 2. Initialize GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // 3. Configure GLFW for OpenGL 3.3 Core Profile (Modern OpenGL)
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // Keep hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // Allow resizing
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_STENCIL_BITS, 8); // Required for NanoVG stencil strokes

        // 4. Create the window
        glfwWindow = glfwCreateWindow(width, height, title, NULL, NULL);
        if (glfwWindow == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // 5. Dynamic Resolution: respond to window / framebuffer resize events.
        //    This callback fires whenever the user drags the window border or
        //    the OS scales the framebuffer (e.g. Retina / HiDPI).
        glfwSetFramebufferSizeCallback(glfwWindow, (window, w, h) -> {
            // Update stored dimensions so the projection matrix uses the
            // correct aspect ratio on the next frame.
            this.width  = w;
            this.height = h;

            // Tell OpenGL to use the entire new framebuffer area.
            glViewport(0, 0, w, h);
        });

        // 6. Make the OpenGL context current
        glfwMakeContextCurrent(glfwWindow);

        // 7. Enable v-sync (locks framerate to your monitor's refresh rate)
        glfwSwapInterval(1);

        // 8. Make the window visible
        glfwShowWindow(glfwWindow);

        // 9. CRITICAL: This line is required for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        GL.createCapabilities();

        // 10. Set the clear color (Background color) - Let's use a very dark retro
        // purple
        glClearColor(0.02f, 0.01f, 0.06f, 1.0f);

        // Enable Depth Testing so 3D objects don't render on top of each other weirdly
        glEnable(GL_DEPTH_TEST);
    }

    public void update() {
        // Swap the color buffers (put what we just drew onto the screen)
        glfwSwapBuffers(glfwWindow);
        // Poll for window events (like clicking the close button or pressing keys)
        glfwPollEvents();
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(glfwWindow);
    }

    public void cleanup() {
        glfwDestroyWindow(glfwWindow);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /** Returns the raw GLFW window handle (needed for input polling). */
    public long getWindowHandle() {
        return glfwWindow;
    }
}