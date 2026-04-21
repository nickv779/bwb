package com.ex.bwb.scenes;

import android.app.Activity;
import android.graphics.Color;
import android.opengl.GLES30;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.ex.bwb.cards.TextureRepository;
import com.ex.bwb.networking.GameClient;
import com.ex.bwb.networking.packets.PlayCardPacket;

import engine.activities.GLActivity;
import engine.models.CardRenderer;
import engine.models.GameObject;
import engine.shaders.Texture;
import engine.J4Q;

import java.util.ArrayList;
import java.util.List;

public class GameScene implements Scene {

    private static final String TAG = "GameScene";

    private final GLActivity glActivity;
    private final Activity activity;
    private final GameClient client;
    private final int myPlayerId;

    // Hand
    private final List<CardRenderer> handCards = new ArrayList<>();
    private String[] currentCardNames = new String[0];
    private volatile String[] pendingHandSync = null;
    private int zoomedCardIndex = -1;

    // Layout constants
    private static final float CARD_SCALE       = 0.7f;
    private static final float CARD_ZOOM_SCALE  = 1.3f;
    private static final float FAN_RADIUS       = 4.5f;   // arc radius
    private static final float FAN_SPREAD       = 12f;    // degrees between cards
    private static final float FAN_Y_OFFSET     = -3.5f;  // arc center below screen

    // Turn state
    private volatile boolean isMyTurn = false;
    private volatile int currentTurnPlayerId = 0;
    private volatile boolean pendingTurnUpdate = false;

    // UI
    private TextView bigBuddyLabel;
    private TextView turnLabel;

    // Touch
    private float touchStartX = 0f;
    private float touchStartY = 0f;
    private static final float SWIPE_UP_THRESHOLD = -150f;

    public GameScene(GLActivity glActivity, GameClient client, int myPlayerId,
                     String[] initialHand, int initialCurrentPlayer) {
        this.glActivity          = glActivity;
        this.activity            = (Activity) glActivity;
        this.client              = client;
        this.myPlayerId          = myPlayerId;
        this.pendingHandSync     = initialHand;
        this.currentTurnPlayerId = initialCurrentPlayer >= 0 ? initialCurrentPlayer : 0;
        this.pendingTurnUpdate   = true;
    }

    // -------------------------------------------------------------------------
    // LIFECYCLE
    // -------------------------------------------------------------------------

    @Override
    public void onEnter() {
        glActivity.scene.background(0.05f, 0.05f, 0.1f);
        setupClientListener();
    }

    @Override
    public void onExit() {
        clearHand();
    }

    @Override
    public void update() {
        // Apply hand sync from network thread
        if (pendingHandSync != null) {
            String[] names = pendingHandSync;
            pendingHandSync = null;
            applyHandSync(names);
        }

        // Update turn label from network thread
        if (pendingTurnUpdate) {
            pendingTurnUpdate = false;
            updateTurnLabel();
        }
    }

    @Override
    public void draw() {
        GLES30.glClearColor(0.05f, 0.05f, 0.1f, 1f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        glActivity.scene.view.identity();
        glActivity.scene.view.translate(0, 0, -5f);
        glActivity.scene.draw();
    }

    @Override
    public void onTouch(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = event.getX();
                touchStartY = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                float dy = event.getY() - touchStartY;
                if (dy < SWIPE_UP_THRESHOLD && zoomedCardIndex >= 0) {
                    handleSwipeUp();
                } else {
                    handleTap();
                }
                break;
        }
    }

    @Override
    public void buildUI(FrameLayout overlay) {
        // Big Buddy name — top left
        bigBuddyLabel = new TextView(activity);
        bigBuddyLabel.setText(getBigBuddyName(myPlayerId));
        bigBuddyLabel.setTextColor(Color.WHITE);
        bigBuddyLabel.setTextSize(18);
        bigBuddyLabel.setPadding(20, 20, 20, 20);
        FrameLayout.LayoutParams bbParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        bbParams.gravity    = Gravity.TOP | Gravity.START;
        bbParams.topMargin  = 40;
        bbParams.leftMargin = 40;
        bigBuddyLabel.setLayoutParams(bbParams);
        overlay.addView(bigBuddyLabel);

        // Turn indicator — top right
        turnLabel = new TextView(activity);
        turnLabel.setText("Waiting...");
        turnLabel.setTextColor(Color.YELLOW);
        turnLabel.setTextSize(18);
        turnLabel.setPadding(20, 20, 20, 20);
        FrameLayout.LayoutParams tlParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        tlParams.gravity    = Gravity.TOP | Gravity.END;
        tlParams.topMargin  = 40;
        tlParams.rightMargin = 40;
        turnLabel.setLayoutParams(tlParams);
        overlay.addView(turnLabel);
    }

    // -------------------------------------------------------------------------
    // HAND MANAGEMENT
    // -------------------------------------------------------------------------

    private void applyHandSync(String[] cardNames) {
        clearHand();
        currentCardNames = cardNames;
        int count = cardNames.length;

        for (int i = 0; i < count; i++) {
            Texture front = nameToTexture(cardNames[i]);
            CardRenderer card = new CardRenderer(front, TextureRepository.BlueBack());
            placeCardInFan(card, i, count, false);
            glActivity.scene.appendChild(card);
            handCards.add(card);
        }
        zoomedCardIndex = -1;
    }

    private void clearHand() {
        for (CardRenderer c : handCards) glActivity.scene.removeChild(c);
        handCards.clear();
        zoomedCardIndex = -1;
    }

    // Places a card at its fan position
    private void placeCardInFan(CardRenderer card, int index, int total, boolean zoomed) {
        if (zoomed) {
            card.transform.identity();
            card.transform.translate(0f, -0.5f, 0.5f);
            card.transform.scale(CARD_ZOOM_SCALE);
            return;
        }

        // Fan arc: cards arranged in a arc at the bottom
        float totalSpread = FAN_SPREAD * (total - 1);
        float startAngle  = -totalSpread / 2f;
        float angle       = startAngle + index * FAN_SPREAD; // degrees
        float rad         = (float) Math.toRadians(angle);

        float x = FAN_RADIUS * (float) Math.sin(rad);
        float y = FAN_Y_OFFSET + FAN_RADIUS * (float) Math.cos(rad);

        card.transform.identity();
        card.transform.translate(x, y, 0f);
        card.transform.rotateZ(-angle);
        card.transform.scale(CARD_SCALE);
    }

    // -------------------------------------------------------------------------
    // TOUCH HANDLING
    // -------------------------------------------------------------------------

    private void handleTap() {
        GameObject touched = J4Q.touchScreen.pickObject(0);
        if (touched == null) return;

        int count = handCards.size();
        for (int i = 0; i < count; i++) {
            if (!handCards.get(i).isHit(touched)) continue;

            if (zoomedCardIndex == i) {
                // Un-zoom
                placeCardInFan(handCards.get(i), i, count, false);
                zoomedCardIndex = -1;
            } else {
                // Restore previously zoomed
                if (zoomedCardIndex >= 0 && zoomedCardIndex < count) {
                    placeCardInFan(handCards.get(zoomedCardIndex),
                            zoomedCardIndex, count, false);
                }
                // Zoom this one
                placeCardInFan(handCards.get(i), i, count, true);
                zoomedCardIndex = i;
            }
            return;
        }
    }

    private void handleSwipeUp() {
        if (!isMyTurn) return;
        if (zoomedCardIndex < 0 || zoomedCardIndex >= handCards.size()) return;

        int cardIndex    = zoomedCardIndex;
        int targetId     = (myPlayerId + 1) % 4; // default target: next player

        client.sendAction(new PlayCardPacket(myPlayerId, cardIndex, targetId));

        glActivity.scene.removeChild(handCards.remove(cardIndex));
        currentCardNames = removeIndex(currentCardNames, cardIndex);
        zoomedCardIndex  = -1;

        // Re-layout remaining cards
        int count = handCards.size();
        for (int i = 0; i < count; i++) {
            placeCardInFan(handCards.get(i), i, count, false);
        }
    }

    // -------------------------------------------------------------------------
    // TURN UI
    // -------------------------------------------------------------------------

    private void updateTurnLabel() {
        String text;
        int color;
        if (currentTurnPlayerId == myPlayerId) {
            isMyTurn = true;
            text  = "Your Turn!";
            color = Color.GREEN;
        } else {
            isMyTurn = false;
            text  = "It's " + getBigBuddyName(currentTurnPlayerId) + "'s Turn!";
            color = Color.YELLOW;
        }
        final String finalText  = text;
        final int    finalColor = color;
        activity.runOnUiThread(() -> {
            if (turnLabel != null) {
                turnLabel.setText(finalText);
                turnLabel.setTextColor(finalColor);
            }
        });
    }

    // -------------------------------------------------------------------------
    // NETWORK LISTENER
    // -------------------------------------------------------------------------

    private void setupClientListener() {
        client.setGameEventListener(new GameClient.GameEventListener() {
            @Override public void onConnected() {}
            @Override public void onPlayerJoined(int totalPlayers) {}
            @Override public void onGameStarted(int playerId) {}

            @Override
            public void onHandSync(String[] cardNames) {
                pendingHandSync = cardNames;
            }

            @Override
            public void onTurnUpdate(String message) {}

            @Override
            public void onTurnChanged(int currentPlayerId) {
                currentTurnPlayerId = currentPlayerId;
                pendingTurnUpdate   = true;
            }

            @Override
            public void onMyTurn() {
                isMyTurn = true;
                currentTurnPlayerId = myPlayerId;
                pendingTurnUpdate   = true;
            }
        });
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private String getBigBuddyName(int playerId) {
        switch (playerId) {
            case 0: return "Darrel";
            case 1: return "Fernando";
            case 2: return "Gerald";
            case 3: return "Mr. Ostrich";
            default: return "Unknown";
        }
    }

    private Texture nameToTexture(String name) {
        switch (name) {
            case "Katana":               return TextureRepository.Katana();
            case "MagicWand":            return TextureRepository.MagicWand();
            case "Grenade":              return TextureRepository.Grenade();
            case "Knife":                return TextureRepository.Knife();
            case "Tackle":               return TextureRepository.Tackle();
            case "BedTime":              return TextureRepository.BedTime();
            case "DumpsterDiving":       return TextureRepository.DumpsterDiving();
            case "BeachEpisode":         return TextureRepository.BeachEpisode();
            case "PickPocket":           return TextureRepository.PickPocket();
            case "NoSoliciting":         return TextureRepository.NoSoliciting();
            case "ChangeChannel":        return TextureRepository.ChangeChannel();
            case "SpareChange":          return TextureRepository.SpareChange();
            case "SpaDay":               return TextureRepository.SpaDay();
            case "OlReliable":           return TextureRepository.OlReliable();
            case "BarterTime":           return TextureRepository.BarterTime();
            case "ResetButton":          return TextureRepository.ResetButton();
            case "InItToWinIt":          return TextureRepository.InItToWinIt();
            case "GooglyEyedRock":       return TextureRepository.GooglyEyedRock();
            case "PhoneAFriend":         return TextureRepository.PhoneAFriend();
            case "FickleFungus":         return TextureRepository.FickleFungus();
            case "UnderdogDuel":         return TextureRepository.UnderdogDuel();
            case "NuclearWinter":        return TextureRepository.NuclearWinter();
            case "FlipSide":             return TextureRepository.FlipSide();
            case "UncertaintyPrinciple": return TextureRepository.UncertaintyPrinciple();
            case "AbsoluteZero":         return TextureRepository.AbsoluteZero();
            case "MassExtinction":       return TextureRepository.MassExtinction();
            case "EconomicRecession":    return TextureRepository.EconomicRegression();
            case "HeatDeathUniverse":    return TextureRepository.HeatDeath();
            case "Party":                return TextureRepository.Party();
            case "OneStepCloser":        return TextureRepository.Objective();
            default:                     return TextureRepository.BlueBack();
        }
    }

    private String[] removeIndex(String[] arr, int index) {
        String[] result = new String[arr.length - 1];
        for (int i = 0, j = 0; i < arr.length; i++) {
            if (i != index) result[j++] = arr[i];
        }
        return result;
    }
}