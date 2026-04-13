package engine.activities;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.view.TextureView;

import androidx.constraintlayout.widget.ConstraintLayout;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import engine.J4Q;
import engine.controllers.TouchScreen;
import engine.physics.PhysicsEngine;

public abstract class GLActivity extends Activity implements GameEngineActivity, GLSurfaceView.Renderer {

    public GameEngineScene scene;
    private GLSurfaceView surfaceView;
    private TextureView textureView;

    public GLSurfaceView getSurfaceView() { return surfaceView; }
    public TextureView getTextureView() { return textureView; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        J4Q.touchScreen = new TouchScreen();

        ConstraintLayout layout = new ConstraintLayout(this);

        textureView = new TextureView(this);
        textureView.layout(0, 0, 0, 0);
        layout.addView(textureView);

        surfaceView = new GLSurfaceView(this);

        ConstraintLayout.LayoutParams svParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
        );
        surfaceView.setLayoutParams(svParams);
        layout.addView(surfaceView);

        setContentView(layout);

        surfaceView.setEGLContextClientVersion(3);
        surfaceView.setZOrderMediaOverlay(true);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        surfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);

        J4Q.activity = this;
        J4Q.physicsEngine = new PhysicsEngine();
        scene = new GameEngineScene(this);
        surfaceView.setRenderer(this);
        surfaceView.setOnTouchListener(J4Q.touchScreen);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        destroy();
    }

    public void destroy() {}

    private int screen_width = 0;
    private int screen_height = 0;

    public int getScreenWidth() { return screen_width; }
    public int getScreenHeight() { return screen_height; }

    public void onSurfaceChanged(GL10 unused, int width, int height) {
        screen_width = width;
        screen_height = height;
        GLES30.glViewport(0, 0, width, height);

        float[] mProjectionMatrix = new float[16];
        float ratio = (float) width / height;
        // Use a standard perspective matrix
        Matrix.perspectiveM(mProjectionMatrix, 0, 45.0f, ratio, 0.1f, 1000.0f);

        scene.setupProjection(mProjectionMatrix);
        J4Q.touchScreen.setup(width, height);
    }

    public void renderObjectPicker() {
        if (J4Q.touchScreen.fingers_down > 0) {
            J4Q.touchScreen.capture(scene);
        }
    }

    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        scene.start();
    }

    public void onDrawFrame(GL10 unused) {
        // Update the scene logic (per-frame animations, physics, etc.)
        scene.update();
        
        // The actual drawing is delegated to the scene, which will call root.draw()
        // and any active SceneManager scenes.
        scene.draw();
    }
}
