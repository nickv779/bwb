package com.ex.bwb.game;

import android.util.Log;

import com.ex.bwb.Player;
import com.ex.bwb.cards.Action;
import com.ex.bwb.cards.Attack;
import com.ex.bwb.cards.Card;
import com.ex.bwb.cards.CardEffect;
import com.ex.bwb.cards.CardType;
import com.ex.bwb.cards.Effects;
import com.ex.bwb.cards.LilBuddy;
import com.ex.bwb.cards.Objective;
import com.ex.bwb.cards.ShakeUp;
import com.ex.bwb.cards.Signature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

public class GameController {

    private static final String TAG = "GameController";

    public GameState state;
    public Player[] players;
    public int input = -1; // target player index set by server before calling any effect

    public GameController() {
        state   = new GameState();
        players = new Player[state.PLAYER_COUNT];
    }

    // -------------------------------------------------------------------------
    // DEBUG — logs "[CardName] nothing happened..." when an effect isn't done yet
    // -------------------------------------------------------------------------
    public void debugNotImplemented(String cardName) {
        Log.d(TAG, "[" + cardName + "] nothing happened...");
    }

    // -------------------------------------------------------------------------
    // TURN MANAGEMENT
    // -------------------------------------------------------------------------
    public void startTurn() {
        Player current = players[state.currentPlayer];

        // AP reset from Player's own maxAP — already correct per BigBuddy ability
        current.currAP = current.maxAP;

        // doubleDraw already set in Player constructor for Fernando
        int drawCount = current.doubleDraw ? 2 : 1;
        drawCards(drawCount, current);

        Log.d(TAG, "Player " + state.currentPlayer + "'s turn — " + current.currAP + " AP");
    }

    // offset=1 normal, offset=2 skips next player (BedTime)
    public void changeTurn(int offset) {
        // HeatDeathUniverse: replay current player's turn once before advancing
        if (state.grantExtraTurn) {
            state.grantExtraTurn = false;
            startTurn();
            return;
        }

        int next = (state.currentPlayer + offset) % state.PLAYER_COUNT;

        // Skip eliminated players
        while (!players[next].isAlive()) {
            next = (next + 1) % state.PLAYER_COUNT;
        }

        // Track full rotations — one rotation = all players have gone once
        if (next <= state.currentPlayer) {
            state.rotationCount++;
            tickRotationEffects();
        }

        state.currentPlayer = next;
        startTurn();
    }

    public void endTurn() {
        applyLilBuddyPassives();
        eliminateDeadPlayers();
        checkWinConditions();
        changeTurn(1);
    }

    // -------------------------------------------------------------------------
    // PLAYER ACTIONS — AP subtracted from player.currAP here and nowhere else
    // -------------------------------------------------------------------------

    public void playCard(int cardIndex, int targetPlayerId) {
        Player current = players[state.currentPlayer];
        if (current.currAP <= 0) return;

        Card card = current.hand.get(cardIndex);

        // AbsoluteZero: skip effect but still consume AP and discard
        if (!current.isCardEffectImmune()) {
            executeCardEffect(card, current, targetPlayerId);
        } else {
            Log.d(TAG, "[AbsoluteZero] card effect blocked for " + current.name);
        }

        current.removeCard(cardIndex);
        state.discardPile.push(card);

        if (card.getType() == CardType.SIGNATURE) {
            // Signature cards do NOT cost AP — they end the turn immediately
            endTurn();
        } else {
            // AP subtracted HERE for Action, ShakeUp, and Attack cards
            current.currAP--;
            if (current.currAP <= 0) endTurn();
        }
    }

    public void punch(int targetPlayerId) {
        Player current = players[state.currentPlayer];
        changeHealth(-1, players[targetPlayerId]);
        Log.d(TAG, "Player " + state.currentPlayer + " punches player " + targetPlayerId);

        // AP subtracted HERE
        current.currAP--;
        if (current.currAP <= 0) endTurn();
    }

    public void drawCard() {
        Player current = players[state.currentPlayer];

        if (state.drawBlockedRotations > 0) {
            Log.d(TAG, "Draw blocked this rotation (Nuclear Winter)");
            return;
        }
        if (current.cannotDrawNextTurn) {
            current.cannotDrawNextTurn = false;
            Log.d(TAG, "Player " + state.currentPlayer + " cannot draw (Fickle Fungus)");
            return;
        }

        drawCards(1, current);

        // AP subtracted HERE
        current.currAP--;
        if (current.currAP <= 0) endTurn();
    }

    // -------------------------------------------------------------------------
    // HEALTH — delegates to Player.changeHealth() which already clamps
    // -------------------------------------------------------------------------

    public void changeHealth(int amount, Player target) {
        target.changeHealth(amount);
    }

    // -------------------------------------------------------------------------
    // CARDS
    // -------------------------------------------------------------------------

    public void drawCards(int count, Player target) {
        for (int i = 0; i < count; i++) {
            if (state.drawPile.isEmpty()) reshuffleDiscard();
            if (!state.drawPile.isEmpty()) target.addCard(state.drawPile.pop());
        }
    }

    public void drawDiscard(int count, Player target) {
        for (int i = 0; i < count; i++) {
            if (!state.discardPile.isEmpty())
                target.addCard(state.discardPile.pop());
        }
    }

    public void shuffleDeck() {
        List<Card> temp = new ArrayList<>(state.drawPile);
        Collections.shuffle(temp);
        state.drawPile.clear();
        state.drawPile.addAll(temp);
    }

    private void reshuffleDiscard() {
        state.drawPile.addAll(state.discardPile);
        state.discardPile.clear();
        shuffleDeck();
    }

    // Dispatches to the correct effect using the actual card subclass — no string switch needed
    private void executeCardEffect(Card card, Player source, int targetPlayerId) {
        input = targetPlayerId;

        if (card instanceof Action) {
            CardEffect effect = ((Action) card).getEffect();
            effect.apply(source, this);

        } else if (card instanceof Attack) {
            // changeHealth(-1, ((Attack) card).getTarget());

        } else if (card instanceof ShakeUp) {
            CardEffect effect = ((ShakeUp) card).getEffect();
            effect.apply(source, this);

        } else if (card instanceof Signature) {
            CardEffect effect = ((Signature) card).getEffect();
            effect.apply(source, this);

        } else if (card instanceof LilBuddy) {
            CardEffect effect = ((LilBuddy) card).getEffect();
            effect.apply(source, this);

        } else {
            debugNotImplemented(card.getName());
        }
    }

    // -------------------------------------------------------------------------
    // LIL' BUDDY PASSIVES — applied at end of each turn
    // -------------------------------------------------------------------------

    private void applyLilBuddyPassives() {
        if (state.lilBuddiesBlockedRotations > 0) return;
        for (Player p : players) {
            if (p == null || !p.isAlive() || p.isLilBuddyImmune()) continue;
            for (LilBuddy lb : p.lilBuddies) {
                if (lb != null && lb.getEffect() != null) {
                    lb.getEffect().apply(p, this);
                }
            }
        }
    }

    public void applyLilBuddyBenefitTo(LilBuddy buddy, Player target) {
        if (buddy != null && buddy.getEffect() != null) {
            buddy.getEffect().apply(target, this);
        }
    }

    // -------------------------------------------------------------------------
    // ROTATION EFFECTS — called when a full rotation completes
    // -------------------------------------------------------------------------

    private void tickRotationEffects() {
        if (state.lilBuddiesBlockedRotations > 0) state.lilBuddiesBlockedRotations--;
        if (state.drawBlockedRotations > 0)        state.drawBlockedRotations--;
        for (Player p : players) {
            if (p != null) p.tickImmuneCounters();
        }
    }

    // -------------------------------------------------------------------------
    // WIN / ELIMINATION
    // -------------------------------------------------------------------------

    private void eliminateDeadPlayers() {
        for (Player p : players) {
            if (p != null && p.currHP <= 0) {
                p.setAlive(false);
                Log.d(TAG, p.name + " has been eliminated!");
            }
        }
    }

    private void checkWinConditions() {
        // Win condition 1: collect 7 objective cards
        for (Player p : players) {
            if (p != null && p.stash.size() >= 7) {
                Log.d(TAG, p.name + " wins with 7 objective cards!");
            }
        }
        // Win condition 2: last Big Buddy standing
        long aliveCount = 0;
        Player lastAlive = null;
        for (Player p : players) {
            if (p != null && p.isAlive()) { aliveCount++; lastAlive = p; }
        }
        if (aliveCount == 1 && lastAlive != null) {
            Log.d(TAG, lastAlive.name + " wins — last one standing!");
        }
    }

    // -------------------------------------------------------------------------
    // SPARE CHANGE HELPERS
    // -------------------------------------------------------------------------

    public Player getPlayerWithMostObjectives() {
        Player best = players[0];
        for (Player p : players) {
            if (p != null && p.stash.size() > best.stash.size()) best = p;
        }
        return best;
    }

    public Player getPlayerWithFewestObjectives() {
        Player worst = players[0];
        for (Player p : players) {
            if (p != null && p.stash.size() < worst.stash.size()) worst = p;
        }
        return worst;
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
}