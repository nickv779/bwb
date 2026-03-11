package com.ex.bwb.cards;

import com.ex.bwb.Player;
import com.ex.bwb.game.GameController;

import java.util.function.BiConsumer;

import gl.shaders.Texture;

public class Action extends Card {

    BiConsumer<Player, GameController> effect;
    public Action(String name, String description, String tagLine, Texture artwork,
                  BiConsumer<Player, GameController> effect) {
        super(name, description, tagLine, CardType.ACTION, artwork);
        this.effect = effect;
    }
}
