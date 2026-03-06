package com.ex.bwb.cards;

import gl.shaders.Texture;

public class Attack extends Card {
    public Attack(String name, String description, String tagLine, CardType type, Texture artwork) {
        super(name, description, tagLine, CardType.ATTACK, artwork);
    }
}
