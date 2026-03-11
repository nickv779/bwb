package com.ex.bwb.networking.packets;

public class StateUpdatePacket extends Packet {
  public int[] playerHPs;
  public int[] playerHandSizes;
  public int[] playerStashCounts;
  public int currentPlayerIndex;
  public int actionPointsRemaining;
  public String message;  // human-readable log of what just happened

  public StateUpdatePacket(int currentPlayer, int actionPoints, String message) {
    super(PacketType.STATE_UPDATE, -1);
    this.playerHPs = new int[]{5, 5, 5, 5};
    this.playerHandSizes = new int[]{0, 0, 0, 0};
    this.playerStashCounts = new int[]{0, 0, 0, 0};
    this.currentPlayerIndex = currentPlayer;
    this.actionPointsRemaining = actionPoints;
    this.message = message;
  }
}
