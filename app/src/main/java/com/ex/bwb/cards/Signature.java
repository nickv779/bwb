package com.ex.bwb.cards;

import gl.shaders.Texture;

public class Signature extends Card {
    public Signature(String name, String description, String tagLine, Texture artwork) {
        super(name, description, tagLine, CardType.SIGNATURE, artwork);
    }
}
