package com.ex.bwb.networking;

import com.ex.bwb.Player;
import com.ex.bwb.cards.BigBuddy;
import com.ex.bwb.cards.CardType;
import com.ex.bwb.game.GameController;
import com.ex.bwb.game.GameState;
import com.ex.bwb.networking.packets.HandSyncPacket;
import com.ex.bwb.networking.packets.Packet;
import com.ex.bwb.networking.packets.PacketType;
import com.ex.bwb.networking.packets.PlayCardPacket;
import com.ex.bwb.networking.packets.PunchPacket;
import com.ex.bwb.networking.packets.StateUpdatePacket;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class StandaloneServer {

  // ===== CLIENT CONNECTION =====

  static class ClientConnection {
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

  // ===== SERVER STATE =====

  static final int MAX_PLAYERS = 4;
  static final int GAME_PORT = 5556;
  static final int WEB_PORT = 8080;
  static final int BROADCAST_PORT = 9877;

  static List<ClientConnection> clients = Collections.synchronizedList(new ArrayList<>());
  static boolean running = true;
  static List<String> eventLog = Collections.synchronizedList(new ArrayList<>());

  static GameController gameController;

  // ===== MAIN =====

  public static void main(String[] args) {
    log("=== Buddies with Benefits Server ===");
    log("Game port: " + GAME_PORT);
    log("Debug web: http://localhost:" + WEB_PORT);

    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
        NetworkInterface ni = interfaces.nextElement();
        Enumeration<InetAddress> addresses = ni.getInetAddresses();
        while (addresses.hasMoreElements()) {
          InetAddress addr = addresses.nextElement();
          if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
            log("Server IP: " + addr.getHostAddress());
          }
        }
      }
    } catch (SocketException e) {
      log("Could not determine server IP");
    }

    gameController = new GameController();

    startDiscoveryBroadcast();
    startDebugWebServer();
    startGameServer();
  }

  // ===== GAME SERVER =====

  static void startGameServer() {
    try {
      ServerSocket serverSocket = new ServerSocket();
      serverSocket.setReuseAddress(true);
      serverSocket.bind(new InetSocketAddress(GAME_PORT));
      log("Game server started on port " + GAME_PORT);
      log("Waiting for " + MAX_PLAYERS + " players...");

      while (running && clients.size() < MAX_PLAYERS) {
        Socket socket = serverSocket.accept();
        int playerId = clients.size();
        ClientConnection conn = new ClientConnection(socket, playerId);
        clients.add(conn);
        log("Player " + playerId + " connected from "
                + socket.getInetAddress().getHostAddress()
                + " (" + clients.size() + "/" + MAX_PLAYERS + ")");

        // Broadcast to all current clients how many players are now connected
        broadcastToAll(new Packet(PacketType.PLAYER_JOINED, clients.size()));
      }

      log("All players connected! Starting game.");
      logEvent("Game started");

      for (ClientConnection conn : clients) {
        startListening(conn);
      }

      for (ClientConnection conn : clients) {
        Packet startPacket = new Packet(PacketType.GAME_START, conn.playerId);
        conn.out.writeObject(startPacket);
        conn.out.flush();
      }

      // Initialize players with Big Buddies
      gameController.players[0] = new Player(new BigBuddy("Darrel",      "Start with +1 Temporary HP.",                          "", CardType.BIG_BUDDY, null));
      gameController.players[1] = new Player(new BigBuddy("Fernando",    "Draw 2 cards at the start of your turn instead of 1.", "", CardType.BIG_BUDDY, null));
      gameController.players[2] = new Player(new BigBuddy("Gerald",      "Have +1 Lil' Buddy",                                  "", CardType.BIG_BUDDY, null));
      gameController.players[3] = new Player(new BigBuddy("Mr. Ostrich", "Have +1 Action Point",                                "", CardType.BIG_BUDDY, null));

      // Build deck and deal
      gameController.buildDeck();
      for (int i = 0; i < gameController.players.length; i++) {
        Player p = gameController.players[i];
        gameController.drawCards(7, p);

        String[] cardNames = new String[p.hand.size()];
        for (int j = 0; j < p.hand.size(); j++) {
          cardNames[j] = p.hand.get(j).getName();
        }
        sendTo(i, new HandSyncPacket(i, cardNames));
      }

      gameController.startTurn();
      broadcastState("Game started — Player 0's turn");
      notifyCurrentPlayer();

    } catch (IOException e) {
      log("Server error: " + e.getMessage());
    }
  }

  static void startListening(ClientConnection conn) {
    new Thread(() -> {
      try {
        while (running) {
          Packet packet = (Packet) conn.in.readObject();
          handlePacket(conn.playerId, packet);
        }
      } catch (Exception e) {
        log("Player " + conn.playerId + " disconnected: " + e.getMessage());
      }
    }).start();
  }

  static synchronized void handlePacket(int playerId, Packet packet) {
    log("Received " + packet.type + " from player " + playerId);

    if (playerId != gameController.state.currentPlayer
        && packet.type != PacketType.JOIN_REQUEST) {
      log("REJECTED: Not player " + playerId + "'s turn (current: "
          + gameController.state.currentPlayer + ")");
      sendTo(playerId, new Packet(PacketType.ACTION_REJECTED, playerId));
      return;
    }

    switch (packet.type) {
      case PLAY_CARD:
        PlayCardPacket pcp = (PlayCardPacket) packet;
        String cardName = gameController.players[playerId].hand.get(pcp.cardIndex).getName();
        log("Player " + playerId + " plays [" + cardName + "] targeting player " + pcp.targetPlayerId);
        logEvent("Player " + playerId + " played [" + cardName + "] on player " + pcp.targetPlayerId);

        int[] hpBefore = new int[gameController.players.length];
        int[] handBefore = new int[gameController.players.length];
        for (int i = 0; i < gameController.players.length; i++) {
          hpBefore[i] = gameController.players[i].currHP;
          handBefore[i] = gameController.players[i].hand.size();
        }

        gameController.input = pcp.targetPlayerId;
        gameController.playCard(pcp.cardIndex, pcp.targetPlayerId);

        for (int i = 0; i < gameController.players.length; i++) {
          int hpDelta = gameController.players[i].currHP - hpBefore[i];
          int handDelta = gameController.players[i].hand.size() - handBefore[i];
          String hpStr = hpDelta != 0 ? " HP:" + (hpDelta > 0 ? "+" : "") + hpDelta : "";
          String handStr = handDelta != 0 ? " Hand:" + (handDelta > 0 ? "+" : "") + handDelta : "";
          if (!hpStr.isEmpty() || !handStr.isEmpty()) {
            log("  → Player " + i + hpStr + handStr);
          }
        }
        break;

      case DRAW_CARD:
        log("Player " + playerId + " draws a card");
        logEvent("Player " + playerId + " drew a card");
        gameController.drawCard();
        break;

      case PUNCH:
        PunchPacket pp = (PunchPacket) packet;
        log("Player " + playerId + " punches player " + pp.targetPlayerId);
        logEvent("Player " + playerId + " punched player " + pp.targetPlayerId);
        gameController.punch(pp.targetPlayerId);
        break;

      case END_TURN:
        log("Player " + playerId + " ends their turn");
        logEvent("Player " + playerId + " ended turn");
        gameController.endTurn();
        break;

      default:
        log("Unhandled packet type: " + packet.type);
        break;
    }

    // Log player states
    for (int i = 0; i < gameController.players.length; i++) {
      Player p = gameController.players[i];
      if (p != null) {
        log("  Player " + i + " — HP: " + p.currHP
            + " | AP: " + p.currAP
            + " | Hand: " + p.hand.size()
            + " | Stash: " + p.stash.size());
      }
    }

    int currentPlayer = gameController.state.currentPlayer;
    int apRemaining = gameController.players[currentPlayer] != null
        ? gameController.players[currentPlayer].currAP : 0;
    broadcastState("Player " + currentPlayer + "'s turn — " + apRemaining + " AP");
    notifyCurrentPlayer();
  }

  static void notifyCurrentPlayer() {
    int current = gameController.state.currentPlayer;
    sendTo(current, new Packet(PacketType.YOUR_TURN, current));
  }

  static void broadcastState(String message) {
    int current = gameController.state.currentPlayer;
    int ap = gameController.players[current] != null
        ? gameController.players[current].currAP : 0;
    StateUpdatePacket statePacket = new StateUpdatePacket(current, ap, message);
    broadcastToAll(statePacket);
  }

  static void broadcastToAll(Packet packet) {
    for (ClientConnection conn : clients) {
      try {
        conn.out.writeObject(packet);
        conn.out.flush();
      } catch (IOException e) {
        log("Error sending to player " + conn.playerId + ": " + e.getMessage());
      }
    }
  }

  static void sendTo(int playerId, Packet packet) {
    try {
      clients.get(playerId).out.writeObject(packet);
      clients.get(playerId).out.flush();
    } catch (IOException e) {
      log("Error sending to player " + playerId + ": " + e.getMessage());
    }
  }

  // ===== UDP DISCOVERY =====

  static void startDiscoveryBroadcast() {
    new Thread(() -> {
      try {
        DatagramSocket socket = new DatagramSocket();
        socket.setBroadcast(true);
        String message = "BWB_GAME:BuddiesWithBenefits:" + GAME_PORT;

        while (running) {
          DatagramPacket packet = new DatagramPacket(
              message.getBytes(),
              message.length(),
              InetAddress.getByName("255.255.255.255"),
              BROADCAST_PORT
          );
          socket.send(packet);
          Thread.sleep(1000);
        }
        socket.close();
      } catch (Exception e) {
        log("Broadcast error: " + e.getMessage());
      }
    }).start();
    log("Discovery broadcast started on port " + BROADCAST_PORT);
  }

  // ===== DEBUG WEB SERVER =====

  static void startDebugWebServer() {
    new Thread(() -> {
      try {
        ServerSocket webSocket = new ServerSocket(WEB_PORT);
        while (running) {
          Socket client = webSocket.accept();
          new Thread(() -> handleWebRequest(client)).start();
        }
      } catch (IOException e) {
        log("Web server error: " + e.getMessage());
      }
    }).start();
    log("Debug web server started on port " + WEB_PORT);
  }

  static void handleWebRequest(Socket client) {
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
      reader.readLine();

      String html = buildDebugPage();
      String response = "HTTP/1.1 200 OK\r\n"
          + "Content-Type: text/html; charset=utf-8\r\n"
          + "Content-Length: " + html.getBytes().length + "\r\n"
          + "Connection: close\r\n"
          + "\r\n"
          + html;

      OutputStream out = client.getOutputStream();
      out.write(response.getBytes());
      out.flush();
      client.close();
    } catch (IOException e) {
      log("Web request error: " + e.getMessage());
    }
  }

  static String buildDebugPage() {
    int currentPlayer = gameController.state.currentPlayer;
    int ap = gameController.players[currentPlayer] != null
        ? gameController.players[currentPlayer].currAP : 0;

    StringBuilder sb = new StringBuilder();
    sb.append("<!DOCTYPE html><html><head>")
        .append("<meta http-equiv='refresh' content='2'>")
        .append("<title>BWB Server</title>")
        .append("<style>")
        .append("body { font-family: monospace; background: #1e1e1e; color: #0f0; padding: 20px; }")
        .append("h1 { color: #0ff; } h2 { color: #0ff; }")
        .append("table { border-collapse: collapse; width: 100%; margin-bottom: 20px; }")
        .append("td, th { border: 1px solid #333; padding: 8px; text-align: left; }")
        .append("th { background: #333; }")
        .append(".log { color: #888; font-size: 12px; }")
        .append(".dead { color: #f44; }")
        .append("</style></head><body>")
        .append("<h1>Buddies with Benefits - Server</h1>")
        .append("<p>Players: ").append(clients.size()).append("/").append(MAX_PLAYERS).append("</p>")
        .append("<p>Current turn: Player ").append(currentPlayer).append("</p>")
        .append("<p>Action points: ").append(ap).append("</p>")
        .append("<p>Rotation: ").append(gameController.state.rotationCount).append("</p>")
        .append("<p>Draw pile: ").append(gameController.state.drawPile.size()).append("</p>")
        .append("<p>Discard pile: ").append(gameController.state.discardPile.size()).append("</p>");

    // Player table
    sb.append("<h2>Players</h2>")
        .append("<table><tr><th>ID</th><th>Big Buddy</th><th>HP</th><th>AP</th><th>Hand</th><th>Stash</th><th>IP</th><th>Status</th></tr>");
    for (int i = 0; i < gameController.players.length; i++) {
      Player p = gameController.players[i];
      ClientConnection conn = i < clients.size() ? clients.get(i) : null;
      String rowClass = (p != null && !p.isAlive()) ? " class='dead'" : "";
      sb.append("<tr").append(rowClass).append(">")
          .append("<td>Player ").append(i).append("</td>")
          .append("<td>").append(p != null ? p.getBigBuddy().getName() : "—").append("</td>")
          .append("<td>").append(p != null ? p.currHP + "/" + p.maxHP : "—").append("</td>")
          .append("<td>").append(p != null ? p.currAP + "/" + p.maxAP : "—").append("</td>")
          .append("<td>").append(p != null ? p.hand.size() : "—").append("</td>")
          .append("<td>").append(p != null ? p.stash.size() + "/7" : "—").append("</td>")
          .append("<td>").append(conn != null ? conn.socket.getInetAddress().getHostAddress() : "—").append("</td>")
          .append("<td>").append(p != null && p.isAlive() ? "Alive" : "Dead").append("</td>")
          .append("</tr>");
    }
    sb.append("</table>");

    // Event log
    sb.append("<h2>Event Log</h2>");
    int start = Math.max(0, eventLog.size() - 30);
    for (int i = eventLog.size() - 1; i >= start; i--) {
      sb.append("<div class='log'>").append(eventLog.get(i)).append("</div>");
    }

    sb.append("</body></html>");
    return sb.toString();
  }

  // ===== LOGGING =====

  static void log(String message) {
    String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
    System.out.println("[" + timestamp + "] " + message);
  }

  static void logEvent(String event) {
    String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
    eventLog.add("[" + timestamp + "] " + event);
  }
}