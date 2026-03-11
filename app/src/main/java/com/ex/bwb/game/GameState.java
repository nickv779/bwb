package com.ex.bwb.game;

import com.ex.bwb.Player;
import com.ex.bwb.cards.Card;

import java.util.Stack;
public class GameState {

    // Constants
    int DECK_SIZE = 56;
    int PLAYER_COUNT = 4;

    // Game Variables
    int turnNumber;
    int currentPlayer;
    Player[] players;
    boolean active;
    Stack<Card> drawPile;
    Stack<Card> discardPile;
    public GameState() {}
}