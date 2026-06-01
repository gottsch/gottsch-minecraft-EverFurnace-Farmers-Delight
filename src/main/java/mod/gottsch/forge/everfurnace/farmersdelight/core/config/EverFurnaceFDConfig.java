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
package mod.gottsch.forge.everfurnace.farmersdelight.core.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Forge configuration for EverFurnace: Farmer's Delight.
 *
 * <p>A single {@link Client} spec is used, stored in
 * {@code config/everfurnace_farmersdelight-client.toml}. The catch-up behaviour itself
 * (enable, max ticks, min delta) is governed by EverFurnace <em>core</em>'s config via
 * {@code EverFurnaceApi}; this addon config only owns the per-player message preference.
 *
 * <p>Why CLIENT: the catch-up completion message is sent from the server, but whether an
 * individual player wants to <em>see</em> it is a personal preference. The server always sends
 * the message packet to nearby players; each client decides whether to display it by reading
 * this config (mirroring how core gates particles/sound). This means the value is unavailable
 * server-side on a dedicated server, so the gate must run client-side.
 *
 * @author Mark Gottschling
 */
public final class EverFurnaceFDConfig {

    public static final Client CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        final Pair<Client, ForgeConfigSpec> clientPair =
                new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT      = clientPair.getLeft();
        CLIENT_SPEC = clientPair.getRight();
    }

    private EverFurnaceFDConfig() {}

    /** Register the config spec with Forge. Call from the mod constructor. */
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
    }

    public static final class Client {

        /**
         * When {@code true}, an action-bar message is shown on this client when a nearby
         * cooking pot or stove finishes cooking during catch-up.
         *
         * <p>Client-side: the server always sends the message packet to nearby players; this
         * toggle decides whether <em>this</em> client renders it. Toggle at runtime with the
         * client command {@code /everfurnacefd message <on|off>}.
         */
        public final ForgeConfigSpec.BooleanValue catchupMessageEnabled;

        Client(ForgeConfigSpec.Builder builder) {
            builder.comment("EverFurnace: Farmer's Delight — Client-side configuration")
                    .push("notifications");

            catchupMessageEnabled = builder
                    .comment("Show an action-bar message on this client when a nearby cooking pot or",
                            "stove finishes cooking during catch-up (chunk reload after being away).",
                            "Particles and sound are controlled by EverFurnace core's client config.",
                            "Toggle in-game with /everfurnacefd message <on|off>.",
                            "Default: true")
                    .define("catchupMessageEnabled", true);

            builder.pop();
        }
    }
}
