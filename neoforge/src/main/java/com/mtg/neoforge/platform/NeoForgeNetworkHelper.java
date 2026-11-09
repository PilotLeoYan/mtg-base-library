package com.mtg.neoforge.platform;

import com.mtg.network.CustomPacket;
import com.mtg.platform.INetworkHelper;

public class NeoForgeNetworkHelper implements INetworkHelper {
    @Override
    public void sendToServer(CustomPacket packet) {
        // Dispatched via PacketDistributor.sendToServer in runtime
    }

    @Override
    public void sendToPlayer(Object player, CustomPacket packet) {
        // Dispatched via PacketDistributor.sendToPlayer in runtime
    }

    @Override
    public void sendToAllTracking(Object entityOrLocation, CustomPacket packet) {
        // Dispatched via PacketDistributor.sendToPlayersTrackingEntity in runtime
    }
}
