package com.ex.bwb.scenes;

import android.app.Activity;
import android.opengl.GLES30;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;

import engine.activities.GLActivity;
import engine.formats.OBJFile;
import engine.materials.PhongMaterial;
import engine.models.GameObject;

public class ModelScene implements Scene {

    private final GLActivity glActivity;
    private final Activity activity;
    private GameObject model;
    private float rotationAngle = 0f;

    private float scale = 1f;
    private float[] center = new float[3];

    // Change this to your actual OBJ filename in assets/
    private static final String OBJ_FILENAME = "objects/armadillo.obj";

    public ModelScene(GLActivity glActivity) {
        this.glActivity = glActivity;
        this.activity = (Activity) glActivity;
    }

    @Override
    public void onEnter() {
        glActivity.scene.background(0.15f, 0.15f, 0.15f);
        glActivity.scene.setLightDir(0.5f, 1f, 1f);

        OBJFile objFile = new OBJFile(OBJ_FILENAME);
        model = objFile.getModel();

        // Get model bounds to center and scale it
        center = objFile.getCenter();
        float[] size = objFile.getSize();
        float maxSide = Math.max(size[0], Math.max(size[1], size[2]));
        if (maxSide > 0) {
            scale = 1.5f / maxSide; // Scale to fit comfortably in view
        } else {
            scale = 1.0f;
        }

        PhongMaterial material = new PhongMaterial();
        material.setAmbientColor(new float[]{0.8f, 0.8f, 0.8f}); // bright ambient so it shows regardless of lighting
        material.setDiffuseColor(new float[]{0.6f, 0.6f, 0.6f});
        material.setSpecularColor(new float[]{0.5f, 0.5f, 0.5f});
        material.setSpecularExponent(32f);
        model.setShader(material);

        glActivity.scene.appendChild(model);

        android.util.Log.d("MODEL", "Parts: " + objFile.parts.size());
        android.util.Log.d("MODEL", "Center: " + center[0] + " " + center[1] + " " + center[2]);
        android.util.Log.d("MODEL", "Size: " + size[0] + " " + size[1] + " " + size[2]);
        android.util.Log.d("MODEL", "Computed Scale: " + scale);
    }

    @Override
    public void onExit() {
        // Remove model from scene graph on exit
        if (model != null) {
            glActivity.scene.removeChild(model);
            model = null;
        }
    }

    @Override
    public void update() {
        // Slowly rotate the model each frame
        rotationAngle += glActivity.scene.perSec() * 45f; // 45 degrees per second
        if (rotationAngle > 360f) rotationAngle -= 360f;

        if (model != null) {
            model.transform.identity();
            // Transform order (applied right-to-left): Scale * Rotate * Translate
            model.transform.scale(scale);
            model.transform.rotateY(rotationAngle);
            model.transform.translate(-center[0], -center[1], -center[2]);
        }
    }

    @Override
    public void draw() {
        // The GameEngineScene calls root.updateGlobalPositions() and root.draw()
        // Here we just set up the camera (view matrix)
        glActivity.scene.view.identity();
        glActivity.scene.view.translate(0, 0, -3f); // Move camera back
        glActivity.scene.draw();
    }

    @Override
    public void buildUI(FrameLayout overlay) {
        Button backBtn = new Button(activity);
        backBtn.setText("Back");

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
