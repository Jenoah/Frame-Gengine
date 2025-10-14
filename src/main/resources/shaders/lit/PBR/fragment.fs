#version 400 core
const int MAXIMUM_POINT_LIGHTS = 5;
const int MAXIMUM_SPOT_LIGHTS = 5;
const float PI = 3.14159265359;

in vec2 UV;
in vec3 fragPosition;
//in float fogFactor;
in mat3 TBN;
in vec4 shadowCoords;

out vec4 color;

struct Material {
    vec4 diffuse;
    vec4 specular;
    int hasTexture;
    float reflectance;
    float roughness;
};

struct DirectionalLight {
    vec3 color;
    vec3 direction;
    float intensity;
};

struct PointLight {
    vec3 color;
    vec3 position;
    float intensity;
    float constant;
    float linear;
    float exponent;
};

struct SpotLight {
    vec3 color;
    vec3 position;
    float intensity;
    float constant;
    float linear;
    float exponent;
    vec3 coneDirection;
    float cutOff;
    float outerCutOff;
};

// UNIFORMS
uniform Material material;
uniform sampler2D albedoMap;
uniform sampler2D normalMap;
uniform sampler2D roughnessMap;
uniform sampler2D metallicMap;
uniform sampler2D aoMap;
uniform sampler2D shadowMap;
uniform samplerCube skyboxTexture;
uniform int hasAlbedoMap = 0;
uniform int hasNormalMap = 0;
uniform int hasRoughnessMap = 0;
uniform int hasMetallicMap = 0;
uniform int hasAOMap;
uniform float metallic = .1;
uniform float specularPower = 0;
uniform float shadowBias;
uniform int shadowPCFCount = 2;
uniform int shadowMapSize;

uniform float fogDensity;
uniform float fogGradient;

uniform int pointLightCount = 0;
uniform int spotLightCount = 0;

uniform vec3 viewPosition;
uniform vec3 ambientColor;
uniform vec3 fogColor;

uniform DirectionalLight directionalLight;
uniform PointLight pointLights[MAXIMUM_POINT_LIGHTS];
uniform SpotLight spotLights[MAXIMUM_SPOT_LIGHTS];

// HELPER FUNCTIONS (PBR)
vec3 fresnelSchlick(float cosTheta, vec3 F0)
{
    return F0 + (1.0 - F0) * pow(1.0 - cosTheta, 5.0);
}

float DistributionGGX(vec3 N, vec3 H, float roughness)
{
    float a = roughness * roughness;
    float a2 = a * a;
    float NdotH = max(dot(N, H), 0.0);
    float NdotH2 = NdotH * NdotH;

    float denom = (NdotH2 * (a2 - 1.0) + 1.0);
    denom = PI * denom * denom;

    return a2 / denom;
}

float GeometrySchlickGGX(float NdotV, float roughness)
{
    float r = roughness + 1.0;
    float k = (r * r) * 0.125;

    return NdotV / (NdotV * (1.0 - k) + k);
}

float GeometrySmith(vec3 N, vec3 V, vec3 L, float roughness)
{
    float NdotV = max(dot(N, V), 0.0);
    float NdotL = max(dot(N, L), 0.0);
    float ggx2 = GeometrySchlickGGX(NdotV, roughness);
    float ggx1 = GeometrySchlickGGX(NdotL, roughness);
    return ggx1 * ggx2;
}

// TEXTURE SAMPLING
vec4 getAlbedo()
{
    if (hasAlbedoMap == 1)
    return texture(albedoMap, UV) * material.diffuse;
    else
    return material.diffuse;
}

vec3 getNormal()
{
    if (hasNormalMap == 1) {
        vec3 normalSample = texture(normalMap, UV).rgb;
        normalSample = normalize(normalSample * 2.0 - 1.0);
        return normalize(TBN * normalSample);
    } else {
        return normalize(TBN[2]);
    }
}

float getRoughness() { return (hasRoughnessMap == 1) ? texture(roughnessMap, UV).r * material.roughness : material.roughness; }
float getMetallic()  { return (hasMetallicMap == 1)  ? texture(metallicMap, UV).r  : metallic; }
float getAO()        { return (hasAOMap == 1)        ? texture(aoMap, UV).r        : 1.0; }

// UNIFIED PBR LIGHT FUNCTION
vec3 calculatePBRLight(
vec3 lightColor,
vec3 lightDirection,
float attenuation,
vec3 N, // Normal
vec3 V, // View dir
vec3 albedo,
float roughness,
float metallic,
float shadowInfluence,
vec3 skybox // Skybox color for the reflected vector
) {
    vec3 L = normalize(lightDirection);
    vec3 H = normalize(V + L);

    float NdotL = max(dot(N, L), 0.0);
    float NdotV = max(dot(N, V), 0.0);
    float NdotH = max(dot(N, H), 0.0);
    float VdotH = max(dot(V, H), 0.0);

    // Fresnel factor (Schlick)
    vec3 F0 = mix(vec3(material.reflectance), albedo, metallic);
    vec3 F = fresnelSchlick(VdotH, F0);

    // Microfacet distribution & geometry
    float D = DistributionGGX(N, H, roughness);
    float G = GeometrySmith(N, V, L, roughness);

    // Specular
    float denom = 4.0 * NdotV * NdotL + 0.0001;
    vec3 specular = (D * G * F) / denom;

    // Diffuse
    vec3 kS = F;
    vec3 kD = (1.0 - kS) * (1.0 - metallic);
    vec3 diffuse = kD * albedo / PI;

    // *** Skybox Reflection - simple, energy conserving blend ***
    float reflectance = max(max(F0.r, F0.g), F0.b);
    float environmentFactor = (1.0 - roughness) * F.r * reflectance; // sharper with low roughness
    vec3 reflection = skybox * environmentFactor;

    // Final output
    vec3 lightResult = (diffuse + specular) * lightColor * NdotL * attenuation * shadowInfluence;

    // Lerp between normal lighting and added environment reflection
    return lightResult + reflection;
}

float calculateShadowFactor(){
    float shadowTotalTexels = (shadowPCFCount * 2.0 + 1.0);
    float shadowMapTexelSize = 1.0 / shadowMapSize;
    float shadowFactorTotal = 0.0;

    for(int x = -shadowPCFCount; x <= shadowPCFCount; x++){
        for(int y = -shadowPCFCount; y <= shadowPCFCount; y++){
            float objectNearestLight = texture(shadowMap, shadowCoords.xy + vec2(x, y) * shadowMapTexelSize).r;
            if(shadowCoords.z > objectNearestLight + shadowBias){
                shadowFactorTotal += 1.0;
            }
        }
    }

    shadowFactorTotal /= shadowTotalTexels;

    return clamp(1.0 - (shadowFactorTotal * shadowCoords.w), 0.0, 1.0);
}

// MAIN
void main()
{
    vec4 albedoSample = getAlbedo();
    vec3 albedo = albedoSample.rgb;
    vec3 N = getNormal();
    vec3 V = normalize(viewPosition - fragPosition);

    float rough = clamp((1 - getRoughness()), 0.05, 1.0); // avoid 0
    float metal = clamp(getMetallic(), 0.0, 1.0);
    float ao = getAO();

    float irradiance = 0.5f; //irradiance should by replaced by the skybox contribution factor (IBL)
    vec3 ambient = ambientColor * albedo * irradiance * ao;

    //Shadow calculation
    float shadowFactor = calculateShadowFactor();

    //Skybox
    vec3 reflectionDirection = reflect(-V, N);
    vec3 skybox = texture(skyboxTexture, reflectionDirection).rgb;

    // Directional Light
    vec3 result = ambient;
    if (directionalLight.intensity > 0.0) {
        vec3 L = normalize(-directionalLight.direction);
        float attenuation = directionalLight.intensity;
        result += calculatePBRLight(directionalLight.color, L, attenuation, N, V, albedo, rough, metal, shadowFactor, skybox);
    }

    // Point Lights
    for (int i = 0; i < pointLightCount; ++i) {
        if (pointLights[i].intensity > 0.0) {
            vec3 L = pointLights[i].position - fragPosition;
            float distance = length(L);
            float attenuation = pointLights[i].intensity /
            (pointLights[i].constant +
            pointLights[i].linear * distance +
            pointLights[i].exponent * distance * distance);
            result += calculatePBRLight(pointLights[i].color, L, attenuation, N, V, albedo, rough, metal, shadowFactor, skybox);
        }
    }

    // Spot Lights
    for (int i = 0; i < spotLightCount; ++i) {
        if (spotLights[i].intensity > 0.0) {
            vec3 L = spotLights[i].position - fragPosition;
            float distance = length(L);
            vec3 lightDir = normalize(L);
            float theta = dot(lightDir, normalize(-spotLights[i].coneDirection));
            float epsilon = spotLights[i].cutOff - spotLights[i].outerCutOff;
            float intensity = clamp((theta - spotLights[i].outerCutOff) / epsilon, 0.0, 1.0);

            float attenuation = spotLights[i].intensity /
            (spotLights[i].constant +
            spotLights[i].linear * distance +
            spotLights[i].exponent * distance * distance);

            attenuation *= intensity;

            if (attenuation > 0.0)
            result += calculatePBRLight(spotLights[i].color, L, attenuation, N, V, albedo, rough, metal, shadowFactor, skybox);
        }
    }

    // Fog
    float fogFactor = clamp(exp(-pow((length(viewPosition.xyz - fragPosition.xyz)*fogDensity), fogGradient)), 0, 1);
    result = mix(fogColor, result, fogFactor);

    color = vec4(result, albedoSample.a);
}
