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
package mod.gottsch.forge.everfurnace.farmersdelight.core.catchup;

import mod.gottsch.forge.everfurnace.api.CookingCatchupHandler;
import mod.gottsch.forge.everfurnace.farmersdelight.EverFurnaceFD;
import mod.gottsch.forge.everfurnace.farmersdelight.core.mixin.ISkilletMixin;
import mod.gottsch.forge.everfurnace.farmersdelight.core.notify.CatchupFeedback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import vectorwing.farmersdelight.common.block.entity.SkilletBlockEntity;

/**
 * Catch-up handler for {@code SkilletBlockEntity}.
 *
 * <h2>Strategy — iterative simulation</h2>
 * <p>The skillet has a single inventory slot, but that slot may hold a stack
 * greater than one. A single offline window can therefore complete several
 * items, so — like the cooking pot, and unlike the stove — we cannot simply
 * push progress to the cap and let one vanilla tick finish the craft. Instead
 * we drive vanilla's own completion method ({@code cookAndOutputItems}) once per
 * finished item.
 *
 * <h2>Cycle completion trick</h2>
 * <p>{@code cookAndOutputItems} does {@code ++cookingTime} and, once it reaches
 * {@code cookingTimeTotal}, assembles + spawns one result, resets
 * {@code cookingTime} to 0, and extracts one item from the slot. Setting
 * {@code cookingTime = total - 1} before the call makes the internal increment
 * land exactly on the total, triggering completion for exactly one item. Reusing
 * the vanilla method keeps every side effect correct — result assembly, item-entity
 * spawn, and the {@code inventoryChanged} client sync (so the slot stops showing
 * the now-consumed raw item).
 *
 * <h2>Guards</h2>
 * <ul>
 *   <li>{@code !isHeated()} — without a heat source below, vanilla cools the
 *       skillet (reduces {@code cookingTime} by 2/tick) instead of cooking, so any
 *       progress we pushed would be undone; skip.</li>
 *   <li>Empty slot — nothing to cook.</li>
 *   <li>{@code cookingTimeTotal <= 0} — no active recipe.</li>
 * </ul>
 *
 * <h2>Heat assumption</h2>
 * <p>The heat source is the block directly below; it persists across unloads and
 * does not deplete, so "heated now → heated the whole interval" is sound.
 *
 * @author Mark Gottschling
 */
public class SkilletCatchupHandler implements CookingCatchupHandler {

    @Override
    public void applyCatchup(BlockEntity blockEntity, long deltaTime, ServerLevel level, BlockPos pos) {

        SkilletBlockEntity skillet  = (SkilletBlockEntity) blockEntity;
        ISkilletMixin      accessor = (ISkilletMixin)(Object) blockEntity;

        // ── Pre-checks ────────────────────────────────────────────────────────

        // Heat source persists across unloads; if it's gone now it was probably
        // gone before. Without heat, vanilla cools instead of cooks.
        if (!skillet.isHeated()) return;
        if (!skillet.hasStoredStack()) return;

        int total = accessor.getCookingTimeTotal();
        if (total <= 0) return;

        // ── Simulate elapsed time ─────────────────────────────────────────────

        int  cookingTime        = accessor.getCookingTime();
        long ticksToFinishFirst = Math.max(0L, total - cookingTime);
        boolean anyCompleted    = false;

        if (deltaTime < ticksToFinishFirst) {
            // Not enough time to finish even the current item — advance progress only.
            accessor.setCookingTime(cookingTime + (int) deltaTime);

        } else {
            // Complete the in-progress item.
            deltaTime -= ticksToFinishFirst;
            accessor.setCookingTime(total - 1);
            accessor.callCookAndOutputItems(skillet.getStoredStack(), level);
            anyCompleted = true;

            // Keep completing whole items while time and stock remain. Each fresh
            // item needs a full `total` ticks (slot holds a stack of one item type,
            // so the recipe — and its cook time — stays the same).
            while (deltaTime > 0 && skillet.hasStoredStack()) {
                if (deltaTime < total) {
                    // Partial progress on the next item — don't complete it.
                    accessor.setCookingTime((int) deltaTime);
                    break;
                }
                deltaTime -= total;
                accessor.setCookingTime(total - 1);
                accessor.callCookAndOutputItems(skillet.getStoredStack(), level);
            }
        }

        // ── Post-catch-up bookkeeping ─────────────────────────────────────────

        if (anyCompleted) {
            skillet.setChanged();
            CatchupFeedback.notify(level, pos);
            EverFurnaceFD.LOGGER.debug("EverFurnace FD: skillet at {} finished catch-up", pos);
        }
    }
}
