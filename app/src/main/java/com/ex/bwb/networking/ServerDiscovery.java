package com.ex.bwb.networking;

import android.util.Log;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ServerDiscovery {
    private static final String TAG = "ServerDiscovery";
    private static final int BROADCAST_PORT = 9877;
    private boolean running = false;

    public interface OnServerFoundListener {
        void onServerFound(String serverIP, int tcpPort, String gameName);
    }

    // Server side: broadcast "I'm here" every second
    public void startBroadcasting(String gameName, int tcpPort) {
        running = true;
        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket();
                socket.setBroadcast(true);
                String message = "BWB_GAME:" + gameName + ":" + tcpPort;

                while (running) {
                    DatagramPacket packet = new DatagramPacket(
                        message.getBytes(),
                        message.length(),
                        InetAddress.getByName("255.255.255.255"),
                        BROADCAST_PORT
                    );
                    socket.send(packet);
                    Log.d(TAG, "Broadcast sent: " + message);
                    Thread.sleep(1000);
                }
                socket.close();
            } catch (Exception e) {
                Log.e(TAG, "Broadcast error: " + e.getMessage());
            }
        }).start();
    }

    // Client side: listen for server broadcasts
    public void listenForServers(OnServerFoundListener listener) {
        running = true;
        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket(BROADCAST_PORT);
                socket.setBroadcast(true);
                byte[] buffer = new byte[256];

                Log.d(TAG, "Listening for servers...");
                while (running) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet); // blocks until a broadcast arrives

                    String message = new String(packet.getData(), 0, packet.getLength());
                    String senderIP = packet.getAddress().getHostAddress();
                    Log.d(TAG, "Found server: " + message + " at " + senderIP);

                    // Parse: "BWB_GAME:gameName:port"
                    String[] parts = message.split(":");
                    if (parts.length == 3 && parts[0].equals("BWB_GAME")) {
                        listener.onServerFound(senderIP, Integer.parseInt(parts[2]), parts[1]);
                        socket.close();
                        return;
                    }
                }
                socket.close();
            } catch (Exception e) {
                Log.e(TAG, "Listen error: " + e.getMessage());
            }
        }).start();
    }

    public void stop() {
        running = false;
    }
}