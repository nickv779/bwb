package com.ex.bwb.cards;

import com.ex.bwb.Player;
import com.ex.bwb.game.GameController;

public class Effects {

    // -------------------------------------------------------------------------
    // ACTION CARDS
    // -------------------------------------------------------------------------

    public static void BedTime(Player source, GameController gc) {
        gc.changeTurn(2); // FIXED: was changeTurn(1), needs 2 to actually skip the next player
    }

    public static void DumpsterDiving(Player source, GameController gc) {
        gc.debugNotImplemented("DumpsterDiving"); // ADDED: remove when implemented
    }

    public static void BeachEpisode(Player source, GameController gc) {
        gc.debugNotImplemented("BeachEpisode"); // ADDED: remove when implemented
    }

    public static void PickPocket(Player source, GameController gc) {
        gc.debugNotImplemented("PickPocket"); // ADDED: remove when implemented
    }

    public static void NoSoliciting(Player source, GameController gc) {
        gc.debugNotImplemented("NoSoliciting"); // ADDED: remove when implemented
    }

    public static void ChangeChannel(Player source, GameController gc) {
        gc.debugNotImplemented("ChangeChannel"); // ADDED: remove when implemented
    }

    public static void SpareChange(Player source, GameController gc) {
        gc.debugNotImplemented("SpareChange"); // ADDED: remove when implemented
    }

    public static void SpaDay(Player source, GameController gc) {
        // FIXED: was "gameController.players[gameController.input.]" — incomplete expression
        Player target = gc.players[gc.input];
        gc.changeHealth(1, source);
        gc.changeHealth(1, target);
    }

    public static void OlReliable(Player source, GameController gc) {
        gc.drawCards(2, source); // IMPLEMENTED: draw 2 cards
    }

    public static void BarterTime(Player source, GameController gc) {
        gc.debugNotImplemented("BarterTime"); // ADDED: remove when implemented
    }

    public static void ResetButton(Player source, GameController gc) {
        gc.debugNotImplemented("ResetButton"); // ADDED: remove when implemented
    }

    public static void InItToWinIt(Player source, GameController gc) {
        gc.debugNotImplemented("InItToWinIt"); // ADDED: remove when implemented
    }

    public static void GooglyEyedRock(Player source, GameController gc) {
        gc.debugNotImplemented("GooglyEyedRock"); // ADDED: remove when implemented
    }

    public static void PhoneAFriend(Player source, GameController gc) {
        gc.debugNotImplemented("PhoneAFriend"); // ADDED: remove when implemented
    }

    public static void FickleFungus(Player source, GameController gc) {
        gc.debugNotImplemented("FickleFungus"); // ADDED: remove when implemented
    }

    public static void UnderdogDuel(Player source, GameController gc) {
        gc.debugNotImplemented("UnderdogDuel"); // ADDED: remove when implemented
    }

    // -------------------------------------------------------------------------
    // SHAKE-UP CARDS
    // -------------------------------------------------------------------------

    public static void NuclearWinter(Player source, GameController gc) {
        gc.state.setDrawBlockedRotations(1); // IMPLEMENTED
    }

    public static void FlipSide(Player source, GameController gc) {
        gc.debugNotImplemented("FlipSide"); // ADDED: remove when implemented
    }

    public static void UncertaintyPrinciple(Player source, GameController gc) {
        gc.debugNotImplemented("UncertaintyPrinciple"); // ADDED: remove when implemented
    }

    public static void AbsoluteZero(Player source, GameController gc) {
        source.setCardEffectImmune(1); // IMPLEMENTED
    }

    // -------------------------------------------------------------------------
    // SIGNATURE CARDS
    // -------------------------------------------------------------------------

    public static void MassExtinction(Player source, GameController gc) {
        // IMPLEMENTED: everyone except source takes -2 HP
        for (Player p : gc.players) {
            if (p != null && p != source) gc.changeHealth(-2, p);
        }
    }

    public static void EconomicRecession(Player source, GameController gc) {
        // IMPLEMENTED: everyone except source discards their entire hand
        for (Player p : gc.players) {
            if (p != null && p != source) {
                gc.state.discardPile.addAll(p.hand);
                p.hand.clear();
            }
        }
    }

    public static void HeatDeathUniverse(Player source, GameController gc) {
        gc.state.grantExtraTurn = true; // IMPLEMENTED: handled in changeTurn()
    }

    public static void Party(Player source, GameController gc) {
        gc.debugNotImplemented("Party"); // ADDED: remove when implemented
    }

    // -------------------------------------------------------------------------
    // LIL' BUDDY PASSIVES
    // -------------------------------------------------------------------------

    public static void FighterSnail(Player source, GameController gc) {
        gc.debugNotImplemented("FighterSnail"); // ADDED: remove when implemented
    }

    public static void InferiorTowing(Player source, GameController gc) {
        gc.debugNotImplemented("InferiorTowing"); // ADDED: remove when implemented
    }

    public static void WormholeWorm(Player source, GameController gc) {
        gc.debugNotImplemented("WormholeWorm"); // ADDED: remove when implemented
    }

    public static void SockPuppet(Player source, GameController gc) {
        gc.changeHealth(1, source); // IMPLEMENTED: +1 HP on placement
    }

    public static void WashingMachine(Player source, GameController gc) {
        gc.debugNotImplemented("WashingMachine"); // ADDED: remove when implemented
    }

    public static void FiveHundredDuck(Player source, GameController gc) {
        gc.debugNotImplemented("FiveHundredDuck"); // ADDED: remove when implemented
    }

    public static void FlyingFish(Player source, GameController gc) {
        gc.debugNotImplemented("FlyingFish"); // ADDED: remove when implemented
    }

    public static void DiscoGhost(Player source, GameController gc) {
        gc.debugNotImplemented("DiscoGhost"); // ADDED: remove when implemented
    }

    public static void DarrelJr(Player source, GameController gc) {
        gc.debugNotImplemented("DarrelJr"); // ADDED: remove when implemented
    }

    public static void PiggyBank(Player source, GameController gc) {
        gc.debugNotImplemented("PiggyBank"); // ADDED: remove when implemented
    }

    public static void HeraldBerald(Player source, GameController gc) {
        gc.debugNotImplemented("HeraldBerald"); // ADDED: remove when implemented
    }

    public static void MagicMirror(Player source, GameController gc) {
        gc.debugNotImplemented("MagicMirror"); // ADDED: remove when implemented
    }

    public static void SillyGoose(Player source, GameController gc) {
        gc.debugNotImplemented("SillyGoose"); // ADDED: remove when implemented
    }

    public static void JamJellyfish(Player source, GameController gc) {
        gc.debugNotImplemented("JamJellyfish"); // ADDED: remove when implemented
    }

    public static void HotPotato(Player source, GameController gc) {
        gc.debugNotImplemented("HotPotato"); // ADDED: remove when implemented
    }

    public static void LlamaAlpaca(Player source, GameController gc) {
        gc.debugNotImplemented("LlamaAlpaca"); // ADDED: remove when implemented
    }

    public static void EarlyBird(Player source, GameController gc) {
        gc.debugNotImplemented("EarlyBird"); // ADDED: remove when implemented
    }

    public static void SleepyGiraffe(Player source, GameController gc) {
        gc.debugNotImplemented("SleepyGiraffe"); // ADDED: remove when implemented
    }
}