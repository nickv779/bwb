package com.ex.bwb.game;

import com.ex.bwb.cards.Action;
import com.ex.bwb.cards.Attack;
import com.ex.bwb.cards.Card;
import com.ex.bwb.cards.Effects;

import java.util.Random;

public class GameController {

    GameState gameState;
    public GameController(GameState gameState) { this.gameState = gameState; }

    public void startGame() {
        // Initialize players

        // Initialize deck
        Card[] deck = initializeCards(this.gameState.DECK_SIZE);
        shuffleCards(deck);
    }

    private Card[] initializeCards(int deckSize) {
        // initializes all the cards, attack card done below
        Card[] cards = new Card[deckSize];
        cards[0] = new Attack("Katana",
                "The person to your left takes -1 HP.",
                "Chop Chop",
                null,
                1,
                "LEFT");
        cards[1] = new Action("Bed Time",
                "The player who goes next gets skipped.",
                "Have a Short Rest",
                null,
                Effects::BedTime);
        return cards;
    }

    private void shuffleCards(Card[] cards) {
        // shuffle the cards randomly via Fisher-Yates
        Random rand = new Random();
        for (int i = cards.length - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            Card temp = cards[i];
            cards[i] = cards[j];
            cards[j] = temp;
        }
    }

    // below is used for any turn-affecting card
    // change the current user by some shift, i.e. shift == 1 to skip
    // the next player, this might change depending on how currentPlayer
    // is incremented in later implementation
    public void changeTurn(int shift) {
        this.gameState.currentPlayer = (this.gameState.currentPlayer) % 4;
    }

}