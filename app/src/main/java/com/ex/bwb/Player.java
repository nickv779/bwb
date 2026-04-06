package com.ex.bwb;

import com.ex.bwb.cards.BigBuddy;
import com.ex.bwb.cards.Card;
import com.ex.bwb.cards.LilBuddy;

import java.util.ArrayList;
import java.util.List;

public class Player {

    int maxHP;
    int currHP;
    int maxAP;
    int currAP;
    boolean doubleDraw;

    boolean showHand;
    BigBuddy bigBuddy;
    LilBuddy[] lilBuddies;
    List<Card> hand;
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

    public void changeHealth(int amount) { this.currHP += amount; }

    public void addCard(Card card) { hand.add(card); }

    public Card removeCard(int card) {
        Card specificCard = hand.get(card);
        hand.remove(card);
        return specificCard;
    }


    public void stopBenefits(int cooldown){ cooldown++; } //temporary TODO: proper implementation

    public void showHand(boolean showHand){ this.showHand = showHand; }
}
