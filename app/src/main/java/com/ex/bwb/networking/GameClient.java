package com.ex.bwb.networking;

import android.util.Log;
import java.io.IOException;
import java.net.Socket;

public class GameClient {
  public static final String TAG = "Game Client";

  private Socket socket;
  private boolean connected = false;

  public void connect(String serverIP, int port) {
    new Thread(() -> {
      try {
        socket = new Socket(serverIP, port);
        connected = true;
        Log.d(TAG, "Connected to server at " + serverIP + ":" + port);
      }

      catch(IOException e) {
        Log.e(TAG, "Connection failed: " + e.getMessage());
      }
    }).start();
  }

  public void disconnect() {
    try {
      if (socket != null) socket.close();
      connected = false;
    }

    catch (IOException e) {
      Log.e(TAG, "Error disconnecting: " + e.getMessage());
    }
  }

  public boolean isConnected() {
    return connected;
  }
}
