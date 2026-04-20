package com.ex.bwb.networking;

import android.util.Log;

import com.ex.bwb.game.GameController;
import com.ex.bwb.game.GameState;
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
  private static final int MAX_PLAYERS = 4; // FIXED: was 3

  private ServerSocket serverSocket;
  private List<ClientConnection> clients = new ArrayList<>();
  private boolean running = false;
  private int port;

  // ADDED: GameController is the single source of truth for game state
  private GameController gameController;
  private GameState gameState;

  // REMOVED: currentPlayerIndex — now gameController.state.currentPlayer
  // REMOVED: actionPointsRemaining — now players[currentPlayer].currAP

  private static class ClientConnection {
    Socket socket;
    ObjectOutputStream out;
    ObjectInputStream in;
    int playerId;

    ClientConnection(Socket socket, int playerId) throws IOException {
      this.socket   = socket;
      this.playerId = playerId;
      this.out      = new ObjectOutputStream(socket.getOutputStream());
      this.in       = new ObjectInputStream(socket.getInputStream());
    }
  }

  public GameServer(int port) {
    this.port = port;
    this.gameController = new GameController(); // ADDED
  }

  public void start() {
    running = true;
    new Thread(() -> {
      try {
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new java.net.InetSocketAddress(port));
        Log.d(TAG, "Server started on port " + port);

        while (running && clients.size() < MAX_PLAYERS) {
          Socket socket = serverSocket.accept();
          int playerId = clients.size();
          ClientConnection conn = new ClientConnection(socket, playerId);
          clients.add(conn);
          Log.d(TAG, "Player " + playerId + " connected ("
                  + clients.size() + "/" + MAX_PLAYERS + ")");
        }

        Log.d(TAG, "All players connected! Starting game.");

        for (ClientConnection conn : clients) {
          startListening(conn);
        }

        for (ClientConnection conn : clients) {
          Packet startPacket = new Packet(PacketType.GAME_START, conn.playerId);
          conn.out.writeObject(startPacket);
          conn.out.flush();
        }

        // ADDED: start the first turn and sync state to all clients
        gameController.startTurn();
        broadcastState("Game started — Player 0's turn");
        notifyCurrentPlayer();

      } catch (IOException e) {
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
      } catch (Exception e) {
        Log.e(TAG, "Player " + conn.playerId + " disconnected: " + e.getMessage());
      }
    }).start();
  }

  private synchronized void handlePacket(int playerId, Packet packet) {
    Log.d(TAG, "Received " + packet.type + " from player " + playerId);

    // Turn validation — uses GameController as source of truth
    if (playerId != gameController.state.currentPlayer
            && packet.type != PacketType.JOIN_REQUEST) {
      Log.d(TAG, "REJECTED: Not player " + playerId + "'s turn (current: "
              + gameController.state.currentPlayer + ")");
      sendTo(playerId, new Packet(PacketType.ACTION_REJECTED, playerId));
      return;
    }

    switch (packet.type) {
      case PLAY_CARD:
        PlayCardPacket pcp = (PlayCardPacket) packet;
        Log.d(TAG, "Player " + playerId + " plays card " + pcp.cardIndex
                + " targeting player " + pcp.targetPlayerId);
        // CHANGED: was TODO — now wired to GameController
        gameController.input = pcp.targetPlayerId;
        gameController.playCard(pcp.cardIndex, pcp.targetPlayerId);
        break;

      case DRAW_CARD:
        Log.d(TAG, "Player " + playerId + " draws a card");
        // CHANGED: was TODO — now wired to GameController
        gameController.drawCard();
        break;

      case PUNCH:
        PunchPacket pp = (PunchPacket) packet;
        Log.d(TAG, "Player " + playerId + " punches player " + pp.targetPlayerId);
        // CHANGED: was TODO — now wired to GameController
        gameController.punch(pp.targetPlayerId);
        break;

      case END_TURN:
        Log.d(TAG, "Player " + playerId + " ends their turn");
        gameController.endTurn();
        break;

      default:
        Log.d(TAG, "Unhandled packet type: " + packet.type);
        break;
    }

    // CHANGED: broadcast after every action using actual state from GameController
    // REMOVED: the old "if actionPointsRemaining <= 0" block — GameController handles auto end-turn
    int currentPlayer = gameController.state.currentPlayer;
    int apRemaining   = gameController.players[currentPlayer] != null
            ? gameController.players[currentPlayer].currAP : 0;
    broadcastState("Player " + currentPlayer + "'s turn — " + apRemaining + " AP");
    notifyCurrentPlayer();
  }

  private void notifyCurrentPlayer() {
    int current = gameController.state.currentPlayer;
    sendTo(current, new Packet(PacketType.YOUR_TURN, current));
  }

  private void broadcastState(String message) {
    int current = gameController.state.currentPlayer;
    int ap      = gameController.players[current] != null
            ? gameController.players[current].currAP : 0;
    StateUpdatePacket statePacket = new StateUpdatePacket(current, ap, message);
    broadcastToAll(statePacket);
  }

  private void broadcastToAll(Packet packet) {
    for (ClientConnection conn : clients) {
      try {
        conn.out.writeObject(packet);
        conn.out.flush();
      } catch (IOException e) {
        Log.e(TAG, "Error sending to player " + conn.playerId + ": " + e.getMessage());
      }
    }
  }

  private void sendTo(int playerId, Packet packet) {
    try {
      clients.get(playerId).out.writeObject(packet);
      clients.get(playerId).out.flush();
    } catch (IOException e) {
      Log.e(TAG, "Error sending to player " + playerId + ": " + e.getMessage());
    }
  }

  public void stop() {
    running = false;
    try {
      if (serverSocket != null) serverSocket.close();
      for (ClientConnection c : clients) c.socket.close();
    } catch (IOException e) {
      Log.e(TAG, "Error stopping server: " + e.getMessage());
    }
  }
}