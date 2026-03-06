package com.ex.bwb.cards;

import gl.shaders.Texture;

public abstract class Card {
    protected String name;
    protected String description;
    protected String tagLine;
    protected CardType type;
    protected Texture artwork;

    protected Card(String name, String description, String tagLine, CardType type, Texture artwork) {
        this.name = name;
        this.description = description;
        this.tagLine = tagLine;
        this.type = type;
        this.artwork = artwork;
    }

    // public abstract void play(GameState state, Player user);
}

