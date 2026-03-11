package com.ex.bwb.networking.packets;

public enum PacketType {
  // Client -> Server
  JOIN_REQUEST,
  PLAY_CARD,
  DRAW_CARD,
  PUNCH,
  HEAL,
  SWAP_LIL_BUDDY,
  END_TURN,

  // Server -> Client
  JOIN_ACCEPTED,
  GAME_START,
  STATE_UPDATE,
  YOUR_TURN,
  ACTION_REJECTED,
  GAME_OVER
}
