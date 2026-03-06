package com.ex.bwb.cards;

import com.ex.bwb.GameState;
import com.ex.bwb.Player;

public interface CardEffect {
    void execute(Player player, GameState state);
}
