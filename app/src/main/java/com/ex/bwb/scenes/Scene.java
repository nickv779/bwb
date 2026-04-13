package com.ex.bwb.scenes;

public interface Scene {
    void onEnter();           // called once when transitioning to this scene
    void onExit();            // called once when leaving this scene
    void update();            // called every frame on GL thread
    void draw();              // called every frame on GL thread
    void buildUI(android.widget.FrameLayout overlay); // called to populate UI
}