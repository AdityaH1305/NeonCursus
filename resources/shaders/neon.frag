#version 330 core
out vec4 FragColor;

in float FragDepth;

// Phase 3: Now a vec4 to support alpha transparency for "fake bloom" glow passes.
// The RGB channels carry the neon color; the alpha channel controls opacity.
// Pass (0.0, 1.0, 1.0, 1.0) for solid Cyan, (1.0, 0.0, 1.0, 0.4) for dim glow, etc.
uniform vec4 neonColor;
uniform vec3 fogColor;  // Use a dark purple like (0.05, 0.0, 0.1)

void main() {
    // Simple fog: Objects fade into the background as they get further away
    float fogDensity = 0.07;
    float fogFactor = exp(-pow(FragDepth * fogDensity, 2.0));
    fogFactor = clamp(fogFactor, 0.0, 1.0);

    // Mix the neon color with the fog color based on distance
    vec3 finalColor = mix(fogColor, neonColor.rgb, fogFactor);

    // Preserve the alpha channel from the uniform (used for glow transparency)
    FragColor = vec4(finalColor, neonColor.a);
}