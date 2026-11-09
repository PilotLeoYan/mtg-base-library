package com.mtg.fabric.platform;

import com.mtg.network.CustomPacket;
import com.mtg.platform.INetworkHelper;

public class FabricNetworkHelper implements INetworkHelper {
    @Override
    public void sendToServer(CustomPacket packet) {
        // Dispatched via Fabric ClientPlayNetworking in runtime
    }

    @Override
    public void sendToPlayer(Object player, CustomPacket packet) {
        // Dispatched via Fabric ServerPlayNetworking in runtime
    }

    @Override
    public void sendToAllTracking(Object entityOrLocation, CustomPacket packet) {
        // Dispatched via PlayerLookup.tracking in runtime
    }
}
