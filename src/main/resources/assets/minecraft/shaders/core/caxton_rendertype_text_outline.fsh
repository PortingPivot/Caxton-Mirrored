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
in vec4 vertexColor0;
in vec4 vertexColor1;
in vec2 texCoord0;

out vec4 fragColor;

float screenPxRange() {
    // More expensive calculation; perhaps switch based on graphics settings?
    // vec2 screenTexSize = vec2(1.0) / length(vec2(length(dFdx(texCoord0)), length(dFdy(texCoord0))));
    vec2 screenTexSize = vec2(1.0) / fwidth(texCoord0);
    return max(0.5 * dot(vec2(UnitRange), screenTexSize), 1.0);
}

void main() {
    float range = screenPxRange();
    float innerOpacity = msdf(Sampler0, texCoord0, range);
    float outerOpacity = sdf0(Sampler0, texCoord0, range);
    vec4 outerColor = vertexColor1 * ColorModulator;
    outerColor.a *= outerOpacity;
    vec4 color = mix(outerColor, vertexColor0, innerOpacity);
    if (color.a < 0.1) discard;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
//     fragColor = vec4(fragColor.rgb * fragColor.a, 1.0);
}
