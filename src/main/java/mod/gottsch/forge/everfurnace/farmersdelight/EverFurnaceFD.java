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
package mod.gottsch.forge.everfurnace.farmersdelight;

import mod.gottsch.forge.everfurnace.api.EverFurnaceApi;
import mod.gottsch.forge.everfurnace.farmersdelight.core.catchup.CookingPotCatchupHandler;
import mod.gottsch.forge.everfurnace.farmersdelight.core.catchup.SkilletCatchupHandler;
import mod.gottsch.forge.everfurnace.farmersdelight.core.catchup.StoveCatchupHandler;
import mod.gottsch.forge.everfurnace.farmersdelight.core.command.EverFurnaceFDCommand;
import mod.gottsch.forge.everfurnace.farmersdelight.core.config.EverFurnaceFDConfig;
import mod.gottsch.forge.everfurnace.farmersdelight.core.network.ModNetwork;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import vectorwing.farmersdelight.common.registry.ModBlockEntityTypes;

/**
 * Mod entry point for EverFurnace: Farmer's Delight.
 *
 * Registers catch-up handlers for Farmer's Delight cooking blocks via the
 * EverFurnace public API. EverFurnace must load before this mod (declared in
 * mods.toml with ordering=AFTER).
 *
 * @author Mark Gottschling
 */
@Mod(EverFurnaceFD.MOD_ID)
public class EverFurnaceFD {

    public static final Logger LOGGER = LogManager.getLogger(EverFurnaceFD.MOD_ID);
    public static final String MOD_ID = "everfurnace_farmersdelight";

    public EverFurnaceFD() {
        // Register config before setup so values are available when handlers run.
        EverFurnaceFDConfig.register();

        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::commonSetup);
        // FMLClientSetupEvent only fires on the physical client, so the client-only
        // RegisterClientCommandsEvent class is never referenced on a dedicated server.
        modBus.addListener(this::clientSetup);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        // /everfurnacefd is a CLIENT command (toggles a per-player client preference),
        // registered on the Forge event bus via RegisterClientCommandsEvent.
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterClientCommands);
    }

    private void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        EverFurnaceFDCommand.register(event.getDispatcher());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModNetwork.register();

            EverFurnaceApi.registerHandler(
                    ModBlockEntityTypes.COOKING_POT.get(),
                    new CookingPotCatchupHandler());

            EverFurnaceApi.registerHandler(
                    ModBlockEntityTypes.STOVE.get(),
                    new StoveCatchupHandler());

            EverFurnaceApi.registerHandler(
                    ModBlockEntityTypes.SKILLET.get(),
                    new SkilletCatchupHandler());

            LOGGER.debug("EverFurnace: Farmer's Delight initialised — cooking pot, stove, and skillet catch-up registered.");
        });
    }
}
