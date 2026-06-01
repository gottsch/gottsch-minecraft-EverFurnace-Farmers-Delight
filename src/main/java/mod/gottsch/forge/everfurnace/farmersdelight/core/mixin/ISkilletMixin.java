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

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import vectorwing.farmersdelight.common.block.entity.SkilletBlockEntity;

/**
 * Mixin interface providing accessor/invoker access to private cooking members
 * on {@code SkilletBlockEntity}.
 *
 * <p>Usage — cast any {@code SkilletBlockEntity} instance to this interface:
 * <pre>{@code
 *   ISkilletMixin accessor = (ISkilletMixin)(Object) skilletEntity;
 *   int total = accessor.getCookingTimeTotal();
 * }</pre>
 *
 * @author Mark Gottschling
 */
@Mixin(SkilletBlockEntity.class)
public interface ISkilletMixin {

    @Accessor("cookingTime")
    int getCookingTime();

    @Accessor("cookingTime")
    void setCookingTime(int cookingTime);

    @Accessor("cookingTimeTotal")
    int getCookingTimeTotal();

    /**
     * Vanilla's per-tick completion step: increments {@code cookingTime} and, when it
     * reaches {@code cookingTimeTotal}, assembles + spawns one result, resets
     * {@code cookingTime} to 0, and extracts one item from the slot (which syncs the
     * client via {@code inventoryChanged}).
     */
    @Invoker("cookAndOutputItems")
    void callCookAndOutputItems(ItemStack cookingStack, Level level);
}
