package com.ex.bwb.networking.packets;

public class EndTurnPacket extends Packet {
  public EndTurnPacket(int playerId) {
    super(PacketType.END_TURN, playerId);
  }
}
