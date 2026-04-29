#version 330 core
out vec4 FragColor;

in float FragDepth;

uniform vec3 neonColor; // Pass (0.0, 1.0, 1.0) for Cyan or (1.0, 0.0, 1.0) for Magenta
uniform vec3 fogColor;  // Use a dark purple like (0.1, 0.0, 0.2)

void main() {
    // Simple fog: Objects fade into the background as they get further away
    float fogDensity = 0.07;
    float fogFactor = exp(-pow(FragDepth * fogDensity, 2.0));
    fogFactor = clamp(fogFactor, 0.0, 1.0);

    // Mix the neon color with the fog color based on distance
    vec3 finalColor = mix(fogColor, neonColor, fogFactor);
    FragColor = vec4(finalColor, 1.0);
}