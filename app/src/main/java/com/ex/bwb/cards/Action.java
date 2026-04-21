package com.ex.bwb.cards;

import com.ex.bwb.Player;
import com.ex.bwb.game.GameController;

import java.util.function.BiConsumer;

import gl.shaders.Texture;

public class Action extends Card {

    CardEffect effect;
    public Action(String name, String description, String tagLine, CardType type, Texture artwork,
                  CardEffect effect) {
        super(name, description, tagLine, CardType.ACTION, artwork);
        this.effect = effect;
    }

    public CardEffect getEffect() { return this.effect; }
}
