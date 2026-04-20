package com.ex.bwb.game;

import com.ex.bwb.cards.Card;

import java.util.Stack;

public class GameState {

    // Constants
    public int DECK_SIZE = 56;
    public int PLAYER_COUNT = 4;

    // Game Variables
    public int turnNumber;
    public int rotationCount;
    public int currentPlayer;
    public boolean active;
    public Stack<Card> drawPile;
    public Stack<Card> discardPile;

    // REMOVED: players       — owned by GameController
    // REMOVED: actionPointsRemaining — owned by Player.currAP

    // Status flags for card effects
    public int lilBuddiesBlockedRotations = 0;  // BeachEpisode
    public int drawBlockedRotations = 0;         // NuclearWinter
    public boolean grantExtraTurn = false;        // ADDED: HeatDeathUniverse
    public int rockThrowCount = 0;               // GooglyEyedRock
    // ADD these fields to GameState.java
    public boolean rockGoesToHand = false;   // GooglyEyedRock
    public int rockTargetIndex = -1;         // GooglyEyedRock

    public GameState() {
        drawPile    = new Stack<>();
        discardPile = new Stack<>();
    }

    public void setLilBuddiesBlocked(int rotations) {
        lilBuddiesBlockedRotations = rotations;
    }

    public void setDrawBlockedRotations(int rotations) {
        drawBlockedRotations = rotations;
    }
}