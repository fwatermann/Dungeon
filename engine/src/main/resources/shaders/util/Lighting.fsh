#define LIGHT_TYPE_POINT 0
#define LIGHT_TYPE_SPOTLIGHT 1
#define LIGHT_TYPE_DIRECTIONAL 2
#define LIGHT_TYPE_AMBIENT 3

#define MAX_NUMBER_LIGHTS 240
#define SPECULAR_POWER 10.0f

#include "./Material.fsh"

struct Light {
  vec3 position;
  float intensity;
  vec3 direction;
  float constant;
  vec3 color;
  float linear;
  float exponent;
  float cutOff;
  float cutOffAngle;
  int type;
};

layout (std140) uniform uLighting {
  int numLights;
  Light lights[MAX_NUMBER_LIGHTS];
};

vec4 max4(vec4 a, vec4 b) {
  return vec4(max(a.x, b.x), max(a.y, b.y), max(a.z, b.z), max(a.w, b.w));
}

vec4 min4(vec4 a, vec4 b) {
  return vec4(min(a.x, b.x), min(a.y, b.y), min(a.z, b.z), min(a.w, b.w));
}


vec4 calcLightColor(Material material, Light light, vec3 normal, vec3 worldPos, vec2 texCoord, vec3 toCamera) {
  vec4 diffuseColor = vec4(0.0f, 0.0f, 0.0f, 1.0f);
  vec4 specularColor = vec4(0.0f, 0.0f, 0.0f, 1.0f);

  vec4 inDiffColor = ((material.flags & MATERIAL_FLAG_HAS_DIFFUSE_TEXTURE) != 0 ? texture(material.diffuseTexture, texCoord) : material.diffuseColor);
  vec4 inSpecColor = ((material.flags & MATERIAL_FLAG_HAS_SPECULAR_TEXTURE) != 0 ? texture(material.specularTexture, texCoord) : material.specularColor);

  //Diffuse lighting
  float diffuseFactor = max(dot(normal, light.position - worldPos), 0.0f);
  diffuseColor = inDiffColor * vec4(light.color, 1.0f) * light.intensity * diffuseFactor;

  //Specular lighting
  vec3 fromLightDir = normalize(worldPos - light.position);
  vec3 reflectDir = normalize(reflect(-fromLightDir, normal));
  float specFactor = max(dot(toCamera, reflectDir), 0.0f);
  specFactor = pow(specFactor, SPECULAR_POWER);
  specularColor = inSpecColor * light.intensity * specFactor * material.reflectivity * vec4(light.color, 1.0f);

  vec4 output = (diffuseColor + specularColor);
  return output;
}

vec4 calcAmbientLight(Light light, Material material, vec2 texCoord) {
  vec4 ambientColor = ((material.flags & MATERIAL_FLAG_HAS_DIFFUSE_TEXTURE) != 0 ? texture(material.diffuseTexture, texCoord) : material.diffuseColor);
  return vec4(light.intensity * light.color, 1.0f) * ambientColor;
}

vec4 calcPointLight(Light light, Material material, vec3 worldPos, vec3 normal, vec2 texCoords, vec3 toCamera) {
  vec4 lightColor = calcLightColor(material, light, normal, worldPos, texCoords, toCamera);
  float dist = length(light.position - worldPos);
  float attenInv = light.constant + light.linear * dist + light.exponent * dist * dist;
  return lightColor / attenInv;
}

vec4 calcSpotLight(Light light, Material material, vec3 worldPos, vec3 normal, vec2 texCoords, vec3 toCamera) {
  vec3 lightDirection = normalize(worldPos - light.position);
  float factor = dot(lightDirection, normalize(light.direction));

  vec4 color = vec4(0, 0, 0, 0);

  if (factor > light.cutOff)
  {
    color = calcPointLight(light, material, normal, worldPos, texCoords, toCamera);
    color *= (1.0f - (1.0f - factor) / (1.0f - light.cutOff));
  }
  return color;
}

vec4 calcDirectionalLight(Light light, Material material, vec3 worldPos, vec3 normal, vec2 texCoords, vec3 toCamera) {
  vec4 diffuseColor = vec4(0.0f, 0.0f, 0.0f, 1.0f);
  vec4 inDiffColor = ((material.flags & MATERIAL_FLAG_HAS_DIFFUSE_TEXTURE) != 0 ? texture(material.diffuseTexture, texCoords) : material.diffuseColor);
  float diffuseFactor = max(dot(normal, -light.direction), 0.0f);
  return inDiffColor * vec4(light.color, 1.0f) * light.intensity * diffuseFactor;
}

vec4 calcLighting(Material material, vec2 texCoord, vec3 normal, vec3 worldPos, vec3 cameraPos) {
  vec4 color = vec4(0.0f, 0.0f, 0.0f, 1.0f);
  vec3 toCamera = normalize(cameraPos - worldPos);
  for (int i = 0; i < numLights; i++) {
    Light light = lights[i];
    switch (light.type) {
      case LIGHT_TYPE_POINT:
            color += calcPointLight(light, material, worldPos, normal, texCoord, toCamera);
            break;
      case LIGHT_TYPE_SPOTLIGHT:
            color += calcSpotLight(light, material, worldPos, normal, texCoord, toCamera);
            break;
      case LIGHT_TYPE_DIRECTIONAL:
            color += calcDirectionalLight(light, material, worldPos, normal, texCoord, toCamera);
            break;
      case LIGHT_TYPE_AMBIENT:
            color += calcAmbientLight(light, material, texCoord);
            continue;
    }
  }
  return color;
}
