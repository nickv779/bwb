package com.ex.bwb;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.ex.bwb.scenes.*;

import engine.J4Q;
import engine.activities.GLActivity;

public class Game extends GLActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSurfaceView().post(this::buildUI);
    }

    @SuppressLint("ClickableViewAccessibility")
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

        getSurfaceView().setOnTouchListener((v, event) -> {
            J4Q.touchScreen.onTouch(v, event);  // existing picking logic
            Scene current = SceneManager.get().getCurrentScene();
            if (current != null) current.onTouch(event);  // route to scene
            return true;
        });
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
        scene.update();
        renderObjectPicker();
        SceneManager.get().draw();
    }

    @Override
    public void destroy() {
        SceneManager.get().release();
    }
}
