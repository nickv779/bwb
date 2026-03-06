package com.ex.bwb.networking;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ServerDiscovery {
    private static final int BROADCAST_PORT = 9876;

    // Server: broadcast availability every second
    public void startBroadcasting(String gameName, int tcpPort) throws SocketException, UnknownHostException {
        DatagramSocket socket = new DatagramSocket();
        socket.setBroadcast(true);
        String message = "BWB_GAME:" + gameName + ":" + tcpPort;
        DatagramPacket packet = new DatagramPacket(
                message.getBytes(),
                message.length(),
                InetAddress.getByName("255.255.255.255"),
                BROADCAST_PORT
        );
    }

    // Client: listen for broadcasts
    public void listenForServers(OnServerFoundListener listener) throws IOException {
        DatagramSocket socket = new DatagramSocket(BROADCAST_PORT);
        DatagramPacket packet = new DatagramPacket(new byte[256], 256);
        socket.receive(packet);
    }

    public interface OnServerFoundListener {
        void onServerFound(String serverIP, int tcpPort, String gameName);
    }
}
