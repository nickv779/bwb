package com.ex.bwb.networking.packets;

import java.io.Serializable;

public class HandSyncPacket extends Packet implements Serializable {
    public String[] cardNames;

    public HandSyncPacket(int playerId, String[] cardNames) {
        super(PacketType.HAND_SYNC, playerId);
        this.cardNames = cardNames;
    }
}