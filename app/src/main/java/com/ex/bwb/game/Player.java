package com.ex.bwb.game;

import com.ex.bwb.cards.BigBuddy;
import com.ex.bwb.cards.Card;
import com.ex.bwb.cards.LilBuddy;

public class Player {

    int maxHP;
    int currHP;
    int maxAP;
    int currAP;
    boolean doubleDraw;
    BigBuddy bigBuddy;
    LilBuddy[] lilBuddies;
    Card[] hand;
    public Player(BigBuddy bigBuddy) {
        this.maxHP = 5;
        this.currHP = 5;
        this.maxAP = 3;
        this.currAP = 3;
        this.doubleDraw = false;
        this.bigBuddy = bigBuddy;
        this.lilBuddies = new LilBuddy[3];

        // BigBuddy abilities are parsed
        switch (bigBuddy.getName()) {
            case "Gerald":
                this.lilBuddies = new LilBuddy[4];
                break;
            case "Darrel":
                this.currHP = 6;
                break;
            case "Mr. Ostrich":
                this.maxAP = 4;
                break;
            case "Fernando":
                this.doubleDraw = true;
        }
    }
}
