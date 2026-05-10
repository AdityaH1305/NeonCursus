# 🌌 Neon Cursus

A 3D Vaporwave infinite runner built entirely from scratch in Java using **LWJGL 3 (OpenGL 3.3 Core Profile)** and **JOML**. 

Created as a university Computer Graphics lab project, this engine implements raw matrix mathematics, custom GLSL shaders, and core 3D rendering algorithms without relying on commercial game engines like Unity or Unreal.

## 🚀 Features & Graphics Concepts

* **The Programmable Graphics Pipeline:** Custom Vertex and Fragment shaders (`.vert` / `.frag`) written in GLSL.
* **3D Transformations ($T \cdot R \cdot S$):** Dynamic calculation of Model Matrices to handle Translations (moving down the track), Rotations (ship banking and obstacle tumbling), and Scaling (pulsing obstacles to a beat).
* **The Camera Pipeline:** Custom View and Projection matrices, featuring dynamic FOV expansion ("Warp Speed") and randomized camera shake upon collision.
* **Infinite Translational Loops:** A memory-efficient scrolling grid that snaps backward by exactly 1.0 unit every frame, creating the illusion of endless movement.
* **Axis-Aligned Bounding Box (AABB) Collision:** Highly efficient 3D collision detection across the X, Y, and Z axes.
* **Linear Interpolation (Lerp):** Asymptotic averaging applied to X-coordinates and Z-axis rotations for buttery-smooth lane shifting and ship banking.
* **Multipass Rendering (Fake Bloom):** Objects are drawn twice per frame using `GL_BLEND` and varying `glLineWidth` to create a glowing neon "halo" around a bright core, bypassing the need for expensive Framebuffer Objects (FBOs).
* **Frame-Rate Independence (Delta Time):** All movement and physics are decoupled from CPU clock speed.
* **Arcade Polish:** CRT scanline and vignette post-processing, player speed trails, dynamic window resizing, and a crisp **NanoVG** Heads-Up Display (HUD) with high-score persistence.

## 🎮 Controls

* **[ A ] / [ ◀ ]** : Switch to Left Lane
* **[ D ] / [ ▶ ]** : Switch to Right Lane
* **[ SPACE ]** : Start Run / Restart after Game Over
* **[ ESC ]** : Pause / Resume Game

## 🛠️ How to Run the Game

This project is built using raw Java and does not use a build manager like Maven or Gradle. All necessary LWJGL and JOML libraries are bundled in the `lib/` directory.

### Method 1: Using Visual Studio Code (Recommended)
1. Ensure you have the **Extension Pack for Java** installed in VS Code.
2. Open the `NeonCursus` folder in VS Code.
3. Open `src/game/Main.java`.
4. Check the "Java Projects" tab in your explorer and ensure all `.jar` files in the `lib/` folder are added to your "Referenced Libraries".
5. Click the **"Run"** button that appears above the `public static void main(String[] args)` method, or press `F5`. 

### Method 2: Command Line (Windows)
If you prefer to compile and run the game manually via the terminal, ensure you have the JDK installed and added to your system PATH.

1. Open your terminal and navigate to the root directory of the project.
2. **Compile the source code** into a `bin` directory:
   ```bash
   mkdir bin
   javac -d bin -cp "lib/*" src/engine/*.java src/objects/*.java src/game/*.java


   NeonCursus/
├── lib/                     # LWJGL 3, JOML, and NanoVG jar files
├── resources/
│   └── shaders/             # Custom GLSL shader files (neon, crt)
├── src/
│   ├── engine/              # Core OpenGL wrapper (Window, ShaderProgram, Utils)
│   ├── game/                # Game logic (Main loop, Renderer, HUD)
│   └── objects/             # 3D GameObjects (Player, Obstacle, Track, Stars, LaneIndicators)
├── .gitignore               
├── highscore.txt            # Auto-generated save file
└── README.md                # You are here

Run the game:

Bash
java -cp "bin;lib/*" game.Main


🎓 Academic Fulfillment
This project was developed to satisfy the requirements for a Computer Graphics laboratory assignment, specifically demonstrating proficiency in 3D viewing, projections, transformations, depth cueing, and interactive rendering loops.
