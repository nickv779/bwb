package engine.shaders;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;

import java.io.IOException;
import java.io.InputStream;

import engine.J4Q;

public class AtlasTexture extends Texture {

    public AtlasTexture(String filename, int x, int y, int width, int height) {
        super(); // allocates the GL handle and binds

        try {
            InputStream is = J4Q.activity.getAssets().open(filename);
            Bitmap atlas = BitmapFactory.decodeStream(is);

            // Crop the region we want
            Bitmap region = Bitmap.createBitmap(atlas, x, y, width, height);
            atlas.recycle();

            // Set filtering
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, gles_handle);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR);

            loadToGPU(region); // parent handles the flip + GPU upload
            region.recycle();

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error loading atlas texture.");
        }
    }

    public AtlasTexture(Bitmap bitmap) {
        super();
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, gles_handle);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR);
        loadToGPU(bitmap);
        bitmap.recycle();
    }
}