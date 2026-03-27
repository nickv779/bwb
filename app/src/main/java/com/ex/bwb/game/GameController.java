package com.ex.bwb.game;

import com.ex.bwb.Player;
import com.ex.bwb.cards.Action;
import com.ex.bwb.cards.Attack;
import com.ex.bwb.cards.Card;
import com.ex.bwb.cards.CardType;
import com.ex.bwb.cards.Effects;
import com.ex.bwb.cards.LilBuddy;
import com.ex.bwb.cards.Objective;
import com.ex.bwb.cards.ShakeUp;
import com.ex.bwb.cards.Signature;

import java.util.Random;
import java.util.Stack;

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
                CardType.ATTACK,
                null,
                1,
                "LEFT");
        cards[1] = new Attack("Magic Wand",
                "-1 HP to an opponent of your choice.",
                "Sometimes You Gotta Close a Door...",
                CardType.ATTACK,
                null,
                1,
                "ANY");
        cards[2] = new Attack("Grenade",
                "-1 HP to everyone, including you.",
                "Tick, Tick... Boom",
                CardType.ATTACK,
                null,
                1,
                "ALL");
        cards[3] = new Attack("Knife",
                "-1 HP tot he player to the right.",
                "Backstabber",
                CardType.ATTACK,
                null,
                1,
                "RIGHT");
        cards[4] = new Attack("Tackle",
                "The player directly across from you takes -1 HP.",
                "Get Your Head in the Game",
                CardType.ATTACK,
                null,
                1,
                "ACROSS");
        cards[5] = new Action("Bed Time",
                "The player who goes next gets skipped.",
                "Have a Short Rest",
                CardType.ACTION,
                null,
                Effects::BedTime);
        cards[6] = new Action("Dumpster Diving",
                "Grab one card from the discard pile and put it in your hand.",
                "One Man's Trash is Another Man's Treasure",
                CardType.ACTION,
                null,
                Effects::DumpsterDiving);
        cards[7] = new Action("Beach Episode",
                "Lil' Buddies' benefits cannot be used for one rotaiton.",
                "They are on Vacation :)",
                CardType.ACTION,
                null,
                Effects::BeachEpisode);
        cards[8] = new Action("Pick-Pocket",
                "Steal any card from the player directly across from you.",
                "Five Finger Discount",
                CardType.ACTION,
                null,
                Effects::PickPocket);
        cards[9] = new Action("No Soliciting",
                "For 1 rotation, nobody can use a Lil' Buddy's benefit to affect you.",
                "Get Off My Lawn!",
                CardType.ACTION,
                null,
                Effects::NoSoliciting);
        cards[10] = new Action("Change the Channel",
                "Rotate hands counter-clockwise.",
                "Pass Me the Remote",
                CardType.ACTION,
                null,
                Effects::ChangeChannel);
        cards[11] = new Action("Spare Change",
                "Whoever has the highest number of objective cards must put one of them back into the deck and give away another one to the player with the lowest number of objective cards.",
                "Sorry, I Only Carry Cash",
                CardType.ACTION,
                null,
                Effects::SpareChange);
        cards[12] = new Action("Spa Day",
                "Choose a player to join you!",
                "Self-Care",
                CardType.ACTION,
                null,
                Effects::SpaDay);
        cards[13] = new Action("Ol' Relibale",
                "Draw 2 cards.",
                "If It Ain't Broke, Don't Fix It",
                CardType.ACTION,
                null,
                Effects::OlReliable);
        cards[14] = new Action("Barter Time",
                "Choose a player. You both must reveal your hands. Pick any card from your opponent's hand (This part of the trade is now locked). Your opponent can now choose any card from your hand that they want in return.",
                "Deal or No Deal?",
                CardType.ACTION,
                null,
                Effects::BarterTime);
        cards[15] = new Action("Reset Button",
                "Everyone shuffles their hand into the deck and draws four cards.",
                "A Fresh Start",
                CardType.ACTION,
                null,
                Effects::ResetButton);
        cards[16] = new Action("In It To Win It",
                "Search the deck for an objective card and add it to your stash.",
                "Finders, Keepers",
                CardType.ACTION,
                null,
                Effects::InItToWinIt);
        cards[17] = new Action("Googly-Eyed Rock",
                "Throw at an enemy to do -1 HP. Instead of being discarded, this card gets added to their hand. During their turn, they can throw it too for the same effect. After 4 throws the rock must be discarded.",
                "I Guess You Hit Rock Bottom",
                CardType.ACTION,
                null,
                Effects::GooglyEyedRock);
        cards[18] = new Action("Phone-A-Friend",
                "Choose one of your opponents' Lil' Buddies and use their ability to help yourself.",
                "The Best Lifeline",
                CardType.ACTION,
                null,
                Effects::PhoneAFriend);
        cards[19] = new Action("Fickle Fungus",
                "Pick an opponent. They cannot draw at the start of their next turn.",
                "Not A Fun Guy",
                CardType.ACTION,
                null,
                Effects::FickleFungus);
        cards[20] = new Action("Underdog Duel",
                "Everyone reveals their hand. Everyone who does not have a trump card or a shake-up card gets +1 HP.",
                "Chance Time!",
                CardType.ACTION,
                null,
                Effects::UnderdogDuel);
        cards[21] = new ShakeUp("Nuclear Winter",
                "For one rotation, nobody can draw unless it is from an action card.",
                "Maybe It's Time to Break the Ice",
                CardType.SHAKE_UP,
                null,
                Effects::NuclearWinter);
        cards[22] = new ShakeUp("The Flip Side",
                "Reshuffle the discard pile back into the deck. This card should be the only thing in the discard pile after it is used.",
        "See You There!",
                CardType.SHAKE_UP,
        null,
                Effects::FlipSide);
        cards[23] = new ShakeUp("The Uncertainty Principle",
      "Choose 1 of the following:" +
                        "/n 1) Choose 1 random card from every opponent." +
                        "/n 2) Search the deck for any one card.",
                "You Can Never Be Too Sure",
                CardType.SHAKE_UP,
                null,
                Effects::UncertaintyPrinciple);
        cards[24] = new ShakeUp("Absolute Zero",
                "For one rotation, no cards will effect you, good or bad.",
                "Who Turned Down the Thermostat?",
                CardType.SHAKE_UP,
                null,
                Effects::AbsoluteZero);
        cards[25] = new Signature("Mass Extinction",
                "Everyone but you takes -2 HP.",
                "Using a Trump Card Ends Your Turn.",
                CardType.SIGNATURE,
                null,
                Effects::MassExtinction);
        cards[26] = new Signature("Economic Recession",
                "Everyone but you discards their entire hand.",
                "Using a Trump Card Ends Your Turn.",
                CardType.SIGNATURE,
                null,
                Effects::EconomicRecession);
        cards[27] = new Signature("Heat Death of the Universe",
                "Take a 2nd turn.",
                "Using a Trump Card Ends Your Turn.",
                CardType.SIGNATURE,
                null,
                Effects::HeatDeathUniverse);
        cards[28] = new Signature("Party!",
                "Everyone reveals their hand and you may pick 1 card from each of them.",
                "Using a Trump Card Ends Your Turn.",
                CardType.SIGNATURE,
                null,
                Effects::Party);
        cards[29] = new LilBuddy("Fighter Snail",
      "Block one action to you every other rotation",
                null,
                CardType.LIL_BUDDY,
                null,
                Effects::FighterSnail);
        cards[30] = new LilBuddy("Inferior Towing LLC",
                "Discard an opponent's Lil' Buddy every 3 rotaitons",
                null,
                CardType.LIL_BUDDY,
                null,
                Effects::InferiorTowing);
        cards[31] = new LilBuddy("Wormhole Worm",
                "Draw a card from the bottom of the discard pile every other rotation.",
                null,
                CardType.LIL_BUDDY,
                null,
                Effects::WormholeWorm);
        cards[32] = new LilBuddy("Sock Puppet",
                "Everytime you heal your character you get +1 HP bonus.",
                null,
                CardType.LIL_BUDDY,
                null,
                Effects::SockPuppet);
        cards[33] = new LilBuddy("Washing Machine",
                "Discard a random card from one of your opponent's hand every other rotation.",
                null,
                CardType.LIL_BUDDY,
                null,
                Effects::WashingMachine);
        cards[34] = new LilBuddy("500 1lb Ducks",
                "Take 1 HP damage less from attacks every other rotation",
                null,
                CardType.LIL_BUDDY,
                null,
                Effects::FiveHundredDuck);
        cards[35] = new LilBuddy("500lb Duck",
                "Take 1 HP damage less from attacks every other rotation",
                null,
                CardType.LIL_BUDDY,
                null,
                Effects::FiveHundredDuck);
        cards[36] = new LilBuddy("Flying Fish",
                "Assign to a Lil' Buddy on your bench. That Lil' Buddy now has one less rotation on their cooldown.",
                null,
                CardType.LIL_BUDDY,
                null,
                Effects::FlyingFish);
        cards[37] = new LilBuddy("Disco the Ghost",
                "Your Big Buddy gains +1 HP every other rotation.",
                null,
                CardType.LIL_BUDDY,
                null,
                Effects::DiscoGhost);
        cards[38] = new LilBuddy("Darrel Jr.",
                "All damage to opponents from action cards get an extra damage point every other rotation.",
                null,
                CardType.LIL_BUDDY,
                null,
                Effects::DarrelJr);
        cards[39] = new LilBuddy("Piggy Bank",
                "Draw one additional card every other rotation.",
                null,
                CardType.LIL_BUDDY,
                null,
                Effects::PiggyBank);
        cards[40] = new LilBuddy("Herald & Berald",
                "An enemy Big Buddy loses 1 HP every other rotation.",
                null,
                CardType.LIL_BUDDY,
                null,
                Effects::HeraldBerald);
        cards[41] = new LilBuddy("Magic Mirror",
                "Choose one of your opponent's Lil' Buddy benefits. This Lil' Buddy now has that benefit for as long as the other is in play.",
                null,
                CardType.LIL_BUDDY,
                null,
                Effects::MagicMirror);
        cards[42] = new LilBuddy("Silly Goose",
                "Steal an objective card from an opponent every 3 rotations.",
                null,
                CardType.LIL_BUDDY,
                null,
                Effects::SillyGoose);
        cards[43] = new LilBuddy("Jam the Jellyfish",
                "If attacked, opponent loses 1 HP every other rotation.",
                null,
                CardType.LIL_BUDDY,
                null,
                Effects::JamJellyfish);
        cards[44] = new LilBuddy("Hot Potato",
                "Every other rotation, swap this Lil' Buddy with an opponent's Lil' Buddy. They must have the Hot Potato on their bench for at least one rotation before using its benefit.",
                null,
                CardType.LIL_BUDDY,
                null,
                Effects::HotPotato);
        cards[45] = new LilBuddy("Larry the Llama",
                "Trade a card from your hand with one that is in the discard pile every other turn.",
                null,
                CardType.LIL_BUDDY,
                null,
                Effects::LlamaAlpaca);
        cards[46] = new LilBuddy("Albert the Alpaca",
                "Trade a card from your hand with one that is in the discard pile every other turn.",
                null,
                CardType.LIL_BUDDY,
                null,
                Effects::LlamaAlpaca);
        cards[47] = new LilBuddy("Early Bird",
                "Draw a card from the top of the discard pile every other rotation.",
                null,
                CardType.LIL_BUDDY,
                null,
                Effects::EarlyBird);
        cards[48] = new LilBuddy("Sleepy Giraffe",
                "Every other rotation an opponent Lil' Buddy gets put to sleep and cannot use their ability.",
                null,
                CardType.LIL_BUDDY,
                null,
                Effects::SleepyGiraffe);
        cards[49] = new Objective("One Step Closer!",
                "You need a total of 7 of these in your stash to win!",
                null,
                CardType.OBJECTIVE,
                null);
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
    public void drawDeck(int amount){
        //this.gameState.currentPlayer.hand.add(amount);
        for (int i = 0; i < amount; i++){
            Card specificCard = this.gameState.drawPile.pop();
            this.gameState.players[this.gameState.currentPlayer].addCard(specificCard);
        }
    }
    public void drawDiscard(int amount, int cardID){
        for (int i = 0; i < amount; i++){
            //this.gameState.currentPlayer.hand.add(amount);
            //gameState.discardPile.pop(Card[cardID]);
        }
    }

    public void drawDeckSpecific(int amount,  Card[] cardID){
        // 1. find the card in draw pile
        for (int i = 0; i < amount; i++){
            //this.gameState.currentPlayer.hand.add(amount);
            //gameState.discardPile.pop(Card[cardID]);
        }
    }

    public void changeHealth(int changeAmount, Player target){
        target.changeHealth(changeAmount);
    }


}

