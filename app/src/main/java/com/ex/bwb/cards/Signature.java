package com.ex.bwb.cards;

import com.ex.bwb.Player;
import com.ex.bwb.game.GameController;
import gl.shaders.Texture;

public class Signature extends Card {

    CardEffect effect;

    public Signature(String name, String description, String tagLine,
                     CardType type, Texture artwork, CardEffect effect) {
        super(name, description, tagLine, CardType.SIGNATURE, artwork);
        this.effect = effect; // FIXED: was missing
    }

    public CardEffect getEffect() { return this.effect; }
}