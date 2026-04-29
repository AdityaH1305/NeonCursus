#version 330 core
layout (location = 0) in vec3 aPos;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

out float FragDepth;

void main() {
    // The core matrix multiplication for 3D perspective
    gl_Position = projection * view * model * vec4(aPos, 1.0);
    
    // We pass the depth to the fragment shader to calculate fog
    FragDepth = gl_Position.z;
}