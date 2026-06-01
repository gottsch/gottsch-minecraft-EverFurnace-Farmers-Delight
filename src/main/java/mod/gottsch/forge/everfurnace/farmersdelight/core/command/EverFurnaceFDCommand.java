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
package mod.gottsch.forge.everfurnace.farmersdelight.core.command;

import com.mojang.brigadier.CommandDispatcher;
import mod.gottsch.forge.everfurnace.farmersdelight.core.config.EverFurnaceFDConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Client-side command for EverFurnace: Farmer's Delight.
 *
 * <p>{@code /everfurnacefd message}            — show whether this client displays the catch-up
 *                                                action-bar message.
 * <p>{@code /everfurnacefd message <on|off>}   — enable/disable it for this client (persists to
 *                                                the client config toml).
 *
 * <p>Registered via {@code RegisterClientCommandsEvent}, so it runs locally on the player's
 * client — it toggles a per-player CLIENT preference and therefore needs no op permission and
 * works identically in singleplayer and on servers.
 *
 * @author Mark Gottschling
 */
public class EverFurnaceFDCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("everfurnacefd")

                // /everfurnacefd message            → query
                // /everfurnacefd message on|off     → set
                .then(Commands.literal("message")
                    .executes(ctx -> queryMessage(ctx.getSource()))
                    .then(Commands.literal("on")
                        .executes(ctx -> setMessage(ctx.getSource(), true)))
                    .then(Commands.literal("off")
                        .executes(ctx -> setMessage(ctx.getSource(), false))))
        );
    }

    // ------------------------------------------------------------------
    // /everfurnacefd message
    // ------------------------------------------------------------------

    private static int queryMessage(CommandSourceStack source) {
        boolean enabled = EverFurnaceFDConfig.CLIENT.catchupMessageEnabled.get();
        source.sendSuccess(() -> Component.literal(
                "Catch-up action-bar message is currently " + (enabled ? "ON" : "OFF") + ".")
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY), false);
        return enabled ? 1 : 0;
    }

    // ------------------------------------------------------------------
    // /everfurnacefd message <on|off>
    // ------------------------------------------------------------------

    private static int setMessage(CommandSourceStack source, boolean enabled) {
        EverFurnaceFDConfig.CLIENT.catchupMessageEnabled.set(enabled); // persists to toml
        source.sendSuccess(() -> Component.literal(
                "Catch-up action-bar message " + (enabled ? "enabled" : "disabled") + ".")
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.YELLOW), false);
        return enabled ? 1 : 0;
    }
}
