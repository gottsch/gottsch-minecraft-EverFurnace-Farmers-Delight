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
package mod.gottsch.neoforge.everfurnace.farmersdelight.core.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers network payload types for EverFurnace: Farmer's Delight.
 *
 * <p>Must be subscribed to the <em>mod</em> event bus. In {@code EverFurnaceFD}:
 * <pre>{@code
 * modEventBus.addListener(ModNetwork::onRegisterPayloads);
 * }</pre>
 *
 * <p>Carries a single S2C packet ({@link CatchupMessagePacket}) used to deliver the
 * catch-up completion action-bar message. The client decides whether to display it based
 * on its own {@code EverFurnaceFDConfig.CLIENT.catchupMessageEnabled} — so the per-player
 * preference is honoured even on dedicated servers.
 *
 * @author Mark Gottschling
 */
public class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static void onRegisterPayloads(final RegisterPayloadHandlersEvent event) {
        // .optional() — clients without the mod silently skip this packet
        // rather than disconnecting with an unknown-payload error.
        final PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION).optional();

        registrar.playToClient(
                CatchupMessagePacket.TYPE,
                CatchupMessagePacket.STREAM_CODEC,
                CatchupMessagePacket::handle
        );
    }

    /**
     * Sends a {@link CatchupMessagePacket} to nearby players that have this mod installed.
     *
     * <p>Two cases require guarding:
     * <ol>
     *   <li>{@code ConnectionType.OTHER} (vanilla / non-NeoForge) clients have no payload
     *       channel registry at all — skipped up front.</li>
     *   <li>NeoForge clients that do NOT have this addon installed pass the connection-type
     *       check but still lack the channel in their per-connection payload setup.
     *       We catch and silently ignore that case.</li>
     * </ol>
     */
    public static void sendCatchupMessage(ServerLevel level, BlockPos pos) {
        CatchupMessagePacket packet = new CatchupMessagePacket(pos);
        double radiusSq = 32.0 * 32.0;
        double cx = pos.getX() + 0.5, cy = pos.getY() + 0.5, cz = pos.getZ() + 0.5;
        for (ServerPlayer player : level.players()) {
            if (player.connection.getConnectionType().isOther()) continue;
            if (player.distanceToSqr(cx, cy, cz) > radiusSq) continue;
            try {
                PacketDistributor.sendToPlayer(player, packet);
            } catch (UnsupportedOperationException ignored) {
                // NeoForge client without this addon — channel not in their payload setup
            }
        }
    }
}
