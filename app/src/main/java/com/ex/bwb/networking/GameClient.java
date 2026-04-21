package com.ex.bwb.networking;

import android.util.Log;

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
    void onConnected();
    void onGameStarted(int playerId);
    void onTurnUpdate(String message);
    void onMyTurn();
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
        in = new ObjectInputStream(socket.getInputStream());
        connected = true;
        Log.d(TAG, "Connected to server at " + serverIP + ":" + port);
        if (listener != null) listener.onConnected();

        // Read the first packet — now GAME_START carries our player ID
        Packet firstPacket = (Packet) in.readObject();
        Log.d(TAG, "First packet received: type=" + firstPacket.type + " playerId=" + firstPacket.playerId);
        if (firstPacket.type == PacketType.GAME_START) {
          myPlayerId = firstPacket.playerId;
          Log.d(TAG, "I am player " + myPlayerId + " — game started!");
        }

        // Now start the normal listening loop
        while (connected) {
          Packet packet = (Packet) in.readObject();
          handleServerPacket(packet);
        }
      }

      catch (Exception e) {
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
        Log.d(TAG, "[Player " + myPlayerId + "] Game has started!");
        if (listener != null) listener.onGameStarted(myPlayerId);
        break;

      case YOUR_TURN:
        Log.d(TAG, "[Player " + myPlayerId + "] It's my turn!");
        if (listener != null) listener.onMyTurn();
        break;

      case STATE_UPDATE:
        StateUpdatePacket state = (StateUpdatePacket) packet;
        Log.d(TAG, "[Player " + myPlayerId + "] State update: " + state.message);
        if (listener != null) listener.onTurnUpdate(state.message);
        break;

      case ACTION_REJECTED:
        Log.d(TAG, "[Player " + myPlayerId + "] Action was rejected!");
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