package com.ex.bwb.scenes;

import android.app.Activity;
import android.opengl.GLES30;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;

import engine.activities.GLActivity;

public class Lobby implements Scene {

    private final Activity activity;
    private final GLActivity glActivity;

    public Lobby(GLActivity glActivity) {
        this.glActivity = glActivity;
        this.activity = (Activity) glActivity;
    }

    @Override
    public void onEnter() {
        // initialize lobby state
    }

    @Override
    public void onExit() {
        // cleanup lobby state
    }

    @Override
    public void update() {
        // lobby per-frame logic
    }

    @Override
    public void draw() {
        GLES30.glClearColor(0.2f, 0.1f, 0.1f, 1); // dark red
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
    }

    @Override
    public void buildUI(FrameLayout overlay) {
        Button backBtn = new Button(activity);
        backBtn.setText("Back to Menu");

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.bottomMargin = 100;
        backBtn.setLayoutParams(params);

        backBtn.setOnClickListener(v ->
                SceneManager.get().transitionTo(new MainMenu(glActivity))
        );

        overlay.addView(backBtn);
    }
}