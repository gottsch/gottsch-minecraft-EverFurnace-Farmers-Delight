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
package mod.gottsch.forge.everfurnace.farmersdelight.core.mixin;

import mod.gottsch.forge.everfurnace.api.EverFurnaceApi;
import mod.gottsch.forge.everfurnace.farmersdelight.core.cooking.IEverFurnaceFDStove;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vectorwing.farmersdelight.common.block.entity.AbstractStoveBlockEntity;

/**
 * Injects offline catch-up timing into {@code AbstractStoveBlockEntity}.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Stores {@code everfurnacefd$lastGameTime} — the game-time stamp of the
 *       last tick this stove ran, persisted to NBT.</li>
 *   <li>On each {@code serverTick} (HEAD): computes {@code deltaTime},
 *       then delegates to the registered {@link mod.gottsch.forge.everfurnace.api.CookingCatchupHandler}
 *       via {@link EverFurnaceApi}.</li>
 * </ul>
 *
 * <p>Targets {@code AbstractStoveBlockEntity} because {@code serverTick} is
 * declared there. The {@code @Unique} field is injected into all concrete
 * subclasses (e.g. {@code StoveBlockEntity}).
 *
 * <p>All cooking logic lives in
 * {@link mod.gottsch.forge.everfurnace.farmersdelight.core.catchup.StoveCatchupHandler}.
 *
 * @author Mark Gottschling
 */
@Mixin(AbstractStoveBlockEntity.class)
public abstract class StoveBlockEntityMixin implements IEverFurnaceFDStove {

    private static final String NBT_LAST_GAME_TIME = "everfurnacefd_lastGameTime";

    @Unique
    private long everfurnacefd$lastGameTime = 0L;

    // -------------------------------------------------------------------------
    // IEverFurnaceFDStove
    // -------------------------------------------------------------------------

    @Override
    public long everfurnacefd$getLastGameTime() {
        return this.everfurnacefd$lastGameTime;
    }

    @Override
    public void everfurnacefd$setLastGameTime(long time) {
        this.everfurnacefd$lastGameTime = time;
    }

    // -------------------------------------------------------------------------
    // NBT persistence
    // -------------------------------------------------------------------------

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void everfurnacefd$saveAdditional(CompoundTag compound, CallbackInfo ci) {
        compound.putLong(NBT_LAST_GAME_TIME, this.everfurnacefd$lastGameTime);
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void everfurnacefd$load(CompoundTag compound, CallbackInfo ci) {
        this.everfurnacefd$lastGameTime = compound.getLong(NBT_LAST_GAME_TIME);
    }

    // -------------------------------------------------------------------------
    // Catch-up tick
    // -------------------------------------------------------------------------

    /**
     * HEAD inject into the static {@code serverTick}. Computes elapsed game
     * time since the last tick and fires the registered catch-up handler.
     *
     * <p>Guards:
     * <ul>
     *   <li>Client side — skip.</li>
     *   <li>Catch-up disabled — skip.</li>
     *   <li>First tick ({@code lastGameTime == 0}) — stamp and return; no delta yet.</li>
     *   <li>{@code deltaTime < minDeltaThreshold} — skip (normal inter-tick noise).</li>
     * </ul>
     */
    @Inject(method = "serverTick", at = @At("HEAD"), remap = false)
    private static void everfurnacefd$serverTick(Level level, BlockPos pos, BlockState state,
                                                  AbstractStoveBlockEntity stoveEntity, CallbackInfo ci) {
        if (level.isClientSide()) return;
        if (!EverFurnaceApi.isCatchupEnabled()) return;

        IEverFurnaceFDStove stove = (IEverFurnaceFDStove)(Object) stoveEntity;

        long currentGameTime = level.getGameTime();
        long lastGameTime    = stove.everfurnacefd$getLastGameTime();

        if (lastGameTime == 0L) {
            stove.everfurnacefd$setLastGameTime(currentGameTime);
            return;
        }

        long deltaTime = currentGameTime - lastGameTime;
        stove.everfurnacefd$setLastGameTime(currentGameTime);

        if (deltaTime < EverFurnaceApi.getMinDeltaThreshold()) return;
        deltaTime = Math.min(deltaTime, EverFurnaceApi.getMaxCatchupTicks());

        final long finalDelta = deltaTime;
        EverFurnaceApi.findHandler(stoveEntity)
                .ifPresent(h -> h.applyCatchup(stoveEntity, finalDelta, (ServerLevel) level, pos));
    }
}
