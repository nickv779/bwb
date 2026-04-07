package com.ex.bwb; // change to your package

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import engine.activities.GLActivity;

public class Game extends GLActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // GL, touch, physics all set up here

        // Set background color (called before GL surface is ready, so do it in Start())
        // Add Android UI on top of the GL canvas
        addUI();
    }

    private void addUI() {
        // GLActivity uses a ConstraintLayout as its root — we wrap it in a FrameLayout
        // so we can layer Android views on top of the GL surface
        FrameLayout overlay = new FrameLayout(this);
        overlay.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        Button btn = new Button(this);
        btn.setText("Click Me");

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL;
        params.bottomMargin = 100;
        btn.setLayoutParams(params);

        btn.setOnClickListener(v -> {
            onButtonClick();
        });

        overlay.addView(btn);

        // Add the overlay on top of everything GLActivity set up
        addContentView(overlay, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
    }

    private void onButtonClick() {
        // X happens here
        scene.background(1f, 0f, 0f); // example: turn background red on click
    }

    @Override
    public void Start() {
        // GL surface is ready here — safe to call GL stuff
        scene.background(0.1f, 0.1f, 0.1f); // dark gray background
    }

    @Override
    public void Update() {
        // Called every frame — game logic goes here
        // scene.getElapsedDisplayTime() gives you time since start
        // scene.perSec() gives you delta time for frame-rate independent movement
    }
}