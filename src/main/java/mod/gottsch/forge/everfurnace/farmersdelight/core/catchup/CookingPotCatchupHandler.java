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
import mod.gottsch.forge.everfurnace.farmersdelight.core.mixin.ICookingPotMixin;
import mod.gottsch.forge.everfurnace.farmersdelight.core.notify.CatchupFeedback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;

import java.util.Optional;

/**
 * Catch-up handler for {@code CookingPotBlockEntity}.
 *
 * <h2>Strategy — iterative simulation (Option B: capped)</h2>
 * <p>Rather than multiplying elapsed time by some rate, this handler simulates
 * the pot's cooking one cycle at a time for the full {@code deltaTime} window
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
 * <h2>Heat assumption</h2>
 * <p>The heat source (a stove, campfire, or other heated block directly below)
 * is a block in the same chunk column. It persists across unloads and does not
 * deplete, so "heated now → heated the whole interval" is sound.
 *
 * @author Mark Gottschling
 */
public class CookingPotCatchupHandler implements CookingCatchupHandler {

    @Override
    public void applyCatchup(BlockEntity blockEntity, long deltaTime, ServerLevel level, BlockPos pos) {

        CookingPotBlockEntity cookingPot = (CookingPotBlockEntity) blockEntity;
        ICookingPotMixin      accessor   = (ICookingPotMixin)(Object) blockEntity;

        // ── Pre-checks ────────────────────────────────────────────────────────

        // Heat source persists across unloads; if it's gone now it was probably
        // gone before, so skip.
        if (!cookingPot.isHeated()) return;
        if (!accessor.callHasInput()) return;

        // Force a fresh recipe search — cached lastRecipeID may be stale after
        // ingredients changed in a previous catch-up cycle.
        accessor.setCheckNewRecipe(true);
        Optional<CookingPotRecipe> recipeOpt =
                accessor.callGetMatchingRecipe(new RecipeWrapper(cookingPot.getInventory()));
        if (recipeOpt.isEmpty()) return;

        CookingPotRecipe recipe = recipeOpt.get();
        if (!accessor.callCanCook(recipe)) return;

        int cookTimeTotal = recipe.getCookTime();
        if (cookTimeTotal <= 0) return;

        // ── Simulate elapsed time ─────────────────────────────────────────────

        int  cookTime            = accessor.getCookTime();
        long ticksToFinishFirst  = Math.max(0L, cookTimeTotal - cookTime);
        boolean didCook          = false;

        if (deltaTime < ticksToFinishFirst) {
            // Not enough time to finish even the current item — advance progress only.
            accessor.setCookTime(cookTime + (int) deltaTime);

        } else {
            // Complete at least one full cycle.
            deltaTime -= ticksToFinishFirst;

            // Set cookTime to total-1 so processCooking's ++cookTime hits exactly
            // total, triggering the completion path and resetting to 0.
            accessor.setCookTime(cookTimeTotal - 1);
            if (accessor.callProcessCooking(recipe, cookingPot)) {
                didCook = true;
            }

            // Keep completing cycles while deltaTime allows.
            while (deltaTime > 0) {
                // Re-check recipe after each cycle — ingredients were consumed.
                accessor.setCheckNewRecipe(true);
                recipeOpt = accessor.callGetMatchingRecipe(new RecipeWrapper(cookingPot.getInventory()));
                if (recipeOpt.isEmpty()) break;

                recipe = recipeOpt.get();
                if (!accessor.callCanCook(recipe)) break;

                cookTimeTotal = recipe.getCookTime();
                if (cookTimeTotal <= 0) break;

                if (deltaTime < cookTimeTotal) {
                    // Partial progress on the next item — don't complete it.
                    accessor.setCookTime((int) deltaTime);
                    break;
                }

                deltaTime -= cookTimeTotal;
                accessor.setCookTime(cookTimeTotal - 1);
                if (accessor.callProcessCooking(recipe, cookingPot)) {
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
