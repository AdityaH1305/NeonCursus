package engine;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

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

        // 4. Create the window
        glfwWindow = glfwCreateWindow(width, height, title, NULL, NULL);
        if (glfwWindow == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // 5. Make the OpenGL context current
        glfwMakeContextCurrent(glfwWindow);

        // 6. Enable v-sync (locks framerate to your monitor's refresh rate)
        glfwSwapInterval(1);

        // 7. Make the window visible
        glfwShowWindow(glfwWindow);

        // 8. CRITICAL: This line is required for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        GL.createCapabilities();

        // 9. Set the clear color (Background color) - Let's use a very dark retro
        // purple
        glClearColor(0.05f, 0.0f, 0.1f, 1.0f);

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