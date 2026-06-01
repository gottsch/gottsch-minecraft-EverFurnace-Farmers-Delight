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

import mod.gottsch.neoforge.everfurnace.farmersdelight.core.config.EverFurnaceFDConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Client-only handler for {@link CatchupMessagePacket}.
 *
 * <p>Isolated into its own class so the server never attempts to load any client-only classes
 * (Minecraft, LocalPlayer, etc.). Mirrors core's {@code CatchupParticleHandler}.
 *
 * <p>The per-player preference is honoured here: the server always sends the packet, but the
 * action-bar message is only displayed if this client has it enabled in its config.
 *
 * @author Mark Gottschling
 */
@OnlyIn(Dist.CLIENT)
public class CatchupMessageHandler {

    /** Lang key for the action-bar message. */
    private static final String MSG_KEY = "message.everfurnace_farmersdelight.catchup_complete";

    public static void handle(BlockPos pos) {
        if (!EverFurnaceFDConfig.CLIENT.catchupMessageEnabled.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // true = action bar (above the hotbar), not the chat log.
        mc.player.displayClientMessage(Component.translatable(MSG_KEY), true);
    }
}
