package com.ex.bwb;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.ex.bwb.scenes.*;
import engine.activities.GLActivity;

public class Game extends GLActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSurfaceView().post(this::buildUI);
    }

    private void buildUI() {
        ConstraintLayout root = (ConstraintLayout) getSurfaceView().getParent();

        FrameLayout overlay = new FrameLayout(this);
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
        );
        overlay.setLayoutParams(params);
        root.addView(overlay);
        overlay.bringToFront();
        root.invalidate();

        SceneManager.get().init(this, overlay);
        SceneManager.get().transitionTo(new MainMenu(this));
    }

    @Override
    public void Start() {}

    @Override
    public void Update() {
        // This is called by scene.update() -> activity.Update()
        SceneManager.get().update();
    }

    @Override
    public void onDrawFrame(javax.microedition.khronos.opengles.GL10 unused) {
        // Step 1: Update the engine scene.
        // This calculates timing, calls Game.Update() (and thus SceneManager.update()),
        // and most importantly, it uploads model/normal matrices for all GameObjects.
        scene.update();

        // Step 2: Draw the current scene via SceneManager.
        // SceneManager calls ModelScene.draw(), which sets the camera and calls scene.draw().
        SceneManager.get().draw();
    }

    @Override
    public void destroy() {
        SceneManager.get().release();
    }
}
