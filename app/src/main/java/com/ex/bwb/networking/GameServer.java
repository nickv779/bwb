package com.ex.bwb.networking;

import android.util.Log;

import com.ex.bwb.networking.packets.Packet;
import com.ex.bwb.networking.packets.PacketType;
import com.ex.bwb.networking.packets.PlayCardPacket;
import com.ex.bwb.networking.packets.PunchPacket;
import com.ex.bwb.networking.packets.StateUpdatePacket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class GameServer {
  private static final String TAG = "GameServer";
  private static final int MAX_PLAYERS = 4;

  private ServerSocket serverSocket;
  private List<ClientConnection> clients = new ArrayList<>();
  private boolean running = false;
  private int port;
  private int currentPlayerIndex = 0;
  private int actionPointsRemaining = 3;

  private static class ClientConnection {
    Socket socket;
    ObjectOutputStream out;
    ObjectInputStream in;
    int playerId;

    ClientConnection(Socket socket, int playerId) throws IOException {
      this.socket = socket;
      this.playerId = playerId;
      this.out = new ObjectOutputStream(socket.getOutputStream());
      this.in = new ObjectInputStream(socket.getInputStream());
    }
  }

  public GameServer(int port) {
    this.port = port;
  }

  public void start() {
    running = true;
    new Thread(() -> {
      try {
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new java.net.InetSocketAddress((port)));
        Log.d(TAG, "Server started on port " + port);

        // Accept 4 players
        while (running && clients.size() < MAX_PLAYERS) {
          Socket socket = serverSocket.accept();
          int playerId = clients.size();
          ClientConnection conn = new ClientConnection(socket, playerId);
          clients.add(conn);
          Log.d(TAG, "Player " + playerId + " connected (" + clients.size() + "/" + MAX_PLAYERS + ")");
        }

        Log.d(TAG, "All players connected! Starting game.");

        // Start listening threads first
        for (ClientConnection conn : clients) {
          startListening(conn);
        }

        // Send each client their player ID via GAME_START
        for (ClientConnection conn : clients) {
          Packet startPacket = new Packet(PacketType.GAME_START, conn.playerId);
          conn.out.writeObject(startPacket);
          conn.out.flush();
        }

        notifyCurrentPlayer();
      }

      catch (IOException e) {
        Log.e(TAG, "Server error: " + e.getMessage());
      }
    }).start();
  }

  private void startListening(ClientConnection conn) {
    new Thread(() -> {
      try {
        while (running) {
          Packet packet = (Packet) conn.in.readObject();
          handlePacket(conn.playerId, packet);
        }
      }

      catch (Exception e) {
        Log.e(TAG, "Player " + conn.playerId + " disconnected: " + e.getMessage());
      }
    }).start();
  }

  private synchronized void handlePacket(int playerId, Packet packet) {
    Log.d(TAG, "Received " + packet.type + " from player " + playerId);

    // Turn validation
    if (playerId != currentPlayerIndex && packet.type != PacketType.JOIN_REQUEST) {
      Log.d (TAG, "REJECTED: Not player " + playerId + "'s turn (current: " + currentPlayerIndex + ")");
      sendTo(playerId, new Packet(PacketType.ACTION_REJECTED, playerId));
      return;
    }

    switch (packet.type) {
      case PLAY_CARD:
        PlayCardPacket pcp = (PlayCardPacket) packet;
        Log.d(TAG, "Player " + playerId + " plays card " + pcp.cardIndex + " targeting player " + pcp.targetPlayerId);
        // TODO: real card logic
        actionPointsRemaining--;
        broadcastState("Player " + playerId + " played a card on player " + pcp.targetPlayerId);
        break;

      case DRAW_CARD:
        Log.d(TAG, "Player " + playerId + " draws a card");
        // TODO: real draw logic
        actionPointsRemaining--;
        broadcastState("Player " + playerId + " drew a card");
        break;

      case PUNCH:
        PunchPacket pp = (PunchPacket) packet;
        Log.d(TAG, "Player " + playerId + " punches player " + pp.targetPlayerId);
        // TODO: real punch logic
        actionPointsRemaining--;
        broadcastState("Player " + playerId + " punched player " + pp.targetPlayerId);
        break;

      case END_TURN:
        Log.d(TAG, "Player " + playerId + " ends their turn");
        nextTurn();
        break;

      default:
        Log.d(TAG, "Unhandled packet type: " + packet.type);
    }

    if (actionPointsRemaining <= 0 && packet.type != PacketType.END_TURN) {
      Log.d(TAG, "Player " + playerId + " out of action points, auto-ending turn");
      nextTurn();
    }
  }

  private void nextTurn() {
    currentPlayerIndex = (currentPlayerIndex + 1) % MAX_PLAYERS;
    actionPointsRemaining = 3;
    Log.d(TAG, "It is now player " + currentPlayerIndex + "'s turn");
    broadcastState("Player " + currentPlayerIndex + "'s turn");
    notifyCurrentPlayer();
  }

  private void notifyCurrentPlayer() {
    sendTo(currentPlayerIndex, new Packet(PacketType.YOUR_TURN, currentPlayerIndex));
  }

  private void broadcastState(String message) {
    StateUpdatePacket state = new StateUpdatePacket(currentPlayerIndex, actionPointsRemaining, message);
    broadcastToAll(state);
  }

  private void broadcastToAll(Packet packet) {
    for (ClientConnection conn : clients) {
      try {
        conn.out.writeObject(packet);
        conn.out.flush();
      }

      catch (IOException e) {
        Log.e(TAG, "Error sending to player " + conn.playerId + ": " + e.getMessage());
      }
    }
  }

  private void sendTo(int playerId, Packet packet) {
    try {
      clients.get(playerId).out.writeObject(packet);
      clients.get(playerId).out.flush();
    }

    catch (IOException e) {
      Log.e(TAG, "Error sending to player " + playerId + ": " + e.getMessage());
    }
  }

  public void stop() {
    running = false;
    try {
      if (serverSocket != null) serverSocket.close();
      for (ClientConnection c : clients) c.socket.close();
    }

    catch (IOException e) {
      Log.e(TAG, "Error stopping server: " + e.getMessage());
    }
  }
}
