package com.ex.bwb.scenes;

import android.app.Activity;
import android.opengl.GLES30;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.FrameLayout;

import com.ex.bwb.cards.TextureRepository;

import engine.J4Q;
import engine.activities.GLActivity;
import engine.formats.OBJFile;
import engine.materials.PhongMaterial;
import engine.models.CardRenderer;
import engine.models.GameObject;
import engine.models.ObjectMaker;
import engine.shaders.ColorShader;
import engine.shaders.TextureNormalMapPhongShader;
import engine.shaders.TextureShader;

public class ModelScene implements Scene {

    private final GLActivity glActivity;
    private final Activity activity;
    private CardRenderer model;
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
        TextureRepository.init();
        model = new CardRenderer(TextureRepository.AbsoluteZero(), TextureRepository.BlueBack());
//        ObjectMaker objMaker = new ObjectMaker();
//
////        OBJFile objFile = new OBJFile(OBJ_FILENAME);
////        model = objFile.getModel();
////
////        // Get model bounds to center and scale it
////        center = objFile.getCenter();
////        float[] size = objFile.getSize();
////        float maxSide = Math.max(size[0], Math.max(size[1], size[2]));
////        if (maxSide > 0) {
////            scale = 1.5f / maxSide; // Scale to fit comfortably in view
////        } else {
////            scale = 1.0f;
////        }
//
////        PhongMaterial material = new PhongMaterial();
////        material.setAmbientColor(new float[]{0.8f, 0.8f, 0.8f}); // bright ambient so it shows regardless of lighting
////        material.setDiffuseColor(new float[]{0.6f, 0.6f, 0.6f});
////        material.setSpecularColor(new float[]{0.5f, 0.5f, 0.5f});
////        material.setSpecularExponent(32f);
////        model.setShader(material);
//
//        // Front face — with your texture
//        objMaker.rectangle(0.75f, 1f);
//        GameObject front = objMaker.flushModel(true, true, true, true);
//        TextureShader frontShader = new TextureShader();
//        frontShader.setTexture(TextureRepository.AbsoluteZero());
//        front.setShader(frontShader);
//
//        // Back face — different texture
//        objMaker.translate(0, 0, -0.01f); // tiny offset so they don't z-fight
//        objMaker.rotateY(180);
//        objMaker.rectangle(0.75f, 1f);
//        GameObject back = objMaker.flushModel(true, true, true, true);
//        TextureShader backShader = new TextureShader();
//        backShader.setTexture(TextureRepository.BlueBack());
//        back.setShader(backShader);
//
//        float w = 0.75f;
//        float h = 1f;
//        float d = 0.01f;
//
//        objMaker.color(0, 0, 0);
//
//        // Right
//        objMaker.pushMatrix();
//        objMaker.translate(w/2, 0, 0);
//        objMaker.rotateY(90);
//        objMaker.rectangle(d, h);
//        objMaker.popMatrix();
//
//        // Left
//        objMaker.pushMatrix();
//        objMaker.translate(-w/2, 0, 0);
//        objMaker.rotateY(-90);
//        objMaker.rectangle(d, h);
//        objMaker.popMatrix();
//
//        // Top
//        objMaker.pushMatrix();
//        objMaker.translate(0, h/2, 0);
//        objMaker.rotateX(-90);
//        objMaker.rectangle(w, d);
//        objMaker.popMatrix();
//
//        // Bottom
//        objMaker.pushMatrix();
//        objMaker.translate(0, -h/2, 0);
//        objMaker.rotateX(90);
//        objMaker.rectangle(w, d);
//        objMaker.popMatrix();
//
//        GameObject sides = objMaker.flushModel(true, false, true, false);
//        ColorShader sideShader = new ColorShader();
//        sides.setShader(sideShader);
//
//        // Parent them together so they move as one
//        model = new GameObject();
//        model.appendChild(front);
//        model.appendChild(back);
//        model.appendChild(sides);

//        TextureShader shader = new TextureShader();
//        shader.setTexture(TextureRepository.AbsoluteZero());
//        shader.setAmbientColor(0.2f, 0.2f, 0.2f);
//        shader.setDiffuseColor(1f, 1f, 1f);
//        shader.setSpecularColor(1f, 1f, 1f);
//        shader.setSpecularExponent(32);

        // model.setShader(shader);


        glActivity.scene.appendChild(model);
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
        rotationAngle += glActivity.scene.perSec() * 15f; // 45 degrees per second
        if (rotationAngle > 360f) rotationAngle -= 360f;
        if (model != null) {
            model.transform.identity();
            // Transform order (applied right-to-left): Scale * Rotate * Translate
            model.transform.scale(scale);
            model.transform.rotateY(rotationAngle);
            model.transform.translate(-center[0], -center[1], -center[2]);
        }
        if (fingerDown) {
            GameObject touched = J4Q.touchScreen.pickObject(0);
            android.util.Log.d("TOUCHSCREEN", "J4Q " + J4Q.getObjectCount());
            for (GameObject child : model.getChildren()) {
                if (touched == child) model.transform.scale(1.25f);
            }
        }
        android.util.Log.d("PICK", "fingers: " + J4Q.touchScreen.fingers_down
                + " | raw id: " + J4Q.touchScreen.pick(0)
                + " | touched: " + J4Q.touchScreen.pickObject(0)
                + " | model: " + model);
    }

    @Override
    public void draw() {
        GLES30.glClearColor(0.15f, 0.15f, 0.15f, 1f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        glActivity.scene.view.identity();
        glActivity.scene.view.translate(0, 0, -3f);
        glActivity.scene.draw();
    }

    private boolean fingerDown = false;

    @Override
    public void onTouch(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                android.util.Log.d("ModelScene", "Fingerdown True");
                fingerDown = true;
                break;
            case MotionEvent.ACTION_UP:
                android.util.Log.d("ModelScene", "Fingerdown False");
                fingerDown = false;
                break;
        }
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
