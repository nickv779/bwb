package com.ex.bwb.cards;

import com.ex.bwb.Player;
import com.ex.bwb.game.GameController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Effects {

    // -------------------------------------------------------------------------
    // ACTION CARDS
    // -------------------------------------------------------------------------

    public static void BedTime(Player source, GameController gc) {
        // "The player who goes next gets skipped"
        gc.changeTurn(2);
    }

    public static void DumpsterDiving(Player source, GameController gc) {
        // "Grab one card from the discard pile and put it in your hand"
        gc.drawDiscard(1, source);
    }

    public static void BeachEpisode(Player source, GameController gc) {
        // "Lil' Buddies' benefits cannot be used for one rotation"
        gc.state.setLilBuddiesBlocked(1);
    }

    public static void PickPocket(Player source, GameController gc) {
        // "Steal any card from the player directly across from you"
        // NOTE: gc.input must be set to the card index to steal before calling
        int acrossIndex = (gc.state.currentPlayer + 2) % gc.state.PLAYER_COUNT;
        Player target = gc.players[acrossIndex];
        if (!target.hand.isEmpty()) {
            int stealIndex = Math.max(0, Math.min(gc.input, target.hand.size() - 1));
            source.hand.add(target.removeCard(stealIndex));
        }
    }

    public static void NoSoliciting(Player source, GameController gc) {
        // "For 1 rotation, nobody can use a Lil' Buddy's benefit to affect you"
        source.setLilBuddyImmune(1);
    }

    public static void ChangeChannel(Player source, GameController gc) {
        // "Rotate hands counter clockwise"
        // Counter-clockwise = each player gets the hand of the player before them
        int count = gc.state.PLAYER_COUNT;
        List<Card> firstHand = new ArrayList<>(gc.players[0].hand);
        for (int i = 0; i < count - 1; i++) {
            gc.players[i].hand.clear();
            gc.players[i].hand.addAll(gc.players[i + 1].hand);
        }
        gc.players[count - 1].hand.clear();
        gc.players[count - 1].hand.addAll(firstHand);
    }

    public static void SpareChange(Player source, GameController gc) {
        // "Whoever has the highest number of objective cards must put one back
        //  into the deck and give another to the player with the fewest"
        Player richest = gc.getPlayerWithMostObjectives();
        Player poorest = gc.getPlayerWithFewestObjectives();
        if (richest != poorest && richest.stash.size() > 0) {
            // Put one objective back into the deck
            Card returned = richest.stash.remove(richest.stash.size() - 1);
            gc.state.drawPile.push(returned);
            gc.shuffleDeck();
            // Give another to the poorest if richest still has one
            if (richest.stash.size() > 0) {
                Card given = richest.stash.remove(richest.stash.size() - 1);
                poorest.stash.add(given);
            }
        }
    }

    public static void SpaDay(Player source, GameController gc) {
        // "Choose a player to join you! Both of you heal +1 HP"
        // NOTE: gc.input must be set to target player index before calling
        Player target = gc.players[gc.input];
        gc.changeHealth(1, source);
        gc.changeHealth(1, target);
    }

    public static void OlReliable(Player source, GameController gc) {
        // "Draw 2 cards"
        gc.drawCards(2, source);
    }

    public static void BarterTime(Player source, GameController gc) {
        // "Choose a player. You both reveal hands. Source picks from target first
        //  (locked), then target picks from source in return."
        // NOTE: full implementation needs 2-step UI input — first pick is gc.input
        // for target player, second pick needs separate input for card indices.
        // Partial implementation: swaps first card of each hand as placeholder
        // until UI input chaining is available.
        int targetIndex = gc.input;
        Player target = gc.players[targetIndex];
        if (!source.hand.isEmpty() && !target.hand.isEmpty()) {
            Card fromTarget = target.removeCard(0);
            Card fromSource = source.removeCard(0);
            source.hand.add(fromTarget);
            target.hand.add(fromSource);
        }
    }

    public static void ResetButton(Player source, GameController gc) {
        // "Everyone shuffles their hand into the deck and draws four cards"
        for (Player p : gc.players) {
            if (p != null) {
                gc.state.drawPile.addAll(p.hand);
                p.hand.clear();
            }
        }
        gc.shuffleDeck();
        for (Player p : gc.players) {
            if (p != null) gc.drawCards(4, p);
        }
    }

    public static void InItToWinIt(Player source, GameController gc) {
        // "Search the deck for an objective card and add it to your stash"
        List<Card> pile = new ArrayList<>(gc.state.drawPile);
        for (int i = 0; i < pile.size(); i++) {
            if (pile.get(i).type == CardType.OBJECTIVE) {
                Card found = pile.remove(i);
                gc.state.drawPile.clear();
                gc.state.drawPile.addAll(pile);
                gc.shuffleDeck();
                source.stash.add(found);
                break;
            }
        }
    }

    public static void GooglyEyedRock(Player source, GameController gc) {
        // "Throw at an enemy to do -1HP. Instead of being discarded it gets
        //  added to their hand. After 4 throws the rock must be discarded."
        // NOTE: gc.input must be set to target player index before calling.
        // The card going to target's hand instead of discard is handled
        // in GameController.playCard() by checking rockThrowCount.
        int targetIndex = gc.input;
        Player target = gc.players[targetIndex];
        gc.changeHealth(-1, target);
        gc.state.rockThrowCount++;
        if (gc.state.rockThrowCount < 4) {
            // Put rock in target's hand instead of discard — signal with flag
            gc.state.rockGoesToHand = true;
            gc.state.rockTargetIndex = targetIndex;
        } else {
            // After 4 throws it goes to discard normally
            gc.state.rockThrowCount = 0;
            gc.state.rockGoesToHand = false;
        }
    }

    public static void PhoneAFriend(Player source, GameController gc) {
        // "Choose one of your opponent's Lil' Buddies and use their ability to help yourself"
        // NOTE: gc.input = target player index, gc.input2 = their buddy slot index
        int targetIndex = gc.input;
        int buddySlot   = gc.input2;
        Player target   = gc.players[targetIndex];
        if (target.lilBuddies[buddySlot] != null) {
            gc.applyLilBuddyBenefitTo(target.lilBuddies[buddySlot], source);
        }
    }

    public static void FickleFungus(Player source, GameController gc) {
        // "Pick an opponent. They cannot draw at the start of their next turn."
        // NOTE: gc.input must be set to target player index before calling
        gc.players[gc.input].setCannotDrawNextTurn(true);
    }

    public static void UnderdogDuel(Player source, GameController gc) {
        // "Everyone reveals their hand. Everyone who does not have a Signature
        //  card or Shake-Up card gets +1HP."
        for (Player p : gc.players) {
            if (p == null) continue;
            boolean hasPowerCard = false;
            for (Card c : p.hand) {
                if (c.type == CardType.SIGNATURE || c.type == CardType.SHAKE_UP) {
                    hasPowerCard = true;
                    break;
                }
            }
            if (!hasPowerCard) gc.changeHealth(1, p);
        }
    }

    // -------------------------------------------------------------------------
    // SHAKE-UP CARDS
    // -------------------------------------------------------------------------

    public static void NuclearWinter(Player source, GameController gc) {
        // "For one rotation, nobody can draw unless it is from an action card"
        gc.state.setDrawBlockedRotations(1);
    }

    public static void FlipSide(Player source, GameController gc) {
        // "Reshuffle the discard pile back into the deck. This card should be
        //  the only thing in the discard pile after it is used."
        // Move all discard except this card back to deck
        gc.state.drawPile.addAll(gc.state.discardPile);
        gc.state.discardPile.clear();
        gc.shuffleDeck();
        // FlipSide itself stays in discard — GameController.playCard() pushes
        // the card to discard after this effect runs, so this is automatic
    }

    public static void UncertaintyPrinciple(Player source, GameController gc) {
        // "Choose 1 of the following:
        //  1) Choose 1 random card from every opponent
        //  2) Search the deck for any one card"
        // NOTE: gc.input = 1 or 2 for the choice, gc.input2 = card index for option 2
        if (gc.input == 1) {
            for (Player p : gc.players) {
                if (p == null || p == source || p.hand.isEmpty()) continue;
                int randIndex = (int)(Math.random() * p.hand.size());
                source.hand.add(p.removeCard(randIndex));
            }
        } else {
            // Search deck for any one card by index
            List<Card> pile = new ArrayList<>(gc.state.drawPile);
            int targetIndex = Math.max(0, Math.min(gc.input2, pile.size() - 1));
            if (!pile.isEmpty()) {
                Card chosen = pile.remove(targetIndex);
                gc.state.drawPile.clear();
                gc.state.drawPile.addAll(pile);
                source.hand.add(chosen);
            }
        }
    }

    public static void AbsoluteZero(Player source, GameController gc) {
        // "For one rotation, no cards will effect you, good or bad"
        source.setCardEffectImmune(1);
    }

    // -------------------------------------------------------------------------
    // SIGNATURE CARDS
    // -------------------------------------------------------------------------

    public static void MassExtinction(Player source, GameController gc) {
        // "Everyone but you takes -2 HP" (Darrel's)
        for (Player p : gc.players) {
            if (p != null && p != source) gc.changeHealth(-2, p);
        }
    }

    public static void EconomicRecession(Player source, GameController gc) {
        // "Everyone but you discards their entire hand" (Fernando's)
        for (Player p : gc.players) {
            if (p != null && p != source) {
                gc.state.discardPile.addAll(p.hand);
                p.hand.clear();
            }
        }
    }

    public static void HeatDeathUniverse(Player source, GameController gc) {
        // "Take a 2nd turn" (Gerald's)
        gc.state.grantExtraTurn = true;
    }

    public static void Party(Player source, GameController gc) {
        // "Everyone reveals their hand and you may pick 1 card from each" (Mr. Ostrich's)
        // NOTE: gc.input is used per-opponent — needs UI input chaining for full
        // implementation. Current implementation takes the first card from each opponent.
        for (Player p : gc.players) {
            if (p != null && p != source && !p.hand.isEmpty()) {
                source.hand.add(p.removeCard(0));
            }
        }
    }

    // -------------------------------------------------------------------------
    // LIL' BUDDY PASSIVES
    // These are called by applyLilBuddyPassives() at end of each turn.
    // "Every other rotation" checks gc.state.rotationCount % 2 == 0.
    // "Every 3 rotations"    checks gc.state.rotationCount % 3 == 0.
    // -------------------------------------------------------------------------

    public static void FighterSnail(Player source, GameController gc) {
        // "Block one action to you every other rotation"
        // Sets a one-turn action block flag on source
        if (gc.state.rotationCount % 2 == 0) {
            source.setActionBlocked(true);
        }
    }

    public static void InferiorTowing(Player source, GameController gc) {
        // "Discard an opponent's Lil' Buddy every 3 rotations"
        if (gc.state.rotationCount % 3 == 0) {
            // Targets the opponent with the most Lil' Buddies
            Player target = gc.getPlayerWithMostLilBuddies(source);
            if (target != null) {
                for (int i = 0; i < target.lilBuddies.length; i++) {
                    if (target.lilBuddies[i] != null) {
                        gc.state.discardPile.push(target.lilBuddies[i]);
                        target.lilBuddies[i] = null;
                        break;
                    }
                }
            }
        }
    }

    public static void WormholeWorm(Player source, GameController gc) {
        // "Draw a card from the bottom of the discard pile every other rotation"
        // Anti-Synergy with EarlyBird: nullifies if paired on same player
        if (gc.state.rotationCount % 2 == 0) {
            if (hasAntisynergy(source, "EarlyBird")) return;
            if (!gc.state.discardPile.isEmpty()) {
                // Get bottom of discard (index 0)
                Card bottom = gc.state.discardPile.remove(0);
                source.addCard(bottom);
            }
        }
    }

    public static void SockPuppet(Player source, GameController gc) {
        // "Everytime you heal your character you get a +1HP bonus"
        // Passive bonus is handled inside GameController.changeHealth() by
        // checking source.hasLilBuddy(LilBuddy type) — placement gives +1 HP
        gc.changeHealth(1, source);
    }

    public static void WashingMachine(Player source, GameController gc) {
        // "Discard a random card from one of your opponent's hand every other rotation"
        // Synergy with Sock Puppet: cooldown lowers by 1 rotation
        int interval = hasSynergy(source, "SockPuppet") ? 1 : 2;
        if (gc.state.rotationCount % interval == 0) {
            // Pick a random opponent with cards
            List<Player> targets = new ArrayList<>();
            for (Player p : gc.players) {
                if (p != null && p != source && !p.hand.isEmpty()) targets.add(p);
            }
            if (!targets.isEmpty()) {
                Player target = targets.get((int)(Math.random() * targets.size()));
                int randIndex = (int)(Math.random() * target.hand.size());
                gc.state.discardPile.push(target.removeCard(randIndex));
            }
        }
    }

    public static void FiveHundredDuck(Player source, GameController gc) {
        // "Take -1HP from attacks every other rotation"
        // Anti-Synergy with 500 1lb Ducks: nullifies if paired
        // Passive — actual damage reduction is checked in Attack.Activate()
        // This function just flags the player as having the buff active
        if (gc.state.rotationCount % 2 == 0) {
            if (hasAntisynergy(source, "FiveHundredDucks")) return;
            source.setAttackDamageReduction(1);
        }
    }

    public static void FiveHundredDucks(Player source, GameController gc) {
        // "Take -1HP from attacks every other rotation"
        // Anti-Synergy with 500 lb Duck: nullifies if paired
        if (gc.state.rotationCount % 2 == 0) {
            if (hasAntisynergy(source, "FiveHundredDuck")) return;
            source.setAttackDamageReduction(1);
        }
    }

    public static void FlyingFish(Player source, GameController gc) {
        // "Assign to a Lil' Buddy on your bench. That Lil' Buddy now has
        //  -1 rotation on their cooldown."
        // NOTE: gc.input = index of buddy slot to reduce cooldown on
        int slot = gc.input;
        if (slot >= 0 && slot < source.lilBuddies.length
                && source.lilBuddies[slot] != null) {
            if (source.lilBuddies[slot].cooldown > 0) {
                source.lilBuddies[slot].cooldown--;
            }
        }
    }

    public static void DiscoGhost(Player source, GameController gc) {
        // "Your Big Buddy gains +1 HP every other rotation"
        // Synergy with Mr. Ostrich: cooldown lowers by 1 rotation
        int interval = hasSynergy(source, "Mr. Ostrich") ? 1 : 2;
        if (gc.state.rotationCount % interval == 0) {
            gc.changeHealth(1, source);
        }
    }

    public static void DarrelJr(Player source, GameController gc) {
        // "All damage to opponents gets a -1HP bonus every other rotation
        //  [only works on action cards]"
        // Synergy with Darrel: cooldown lowers by 1 rotation
        // Passive flag — checked in executeCardEffect for Action cards
        int interval = hasSynergy(source, "Darrel") ? 1 : 2;
        if (gc.state.rotationCount % interval == 0) {
            source.setActionDamageBonus(1);
        }
    }

    public static void PiggyBank(Player source, GameController gc) {
        // "Draw +1 card every other rotation"
        // Synergy with Fernando: cooldown lowers by 1 rotation
        int interval = hasSynergy(source, "Fernando") ? 1 : 2;
        if (gc.state.rotationCount % interval == 0) {
            gc.drawCards(1, source);
        }
    }

    public static void HeraldBerald(Player source, GameController gc) {
        // "An enemy Big Buddy loses -1HP every other rotation"
        // Synergy with Gerald: cooldown lowers by 1 rotation
        int interval = hasSynergy(source, "Gerald") ? 1 : 2;
        if (gc.state.rotationCount % interval == 0) {
            // Targets the player with the most HP who isn't source
            Player target = gc.getPlayerWithMostHP(source);
            if (target != null) gc.changeHealth(-1, target);
        }
    }

    public static void MagicMirror(Player source, GameController gc) {
        // "Choose one of your opponent's Lil' Buddy's benefits. This Lil' Buddy
        //  now has that benefit for as long as the other is in play."
        // NOTE: gc.input = target player index, gc.input2 = their buddy slot
        // Copies the effect BiConsumer reference from target buddy to this buddy
        int targetIndex = gc.input;
        int buddySlot   = gc.input2;
        Player target   = gc.players[targetIndex];
        // Find which slot MagicMirror is in on source
        for (int i = 0; i < source.lilBuddies.length; i++) {
            if (source.lilBuddies[i] != null
                    && source.lilBuddies[i].getName().equals("MagicMirror")) {
                if (target.lilBuddies[buddySlot] != null) {
                    source.lilBuddies[i].effect = target.lilBuddies[buddySlot].effect;
                }
                break;
            }
        }
    }

    public static void SillyGoose(Player source, GameController gc) {
        // "Steal an objective card from an opponent every 3 rotations"
        if (gc.state.rotationCount % 3 == 0) {
            Player target = gc.getPlayerWithMostObjectives();
            if (target != source && !target.stash.isEmpty()) {
                Card stolen = target.stash.remove(target.stash.size() - 1);
                source.stash.add(stolen);
            }
        }
    }

    public static void JamJellyfish(Player source, GameController gc) {
        // "One opponent who attacks you takes -1 HP every other rotation"
        // Passive flag — checked in Attack.Activate() when target has this buddy
        if (gc.state.rotationCount % 2 == 0) {
            source.setRetaliationActive(true);
        }
    }

    public static void HotPotato(Player source, GameController gc) {
        // "Every other rotation, swap this Lil' Buddy with an opponent's Lil' Buddy.
        //  They must have the Hot Potato for at least one rotation before using it."
        if (gc.state.rotationCount % 2 == 0) {
            // Find source's HotPotato slot
            int sourceSlot = -1;
            for (int i = 0; i < source.lilBuddies.length; i++) {
                if (source.lilBuddies[i] != null
                        && source.lilBuddies[i].getName().equals("HotPotato")) {
                    sourceSlot = i;
                    break;
                }
            }
            if (sourceSlot == -1) return;

            // Pick a random opponent and a random slot they have filled
            List<int[]> options = new ArrayList<>();
            for (int p = 0; p < gc.players.length; p++) {
                if (gc.players[p] == null || gc.players[p] == source) continue;
                for (int s = 0; s < gc.players[p].lilBuddies.length; s++) {
                    if (gc.players[p].lilBuddies[s] != null) options.add(new int[]{p, s});
                }
            }
            if (options.isEmpty()) return;
            int[] pick = options.get((int)(Math.random() * options.size()));
            Player target = gc.players[pick[0]];
            int targetSlot = pick[1];

            // Swap
            LilBuddy temp = source.lilBuddies[sourceSlot];
            source.lilBuddies[sourceSlot] = target.lilBuddies[targetSlot];
            target.lilBuddies[targetSlot] = temp;
        }
    }

    public static void LlamaAlpaca(Player source, GameController gc) {
        // Larry the Llama AND Albert the Alpaca share the same effect:
        // "Trade a card from your hand with one in the discard pile every other turn"
        // Anti-Synergy with each other: nullifies if both are on the same player
        if (gc.state.rotationCount % 2 == 0) {
            boolean hasLarry  = hasLilBuddyByName(source, "LarryLlama");
            boolean hasAlbert = hasLilBuddyByName(source, "AlbertAlpaca");
            if (hasLarry && hasAlbert) return; // anti-synergy nullifies
            if (!source.hand.isEmpty() && !gc.state.discardPile.isEmpty()) {
                // Trade first card in hand with top of discard
                Card fromHand    = source.removeCard(0);
                Card fromDiscard = gc.state.discardPile.pop();
                gc.state.discardPile.push(fromHand);
                source.hand.add(fromDiscard);
            }
        }
    }

    public static void EarlyBird(Player source, GameController gc) {
        // "Draw a card from the top of the discard pile every other rotation"
        // Anti-Synergy with WormholeWorm: nullifies if paired
        if (gc.state.rotationCount % 2 == 0) {
            if (hasAntisynergy(source, "WormholeWorm")) return;
            if (!gc.state.discardPile.isEmpty()) {
                source.addCard(gc.state.discardPile.pop());
            }
        }
    }

    public static void SleepyGiraffe(Player source, GameController gc) {
        // "Every other rotation an opponent Lil' Buddy gets put to sleep
        //  and cannot use their ability"
        if (gc.state.rotationCount % 2 == 0) {
            // Puts a random active Lil' Buddy of a random opponent to sleep
            List<int[]> options = new ArrayList<>();
            for (int p = 0; p < gc.players.length; p++) {
                if (gc.players[p] == null || gc.players[p] == source) continue;
                for (int s = 0; s < gc.players[p].lilBuddies.length; s++) {
                    if (gc.players[p].lilBuddies[s] != null
                            && !gc.players[p].lilBuddies[s].asleep) {
                        options.add(new int[]{p, s});
                    }
                }
            }
            if (!options.isEmpty()) {
                int[] pick = options.get((int)(Math.random() * options.size()));
                gc.players[pick[0]].lilBuddies[pick[1]].asleep = true;
            }
        }
    }

    // -------------------------------------------------------------------------
    // PRIVATE HELPERS
    // -------------------------------------------------------------------------

    // Checks if a player has a specific named Lil' Buddy in their bench
    private static boolean hasLilBuddyByName(Player p, String name) {
        for (LilBuddy lb : p.lilBuddies) {
            if (lb != null && lb.getName().equals(name)) return true;
        }
        return false;
    }

    // Synergy: a specific BigBuddy name or LilBuddy name is present on this player
    private static boolean hasSynergy(Player p, String partnerName) {
        if (p.bigBuddy != null && p.bigBuddy.getName().equals(partnerName)) return true;
        return hasLilBuddyByName(p, partnerName);
    }

    // Anti-synergy: the nullifying partner is also present on the same player
    private static boolean hasAntisynergy(Player p, String partnerName) {
        return hasLilBuddyByName(p, partnerName);
    }
}