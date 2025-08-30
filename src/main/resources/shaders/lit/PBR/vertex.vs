#version 330 core

layout (location=0) in vec3 position;
layout (location=1) in vec2 textureCoords;
layout (location=2) in vec3 normal;
layout (location=3) in vec3 tangents;
layout (location=4) in vec3 bitangents;
layout (location=5) in mat4 instanceModelMatrix;

out vec3 fragPosition;
out vec2 UV;
out mat3 TBN;
out float fogFactor;
out vec4 shadowCoords;

//TODO: CALC TANGENT LIGHT POSITION, VIEWPOSITION AND FRAG POSITION IN VERTEX SHADER INSTEAD OF RECALCULATING FOR FRAGMENT AND JUST LET IT LERP BETWEEN AUTOMATICALLY

uniform mat4 modelMatrix;
uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;
uniform mat4 shadowSpaceMatrix;
uniform float fogDensity;
uniform float fogGradient;
uniform float shadowDistance;
uniform float shadowTransitionDistance;
uniform bool useInstancing;

void main(){
    mat4 finalModelMatrix = useInstancing ? instanceModelMatrix : modelMatrix;

    vec4 worldPosition = finalModelMatrix * vec4(position, 1.0);
    vec4 cameraObjectPosition = viewMatrix * worldPosition;

    shadowCoords = shadowSpaceMatrix * worldPosition;

    mat3 normalMatrix = transpose(inverse(mat3(finalModelMatrix)));
    vec3 T = normalize(normalMatrix * tangents);
    vec3 N = normalize(normalMatrix * normal);
    vec3 B = normalize(cross(N, T));
    TBN = mat3(T, B, N);

    gl_Position = projectionMatrix * cameraObjectPosition;

    fragPosition = worldPosition.xyz;
    UV = textureCoords;

    float cameraDistance = length(cameraObjectPosition.xyz);
    fogFactor = clamp(exp(-pow((cameraDistance*fogDensity), fogGradient)), 0.0, 1.0);

    cameraDistance -= (shadowDistance - shadowTransitionDistance);
    cameraDistance /= shadowTransitionDistance;
    shadowCoords.w = clamp(1-cameraDistance, 0, 1);
}