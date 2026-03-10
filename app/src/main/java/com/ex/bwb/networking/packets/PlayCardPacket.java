package com.ex.bwb.networking.packets;

public class PlayCardPacket extends Packet {
  public int cardIndex;
  public int targetPlayerId;

  public PlayCardPacket(int playerId, int cardIndex, int targetPlayerId) {
    super(PacketType.PLAY_CARD, playerId);
    this.cardIndex = cardIndex;
    this.targetPlayerId = targetPlayerId;
  }
}
