package com.ex.bwb;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.ex.bwb.networking.GameClient;
import com.ex.bwb.networking.GameServer;
import com.ex.bwb.networking.ServerDiscovery;

import gl.activities.GyroscopicActivity;

public class MainActivity extends GyroscopicActivity {
    private static final String TAG = "MainActivity";

    // FLIP THIS: true on the host phone, false on the joining phone
    private static final boolean IS_HOST = true;
    private static final int PORT = 5556;

    private GameServer server;
    private GameClient client;
    private ServerDiscovery discovery;
    private WifiManager.MulticastLock multicastLock;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        discovery = new ServerDiscovery();

        // Acquire multicast lock
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        multicastLock = wifi.createMulticastLock("bwb_lock");
        multicastLock.setReferenceCounted(true);
        multicastLock.acquire();

        if (IS_HOST) {
            // Start server and broadcast
            server = new GameServer(PORT);
            server.start();
            discovery.startBroadcasting("TestGame", PORT);
            Log.d(TAG, "Hosting game...");

            // Host also connects as a client to itself
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                client = new GameClient();
                client.connect("127.0.0.1", PORT);
            }, 1000);
        } else {
            // Listen for a server on the network, then connect
            Log.d(TAG, "Searching for game...");
            discovery.listenForServers((serverIP, tcpPort, gameName) -> {
                Log.d(TAG, "Found game: " + gameName + " at " + serverIP + ":" + tcpPort);
                client = new GameClient();
                client.connect(serverIP, tcpPort);
            });
        }
    }

    @Override
    public void Start() {
        scene.background(0, 0, 0);
    }

    @Override
    public void Update() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (discovery != null) discovery.stop();
        if (client != null) client.disconnect();
        if (server != null) server.stop();
        if (multicastLock != null && multicastLock.isHeld()) {
            multicastLock.release();
        }
    }
}