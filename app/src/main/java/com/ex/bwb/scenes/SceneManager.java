package com.ex.bwb.scenes;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.widget.FrameLayout;

public class SceneManager {

    @SuppressLint("StaticFieldLeak")
    private static SceneManager instance;

    private Scene currentScene;
    private FrameLayout uiOverlay;
    private Activity activity;

    private volatile boolean pendingEnter = false;
    private volatile Scene pendingScene = null;

    // Private constructor
    private SceneManager() {}

    public static SceneManager get() {
        if (instance == null) instance = new SceneManager();
        return instance;
    }

    public void init(Activity activity, FrameLayout overlay) {
        this.activity = activity;
        this.uiOverlay = overlay;
    }

    // Call this from Game.onDestroy() to release context references
    public void release() {
        if (currentScene != null) currentScene.onExit();
        currentScene = null;
        uiOverlay = null;
        activity = null;
        instance = null;
    }

    public void transitionTo(Scene next) {
        if (currentScene != null) currentScene.onExit();
        currentScene = next;
        pendingEnter = true;
        pendingScene = next;

        activity.runOnUiThread(() -> {
            uiOverlay.removeAllViews();
            next.buildUI(uiOverlay);
        });
    }

    public void update() {
        if (currentScene != null) currentScene.update();
    }


    public void draw() {
        if (pendingEnter && pendingScene != null) {
            pendingEnter = false;
            pendingScene.onEnter();
            pendingScene = null;
        }
        if (currentScene != null) currentScene.draw();
    }
}