# 🌌 Neon Cursus

A 3D Vaporwave infinite runner built entirely from scratch in Java using **LWJGL 3 (OpenGL 3.3 Core Profile)** and **JOML**.

Created as a university Computer Graphics lab project, this game demonstrates core computer graphics concepts including transformations, viewing matrices, shaders, collision detection, and infinite rendering loops without relying on commercial engines like Unity or Unreal Engine.

---

# 🚀 Features & Graphics Concepts

## 🎨 Graphics Pipeline
- Custom **Vertex** and **Fragment Shaders** written in GLSL
- OpenGL 3.3 Core Profile rendering pipeline
- Perspective projection and depth testing

---

## 🔄 3D Transformations
Implementation of:
- Translation
- Rotation
- Scaling

using Model Matrices:

```math
M = T \cdot R \cdot S
```

Examples:
- Player lane movement
- Ship banking/tilting
- Obstacle rotation
- Dynamic object scaling

---

## 🎥 Camera System
- Custom View Matrix
- Perspective Projection Matrix
- Dynamic camera follow system
- Speed-based Field of View expansion

---

## ♾️ Infinite Runner System
A reusable scrolling track system creates the illusion of endless movement by continuously repositioning floor segments.

Features:
- Infinite translational loop
- Memory-efficient rendering
- Distance fog/depth cueing

---

## 💥 Collision Detection
Efficient collision system using:
- Axis-Aligned Bounding Boxes (AABB)

Collision checks are performed across:
- X-axis
- Y-axis
- Z-axis

---

## 🌈 Vaporwave Visual Style
- Neon colors
- Wireframe-style environment
- Glow effects
- Retro futuristic aesthetic

---

## ⚡ Frame-Rate Independence
All movement is calculated using Delta Time to ensure smooth gameplay regardless of frame rate.

---

# 🎮 Gameplay

The player controls a futuristic anti-gravity vehicle moving through a neon-lit track in space.

## Objective
Avoid incoming obstacles and survive as long as possible while the game speed gradually increases.

---

# 🎮 Controls

| Key | Action |
|-----|--------|
| **A / ←** | Move Left |
| **D / →** | Move Right |
| **SPACE** | Start / Restart |
| **ESC** | Pause Game |

---

# 🛠️ Technologies Used

- Java
- LWJGL 3
- OpenGL 3.3
- GLSL
- JOML (Java OpenGL Math Library)

---

# 📁 Project Structure

```plaintext
NeonCursus/
│
├── lib/
│   └── # LWJGL and JOML libraries
│
├── resources/
│   └── shaders/
│       ├── neon.vert
│       └── neon.frag
│
├── src/
│   ├── engine/
│   │   ├── Window.java
│   │   └── ShaderProgram.java
│   │
│   ├── game/
│   │   ├── Main.java
│   │   ├── Renderer.java
│   │   └── Camera.java
│   │
│   └── objects/
│       ├── GameObject.java
│       ├── Player.java
│       ├── Obstacle.java
│       └── Track.java
│
├── README.md
└── report/
    ├── screenshots/
    └── documentation.pdf
```

---

# ▶️ How to Run

## Method 1: Visual Studio Code (Recommended)

### Requirements
- Java JDK 17+
- Visual Studio Code
- Extension Pack for Java

### Steps
1. Clone or download the repository
2. Open the project folder in VS Code
3. Add all `.jar` files from `lib/` into Referenced Libraries
4. Open:

```plaintext
src/game/Main.java
```

5. Press `F5`

OR

Click the **Run** button above the `main()` method.

---

## Method 2: Command Line

### Compile

```bash
mkdir bin
javac -d bin -cp "lib/*" src/engine/*.java src/game/*.java src/objects/*.java
```

### Run (Windows)

```bash
java -cp "bin;lib/*" game.Main
```

### Run (Mac/Linux)

```bash
java -cp "bin:lib/*" game.Main
```

---

# 🎓 Academic Concepts Demonstrated

This project demonstrates understanding of:

- 3D Viewing Pipeline
- Projection Matrices
- Model/View Transformations
- GLSL Shader Programming
- Infinite Rendering Techniques
- Real-Time Rendering
- Interactive Graphics
- Collision Detection
- Depth Cueing and Fog
- OpenGL Rendering Pipeline

---

# 📸 Screenshots

_Add screenshots here_

---

# 📚 Future Improvements

Possible future additions:
- Dynamic lighting
- Bloom post-processing
- Particle effects
- Procedural obstacle generation
- Score system
- Sound effects

---

# 👨‍💻 Author

Developed as a Computer Graphics Laboratory Project.

---
