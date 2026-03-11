package com.ex.bwb.cards;

import gl.shaders.Texture;

public class LilBuddy extends Card {

    boolean synergy;
    boolean antiSynergy;
    boolean active;
    int cooldown; // number of rotations
    public LilBuddy(String name, String description, String tagLine, CardType type, Texture artwork) {
        super(name, description, tagLine, CardType.LIL_BUDDY, artwork);
    }
}
