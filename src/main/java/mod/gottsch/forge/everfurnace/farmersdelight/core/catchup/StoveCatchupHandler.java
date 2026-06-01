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
import mod.gottsch.forge.everfurnace.farmersdelight.core.mixin.IStoveMixin;
import mod.gottsch.forge.everfurnace.farmersdelight.core.notify.CatchupFeedback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.ItemStackHandler;
import vectorwing.farmersdelight.common.block.AbstractStoveBlock;
import vectorwing.farmersdelight.common.block.entity.AbstractStoveBlockEntity;

/**
 * Catch-up handler for {@code AbstractStoveBlockEntity} (and its subclasses,
 * including {@code StoveBlockEntity}).
 *
 * <h2>Strategy — advance progress, let vanilla complete</h2>
 * <p>Mirrors EverFurnace core's {@code CampfireCatchupHandler}. Rather than
 * re-implementing recipe assembly, item-entity spawning, slot clearing, and the
 * client block-update + game-event that completion requires, this handler simply
 * pushes each slot's {@code cookingProgress} up to its {@code cookingTime} cap.
 * FD's own {@code cookAndOutputItems} — which runs immediately after our HEAD
 * inject in the same {@code serverTick} — then performs completion with all the
 * correct side effects (including {@code sendBlockUpdated}, so the client stops
 * rendering the now-consumed ingredient).
 *
 * <p>Doing the spawn/clear ourselves previously left the slot un-synced on the
 * client (cooked item popped out, raw item still visible on the stove); delegating
 * to vanilla avoids that entirely.
 *
 * <p>Output is inherently bounded to one item per slot — each slot holds a single
 * item and is never restocked mid-cook.
 *
 * <h2>Guards</h2>
 * <ul>
 *   <li>{@code shouldDropItems()} — top covered; vanilla drops items instead of
 *       cooking, so advancing progress would be wasted.</li>
 *   <li>{@code LIT} — if not lit, vanilla {@code coolItems} runs and would undo any
 *       progress we pushed; skip entirely.</li>
 * </ul>
 *
 * @author Mark Gottschling
 */
public class StoveCatchupHandler implements CookingCatchupHandler {

    @Override
    public void applyCatchup(BlockEntity blockEntity, long deltaTime, ServerLevel level, BlockPos pos) {

        AbstractStoveBlockEntity stove    = (AbstractStoveBlockEntity) blockEntity;
        IStoveMixin              accessor = (IStoveMixin)(Object) blockEntity;

        // ── Pre-checks ────────────────────────────────────────────────────────

        // Top covered → vanilla drops items rather than cooking.
        if (stove.shouldDropItems()) return;

        // Not lit → vanilla cools (reduces progress); pushing progress would be undone.
        if (!stove.getBlockState().getValue(AbstractStoveBlock.LIT)) return;

        // ── Advance each slot; vanilla's cookAndOutputItems completes them ─────

        ItemStackHandler items           = stove.getItems();
        int[]            cookingProgress = accessor.getCookingProgress();
        int[]            cookingTime     = accessor.getCookingTime();

        boolean anyCompleted = false;

        for (int i = 0; i < items.getSlots(); i++) {
            if (items.getStackInSlot(i).isEmpty()) continue;

            int total = cookingTime[i];
            if (total <= 0) continue;

            int remaining = total - cookingProgress[i];
            if (deltaTime >= remaining) {
                // Push to the cap; vanilla's ++/threshold check completes the craft this tick.
                cookingProgress[i] = total;
                anyCompleted = true;
            } else {
                cookingProgress[i] += (int) deltaTime;
            }
        }

        // Player-facing cue only when something actually finished this pass.
        if (anyCompleted) {
            CatchupFeedback.notify(level, pos);
        }
    }
}
