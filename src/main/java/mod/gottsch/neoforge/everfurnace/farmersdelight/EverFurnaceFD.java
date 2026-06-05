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
package mod.gottsch.neoforge.everfurnace.farmersdelight;

import com.mojang.logging.LogUtils;
import mod.gottsch.neoforge.everfurnace.api.EverFurnaceApi;
import mod.gottsch.neoforge.everfurnace.farmersdelight.core.catchup.CookingPotCatchupHandler;
import mod.gottsch.neoforge.everfurnace.farmersdelight.core.catchup.SkilletCatchupHandler;
import mod.gottsch.neoforge.everfurnace.farmersdelight.core.catchup.StoveCatchupHandler;
import mod.gottsch.neoforge.everfurnace.farmersdelight.core.command.EverFurnaceFDCommand;
import mod.gottsch.neoforge.everfurnace.farmersdelight.core.config.EverFurnaceFDConfig;
import mod.gottsch.neoforge.everfurnace.farmersdelight.core.network.ModNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import vectorwing.farmersdelight.common.block.entity.AbstractStoveBlockEntity;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;
import vectorwing.farmersdelight.common.block.entity.SkilletBlockEntity;
import vectorwing.farmersdelight.common.registry.ModBlockEntityTypes;

/**
 * Mod entry point for EverFurnace: Farmer's Delight.
 *
 * Registers catch-up handlers for Farmer's Delight cooking blocks via the
 * EverFurnace public API. EverFurnace must load before this mod (declared in
 * neoforge.mods.toml with ordering=AFTER).
 *
 * @author Mark Gottschling
 */
@Mod(EverFurnaceFD.MOD_ID)
public class EverFurnaceFD {

    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MOD_ID = "everfurnace_farmersdelight";

    public EverFurnaceFD(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, EverFurnaceFDConfig.CLIENT_SPEC);

        modEventBus.addListener(ModNetwork::onRegisterPayloads);
        modEventBus.addListener(this::commonSetup);
        // FMLClientSetupEvent only fires on the physical client, so the client-only
        // RegisterClientCommandsEvent class is never referenced on a dedicated server.
        modEventBus.addListener(this::clientSetup);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        // /everfurnacefd is a CLIENT command (toggles a per-player client preference),
        // registered on the NeoForge event bus via RegisterClientCommandsEvent.
        NeoForge.EVENT_BUS.addListener(this::onRegisterClientCommands);
    }

    private void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        EverFurnaceFDCommand.register(event.getDispatcher());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CookingPotCatchupHandler cookingPotHandler = new CookingPotCatchupHandler();
            StoveCatchupHandler      stoveHandler      = new StoveCatchupHandler();
            SkilletCatchupHandler    skilletHandler    = new SkilletCatchupHandler();

            EverFurnaceApi.registerHandler(ModBlockEntityTypes.COOKING_POT.get(), cookingPotHandler);
            EverFurnaceApi.registerHandler(ModBlockEntityTypes.STOVE.get(),       stoveHandler);
            EverFurnaceApi.registerHandler(ModBlockEntityTypes.SKILLET.get(),     skilletHandler);

            // Capability defaults: catch up any mod that subclasses an FD cooking
            // block (reusing its ticker) but registers its own BlockEntityType.
            // The exact-type registrations above remain the override path; these
            // only fill the gap they leave.  Each predicate matches the class its
            // mixin is woven into, so the handler's accessor cast is always safe.
            EverFurnaceApi.registerFallback(be -> be instanceof CookingPotBlockEntity,   cookingPotHandler);
            EverFurnaceApi.registerFallback(be -> be instanceof AbstractStoveBlockEntity, stoveHandler);
            EverFurnaceApi.registerFallback(be -> be instanceof SkilletBlockEntity,      skilletHandler);

            LOGGER.debug("EverFurnace: Farmer's Delight initialised — cooking pot, stove, and skillet catch-up registered.");
        });
    }
}
