#version 400 core

layout (location=0) in vec3 position;
layout (location=1) in vec2 textureCoords;
layout (location=2) in vec3 normal;

out vec3 vertexNormal;
out vec3 fragPosition;
out vec2 texCoords;
out float fogFactor;
out vec4 shadowCoords;

uniform mat4 modelMatrix;
uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;
uniform mat4 shadowSpaceMatrix;
uniform float fogDensity;
uniform float fogGradient;
uniform float shadowDistance;
uniform float shadowTransitionDistance;

void main(){
    vec4 worldPosition = modelMatrix * vec4(position, 1.0);
    vec4 cameraObjectPosition = viewMatrix * worldPosition;

    shadowCoords = shadowSpaceMatrix * worldPosition;

    gl_Position = projectionMatrix * cameraObjectPosition;

    vertexNormal = normal;

    fragPosition = worldPosition.xyz;
    texCoords = textureCoords;

    float cameraDistance = length(cameraObjectPosition.xyz);
    fogFactor = clamp(exp(-pow((cameraDistance*fogDensity), fogGradient)), 0.0, 1.0);

    cameraDistance -= (shadowDistance - shadowTransitionDistance);
    cameraDistance /= shadowTransitionDistance;
    shadowCoords.w = clamp(1-cameraDistance, 0, 1);
}