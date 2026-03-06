package com.ex.bwb.networking;

import android.util.Log;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class GameServer {
    private static final String TAG = "GameServer";
    private static final int MAX_PLAYERS = 4;

    private ServerSocket serverSocket;
    private List<Socket> clients = new ArrayList<>();
    private boolean running = false;
    private int port;

    public GameServer(int port) {
        this.port = port;
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
                    Socket client = serverSocket.accept();
                    clients.add(client);
                    Log.d(TAG, "Player connected: " + client.getInetAddress() + " (" + clients.size() + "/" + MAX_PLAYERS + ")");
                }

                Log.d(TAG, "All players connected!");
            }

            catch (IOException e) {
                Log.e(TAG, "Server Error: " + e.getMessage());
            }
        }).start();
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
            for (Socket s : clients) s.close();
        }

        catch (IOException e) {
            Log.e(TAG, "Error stopping server: " + e.getMessage());
        }
    }

    public int getClientCount() {
        return clients.size();
    }
}
