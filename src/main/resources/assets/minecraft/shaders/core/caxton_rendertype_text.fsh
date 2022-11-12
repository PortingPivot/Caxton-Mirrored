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

void main() {
    float distanceFactor = 4.0; // TODO: use a proper calculation for this
    float opacity = msdf(Sampler0, texCoord0, distanceFactor);
    vec4 color = opacity * vertexColor * ColorModulator;
    if (color.a < 0.1) {
        discard;
    }
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
