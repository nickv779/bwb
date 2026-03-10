package com.ex.bwb.networking.packets;

import java.io.Serializable;

public class Packet implements Serializable {
  public PacketType type;
  public int playerId;

  public Packet(PacketType type, int playerId) {
    this.type = type;
    this.playerId = playerId;
  }
}
