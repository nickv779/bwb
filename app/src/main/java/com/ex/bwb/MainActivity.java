package com.ex.bwb;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.ex.bwb.networking.GameClient;
import com.ex.bwb.networking.GameServer;

import gl.activities.GyroscopicActivity;

public class MainActivity extends GyroscopicActivity {
    private static final String TAG = "MainActivity";

    private GameServer server;
    private GameClient client;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Start server on this device
        server = new GameServer(5556);
        server.start();
        Log.d(TAG, "Server starting...");

        // Wait a second for server to spin up, then connect as a client
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            client = new GameClient();
            client.connect("127.0.0.1", 5556);
            Log.d(TAG, "Client connecting...");
        }, 1000);
    }

    @Override
    public void Start() {
        // Leave empty for now — no 3D scene needed for this test
        scene.background(0, 0, 0);
    }

    @Override
    public void Update() {
        // Nothing to update for this test
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (client != null) client.disconnect();
        if (server != null) server.stop();
    }
}