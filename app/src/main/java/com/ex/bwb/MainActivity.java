package com.ex.bwb;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.graphics.Paint;
import android.graphics.Color;

import com.ex.bwb.networking.GameClient;
import com.ex.bwb.networking.GameServer;
import com.ex.bwb.networking.ServerDiscovery;
import com.ex.bwb.networking.packets.EndTurnPacket;

import gl.activities.GLActivity;
import gl.models.Model;
import gl.models.ObjectMaker;
import gl.shaders.Text;
import gl.shaders.TextureShader;

public class MainActivity extends GLActivity {
  private static final String TAG = "MainActivity";

  enum Mode { CLIENT, HOST, TEST_4P }
  private static final Mode MODE = Mode.TEST_4P;
  private static final int PORT = 5556;

  private GameServer server;
  private GameClient client;
  private ServerDiscovery discovery;
  private WifiManager.MulticastLock multicastLock;
  private Text statusTextGL;
  private Model textModel;
  private volatile String pendingStatus = null;
  private GestureDetector gestureDetector;
  private Paint textPaint;
  private GameClient[] testClients;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
      @Override
      public boolean onDoubleTap(MotionEvent e) {
        if (client != null && client.getMyPlayerId() >= 0) {
          Log.d(TAG, "Double tap — ending turn");
          client.sendAction(new EndTurnPacket(client.getMyPlayerId()));
        }
        return true;
      }
    });

    WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
    multicastLock = wifi.createMulticastLock("bwb_lock");
    multicastLock.setReferenceCounted(true);
    multicastLock.acquire();

    discovery = new ServerDiscovery();

    switch (MODE) {
      case HOST:
        server = new GameServer(PORT);
        server.start();
        discovery.startBroadcasting("BuddiesWithBenefits", PORT);
        Log.d(TAG, "Hosting game...");
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
        Log.d(TAG, "Test mode: server + 4 local clients");
        updateStatus("Test mode: connecting 4 players...");

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
          testClients = new GameClient[4];
          for (int i = 0; i < 4; i++) {
            testClients[i] = new GameClient();
            int idx = i;
            testClients[i].setGameEventListener(new GameClient.GameEventListener() {
              @Override
              public void onGameStarted(int playerId) {
                Log.d(TAG, "[Test P" + playerId + "] Game started");
              }
              @Override
              public void onTurnUpdate(String message) {
                Log.d(TAG, "[Test] " + message);
                updateStatus(message);
              }
              @Override
              public void onMyTurn() {
                // FIXED: was using idx (creation order) which doesn't reliably
                // match playerId since connections are async — use actual playerId instead
                int myId = testClients[idx].getMyPlayerId();
                Log.d(TAG, "[Test P" + myId + "] My turn!");
                updateStatus("Player " + myId + "'s turn");
              }
            });
            testClients[i].connect("127.0.0.1", PORT);
          }
          // Use first client as the main one for double-tap
          client = testClients[0];
        }, 1000);
        break;

      case CLIENT:
      default:
        Log.d(TAG, "Searching for game...");
        updateStatus("Searching for game...");
        discovery.listenForServers((serverIP, tcpPort, gameName) -> {
          Log.d(TAG, "Found game: " + gameName + " at " + serverIP + ":" + tcpPort);
          updateStatus("Found game! Connecting...");
          client = new GameClient();
          setupClientListener(client);
          client.connect(serverIP, tcpPort);
        });
        break;
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    gestureDetector.onTouchEvent(event);
    return super.onTouchEvent(event);
  }

  @Override
  public void Start() {
    scene.background(0, 0, 0);

    // Create a flat quad for the text
    ObjectMaker om = new ObjectMaker();
    om.color(1, 1, 1);
    om.box(4f, 0.5f, 0.01f);
    textModel = om.flushModel(true, true, true, true);

    // Create the text texture
    textPaint = new Paint();
    textPaint.setColor(Color.WHITE);
    textPaint.setTextSize(50);

    statusTextGL = new Text(1024, 128);
    statusTextGL.setText("Connecting...", 0, 40, textPaint);

    // Use a basic texture shader
    TextureShader shader = new TextureShader();
    shader.setTexture(statusTextGL);
    textModel.setShader(shader);

    scene.appendChild(textModel);
    textModel.transform.translate(0, 0, -3);
  }

  @Override
  public void Update() {
    if (pendingStatus != null && statusTextGL != null && textPaint != null) {
      statusTextGL.setText(pendingStatus, 0, 40, textPaint);
      pendingStatus = null;
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (discovery != null) discovery.stop();
    if (client != null) client.disconnect();
    if (testClients != null) {
      for (GameClient c : testClients) {
        if (c != null) c.disconnect();
      }
    }
    if (server != null) server.stop();
    if (multicastLock != null && multicastLock.isHeld()) {
      multicastLock.release();
    }
  }

  // Helper method to update on screen status text from any thread
  private void updateStatus(String message) {
    pendingStatus = message;
  }

  private void setupClientListener(GameClient client) {
    client.setGameEventListener(new GameClient.GameEventListener() {
      @Override
      public void onGameStarted(int playerId) {
        updateStatus("Player " + playerId + "\nGame started!");
      }

      @Override
      public void onTurnUpdate(String message) {
        updateStatus(message);
      }

      @Override
      public void onMyTurn() {
        updateStatus("YOUR TURN!");
      }
    });
  }
}