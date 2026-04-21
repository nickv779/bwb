package com.ex.bwb.cards;

import com.ex.bwb.Player;
import com.ex.bwb.game.GameController;

import gl.shaders.Texture;

public class ShakeUp extends Card {
    CardEffect effect;

    public ShakeUp(String name, String description, String tagLine, CardType type, Texture artwork, CardEffect effect) {
        super(name, description, tagLine, CardType.SHAKE_UP, artwork);
    }

    public CardEffect getEffect() { return this.effect; }

}
