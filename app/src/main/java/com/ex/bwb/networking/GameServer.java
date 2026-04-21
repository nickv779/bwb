package com.ex.bwb.networking;

import android.util.Log;

import com.ex.bwb.Player;
import com.ex.bwb.cards.Action;
import com.ex.bwb.cards.Attack;
import com.ex.bwb.cards.BigBuddy;
import com.ex.bwb.cards.Card;
import com.ex.bwb.cards.CardType;
import com.ex.bwb.cards.Effects;
import com.ex.bwb.cards.Signature;
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
import java.util.Stack;

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
    if (running) return;
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

        Log.d(TAG, "All players connected! Starting game."); // FIXED: now appears exactly once

        for (ClientConnection conn : clients) {             // FIXED: now called exactly once
          startListening(conn);
        }

        for (ClientConnection conn : clients) {             // FIXED: now called exactly once
          Packet startPacket = new Packet(PacketType.GAME_START, conn.playerId);
          conn.out.writeObject(startPacket);
          conn.out.flush();
        }

        gameController.players[0] = new Player(new BigBuddy("Darrel",     "Start with +1 Temporary HP.",                       "", CardType.BIG_BUDDY, null));
        gameController.players[1] = new Player(new BigBuddy("Fernando",   "Draw 2 cards at the start of your turn instead of 1.", "", CardType.BIG_BUDDY, null));
        gameController.players[2] = new Player(new BigBuddy("Gerald",     "Have +1 Lil' Buddy",                                "", CardType.BIG_BUDDY, null));
        gameController.players[3] = new Player(new BigBuddy("Mr. Ostrich","Have +1 Action Point",                              "", CardType.BIG_BUDDY, null));

        // Temporary test deck — add a few of each card type you want to test
        Stack<Card> deck = gameController.state.drawPile;

// Action cards
        gameController.players[0] = new Player(new BigBuddy("Darrel",     "Start with +1 Temporary HP.",                        "", CardType.BIG_BUDDY, null));
        gameController.players[1] = new Player(new BigBuddy("Fernando",   "Draw 2 cards at the start of your turn instead of 1.","", CardType.BIG_BUDDY, null));
        gameController.players[2] = new Player(new BigBuddy("Gerald",     "Have +1 Lil' Buddy",                                 "", CardType.BIG_BUDDY, null));
        gameController.players[3] = new Player(new BigBuddy("Mr. Ostrich","Have +1 Action Point",                               "", CardType.BIG_BUDDY, null));

// Build the real deck and deal 7 cards to each player
        gameController.buildDeck();
        for (Player p : gameController.players) {
          gameController.drawCards(7, p);
        }

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

        // ADD: log the card name so you can see exactly what fired
        String cardName = gameController.players[playerId].hand.get(pcp.cardIndex).getName();
        Log.d(TAG, "Player " + playerId + " plays [" + cardName + "] targeting player " + pcp.targetPlayerId);

        // ADD: snapshot HP and hand sizes before the effect
        int[] hpBefore   = new int[gameController.players.length];
        int[] handBefore = new int[gameController.players.length];
        for (int i = 0; i < gameController.players.length; i++) {
          hpBefore[i]   = gameController.players[i].currHP;
          handBefore[i] = gameController.players[i].hand.size();
        }

        gameController.input = pcp.targetPlayerId;
        gameController.playCard(pcp.cardIndex, pcp.targetPlayerId);

        // ADD: log what changed after the effect
        for (int i = 0; i < gameController.players.length; i++) {
          int hpDelta   = gameController.players[i].currHP   - hpBefore[i];
          int handDelta = gameController.players[i].hand.size() - handBefore[i];
          String hpStr   = hpDelta   != 0 ? " HP:"   + (hpDelta   > 0 ? "+" : "") + hpDelta   : "";
          String handStr = handDelta != 0 ? " Hand:" + (handDelta > 0 ? "+" : "") + handDelta : "";
          if (!hpStr.isEmpty() || !handStr.isEmpty()) {
            Log.d(TAG, "  → Player " + i + hpStr + handStr);
          }
        }
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
    // Add this block right before broadcastState() in handlePacket()
    for (int i = 0; i < gameController.players.length; i++) {
      Player p = gameController.players[i];
      if (p != null) {
        Log.d(TAG, "  Player " + i + " — HP: " + p.currHP
                + " | AP: " + p.currAP
                + " | Hand: " + p.hand.size()
                + " | Stash: " + p.stash.size());
      }
    }
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