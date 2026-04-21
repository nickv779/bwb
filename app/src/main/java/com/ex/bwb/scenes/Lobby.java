package com.ex.bwb.scenes;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.wifi.WifiManager;
import android.opengl.GLES30;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.ex.bwb.cards.TextureRepository;
import com.ex.bwb.networking.GameClient;
import com.ex.bwb.networking.GameServer;
import com.ex.bwb.networking.ServerDiscovery;

import engine.activities.GLActivity;
import engine.models.CardRenderer;
import engine.models.ObjectMaker;
import engine.shaders.Text;
import engine.shaders.TextureShader;
import engine.models.GameObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Lobby implements Scene {

    private static final String TAG = "Lobby";
    private int testValue = 0;
    public enum Mode { CLIENT, HOST, TEST_4P }
    private static final Mode MODE = Mode.CLIENT;
    private static final int PORT = 5556;
    private static final int PLAYIT_PORT = 19156;
    private static final String PLAYIT_HOST = "209.25.140.19";

    private final Activity activity;
    private final GLActivity glActivity;

    // Networking
    private GameClient client;
    private GameClient[] testClients;
    private GameServer server;
    private ServerDiscovery discovery;
    private WifiManager.MulticastLock multicastLock;

    // Card reveal — one card per connected player
    private static final float[] CARD_X = { -2.1f, -0.7f, 0.7f, 2.1f };
    private final List<CardRenderer> playerCards = new ArrayList<>();
    private final float[] cardAngles = new float[4];
    private int connectedPlayers = 0;
    private final AtomicInteger pendingPlayerJoined = new AtomicInteger(0);


    // Status UI
    private TextView connectionLabel;
    private Text statusTextGL;
    private GameObject textModel;
    private volatile String pendingStatus = null;
    private Paint textPaint;

    // Transition
    private volatile boolean pendingTransitionToGame = false;
    private volatile int myPlayerId = -1;
    private static final float TRANSITION_DELAY_SECONDS = 3f;
    private float transitionTimer = 0f;
    private boolean transitionStarted = false;

    // In Lobby — add these fields:
    private volatile String[] receivedHand = null;
    private volatile int receivedCurrentPlayer = -1;

    public Lobby(GLActivity glActivity) {
        this.glActivity = glActivity;
        this.activity = (Activity) glActivity;
    }

    @Override
    public void onEnter() {
        TextureRepository.init();
        glActivity.scene.background(0.05f, 0.05f, 0.1f);

        // Status text
        ObjectMaker om = new ObjectMaker();
        om.color(1, 1, 1);
        om.box(5f, 0.4f, 0.01f);
        textModel = om.flushModel(true, true, true, true);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40);

        statusTextGL = new Text(1024, 128);
        statusTextGL.setText("Waiting for players...", 0, 40, textPaint);

        TextureShader ts = new TextureShader();
        ts.setTexture(statusTextGL);
        textModel.setShader(ts);
        glActivity.scene.appendChild(textModel);
        textModel.transform.translate(0, 2.2f, 0f);

        setupNetworking();
    }

    @Override
    public void onExit() {
        for (CardRenderer c : playerCards) glActivity.scene.removeChild(c);
        playerCards.clear();

        if (textModel != null) {
            glActivity.scene.removeChild(textModel);
            textModel = null;
        }
        if (discovery != null) discovery.stop();
        if (multicastLock != null && multicastLock.isHeld()) multicastLock.release();
        // Note: do NOT disconnect client here — pass it to GameScene
    }

    @Override
    public void update() {
        // Update status text
        if (pendingStatus != null && statusTextGL != null) {
            statusTextGL.setText(pendingStatus, 0, 40, textPaint);
            pendingStatus = null;
        }
        testValue = 10;
        // Reveal a new card when a player joins
        if (pendingPlayerJoined.get() > connectedPlayers) {
            testValue = 100;
            int target = pendingPlayerJoined.get();
            while (connectedPlayers < target) {
                revealNextCard();
            }
        }

        // Animate spinning cards
        float dt = glActivity.scene.perSec();
        for (int i = 0; i < playerCards.size(); i++) {
            cardAngles[i] += dt * 40f;
            if (cardAngles[i] > 360f) cardAngles[i] -= 360f;

            CardRenderer card = playerCards.get(i);
            card.transform.identity();
            card.transform.translate(CARD_X[i], 0f, 0f);
            card.transform.rotateY(cardAngles[i]);
            card.transform.scale(1.3f);
        }

        // Countdown to game transition once all 4 joined
        if (transitionStarted && !pendingTransitionToGame) {
            transitionTimer -= dt;
            updateStatus("Starting in " + (int) Math.ceil(transitionTimer) + "...");
            if (transitionTimer <= 0f) {
                pendingTransitionToGame = true;
            }
        }

        // Transition to GameScene on GL thread
        if (pendingTransitionToGame) {
            pendingTransitionToGame = false;
            // Show what we have
            updateConnectionLabel("Hand: " + (receivedHand == null ? "NULL" : receivedHand.length + " cards")
                    + " Player: " + myPlayerId, Color.WHITE);
            SceneManager.get().transitionTo(
                    new GameScene(glActivity, client, myPlayerId, receivedHand, receivedCurrentPlayer)
            );
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
    public void buildUI(FrameLayout overlay) {
        Button backBtn = new Button(activity);
        backBtn.setText("Back to Menu");
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        p.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        p.bottomMargin = 100;
        backBtn.setLayoutParams(p);
        backBtn.setOnClickListener(v ->
                SceneManager.get().transitionTo(new MainMenu(glActivity)));
        overlay.addView(backBtn);

        connectionLabel = new TextView(activity);
        connectionLabel.setText("⚫ Not connected");
        connectionLabel.setTextColor(Color.WHITE);
        connectionLabel.setTextSize(16);
        connectionLabel.setPadding(20, 20, 20, 20);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.topMargin = 40;
        lp.leftMargin = 40;
        connectionLabel.setLayoutParams(lp);
        overlay.addView(connectionLabel);
    }

    // -------------------------------------------------------------------------
    // CARD REVEAL
    // -------------------------------------------------------------------------

    private void revealNextCard() {
        if (connectedPlayers >= 4) return;
        int i = connectedPlayers;

        CardRenderer card = new CardRenderer(
                getBigBuddyTexture(i),
                TextureRepository.BlueBack()
        );
        card.transform.identity();
        card.transform.translate(CARD_X[i], 0f, 0f);
        card.transform.scale(1.3f);

        glActivity.scene.appendChild(card);
        playerCards.add(card);
        connectedPlayers++;

        updateStatus("Players: " + connectedPlayers + "/4");

        if (connectedPlayers == 4) {
            transitionStarted = true;
            transitionTimer = TRANSITION_DELAY_SECONDS;
            updateConnectionLabel("🟢 All players connected!", Color.GREEN);
        }
    }

    private engine.shaders.Texture getBigBuddyTexture(int index) {
        switch (index) {
            case 0: return TextureRepository.Darrel();
            case 1: return TextureRepository.Fernando();
            case 2: return TextureRepository.Gerald();
            case 3: return TextureRepository.MrOstrich();
            default: return TextureRepository.BlueBack();
        }
    }

    // -------------------------------------------------------------------------
    // NETWORKING
    // -------------------------------------------------------------------------

    private void setupNetworking() {
        WifiManager wifi = (WifiManager) activity
                .getApplicationContext().getSystemService(Activity.WIFI_SERVICE);
        multicastLock = wifi.createMulticastLock("bwb_lock");
        multicastLock.setReferenceCounted(true);
        multicastLock.acquire();

        discovery = new ServerDiscovery();

        activity.runOnUiThread(() -> {
            switch (MODE) {
                case HOST:
                    server = new GameServer(PORT);
                    server.start();
                    discovery.startBroadcasting("BuddiesWithBenefits", PORT);
                    updateStatus("Hosting... Waiting for players...");
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        client = new GameClient();
                        setupClientListener(client);
                        client.connect("127.0.0.1", PORT);
                    }, 1000);
                    break;

                case TEST_4P:
                    server = new GameServer(PORT);
                    server.start();
                    updateStatus("Connecting test players...");
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        testClients = new GameClient[4];
                        for (int i = 0; i < 4; i++) {
                            testClients[i] = new GameClient();
                            int idx = i;
                            testClients[i].setGameEventListener(new GameClient.GameEventListener() {
                                @Override public void onConnected() {
                                    pendingPlayerJoined.incrementAndGet();
                                }
                                @Override public void onGameStarted(int playerId) {
                                    if (idx == 0) {
                                        myPlayerId = playerId;
                                        transitionStarted = true;
                                        transitionTimer = TRANSITION_DELAY_SECONDS;
                                    }
                                }
                                @Override public void onTurnUpdate(String message) { updateStatus(message); }
                                @Override
                                public void onHandSync(String[] cardNames) {
                                    receivedHand = cardNames;
                                }

                                @Override
                                public void onTurnChanged(int currentPlayerId) {
                                    receivedCurrentPlayer = currentPlayerId;
                                }

                                @Override
                                public void onMyTurn() {
                                    if (receivedCurrentPlayer == -1) receivedCurrentPlayer = myPlayerId;
                                }

                                @Override public void onPlayerJoined(int totalPlayers) {
                                    pendingPlayerJoined.set(totalPlayers);
                                }
                            });
                            testClients[i].connect("127.0.0.1", PORT);
                        }
                        client = testClients[0];
                    }, 1000);
                    break;

                case CLIENT:
                default:
                    updateStatus("Connecting to server...");
                    updateConnectionLabel("🟡 Connecting...", Color.YELLOW);
                    client = new GameClient();
                    setupClientListener(client);
                    client.connect(PLAYIT_HOST, PLAYIT_PORT);
                    break;
            }
        });
    }

    private void setupClientListener(GameClient c) {
        c.setGameEventListener(new GameClient.GameEventListener() {
            @Override
            public void onConnected() {
                updateConnectionLabel("🟡 Connected — waiting for players " + testValue, Color.YELLOW);
                // no card reveal here anymore
            }

            @Override
            public void onHandSync(String[] cardNames) {
                receivedHand = cardNames;
            }

            @Override
            public void onTurnChanged(int currentPlayerId) {
                receivedCurrentPlayer = currentPlayerId;
            }

            @Override
            public void onMyTurn() {
                if (receivedCurrentPlayer == -1) receivedCurrentPlayer = myPlayerId;
            }

            @Override
            public void onPlayerJoined(int totalPlayers) {
                testValue = 20;
                pendingPlayerJoined.set(totalPlayers); // use server's count directly
                updateConnectionLabel("🟡 Players: " + totalPlayers + "/4", Color.YELLOW);
            }
            @Override
            public void onGameStarted(int playerId) {
                myPlayerId = playerId;
                updateConnectionLabel("🟢 Game starting!", Color.GREEN);
            }
            @Override
            public void onTurnUpdate(String message) { updateStatus(message); }
        });
    }

    private void updateStatus(String message) { pendingStatus = message; }

    private void updateConnectionLabel(String text, int color) {
        activity.runOnUiThread(() -> {
            if (connectionLabel != null) {
                connectionLabel.setText(text);
                connectionLabel.setTextColor(color);
            }
        });
    }
}