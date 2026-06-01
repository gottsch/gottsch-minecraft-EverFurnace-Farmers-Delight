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
package mod.gottsch.forge.everfurnace.farmersdelight.core.cooking;

import mod.gottsch.forge.everfurnace.farmersdelight.core.mixin.SkilletBlockEntityMixin;

/**
 * Plain interface exposing the {@code @Unique} timing field injected into
 * {@code SkilletBlockEntity} by {@link SkilletBlockEntityMixin}.
 *
 * <p>Always cast to this interface rather than referencing the mixin class
 * directly — mixin classes are dissolved at runtime and cannot be referenced
 * from external code.
 *
 * <pre>{@code
 *   IEverFurnaceFDSkillet skillet = (IEverFurnaceFDSkillet)(Object) skilletEntity;
 *   long last = skillet.everfurnacefd$getLastGameTime();
 * }</pre>
 *
 * @author Mark Gottschling
 */
public interface IEverFurnaceFDSkillet {

    long everfurnacefd$getLastGameTime();

    void everfurnacefd$setLastGameTime(long time);
}
