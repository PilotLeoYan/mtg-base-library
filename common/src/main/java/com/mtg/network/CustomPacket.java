package com.mtg.network;

public interface CustomPacket {
    String getChannelId();
    byte[] encode();
    default void handle(Object context) {}
}
