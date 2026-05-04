#version 330 core

// CRT Overlay Vertex Shader — Phase 3
// A simple passthrough for the full-screen quad. The quad vertices are
// provided in Normalized Device Coordinates (-1 to +1), so no matrices
// are needed.

layout (location = 0) in vec2 aPos;

out vec2 TexCoord;

void main() {
    gl_Position = vec4(aPos, 0.0, 1.0);

    // Map NDC position [-1,+1] to UV coordinates [0,1]
    TexCoord = aPos * 0.5 + 0.5;
}
