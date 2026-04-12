package com.ex.bwb;

import android.opengl.GLES30;
import android.os.Bundle;

import engine.activities.GLActivity;

public class Game extends GLActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void Start() {
        GLES30.glClearColor(1, 0, 0, 1);
    }

    @Override
    public void Update() {

    }

    @Override
    public void onDrawFrame(javax.microedition.khronos.opengles.GL10 unused) {
        // Bypass scene.draw() entirely — call GL directly
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
    }
}