package com.ex.bwb.cards;

import com.ex.bwb.game.GameState;
import com.ex.bwb.game.Player;

public interface CardEffect {
    void execute(Player player, GameState state);
}
