#version 150

#moj_import <fog.glsl>
#moj_import <caxton_sdf.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform float UnitRange;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

float screenPxRange() {
    // More expensive calculation; perhaps switch based on graphics settings?
    // vec2 screenTexSize = vec2(1.0) / length(vec2(length(dFdx(texCoord0)), length(dFdy(texCoord0))));
    vec2 screenTexSize = vec2(1.0) / fwidth(texCoord0);
    return max(0.5 * dot(vec2(UnitRange), screenTexSize), 1.0);
}

void main() {
    float opacity = msdf(Sampler0, texCoord0, screenPxRange());
    vec4 color = vertexColor * ColorModulator;
    color.a *= opacity;
    if (color.a < 0.1) discard;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
//     fragColor = vec4(fragColor.rgb * fragColor.a, 1.0);
}
