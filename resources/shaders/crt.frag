#version 330 core

// CRT Arcade Overlay Fragment Shader — Phase 3
// Draws directly over the framebuffer to simulate an old CRT monitor:
//   1. Horizontal scanlines (dark bands at every other pixel row)
//   2. Vignette (darkened corners, bright center)
// Both effects use low opacity so the underlying game art stays vivid.

in vec2 TexCoord;
out vec4 FragColor;

uniform vec2 resolution; // Viewport size in pixels (width, height)

void main() {
    // ============================================================
    // 1. SCANLINES — horizontal black bars with low opacity
    // ============================================================
    // Convert UV to pixel-space Y, then alternate rows.
    float pixelY   = TexCoord.y * resolution.y;
    float scanline = mod(floor(pixelY), 3.0); // cycle every 3 pixels
    // scanline == 0 on every third row → draw a dark line
    float scanMask = (scanline == 0.0) ? 0.95 : 1.0; // 5% darker on scanline rows

    // ============================================================
    // 2. VIGNETTE — darken the edges, keep the center bright
    // ============================================================
    // UV from center: (0,0) at center, distance grows toward corners
    vec2 uv = TexCoord - 0.5;

    // Slight barrel-distortion "CRT bulge" feel: exaggerate distance from center
    float dist = length(uv * vec2(1.1, 1.0)); // slightly wider horizontal
    float vignette = smoothstep(0.75, 0.35, dist); // 1.0 at center, 0.0 at edges

    // Combine: scanlines and vignette darken the image
    float darkness = scanMask * mix(0.7, 1.0, vignette);

    // Output a semi-transparent black overlay
    // darkness = 1.0 means no tint; < 1.0 darkens the underlying scene
    FragColor = vec4(0.0, 0.0, 0.0, 1.0 - darkness);
}
