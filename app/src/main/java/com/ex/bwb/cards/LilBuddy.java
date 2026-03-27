package com.ex.bwb.cards;

import com.ex.bwb.Player;
import com.ex.bwb.game.GameController;

import java.util.function.BiConsumer;

import gl.shaders.Texture;

public class LilBuddy extends Card {

    boolean synergy;
    boolean antiSynergy;
    boolean active;
    int cooldown; // number of rotations
    public LilBuddy(String name, String description, String tagLine, CardType type, Texture artwork, BiConsumer<Player, GameController> effect) {
        super(name, description, tagLine, CardType.LIL_BUDDY, artwork);
    }
}
