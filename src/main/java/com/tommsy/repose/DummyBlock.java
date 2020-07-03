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

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * This is done to maintain Sponge compatibility.
 */
public final class DummyBlock extends Block {
    public Block actualBlock;
    private final ReposeWorld server;

    public DummyBlock(ReposeWorld server) {
        super(Material.AIR, null);
        this.server = server;
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        server.redirectUpdateTick(actualBlock, world, pos, state, rand);
        actualBlock = null;
    }
}