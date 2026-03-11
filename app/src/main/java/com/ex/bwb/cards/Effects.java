package com.ex.bwb.cards;

import com.ex.bwb.Player;
import com.ex.bwb.game.GameController;

public class Effects {
    // each function should be the name of the card (can be abbreviated)
    public static void BedTime(Player source, GameController gameController) {
        gameController.changeTurn(1);
    }
}