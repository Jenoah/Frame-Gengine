#version 400 core
in vec2 texCoords;
in float fogFactor;

out vec4 outColor;

struct Material {
    //vec4 ambient;
    vec4 diffuse;
    vec4 specular;
    int hasTexture;
    float reflectance;
    float roughness;
    float metallic;
};

uniform sampler2D albedoMap;
uniform vec3 fogColor;
uniform Material material;

void main() {
    if(material.hasTexture == 1){
        vec4 albedoTexture = texture(albedoMap, texCoords);
        outColor = albedoTexture * material.diffuse;
        outColor.a = albedoTexture.a;
    }else{
        outColor = material.diffuse;
    }

    outColor.rgb = mix(vec4(fogColor, 1.0), outColor, fogFactor).rgb;
}
