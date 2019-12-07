/*
 * Sponge-Compatible Repose using Mixins
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
package com.tommsy.repose.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.tommsy.repose.IBlockStateRepose;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @Shadow
    public World world;
    @Shadow
    public boolean noClip;
    @Shadow
    public double posX, posY, posZ;
    @Shadow
    public double prevPosX, prevPosY, prevPosZ;
    @Shadow
    public double motionY;
    @Shadow
    public float width;
    @Shadow
    public boolean onGround;

    @Shadow
    public abstract float getEyeHeight();

    @Shadow
    public abstract void setDead();

    @Shadow
    public abstract void move(MoverType type, double x, double y, double z);

    @Shadow
    public abstract AxisAlignedBB getEntityBoundingBox();

    @Shadow
    public abstract NBTTagCompound getEntityData();

    // There isn't an easy way to mix this in without an overwrite
    @Overwrite
    public boolean isEntityInsideOpaqueBlock() {
        if (this.noClip)
            return false;

        BlockPos.PooledMutableBlockPos pos = BlockPos.PooledMutableBlockPos.retain();
        for (int i = 0; i < 8; ++i) {
            int y = MathHelper.floor(this.posY + (double) (((float) ((i >> 0) % 2) - 0.5F) * 0.1F) + (double) this.getEyeHeight());
            int x = MathHelper.floor(this.posX + (double) (((float) ((i >> 1) % 2) - 0.5F) * this.width * 0.8F));
            int z = MathHelper.floor(this.posZ + (double) (((float) ((i >> 2) % 2) - 0.5F) * this.width * 0.8F));

            if (pos.getX() != x || pos.getY() != y || pos.getZ() != z) {
                pos.setPos(x, y, z);

                IBlockState blockState = this.world.getBlockState(pos);
                if (blockState.causesSuffocation() && !((IBlockStateRepose) blockState).canSlope(pos, world)) {
                    pos.release();
                    return true;
                }
            }
        }

        pos.release();
        return false;
    }
}
