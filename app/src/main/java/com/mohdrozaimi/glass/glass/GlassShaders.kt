package com.mohdrozaimi.glass.glass

/**
 * Shader AGSL untuk kesan liquid glass:
 * - tepi kad membengkokkan imej di belakang (seperti kanta)
 * - sedikit pecahan warna di tepi
 * - kilauan cahaya pada bingkai atas-kiri
 */
object GlassShaders {
    const val REFRACTION = """
uniform shader content;
uniform float2 size;
uniform float radius;
uniform float strength;

float sdRoundRect(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + float2(r, r);
    return min(max(q.x, q.y), 0.0) + length(max(q, float2(0.0, 0.0))) - r;
}

half4 main(float2 coord) {
    float2 center = size * 0.5;
    float2 p = coord - center;
    float d = sdRoundRect(p, center, radius);
    float band = max(radius * 1.4, 18.0);
    float edge = clamp(1.0 + d / band, 0.0, 1.0);
    edge = edge * edge * edge;
    float2 dir = normalize(p + float2(0.0001, 0.0001));
    float2 offs = -dir * edge * strength * 42.0;

    half4 c = content.eval(coord + offs);
    half r = content.eval(coord + offs * 1.10).r;
    half b = content.eval(coord + offs * 0.90).b;
    c = half4(r, c.g, b, c.a);

    float rim = smoothstep(-3.5, 0.0, d);
    float2 lightDir = normalize(float2(-0.55, -1.0));
    float light = clamp(dot(dir, lightDir), 0.0, 1.0);
    float glow = rim * (0.25 + 0.75 * light) * 0.55;
    c.rgb = c.rgb + half3(glow + edge * 0.06);
    return c;
}
"""
}
