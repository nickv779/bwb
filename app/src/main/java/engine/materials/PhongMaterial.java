package engine.materials;

import android.opengl.GLES30;

import engine.shaders.Shader;

public class PhongMaterial extends Shader {

    public PhongMaterial(){
        super(
                        "#version 300 es\n" +
                        "uniform SceneMatrices\n"+
                        "{\n"+
                        "	mat4 ViewMatrix;\n"+
                        "	mat4 ProjectionMatrix;\n"+
                        "   mat4 NormalMatrix;\n"+
                        "   vec4 uLightDir;\n"+
                        "} sm;\n"+
                        "uniform mat4 modelMatrix;\n"+
                        "uniform mat4 normalMatrix;\n"+
                        "in vec3 aPosition;\n"+
                        "in vec3 aNormal;\n"+
                        "in vec3 aColor;\n"+
                        "out vec3 vNormal;\n"+
                        "out vec3 vColor;\n"+
                        "out vec3 vE;\n"+
                        "void main()\n"+
                        "{\n"+
                        "   vec4 tN = normalMatrix * vec4(aNormal, 0.0);\n" +
                        "   vNormal = normalize(tN.xyz);\n"+
                        "   vec4 p = sm.ViewMatrix * modelMatrix * vec4(aPosition, 1.0);\n"+
                        "   gl_Position = sm.ProjectionMatrix * p;\n"+
                        "   vE = normalize(-p.xyz);\n"+
                        "   vColor = aColor;\n"+
                        "}\n",

                "#version 300 es\n"+
                        "precision mediump float;\n"+
                        "uniform SceneMatrices\n"+
                        "{\n"+
                        "	mat4 ViewMatrix;\n"+
                        "	mat4 ProjectionMatrix;\n"+
                        "   mat4 NormalMatrix;\n"+
                        "   vec4 uLightDir;\n"+
                        "} sm;\n"+
                        "uniform vec3 uAmbientColor;\n"+
                        "uniform vec3 uDiffuseColor;\n"+
                        "uniform vec3 uSpecularColor;\n"+
                        "uniform float uSpecularExponent;\n"+

                        "in vec3 vColor;\n"+
                        "in vec3 vNormal;\n"+
                        "in vec3 vE;\n"+
                        "out vec4 outColor;\n"+

                        "void main()\n"+
                        "{\n"+
                        "   vec3 N = normalize(vNormal);\n"+
                        "   vec3 E = normalize(vE);\n"+
                        "   vec3 L = normalize(sm.uLightDir.xyz);\n"+
                        "   vec4 shade = vec4(uAmbientColor, 1.0);\n"+
                        "   shade += vec4(uDiffuseColor, 1.0) * max(dot(N, L), 0.0);\n"+
                        "   vec3 R = reflect(-L, N);\n"+
                        "   shade += vec4(uSpecularColor, 1.0) * pow(max(dot(R, E), 0.0), uSpecularExponent);\n"+
                        "   outColor = vec4(1.0, 1.0, 1.0, 1.0) * shade;\n"+ // Hardcoded white base color for now
                        "}\n",
                new String[]{"aPosition", "aNormal", null, null, "aColor"});

        setAmbientColor(new float[]{0.2f, 0.2f, 0.2f});
        setDiffuseColor(new float[]{0.8f, 0.8f, 0.8f});
        setSpecularColor(new float[]{1.0f, 1.0f, 1.0f});
        setSpecularExponent(50);
    }

    public void setAmbientColor(float[] color){
        int mHandle = GLES30.glGetUniformLocation(shaderProgram, "uAmbientColor");
        GLES30.glUniform3fv(mHandle,1, color,0);
    }

    public void setDiffuseColor(float[] color){
        int mHandle = GLES30.glGetUniformLocation(shaderProgram, "uDiffuseColor");
        GLES30.glUniform3fv(mHandle,1, color,0);
    }

    public void setSpecularColor(float[] color){
        int mHandle = GLES30.glGetUniformLocation(shaderProgram, "uSpecularColor");
        GLES30.glUniform3fv(mHandle,1, color,0);
    }

    public void setSpecularExponent(float exponent){
        int mHandle = GLES30.glGetUniformLocation(shaderProgram, "uSpecularExponent");
        GLES30.glUniform1f(mHandle, exponent);
    }
}
