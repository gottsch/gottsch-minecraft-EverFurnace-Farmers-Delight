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

import net.minecraftforge.items.wrapper.RecipeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;

import java.util.Optional;

/**
 * Mixin interface providing accessor and invoker access to private fields and
 * methods of {@code CookingPotBlockEntity}.
 *
 * <p>Usage — cast any {@code CookingPotBlockEntity} instance to this interface:
 * <pre>{@code
 *   ICookingPotMixin accessor = (ICookingPotMixin)(Object) cookingPot;
 *   int progress = accessor.getCookTime();
 * }</pre>
 *
 * @author Mark Gottschling
 */
@Mixin(CookingPotBlockEntity.class)
public interface ICookingPotMixin {

    // ---- Fields -------------------------------------------------------------

    @Accessor("cookTime")
    int getCookTime();

    @Accessor("cookTime")
    void setCookTime(int cookTime);

    /**
     * Forces a full recipe search on the next {@code getMatchingRecipe} call.
     * Set to {@code true} before each catch-up recipe lookup to ensure stale
     * cached recipe IDs do not mask ingredient changes.
     */
    @Accessor("checkNewRecipe")
    void setCheckNewRecipe(boolean value);

    // ---- Methods ------------------------------------------------------------

    @Invoker("hasInput")
    boolean callHasInput();

    @Invoker("getMatchingRecipe")
    Optional<CookingPotRecipe> callGetMatchingRecipe(RecipeWrapper inventoryWrapper);

    @Invoker("canCook")
    boolean callCanCook(CookingPotRecipe recipe);

    /**
     * Invokes {@code processCooking}. Before calling this, set {@code cookTime}
     * to {@code recipe.getCookTime() - 1} so the internal {@code ++cookTime}
     * lands exactly on the total and the cycle completes.
     */
    @Invoker("processCooking")
    boolean callProcessCooking(CookingPotRecipe recipe, CookingPotBlockEntity cookingPot);
}
