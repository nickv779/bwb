package com.ex.bwb;

import com.ex.bwb.cards.BigBuddy;
import com.ex.bwb.cards.Card;
import com.ex.bwb.cards.LilBuddy;

import java.util.ArrayList;
import java.util.List;

public class Player {

    public int maxHP;
    public int currHP;
    public int maxAP;
    public int currAP;
    public boolean doubleDraw;
    public boolean showHand;
    public String name;                          // ADDED: needed for win messages

    public BigBuddy bigBuddy;
    public LilBuddy[] lilBuddies;
    public List<Card> hand;
    public List<Card> stash;                     // ADDED: objective cards collected

    // ADDED: alive tracking
    private boolean alive = true;

    // ADDED: effect immunity flags (set by card effects)
    public boolean cannotDrawNextTurn = false;
    private int cardEffectImmuneRotations = 0;
    private int lilBuddyImmuneRotations = 0;

    public Player(BigBuddy bigBuddy) {
        this.maxHP = 5;
        this.currHP = 5;
        this.maxAP = 3;
        this.currAP = 3;
        this.doubleDraw = false;
        this.bigBuddy = bigBuddy;
        this.lilBuddies = new LilBuddy[3];
        this.hand = new ArrayList<>();           // FIXED: was never initialized, caused NullPointerException
        this.stash = new ArrayList<>();          // ADDED

        switch (bigBuddy.getName()) {
            case "Gerald":
                this.lilBuddies = new LilBuddy[4];
                break;
            case "Darrel":
                this.maxHP = 6;                  // FIXED: was only setting currHP, not maxHP
                this.currHP = 6;
                break;
            case "Mr. Ostrich":
                this.maxAP = 4;
                this.currAP = 4;                 // FIXED: was only setting maxAP, not currAP
                break;
            case "Fernando":
                this.doubleDraw = true;
                break;
        }
    }

    public void changeHealth(int amount) {
        this.currHP = Math.max(0, Math.min(maxHP, this.currHP + amount)); // FIXED: now clamps to [0, maxHP]
    }

    public void addCard(Card card) { hand.add(card); }

    public Card removeCard(int index) {
        Card specificCard = hand.get(index);
        hand.remove(index);
        return specificCard;
    }

    // ADDED: alive state
    public boolean isAlive() { return alive && currHP > 0; }
    public void setAlive(boolean alive) { this.alive = alive; }

    // ADDED: immunity setters used by card effects
    public void setCannotDrawNextTurn(boolean value) { cannotDrawNextTurn = value; }
    public void setCardEffectImmune(int rotations) { cardEffectImmuneRotations = rotations; }
    public void setLilBuddyImmune(int rotations) { lilBuddyImmuneRotations = rotations; }

    // ADDED: immunity getters used by GameController
    public boolean isCardEffectImmune() { return cardEffectImmuneRotations > 0; }
    public boolean isLilBuddyImmune() { return lilBuddyImmuneRotations > 0; }

    // ADDED: called each rotation to tick down immunity counters
    public void tickImmuneCounters() {
        if (cardEffectImmuneRotations > 0) cardEffectImmuneRotations--;
        if (lilBuddyImmuneRotations > 0)   lilBuddyImmuneRotations--;
    }

    public void stopBenefits(int cooldown) { cooldown++; } // TODO: proper implementation

    public void showHand(boolean showHand) { this.showHand = showHand; }

    public BigBuddy getBigBuddy() { return bigBuddy; }
}