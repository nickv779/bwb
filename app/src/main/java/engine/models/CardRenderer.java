package engine.models;

import com.ex.bwb.cards.TextureRepository;

import engine.shaders.ColorShader;
import engine.shaders.Texture;
import engine.shaders.TextureShader;

public class CardRenderer extends GameObject{
    private GameObject front;
    private GameObject back;
    private GameObject sides;

    private boolean selected;
    private float scaleFactor;

    public CardRenderer(Texture frontCover, Texture backCover) {
        ObjectMaker objMaker = new ObjectMaker();

        // Front face
        objMaker.rectangle(0.75f, 1f);
        this.front = objMaker.flushModel(true, true, true, true);
        TextureShader frontShader = new TextureShader();
        frontShader.setTexture(frontCover);
        this.front.setShader(frontShader);

        // Back face
        objMaker.translate(0, 0, -0.01f); // tiny offset so they don't z-fight
        objMaker.rotateY(180);
        objMaker.rectangle(0.75f, 1f);
        this.back = objMaker.flushModel(true, true, true, true);
        TextureShader backShader = new TextureShader();
        backShader.setTexture(backCover);
        this.back.setShader(backShader);

        float w = 0.75f;
        float h = 1f;
        float d = 0.01f;

        objMaker.color(0, 0, 0);

        // Right
        objMaker.pushMatrix();
        objMaker.translate(w/2, 0, 0);
        objMaker.rotateY(90);
        objMaker.rectangle(d, h);
        objMaker.popMatrix();

        // Left
        objMaker.pushMatrix();
        objMaker.translate(-w/2, 0, 0);
        objMaker.rotateY(-90);
        objMaker.rectangle(d, h);
        objMaker.popMatrix();

        // Top
        objMaker.pushMatrix();
        objMaker.translate(0, h/2, 0);
        objMaker.rotateX(-90);
        objMaker.rectangle(w, d);
        objMaker.popMatrix();

        // Bottom
        objMaker.pushMatrix();
        objMaker.translate(0, -h/2, 0);
        objMaker.rotateX(90);
        objMaker.rectangle(w, d);
        objMaker.popMatrix();

        this.sides = objMaker.flushModel(true, false, true, false);
        ColorShader sideShader = new ColorShader();
        this.sides.setShader(sideShader);

        // Parent them together so they move as one
        this.appendChild(this.front);
        this.appendChild(this.back);
        this.appendChild(this.sides);
    }

    public boolean isHit(GameObject touched) {
        return touched == front || touched == back || touched == sides;
    }

    public void onSelected() {
        selected = !selected;
        scaleFactor = selected ? 1.25f : 1f;
    }

    public void tick() {
        this.transform.identity();
        this.transform.scale(scaleFactor);
    }
}
