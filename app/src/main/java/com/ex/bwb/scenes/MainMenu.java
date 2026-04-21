package com.ex.bwb.scenes;

import android.app.Activity;
import android.opengl.GLES30;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;

import engine.activities.GLActivity;

public class MainMenu implements Scene {

    private final Activity activity;
    private final GLActivity glActivity;


    public MainMenu(GLActivity glActivity) {
        this.glActivity = glActivity;
        this.activity = (Activity) glActivity;
    }

    @Override
    public void onEnter() {
        // reset any state
    }

    @Override
    public void onExit() {
        // cleanup if needed
    }

    @Override
    public void update() {
        // per-frame logic
    }

    @Override
    public void draw() {
        GLES30.glClearColor(0.1f, 0.1f, 0.2f, 1); // dark blue
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
    }

    @Override
    public void buildUI(FrameLayout overlay) {
        Button startBtn = new Button(activity);
        startBtn.setText("Start Game");

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER;
        startBtn.setLayoutParams(params);

        // CHANGE this line in buildUI():
        startBtn.setOnClickListener(v ->
                SceneManager.get().transitionTo(new Lobby(glActivity)) // CHANGED: was ModelScene
        );

        overlay.addView(startBtn);
    }
}