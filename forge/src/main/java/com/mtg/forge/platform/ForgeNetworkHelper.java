package com.mtg.forge.platform;

import com.mtg.network.CustomPacket;
import com.mtg.platform.INetworkHelper;

public class ForgeNetworkHelper implements INetworkHelper {
    @Override
    public void sendToServer(CustomPacket packet) {
        // Dispatched via SimpleChannel.sendToServer in runtime
    }

    @Override
    public void sendToPlayer(Object player, CustomPacket packet) {
        // Dispatched via PacketDistributor.PLAYER in runtime
    }

    @Override
    public void sendToAllTracking(Object entityOrLocation, CustomPacket packet) {
        // Dispatched via PacketDistributor.TRACKING_ENTITY in runtime
    }
}
