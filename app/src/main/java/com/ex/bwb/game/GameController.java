package com.ex.bwb.game;

import com.ex.bwb.cards.Attack;
import com.ex.bwb.cards.Card;

import java.util.Random;

public class GameController {
    public GameController() {}

    void startGame(GameState gameState) {
        // Initialize players

        // Initialize deck
        Card[] deck = initializeCards(gameState.DECK_SIZE);
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
}