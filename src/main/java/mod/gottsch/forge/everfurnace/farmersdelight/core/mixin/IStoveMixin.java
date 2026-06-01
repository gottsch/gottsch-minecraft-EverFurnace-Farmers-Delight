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

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import vectorwing.farmersdelight.common.block.entity.AbstractStoveBlockEntity;

/**
 * Mixin interface providing accessor access to private per-slot cooking arrays
 * on {@code AbstractStoveBlockEntity}.
 *
 * <p>Usage — cast any {@code AbstractStoveBlockEntity} instance to this interface:
 * <pre>{@code
 *   IStoveMixin accessor = (IStoveMixin)(Object) stoveEntity;
 *   int[] progress = accessor.getCookingProgress();
 * }</pre>
 *
 * <p>The returned arrays are the live backing arrays — modifications are
 * reflected in the block entity directly.
 *
 * @author Mark Gottschling
 */
@Mixin(AbstractStoveBlockEntity.class)
public interface IStoveMixin {

    @Accessor("cookingProgress")
    int[] getCookingProgress();

    @Accessor("cookingTime")
    int[] getCookingTime();
}
