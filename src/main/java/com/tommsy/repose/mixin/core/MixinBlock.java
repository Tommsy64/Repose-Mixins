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
package com.tommsy.repose.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.tommsy.repose.IBlockStateRepose;
import com.tommsy.repose.Repose;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Mixin(Block.class)
public class MixinBlock {
    @Inject(method = "onBlockAdded", at = @At(value = "HEAD"), cancellable = true)
    public void onBlockAdded(World world, BlockPos pos, IBlockState state, CallbackInfo ci) {
        if (((IBlockStateRepose) state).canFallFrom(pos, world)) {
            world.scheduleUpdate(pos, (Block) (Object) this, Repose.BLOCK_FALL_DELAY);
            ci.cancel();
        }
        // Else, default implementation
    }

    @Inject(method = "onBlockPlacedBy", at = @At(value = "HEAD"), cancellable = true)
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state_, EntityLivingBase placer, ItemStack stack, CallbackInfo ci) {
        IBlockStateRepose state = (IBlockStateRepose) state_;
        if (state.canSpreadFrom(pos, world)) {
            state.spreadFrom(pos, world);
            ci.cancel();
        }
        // Else, default implementation
    }

    @Inject(method = "onPlayerDestroy", at = @At(value = "TAIL"))
    public void onPlayerDestroy(World world, BlockPos pos, IBlockState state, CallbackInfo ci) {
        Repose.triggerNeighborSpread(pos, world);
    }
}
