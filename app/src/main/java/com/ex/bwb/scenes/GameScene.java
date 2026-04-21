package com.ex.bwb.scenes;

import android.app.Activity;
import android.net.wifi.WifiManager;
import android.opengl.GLES30;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.graphics.Paint;
import android.graphics.Color;

import com.ex.bwb.networking.GameClient;
import com.ex.bwb.networking.GameServer;
import com.ex.bwb.networking.ServerDiscovery;
import com.ex.bwb.networking.packets.DrawCardPacket;
import com.ex.bwb.networking.packets.EndTurnPacket;
import com.ex.bwb.networking.packets.PlayCardPacket;
import com.ex.bwb.cards.TextureRepository;

import engine.activities.GLActivity;
import engine.models.CardRenderer;
import engine.models.GameObject;
import engine.models.ObjectMaker;
import engine.shaders.Text;
import engine.shaders.Texture;
import engine.shaders.TextureShader;
import engine.J4Q;

import java.util.ArrayList;
import java.util.List;

public class GameScene implements Scene {

    private static final String TAG = "GameScene";

    public enum Mode { CLIENT, HOST, TEST_4P }
    private static final Mode MODE = Mode.CLIENT;
    private static final int PORT = 5556;
    private static final int PLAYIT_PORT = 19156; // replace with your actual playit port
    private static final String PLAYIT_HOST = "209.25.140.19"; // replace with your actual playit hostname

    // Add these member variables at the top of GameScene
    private volatile String pendingUIStatus = null;
    private android.widget.TextView connectionLabel;
    private final GLActivity glActivity;
    private final Activity activity;

    private GameServer server;
    private GameClient client;
    private GameClient[] testClients;
    private ServerDiscovery discovery;
    private WifiManager.MulticastLock multicastLock;

    // Status text
    private Text statusTextGL;
    private GameObject textModel;
    private volatile String pendingStatus = null;
    private Paint textPaint;

    private GestureDetector gestureDetector;

    // -------------------------------------------------------------------------
    // PHASE
    // -------------------------------------------------------------------------
    private enum GamePhase { LOBBY, PLAYING }
    private GamePhase phase = GamePhase.LOBBY;

    // -------------------------------------------------------------------------
    // LOBBY — Big Buddy cards spin while connecting
    // -------------------------------------------------------------------------
    // X positions for the 4 lobby cards — stored as field so update() can use them
    private static final float[] LOBBY_X = { -2.1f, -0.7f, 0.7f, 2.1f };
    private final List<CardRenderer> lobbyCards   = new ArrayList<>();
    private final float[]            lobbyAngles  = new float[4]; // spin angles

    // -------------------------------------------------------------------------
    // GAME — hand + turn indicator
    // -------------------------------------------------------------------------
    private final List<CardRenderer> handCards     = new ArrayList<>();
    private CardRenderer             turnIndicator = null; // small Big Buddy top-left
    private int                      currentTurnPlayer = 0;
    private int                      myPlayerId        = -1;

    // Card interaction
    private int   zoomedCardIndex = -1;
    private static final float CARD_HAND_SCALE   = 1.0f;
    private static final float CARD_ZOOMED_SCALE = 1.8f;
    private static final float CARD_INDICATOR_SCALE = 0.35f;

    // Swipe detection
    private float touchStartY = 0f;
    private float touchStartX = 0f;
    private static final float SWIPE_THRESHOLD = -150f;

    // GL-thread transition flags
    private volatile boolean pendingGameTransition      = false;
    private volatile int     pendingTransitionPlayerId  = -1;
    private volatile boolean pendingTurnIndicatorUpdate = false;

    public GameScene(GLActivity glActivity) {
        this.glActivity = glActivity;
        this.activity   = (Activity) glActivity;
    }

    // -------------------------------------------------------------------------
    // LIFECYCLE
    // -------------------------------------------------------------------------

    @Override
    public void onEnter() {
        TextureRepository.init();
        glActivity.scene.background(0.05f, 0.05f, 0.1f);

        // Status text — positioned near top centre
        ObjectMaker om = new ObjectMaker();
        om.color(1, 1, 1);
        om.box(5f, 0.4f, 0.01f);
        textModel = om.flushModel(true, true, true, true);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40);

        statusTextGL = new Text(1024, 128);
        statusTextGL.setText("Connecting...", 0, 40, textPaint);

        TextureShader ts = new TextureShader();
        ts.setTexture(statusTextGL);
        textModel.setShader(ts);

        glActivity.scene.appendChild(textModel);
        // z=0 means 5 units in front of camera (view is at -5)
        textModel.transform.translate(0, 2.0f, 0f);

        buildLobbyCards();
        setupNetworking();
    }

    @Override
    public void onExit() {
        clearLobbyCards();
        clearHandCards();
        removeTurnIndicator();

        if (textModel != null) {
            glActivity.scene.removeChild(textModel);
            textModel = null;
        }
        if (discovery    != null) discovery.stop();
        if (client       != null) client.disconnect();
        if (testClients  != null) {
            for (GameClient c : testClients) if (c != null) c.disconnect();
        }
        if (server       != null) server.stop();
        if (multicastLock != null && multicastLock.isHeld()) multicastLock.release();
    }

    @Override
    public void update() {
        if (client != null && !client.isConnected() && phase == GamePhase.LOBBY) {
            updateConnectionLabel("🔴 Connection failed — check server", Color.RED);
        }

        // Status text
        if (pendingStatus != null && statusTextGL != null) {
            statusTextGL.setText(pendingStatus, 0, 40, textPaint);
            pendingStatus = null;
        }

        // Lobby → game transition (must happen on GL thread)
        if (pendingGameTransition) {
            pendingGameTransition = false;
            phase = GamePhase.PLAYING;
            clearLobbyCards();
            buildHandCards();
            buildTurnIndicator(currentTurnPlayer);
            updateStatus("Game started — Player 0's turn");
        }

        // Turn indicator swap (must happen on GL thread)
        if (pendingTurnIndicatorUpdate) {
            pendingTurnIndicatorUpdate = false;
            buildTurnIndicator(currentTurnPlayer);
        }

        // Animate lobby cards — spin on Y axis
        if (phase == GamePhase.LOBBY) {
            float dt = glActivity.scene.perSec();
            for (int i = 0; i < lobbyCards.size(); i++) {
                lobbyAngles[i] += dt * 40f; // 40 degrees per second
                if (lobbyAngles[i] > 360f) lobbyAngles[i] -= 360f;

                CardRenderer card = lobbyCards.get(i);
                card.transform.identity();
                card.transform.translate(LOBBY_X[i], 0f, 0f);
                card.transform.rotateY(lobbyAngles[i]);
                card.transform.scale(1.3f); // FIXED: was 0.6f → tiny; now 1.3f → readable
            }
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
        // existing back button
        Button backBtn = new Button(activity);
        backBtn.setText("Menu");
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        p.gravity     = Gravity.TOP | Gravity.END;
        p.topMargin   = 40;
        p.rightMargin = 40;
        backBtn.setLayoutParams(p);
        backBtn.setOnClickListener(v ->
                SceneManager.get().transitionTo(new MainMenu(glActivity)));
        overlay.addView(backBtn);

        // Connection status label
        connectionLabel = new android.widget.TextView(activity);
        connectionLabel.setText("⚫ Not connected");
        connectionLabel.setTextColor(Color.WHITE);
        connectionLabel.setTextSize(16);
        connectionLabel.setPadding(20, 20, 20, 20);

        FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        labelParams.gravity   = Gravity.TOP | Gravity.START;
        labelParams.topMargin = 40;
        labelParams.leftMargin = 40;
        connectionLabel.setLayoutParams(labelParams);
        overlay.addView(connectionLabel);
    }

    private void updateConnectionLabel(String text, int color) {
        activity.runOnUiThread(() -> {
            if (connectionLabel != null) {
                connectionLabel.setText(text);
                connectionLabel.setTextColor(color);
            }
        });
    }

    // -------------------------------------------------------------------------
    // LOBBY CARDS
    // -------------------------------------------------------------------------

    private void buildLobbyCards() {
        for (int i = 0; i < 4; i++) {
            CardRenderer card = new CardRenderer(
                    getBigBuddyTexture(i),
                    TextureRepository.BlueBack()
            );
            // Initial transform — update() will animate it every frame
            card.transform.identity();
            card.transform.translate(LOBBY_X[i], 0f, 0f);
            card.transform.scale(1.3f);

            glActivity.scene.appendChild(card);
            lobbyCards.add(card);
        }
    }

    private void clearLobbyCards() {
        for (CardRenderer c : lobbyCards) glActivity.scene.removeChild(c);
        lobbyCards.clear();
    }

    // -------------------------------------------------------------------------
    // HAND CARDS — shown big in the centre of the screen
    // -------------------------------------------------------------------------

    private void buildHandCards() {
        clearHandCards();

        // Placeholder: 7 card-backs until hand-sync packets are implemented
        int   cardCount = 7;
        float spacing   = 0.72f;    // horizontal gap between card centres
        float totalW    = spacing * (cardCount - 1);
        float startX    = -totalW / 2f;
        float yPos      = -0.3f;    // slightly below screen centre
        float zPos      = 0f;       // same z as lobby cards — 5 units from camera

        for (int i = 0; i < cardCount; i++) {
            float x     = startX + i * spacing;
            float tilt  = (i - (cardCount - 1) / 2f) * 4f; // slight fan tilt

            CardRenderer card = new CardRenderer(
                    TextureRepository.BlueBack(),
                    TextureRepository.BlueBack()
            );

            card.transform.identity();
            card.transform.translate(x, yPos, zPos);
            card.transform.rotateZ(-tilt);
            card.transform.scale(CARD_HAND_SCALE);

            glActivity.scene.appendChild(card);
            handCards.add(card);
        }

        zoomedCardIndex = -1;
    }

    private void clearHandCards() {
        for (CardRenderer c : handCards) glActivity.scene.removeChild(c);
        handCards.clear();
        zoomedCardIndex = -1;
    }

    // -------------------------------------------------------------------------
    // TURN INDICATOR — small Big Buddy card in top-left corner
    // -------------------------------------------------------------------------

    private void buildTurnIndicator(int playerIndex) {
        removeTurnIndicator();

        turnIndicator = new CardRenderer(
                getBigBuddyTexture(playerIndex),
                TextureRepository.RedBack()
        );

        // Top-left corner: x=-3.2, y=1.5 at z=0
        turnIndicator.transform.identity();
        turnIndicator.transform.translate(-3.2f, 1.6f, 0f);
        turnIndicator.transform.scale(CARD_INDICATOR_SCALE);

        glActivity.scene.appendChild(turnIndicator);
    }

    private void removeTurnIndicator() {
        if (turnIndicator != null) {
            glActivity.scene.removeChild(turnIndicator);
            turnIndicator = null;
        }
    }

    // -------------------------------------------------------------------------
    // TEXTURE HELPER
    // -------------------------------------------------------------------------

    private Texture getBigBuddyTexture(int index) {
        switch (index) {
            case 0: return TextureRepository.Darrel();
            case 1: return TextureRepository.Fernando();
            case 2: return TextureRepository.Gerald();
            case 3: return TextureRepository.MrOstrich();
            default: return TextureRepository.BlueBack();
        }
    }

    // -------------------------------------------------------------------------
    // CARD INTERACTION
    // -------------------------------------------------------------------------

    private void handleCardTap(MotionEvent e) {
        if (phase != GamePhase.PLAYING) return;

        GameObject touched = J4Q.touchScreen.pickObject(0);
        if (touched == null) return;

        for (int i = 0; i < handCards.size(); i++) {
            if (!handCards.get(i).isHit(touched)) continue;

            // Store base position for this card
            float x    = -((handCards.size() - 1) * 0.72f / 2f) + i * 0.72f;
            float tilt = (i - (handCards.size() - 1) / 2f) * 4f;

            if (zoomedCardIndex == i) {
                // Second tap — shrink back to normal
                handCards.get(i).transform.identity();
                handCards.get(i).transform.translate(x, -0.3f, 0f);
                handCards.get(i).transform.rotateZ(-tilt);
                handCards.get(i).transform.scale(CARD_HAND_SCALE);
                zoomedCardIndex = -1;
            } else {
                // Restore previously zoomed card
                if (zoomedCardIndex >= 0 && zoomedCardIndex < handCards.size()) {
                    float px    = -((handCards.size() - 1) * 0.72f / 2f) + zoomedCardIndex * 0.72f;
                    float ptilt = (zoomedCardIndex - (handCards.size() - 1) / 2f) * 4f;
                    handCards.get(zoomedCardIndex).transform.identity();
                    handCards.get(zoomedCardIndex).transform.translate(px, -0.3f, 0f);
                    handCards.get(zoomedCardIndex).transform.rotateZ(-ptilt);
                    handCards.get(zoomedCardIndex).transform.scale(CARD_HAND_SCALE);
                }
                // Zoom this card — bring to centre, no tilt, bigger
                handCards.get(i).transform.identity();
                handCards.get(i).transform.translate(0f, 0.2f, 0.1f); // slight z offset to appear on top
                handCards.get(i).transform.scale(CARD_ZOOMED_SCALE);
                zoomedCardIndex = i;
            }
            return;
        }
    }

    private void handleSwipeUp() {
        if (phase != GamePhase.PLAYING) return;
        if (zoomedCardIndex < 0) return;

        if (client != null && client.getMyPlayerId() >= 0) {
            int myId     = client.getMyPlayerId();
            int targetId = (myId + 1) % 4;
            Log.d(TAG, "Swipe up — playing card " + zoomedCardIndex + " on player " + targetId);
            client.sendAction(new PlayCardPacket(myId, zoomedCardIndex, targetId));

            glActivity.scene.removeChild(handCards.remove(zoomedCardIndex));
            zoomedCardIndex = -1;
        }
    }

    // -------------------------------------------------------------------------
    // TOUCH
    // -------------------------------------------------------------------------

    @Override
    public void onTouch(MotionEvent event) {
        if (gestureDetector != null) gestureDetector.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = event.getX();
                touchStartY = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                float dy = event.getY() - touchStartY;
                if (dy < SWIPE_THRESHOLD) {
                    handleSwipeUp();
                }
                break;
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
            setupGestureDetector();

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
                    updateStatus("Connecting players...");

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        testClients = new GameClient[4];
                        for (int i = 0; i < 4; i++) {
                            testClients[i] = new GameClient();
                            int idx = i;
                            testClients[i].setGameEventListener(new GameClient.GameEventListener() {
                                @Override
                                public void onConnected() {
                                    updateConnectionLabel("🟡 Connected — waiting for players", Color.YELLOW);
                                }

                                @Override
                                public void onGameStarted(int playerId) {
                                    Log.d(TAG, "[Test P" + playerId + "] Game started");
                                    // Only trigger transition once, from player 0's perspective
                                    if (idx == 0) {
                                        myPlayerId = playerId;
                                        currentTurnPlayer = 0;
                                        pendingTransitionPlayerId = playerId;
                                        pendingGameTransition     = true;
                                    }
                                }
                                @Override
                                public void onTurnUpdate(String message) {
                                    Log.d(TAG, "[Test] " + message);
                                    updateStatus(message);
                                    // Parse "Player X's turn" to track whose turn it is
                                    if (message.startsWith("Player ")) {
                                        try {
                                            int p = Integer.parseInt(
                                                    message.substring(7, message.indexOf("'s")));
                                            if (p != currentTurnPlayer) {
                                                currentTurnPlayer = p;
                                                pendingTurnIndicatorUpdate = true;
                                            }
                                        } catch (Exception ignored) {}
                                    }
                                }
                                @Override
                                public void onMyTurn() {
                                    int myId = testClients[idx].getMyPlayerId();
                                    Log.d(TAG, "[Test P" + myId + "] My turn!");
                                    updateStatus("YOUR TURN — Player " + myId);
                                }
                            });
                            testClients[i].connect("127.0.0.1", PORT);
                        }
                        client = testClients[0];
                    }, 1000);
                    break;

                case CLIENT:
                default:
                    Log.d(TAG, "Connecting to remote server...");
                    updateStatus("Connecting to server...");
                    updateConnectionLabel("🟡 Connecting to " + PLAYIT_HOST + ":" + PLAYIT_PORT, Color.YELLOW);
                    client = new GameClient();
                    setupClientListener(client);
                    client.connect(PLAYIT_HOST, PLAYIT_PORT);
                    break;
            }
        });
    }

    private void setupGestureDetector() {
        gestureDetector = new GestureDetector(activity,
                new GestureDetector.SimpleOnGestureListener() {

                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        if (client != null && client.getMyPlayerId() >= 0) {
                            Log.d(TAG, "Double tap — ending turn");
                            client.sendAction(new EndTurnPacket(client.getMyPlayerId()));
                        }
                        return true;
                    }

                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {
                        // Tap a card to zoom / unzoom
                        handleCardTap(e);
                        return true;
                    }
                });
    }

    private void setupClientListener(GameClient c) {
        c.setGameEventListener(new GameClient.GameEventListener() {
            @Override
            public void onGameStarted(int playerId) {
                myPlayerId                = playerId;
                currentTurnPlayer         = 0;
                pendingTransitionPlayerId = playerId;
                pendingGameTransition     = true;
                updateConnectionLabel("🟢 Connected — Player " + playerId, Color.GREEN);
            }
            @Override
            public void onConnected() {
                updateConnectionLabel("🟡 Connected — waiting for players", Color.YELLOW);
            }
            @Override
            public void onTurnUpdate(String message) {
                updateStatus(message);
                if (message.startsWith("Player ")) {
                    try {
                        int p = Integer.parseInt(
                                message.substring(7, message.indexOf("'s")));
                        if (p != currentTurnPlayer) {
                            currentTurnPlayer          = p;
                            pendingTurnIndicatorUpdate = true;
                        }
                    } catch (Exception ignored) {}
                }
            }
            @Override
            public void onMyTurn() {
                updateStatus("YOUR TURN!");
            }
        });
    }

    private void updateStatus(String message) {
        pendingStatus = message;
    }
}