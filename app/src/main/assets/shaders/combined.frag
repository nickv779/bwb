#version 300 es
precision mediump float;

uniform SceneMatrices
{
    mat4 ViewMatrix;
    mat4 ProjectionMatrix;
    mat4 NormalMatrix;
    vec4 uLightDir;
} sm;

uniform vec3 uAmbientColor;
uniform vec3 uDiffuseColor;
uniform vec3 uSpecularColor;
uniform float uSpecularExponent;
uniform sampler2D uTexture;

in vec3 vNormal;
in vec3 vE;
in vec3 lightDir;
in lowp vec2 vUV;

out lowp vec4 outColor;

void main()
{
    vec3 N = normalize(vNormal);
    vec3 E = normalize(vE);
    vec3 L = normalize(lightDir);

    vec4 ambient = vec4(uAmbientColor, 1.0);
    vec4 diffuse = vec4(uDiffuseColor, 1.0) * max(dot(N, L), 0.0);

    vec3 reflectionDirection = reflect(-L, N);
    vec4 specular = vec4(uSpecularColor, 1.0) * pow(max(dot(reflectionDirection, E), 0.0), uSpecularExponent);

    vec4 shade = ambient + diffuse + specular;

    outColor = texture(uTexture, vUV) * shade;
}