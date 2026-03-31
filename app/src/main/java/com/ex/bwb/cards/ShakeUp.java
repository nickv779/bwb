package com.ex.bwb.cards;

import com.ex.bwb.Player;
import com.ex.bwb.game.GameController;

import java.util.function.BiConsumer;

import gl.shaders.Texture;

public class ShakeUp extends Card {
    public ShakeUp(String name, String description, String tagLine, CardType type, Texture artwork, BiConsumer<Player, GameController> effect) {
        super(name, description, tagLine, CardType.SHAKE_UP, artwork);
    }
}
