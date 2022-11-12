#version 150

#moj_import <caxton_sdf.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

float screenPxRange() {
    vec2 unitRange = vec2(2.0) / vec2(textureSize(Sampler0, 0));
    vec2 screenTexSize = vec2(1.0) / fwidth(texCoord0);
    return max(0.5 * dot(unitRange, screenTexSize), 1.0);
}

void main() {
    float opacity = msdf(Sampler0, texCoord0, screenPxRange());
    vec4 color = vertexColor;
    color.a *= opacity;
    if (color.a < 0.01) discard;
    fragColor = color * ColorModulator;
}
