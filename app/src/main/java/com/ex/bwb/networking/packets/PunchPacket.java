package com.ex.bwb.networking.packets;

public class PunchPacket extends Packet {
  public int targetPlayerId;

  public PunchPacket(int playerId, int targetPlayerId) {
    super(PacketType.PUNCH, playerId);
    this.targetPlayerId = targetPlayerId;
  }
}
