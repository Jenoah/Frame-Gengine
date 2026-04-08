#version 400 core
in vec2 texCoords;
in float fogFactor;

out vec4 outColor;

struct Material {
    vec4 diffuse;
    vec4 specular;
    int hasTexture;
    float reflectance;
    float roughness;
    float metallic;
};

uniform sampler2D textureSampler;
uniform Material material;
uniform vec3 fogColor;

void main() {
    if(material.hasTexture == 1){
        outColor = texture(textureSampler, texCoords) * material.diffuse;
    }else{
        outColor = material.diffuse;
    }

    outColor.rgb = mix(vec4(fogColor, 1.0), outColor, fogFactor).rgb;
}
