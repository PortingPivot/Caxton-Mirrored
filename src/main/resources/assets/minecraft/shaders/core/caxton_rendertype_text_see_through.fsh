#version 150

#moj_import <caxton_sdf.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    float distanceFactor = 4.0;
    float opacity = msdf(Sampler0, texCoord0, distanceFactor);
    vec4 color = opacity * vertexColor * ColorModulator;
    if (color.a < 0.1) {
        discard;
    }
    fragColor = color * ColorModulator;
}
