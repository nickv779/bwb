package com.ex.bwb.cards;

import com.ex.bwb.Player;
import com.ex.bwb.game.GameController;

public class Effects {
    public static void BedTime(Player source, GameController gameController) {
        gameController.changeTurn(1);
    }
}