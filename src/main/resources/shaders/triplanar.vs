#version 400 core

layout (location=0) in vec3 position;
layout (location=1) in vec2 textureCoords;
layout (location=2) in vec3 normal;

out vec3 vertexNormal;
out vec3 fragPosition;
out vec2 texCoords;
out float fogFactor;

uniform mat4 modelMatrix;
uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;
uniform float fogDensity;
uniform float fogGradient;

const vec3 textureScale = vec3(1);

void main(){
    vec4 worldPosition = modelMatrix * vec4(position, 1.0);
    vec4 cameraObjectPosition = viewMatrix * worldPosition;

    gl_Position = projectionMatrix * cameraObjectPosition;

    vertexNormal = normal;

    fragPosition = worldPosition.xyz;
    texCoords = textureCoords;

    float cameraDistance = length(cameraObjectPosition.xyz);
    fogFactor = clamp(exp(-pow((cameraDistance*fogDensity), fogGradient)), 0.0, 1.0);
}