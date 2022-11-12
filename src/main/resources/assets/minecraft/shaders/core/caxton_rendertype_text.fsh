#version 150

#moj_import <fog.glsl>
#moj_import <caxton_sdf.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

float screenPxRange() {
    vec2 unitRange = vec2(2.0) / vec2(textureSize(Sampler0, 0)); // TODO: set this as a uniform instead of hardcoding
    vec2 screenTexSize = vec2(1.0) / fwidth(texCoord0);
    return max(0.5 * dot(unitRange, screenTexSize), 1.0);
}

void main() {
    float opacity = msdf(Sampler0, texCoord0, screenPxRange());
    vec4 color = vertexColor * ColorModulator;
    color.a *= opacity;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
    // fragColor = vec4(fragColor.rgb * fragColor.a, 1.0);
}
