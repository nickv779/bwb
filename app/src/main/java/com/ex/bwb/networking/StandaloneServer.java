package com.ex.bwb.networking;

import com.ex.bwb.networking.packets.*;

import java.io.*;
import java.net.*;
import java.util.*;

//
//  This class it meant to be run on a dedicated server to run the GameServer independent of player devices.
//
//  How to run from root:
//  javac -d out app/src/main/java/com/ex/bwb/networking/packets/*.java app/src/main/java/com/ex/bwb/networking/StandaloneServer.java
//  java -cp out com.ex.bwb.networking.StandaloneServer
//

public class StandaloneServer {

  // Client Connections
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

  // Server State
  static final int MAX_PLAYERS = 4;
  static final int GAME_PORT = 5556;
  static final int WEB_PORT = 8080;
  static final int BROADCAST_PORT = 9877;

  static List<ClientConnection> clients = Collections.synchronizedList(new ArrayList<>());
  static int currentPlayerIndex = 0;
  static int actionPointsRemaining = 3;
  static boolean running = true;
  static List<String> eventLog = Collections.synchronizedList(new ArrayList<>());

  // Main
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
    }

    catch (SocketException e) {
      log("Could not determine server IP");
    }

    startDiscoveryBroadcast();
    startDebugWebServer();
    startGameServer();
  }

  // Game Server
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
        log("Player " + playerId + " connected from " + socket.getInetAddress().getHostAddress()
            + " (" + clients.size() + "/" + MAX_PLAYERS + ")");
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

    if (playerId != currentPlayerIndex && packet.type != PacketType.JOIN_REQUEST) {
      log("REJECTED: Not player " + playerId + "'s turn (current: " + currentPlayerIndex + ")");
      sendTo(playerId, new Packet(PacketType.ACTION_REJECTED, playerId));
      return;
    }

    switch (packet.type) {
      case PLAY_CARD:
        PlayCardPacket pcp = (PlayCardPacket) packet;
        log("Player " + playerId + " plays card " + pcp.cardIndex + " targeting player " + pcp.targetPlayerId);
        logEvent("Player " + playerId + " played card on player " + pcp.targetPlayerId);
        actionPointsRemaining--;
        broadcastState("Player " + playerId + " played a card on player " + pcp.targetPlayerId);
        break;

      case DRAW_CARD:
        log("Player " + playerId + " draws a card");
        logEvent("Player " + playerId + " drew a card");
        actionPointsRemaining--;
        broadcastState("Player " + playerId + " drew a card");
        break;

      case PUNCH:
        PunchPacket pp = (PunchPacket) packet;
        log("Player " + playerId + " punches player " + pp.targetPlayerId);
        logEvent("Player " + playerId + " punched player " + pp.targetPlayerId);
        actionPointsRemaining--;
        broadcastState("Player " + playerId + " punched player " + pp.targetPlayerId);
        break;

      case END_TURN:
        log("Player " + playerId + " ends their turn");
        logEvent("Player " + playerId + " ended turn");
        nextTurn();
        break;

      default:
        log("Unhandled packet type: " + packet.type);
    }

    if (actionPointsRemaining <= 0 && packet.type != PacketType.END_TURN) {
      log("Player " + playerId + " out of action points, auto-ending turn");
      nextTurn();
    }
  }

  static void nextTurn() {
    currentPlayerIndex = (currentPlayerIndex + 1) % MAX_PLAYERS;
    actionPointsRemaining = 3;
    log("It is now player " + currentPlayerIndex + "'s turn");
    logEvent("Turn passed to player " + currentPlayerIndex);
    broadcastState("Player " + currentPlayerIndex + "'s turn");
    notifyCurrentPlayer();
  }

  static void notifyCurrentPlayer() {
    sendTo(currentPlayerIndex, new Packet(PacketType.YOUR_TURN, currentPlayerIndex));
  }

  static void broadcastState(String message) {
    StateUpdatePacket state = new StateUpdatePacket(currentPlayerIndex, actionPointsRemaining, message);
    broadcastToAll(state);
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

  // UDP Discovery
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

  // Debug Web Server
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
        .append("</style></head><body>")
        .append("<h1>Buddies with Benefits - Server</h1>")
        .append("<p>Players: ").append(clients.size()).append("/").append(MAX_PLAYERS).append("</p>")
        .append("<p>Current turn: Player ").append(currentPlayerIndex).append("</p>")
        .append("<p>Action points: ").append(actionPointsRemaining).append("</p>");

    sb.append("<h2>Players</h2>")
        .append("<table><tr><th>ID</th><th>IP</th><th>Status</th></tr>");
    for (ClientConnection conn : clients) {
      sb.append("<tr>")
          .append("<td>Player ").append(conn.playerId).append("</td>")
          .append("<td>").append(conn.socket.getInetAddress().getHostAddress()).append("</td>")
          .append("<td>").append(conn.socket.isConnected() ? "Connected" : "Disconnected").append("</td>")
          .append("</tr>");
    }
    sb.append("</table>");

    sb.append("<h2>Event Log</h2>");
    int start = Math.max(0, eventLog.size() - 20);
    for (int i = eventLog.size() - 1; i >= start; i--) {
      sb.append("<div class='log'>").append(eventLog.get(i)).append("</div>");
    }

    sb.append("</body></html>");
    return sb.toString();
  }

  // Logging
  static void log(String message) {
    String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new Date());
    System.out.println("[" + timestamp + "] " + message);
  }

  static void logEvent(String event) {
    String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new Date());
    eventLog.add("[" + timestamp + "] " + event);
  }
}
