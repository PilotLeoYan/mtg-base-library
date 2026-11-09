package com.mtg.platform;

import com.mtg.network.CustomPacket;

public class MockNetworkHelper implements INetworkHelper {
    @Override
    public void sendToServer(CustomPacket packet) {}

    @Override
    public void sendToPlayer(Object player, CustomPacket packet) {}

    @Override
    public void sendToAllTracking(Object entityOrLocation, CustomPacket packet) {}
}
