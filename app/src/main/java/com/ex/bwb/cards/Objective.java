package com.ex.bwb.cards;

import gl.shaders.Texture;

public class Objective extends Card {
    boolean active;
    public Objective(String name, String description, String tagLine, CardType type, Texture artwork) {
        super(name, description, tagLine, CardType.OBJECTIVE, artwork);
    }

    public void setActive() { this.active = true; }
}
