package com.ex.bwb;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.ex.bwb.networking.packets.DrawCardPacket;
import com.ex.bwb.networking.packets.EndTurnPacket;
import com.ex.bwb.networking.GameClient;
import com.ex.bwb.networking.GameServer;
import com.ex.bwb.networking.packets.PlayCardPacket;
import com.ex.bwb.networking.packets.PunchPacket;
import com.ex.bwb.networking.ServerDiscovery;

import gl.activities.GLActivity;
import gl.activities.GyroscopicActivity;

public class MainActivity extends GLActivity {
  private static final String TAG = "MainActivity";
  private static final int PORT = 5556;

  private GameServer server;
  private final GameClient[] clients = new GameClient[4];
  private ServerDiscovery discovery;
  private WifiManager.MulticastLock multicastLock;
  private final Handler handler = new Handler(Looper.getMainLooper());

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
    multicastLock = wifi.createMulticastLock("bwb_lock");
    multicastLock.setReferenceCounted(true);
    multicastLock.acquire();

    // Start server
    server = new GameServer(PORT);
    server.start();
    Log.d(TAG, "Server starting...");

    // Connect 4 clients after a short delay
    handler.postDelayed(() -> {
      for (int i = 0; i < 4; i++) {
        clients[i] = new GameClient();
        clients[i].connect("127.0.0.1", PORT);
      }
    }, 1000);

    // Run test sequence after everyone has connected
    handler.postDelayed(this::runTestSequence, 3000);
  }

  private void runTestSequence() {
    Log.d(TAG, "=== STARTING TEST SEQUENCE ===");

    // Find which client is player 0, 1, etc.
    GameClient player0 = getClientByPlayerId(0);
    GameClient player1 = getClientByPlayerId(1);
    GameClient player3 = getClientByPlayerId(3);

    if (player0 == null || player1 == null || player3 == null) {
      Log.e(TAG, "Not all players have IDs yet, retrying...");
      handler.postDelayed(this::runTestSequence, 1000);
      return;
    }

    handler.postDelayed(() -> {
      Log.d(TAG, "--- Player 0: Drawing a card ---");
      player0.sendAction(new DrawCardPacket(0));
    }, 500);

    handler.postDelayed(() -> {
      Log.d(TAG, "--- Player 0: Playing card 2 on player 1 ---");
      player0.sendAction(new PlayCardPacket(0, 2, 1));
    }, 1500);

    handler.postDelayed(() -> {
      Log.d(TAG, "--- Player 0: Ending turn ---");
      player0.sendAction(new EndTurnPacket(0));
    }, 2500);

    handler.postDelayed(() -> {
      Log.d(TAG, "--- Player 1: Punching player 2 ---");
      player1.sendAction(new PunchPacket(1, 2));
    }, 3500);

    handler.postDelayed(() -> {
      Log.d(TAG, "--- Player 3: Trying to act out of turn (should be rejected) ---");
      player3.sendAction(new DrawCardPacket(3));
    }, 4500);
  }

  private GameClient getClientByPlayerId(int playerId) {
    for (GameClient c : clients) {
      if (c != null && c.getMyPlayerId() == playerId) return c;
    }
    return null;
  }

  @Override
  public void Start() {
    scene.background(0, 0, 0);
  }

  @Override
  public void Update() {
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (discovery != null) discovery.stop();
    for (GameClient c : clients) {
      if (c != null) c.disconnect();
    }
    if (server != null) server.stop();
    if (multicastLock != null && multicastLock.isHeld()) {
      multicastLock.release();
    }
  }
}