package com.ex.bwb.networking.packets;

public class DrawCardPacket extends Packet {
  public DrawCardPacket(int playerId){
    super(PacketType.DRAW_CARD, playerId);
  }
}
