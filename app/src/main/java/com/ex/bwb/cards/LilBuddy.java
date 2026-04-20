package com.ex.bwb.cards;

import com.ex.bwb.Player;
import com.ex.bwb.game.GameController;

import gl.shaders.Texture;

public class LilBuddy extends Card {

    boolean synergy;
    boolean antiSynergy;
    boolean active;
    int cooldown; // number of rotations
    CardEffect effect;
    public LilBuddy(String name, String description, String tagLine, CardType type, Texture artwork, CardEffect effect) {
        super(name, description, tagLine, CardType.LIL_BUDDY, artwork);
    }
    public CardEffect getEffect() { return this.effect; }

    // ADD this field to LilBuddy.java
    public boolean asleep = false;           // SleepyGiraffe

}
