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
package mod.gottsch.neoforge.everfurnace.farmersdelight.core.notify;

import mod.gottsch.neoforge.everfurnace.core.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Player-facing feedback for a completed catch-up.
 *
 * <p>Two cues, both fired only when at least one item actually finished cooking
 * during a catch-up pass:
 * <ul>
 *   <li><b>Particles + sound</b> — delegated to EverFurnace core's
 *       {@link ModNetwork#sendCatchupParticles(ServerLevel, BlockPos)}, so Farmer's
 *       Delight blocks emit the same flame/smoke burst and crackle as vanilla
 *       furnaces, respecting core's client-side toggles.</li>
 *   <li><b>Action-bar message</b> — sent via this addon's
 *       {@link mod.gottsch.neoforge.everfurnace.farmersdelight.core.network.ModNetwork#sendCatchupMessage(ServerLevel, BlockPos)}
 *       to nearby players. The server always sends it; each client decides whether to display it
 *       based on its own {@code EverFurnaceFDConfig.CLIENT.catchupMessageEnabled} (a per-player
 *       preference, honoured even on dedicated servers).</li>
 * </ul>
 *
 * <p>Catch-up always runs server-side on the first tick after a chunk reloads, which
 * only happens because a player is nearby — so both packets' 32-block radius filters
 * reliably target the player(s) who triggered the load.
 *
 * @author Mark Gottschling
 */
public final class CatchupFeedback {

    private CatchupFeedback() {}

    /**
     * Fire both cues at the given block position.
     *
     * @param level the server level the cooking block lives in
     * @param pos   the cooking block's position
     */
    public static void notify(ServerLevel level, BlockPos pos) {
        // Particles + sound — reuse core's networking (gated by core's client config).
        ModNetwork.sendCatchupParticles(level, pos);

        // Action-bar message — addon's own packet; the client gates display via its config.
        mod.gottsch.neoforge.everfurnace.farmersdelight.core.network.ModNetwork
                .sendCatchupMessage(level, pos);
    }
}
