package com.mtg.platform;

import com.mtg.network.CustomPacket;

public interface INetworkHelper {
    /**
     * Dispatches a network packet from client to logical server.
     */
    void sendToServer(CustomPacket packet);

    /**
     * Dispatches a network packet from server to a specific player.
     */
    void sendToPlayer(Object player, CustomPacket packet);

    /**
     * Dispatches a network packet from server to all players tracking an entity or coordinate.
     */
    void sendToAllTracking(Object entityOrLocation, CustomPacket packet);
}
