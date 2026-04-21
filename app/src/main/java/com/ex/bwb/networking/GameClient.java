package com.ex.bwb.networking;

import android.util.Log;

import com.ex.bwb.networking.packets.HandSyncPacket;
import com.ex.bwb.networking.packets.Packet;
import com.ex.bwb.networking.packets.PacketType;
import com.ex.bwb.networking.packets.StateUpdatePacket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class GameClient {
  private static final String TAG = "GameClient";

  private Socket socket;
  private ObjectOutputStream out;
  private ObjectInputStream in;
  private boolean connected = false;
  private int myPlayerId = -1;

  public interface GameEventListener {
    void onHandSync(String[] cardNames);
    void onPlayerJoined(int totalPlayers);
    void onConnected();
    void onGameStarted(int playerId);
    void onTurnUpdate(String message);
    void onMyTurn();
    void onTurnChanged(int currentPlayerId); // new — for turn indicator
  }

  private GameEventListener listener;

  public void setGameEventListener(GameEventListener listener) {
    this.listener = listener;
  }

  public void connect(String serverIP, int port) {
    new Thread(() -> {
      try {
        socket = new Socket(serverIP, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
        connected = true;
        Log.d(TAG, "Connected to server at " + serverIP + ":" + port);
        if (listener != null) listener.onConnected();

// Route ALL packets through handleServerPacket — no special first packet
        while (connected) {
          Packet packet = (Packet) in.readObject();
          handleServerPacket(packet);
        }
      } catch (Exception e) {
        Log.e(TAG, "Client error: " + e.getMessage());
      }
    }).start();
  }

  private void handleServerPacket(Packet packet) {
    switch (packet.type) {
      case JOIN_ACCEPTED:
        myPlayerId = packet.playerId;
        Log.d(TAG, "I am player " + myPlayerId);
        break;

      case GAME_START:
        myPlayerId = packet.playerId; // assign FIRST
        Log.d(TAG, "I am player " + myPlayerId + " — game started!");
        if (listener != null) listener.onGameStarted(myPlayerId);
        break;

      case YOUR_TURN:
        Log.d(TAG, "[Player " + myPlayerId + "] It's my turn!");
        if (listener != null) listener.onMyTurn();
        break;

      case STATE_UPDATE:
        StateUpdatePacket state = (StateUpdatePacket) packet;
        if (listener != null) {
          listener.onTurnUpdate(state.message);
          listener.onTurnChanged(state.currentPlayerIndex);
        }
        break;

      case ACTION_REJECTED:
        Log.d(TAG, "[Player " + myPlayerId + "] Action was rejected!");
        break;

      case PLAYER_JOINED:
        Log.d(TAG, "Player joined, total: " + packet.playerId);
        if (listener != null) listener.onPlayerJoined(packet.playerId);
        break;

      case HAND_SYNC:
        HandSyncPacket hsp = (HandSyncPacket) packet;
        if (listener != null) listener.onHandSync(hsp.cardNames);
        break;

      default:
        Log.d(TAG, "[Player " + myPlayerId + "] Received: " + packet.type);
    }
  }

  public void sendAction(Packet packet) {
    if (!connected || out == null) {
      Log.e(TAG, "Cannot send - not connected");
      return;
    }

    packet.playerId = myPlayerId;

    new Thread(() -> {
      try {
        out.writeObject(packet);
        out.flush();
      }

      catch (IOException e) {
        Log.e(TAG, "Send error: " + e.getMessage());
      }
    }).start();
  }

  public int getMyPlayerId() {
    return myPlayerId;
  }

  public boolean isConnected() {
    return connected;
  }

  public void disconnect() {
    connected = false;
    try {
      if (socket != null) socket.close();
    }

    catch (IOException e) {
      Log.e(TAG, "Error disconnecting: " + e.getMessage());
    }
  }
}