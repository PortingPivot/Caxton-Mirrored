float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

float msdf(sampler2D tex, vec2 uv, float distanceFactor) {
    vec3 samp = texture(tex, uv).rgb;
    float sigDist = distanceFactor * (median(samp.r, samp.g, samp.b) - 0.5);
    return clamp(sigDist + 0.5, 0.0, 1.0);
}

float sdf(sampler2D tex, vec2 uv, float distanceFactor) {
    float samp = texture(tex, uv).a;
    float sigDist = distanceFactor * (samp - 0.5);
    return clamp(sigDist + 0.5, 0.0, 1.0);
}

float sdf0(sampler2D tex, vec2 uv, float distanceFactor) {
    float samp = texture(tex, uv).a;
    float sigDist = distanceFactor * samp;
    return clamp(sigDist, 0.0, 1.0);
}
