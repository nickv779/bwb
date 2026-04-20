package com.ex.bwb.cards;

import com.ex.bwb.Player;
import com.ex.bwb.game.GameController;

public interface CardEffect {
    void apply(Player source, GameController gc);
}