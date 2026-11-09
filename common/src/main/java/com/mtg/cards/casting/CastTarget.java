package com.mtg.cards.casting;

import com.mtg.network.C2SCastCardPacket;

import java.util.Objects;

/**
 * Immutable value representation of a card cast target.
 * Can target a specific entity, a world position, or self.
 */
public record CastTarget(
    int targetEntityId,
    double targetX,
    double targetY,
    double targetZ,
    boolean hasEntityTarget
) {
    public static CastTarget entity(int entityId, double x, double y, double z) {
        return new CastTarget(entityId, x, y, z, true);
    }

    public static CastTarget position(double x, double y, double z) {
        return new CastTarget(-1, x, y, z, false);
    }

    public static CastTarget self() {
        return new CastTarget(-1, 0.0, 0.0, 0.0, false);
    }

    public static CastTarget fromPacket(C2SCastCardPacket packet) {
        Objects.requireNonNull(packet, "Packet cannot be null");
        boolean hasEntity = packet.targetEntityId() >= 0;
        return new CastTarget(packet.targetEntityId(), packet.targetX(), packet.targetY(), packet.targetZ(), hasEntity);
    }
}
