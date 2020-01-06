/*
 * Makes sand behave more realistically and allows walking up 1 block steps.
 * Copyright (C) 2019  Thomas Pakh
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see LICENSE.md at the root of the project.
 */
package com.tommsy.repose;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Mixed into the IBlockState implementation. This allows outside callers to the mixed-in methods in {@linkplain com.tommsy.repose.mixin.core.MixinBlockStateImplementation
 * MixinBlockStateImplementation}.
 */
public interface IBlockStateRepose extends IBlockState {
    public boolean canSlope(BlockPos pos, World world);

    public boolean canFallFrom(BlockPos pos, World world);

    public boolean canSpreadFrom(BlockPos pos, World world);

    public void spreadFrom(BlockPos pos, World world);

    public boolean canSpreadInAvalanche(World world);

    public void fallFrom(BlockPos pos, BlockPos posOrigin, World world);
}
