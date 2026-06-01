/*
 * This file is part of EverFurnace: Farmer's Delight.
 * Copyright (c) 2026 Mark Gottschling (gottsch)
 *
 * EverFurnace: Farmer's Delight is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * EverFurnace: Farmer's Delight is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with EverFurnace: Farmer's Delight.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */
package mod.gottsch.forge.everfurnace.farmersdelight.core.network;

import mod.gottsch.forge.everfurnace.farmersdelight.EverFurnaceFD;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

/**
 * Network channel for EverFurnace: Farmer's Delight.
 *
 * <p>Carries a single server → client packet ({@link CatchupMessagePacket}) used to deliver the
 * catch-up completion message. The client decides whether to actually display it based on its
 * own {@code EverFurnaceFDConfig.CLIENT.catchupMessageEnabled} — so the per-player preference is
 * honoured even on dedicated servers (where the client config is unavailable server-side).
 *
 * <p>Modelled on EverFurnace core's {@code ModNetwork}.
 *
 * @author Mark Gottschling
 */
public class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";

    // acceptMissingOr — vanilla clients (without the mod) are accepted silently
    // rather than being kicked with a channel-mismatch error.
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EverFurnaceFD.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION::equals),
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        CHANNEL.registerMessage(
                0,
                CatchupMessagePacket.class,
                CatchupMessagePacket::encode,
                CatchupMessagePacket::decode,
                CatchupMessagePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    /**
     * Sends a {@link CatchupMessagePacket} to nearby players that have this mod installed.
     *
     * <p>Vanilla clients (who lack the channel) are skipped via
     * {@link SimpleChannel#isRemotePresent} — this prevents sending to connections that
     * accepted the handshake via {@code acceptMissingOr} but never registered the channel.
     */
    public static void sendCatchupMessage(ServerLevel level, BlockPos pos) {
        CatchupMessagePacket packet = new CatchupMessagePacket(pos);
        double radiusSq = 32.0 * 32.0;
        double cx = pos.getX() + 0.5, cy = pos.getY() + 0.5, cz = pos.getZ() + 0.5;
        for (ServerPlayer player : level.players()) {
            if (!CHANNEL.isRemotePresent(player.connection.connection)) continue;
            if (player.distanceToSqr(cx, cy, cz) > radiusSq) continue;
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }
}
