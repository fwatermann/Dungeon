#version 330 core
#define MAX_ATLASPAGES 32

in vec2 texCoord;
flat in int textureIndex;

uniform sampler2D uTextureAtlasPages[MAX_ATLASPAGES];

out vec4 fragColor;

void main() {
    if(textureIndex == -1) {
        fragColor = vec4(0.0, 0.0, 0.0, 0.0);
        return;
    }
    fragColor = texture(uTextureAtlasPages[textureIndex], texCoord);
}
