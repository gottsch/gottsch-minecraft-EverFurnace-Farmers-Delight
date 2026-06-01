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
package mod.gottsch.neoforge.everfurnace.farmersdelight.core.catchup;

import mod.gottsch.neoforge.everfurnace.api.CookingCatchupHandler;
import mod.gottsch.neoforge.everfurnace.farmersdelight.EverFurnaceFD;
import mod.gottsch.neoforge.everfurnace.farmersdelight.core.mixin.ICookingPotMixin;
import mod.gottsch.neoforge.everfurnace.farmersdelight.core.notify.CatchupFeedback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;

import java.util.Optional;

/**
 * Catch-up handler for {@code CookingPotBlockEntity}.
 *
 * <h2>Strategy — iterative simulation (capped)</h2>
 * <p>Simulates the pot's cooking one cycle at a time for the full {@code deltaTime} window
 * (capped at {@code maxCatchupTicks}). This is necessary because:
 * <ul>
 *   <li>Each cycle consumes ingredients, so the matching recipe may change.</li>
 *   <li>FD's {@code processCooking} ejects ingredient remainders as item entities —
 *       re-using vanilla's method keeps all side effects correct.</li>
 *   <li>XP is tracked via {@code setRecipeUsed} inside {@code processCooking} and
 *       awarded when the player opens the pot — no extra plumbing needed.</li>
 * </ul>
 *
 * <h2>Cycle completion trick</h2>
 * <p>Setting {@code cookTime = recipe.getCookTime() - 1} before invoking
 * {@code processCooking} ensures the internal {@code ++cookTime} lands exactly
 * on the total, triggering vanilla's completion path and then resetting to 0.
 *
 * <h2>Recipe re-check (1.21.1 note)</h2>
 * <p>FD 1.21.1 removed the {@code checkNewRecipe} flag. The {@code CachedCheck}
 * used by {@code getMatchingRecipe} re-evaluates against the current inventory state
 * on each call, so no explicit cache-busting is needed between cycles.
 *
 * @author Mark Gottschling
 */
public class CookingPotCatchupHandler implements CookingCatchupHandler {

    @Override
    public void applyCatchup(BlockEntity blockEntity, long deltaTime, ServerLevel level, BlockPos pos) {

        CookingPotBlockEntity cookingPot = (CookingPotBlockEntity) blockEntity;
        ICookingPotMixin      accessor   = (ICookingPotMixin)(Object) blockEntity;

        // ── Pre-checks ────────────────────────────────────────────────────────

        if (!cookingPot.isHeated()) return;
        if (!accessor.callHasInput()) return;

        Optional<RecipeHolder<CookingPotRecipe>> recipeHolderOpt =
                accessor.callGetMatchingRecipe(new RecipeWrapper(cookingPot.getInventory()));
        if (recipeHolderOpt.isEmpty()) return;

        RecipeHolder<CookingPotRecipe> recipeHolder = recipeHolderOpt.get();
        if (!accessor.callCanCook(recipeHolder.value())) return;

        int cookTimeTotal = recipeHolder.value().getCookTime();
        if (cookTimeTotal <= 0) return;

        // ── Simulate elapsed time ─────────────────────────────────────────────

        int  cookTime           = accessor.getCookTime();
        long ticksToFinishFirst = Math.max(0L, cookTimeTotal - cookTime);
        boolean didCook         = false;

        if (deltaTime < ticksToFinishFirst) {
            // Not enough time to finish even the current item — advance progress only.
            accessor.setCookTime(cookTime + (int) deltaTime);

        } else {
            // Complete at least one full cycle.
            deltaTime -= ticksToFinishFirst;

            // Set cookTime to total-1 so processCooking's ++cookTime hits exactly
            // total, triggering the completion path and resetting to 0.
            accessor.setCookTime(cookTimeTotal - 1);
            if (accessor.callProcessCooking(recipeHolder, cookingPot)) {
                didCook = true;
            }

            // Keep completing cycles while deltaTime allows.
            while (deltaTime > 0) {
                // Re-check recipe after each cycle — ingredients were consumed.
                recipeHolderOpt = accessor.callGetMatchingRecipe(new RecipeWrapper(cookingPot.getInventory()));
                if (recipeHolderOpt.isEmpty()) break;

                recipeHolder = recipeHolderOpt.get();
                if (!accessor.callCanCook(recipeHolder.value())) break;

                cookTimeTotal = recipeHolder.value().getCookTime();
                if (cookTimeTotal <= 0) break;

                if (deltaTime < cookTimeTotal) {
                    // Partial progress on the next item — don't complete it.
                    accessor.setCookTime((int) deltaTime);
                    break;
                }

                deltaTime -= cookTimeTotal;
                accessor.setCookTime(cookTimeTotal - 1);
                if (accessor.callProcessCooking(recipeHolder, cookingPot)) {
                    didCook = true;
                }
            }
        }

        // ── Post-catch-up bookkeeping ─────────────────────────────────────────

        if (didCook) {
            cookingPot.setChanged();
            CatchupFeedback.notify(level, pos);
            EverFurnaceFD.LOGGER.debug("EverFurnace FD: cooking pot at {} finished catch-up", pos);
        }
    }
}
