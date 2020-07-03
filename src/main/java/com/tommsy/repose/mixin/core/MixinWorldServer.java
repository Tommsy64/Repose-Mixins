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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

import com.tommsy.repose.DummyBlock;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.WorldServer;

@Mixin(value = WorldServer.class, priority = 1010)
public class MixinWorldServer extends MixinWorld {
    @Unique
    private final DummyBlock dummyBlock = new DummyBlock(this);

    @Redirect(method = "updateBlockTick", slice = @Slice(from = @At(value = "INVOKE:LAST", target = "Lnet/minecraft/block/Block;updateTick(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Ljava/util/Random;)V", shift = Shift.BY, by = -6)), at = @At(value = "INVOKE", target = "Lnet/minecraft/block/state/IBlockState;getBlock()Lnet/minecraft/block/Block;"), allow = 1)
    private Block returnDummyBlockUpdateBlockTick(IBlockState state) {
        dummyBlock.actualBlock = state.getBlock();
        return dummyBlock;
    }

    @Redirect(method = "tickUpdates", slice = @Slice(from = @At(value = "INVOKE:LAST", target = "Lnet/minecraft/block/Block;isEqualTo(Lnet/minecraft/block/Block;Lnet/minecraft/block/Block;)Z", shift = Shift.AFTER), to = @At(value = "INVOKE:LAST", target = "Lnet/minecraft/block/Block;updateTick(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Ljava/util/Random;)V")), at = @At(value = "INVOKE", target = "Lnet/minecraft/block/state/IBlockState;getBlock()Lnet/minecraft/block/Block;"), allow = 1)
    private Block returnDummyBlockTickUpdates(IBlockState state) {
        dummyBlock.actualBlock = state.getBlock();
        return dummyBlock;
    }
}
