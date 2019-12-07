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
import com.tommsy.repose.Repose;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

@Mixin(EntityFallingBlock.class)
public abstract class MixinEntityFallingBlock extends MixinEntity {

    @Shadow
    private IBlockState fallTile;

    @Shadow
    public int fallTime;

    // @Inject(method = "onUpdate", at = @At(value = "HEAD"), cancellable = true)
    @Overwrite
    public void onUpdate() {
        Block block = fallTile.getBlock();
        fallTime++;

        if (fallTime >= 1000 && !world.isRemote) {
            this.setDead();
            block.dropBlockAsItem(world, new BlockPos(MathHelper.floor(posX), MathHelper.floor(posY), MathHelper.floor(posZ)), fallTile, 0);
        }

        BlockPos posOrigin = new BlockPos(prevPosX, prevPosY, prevPosZ);

        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;

        motionY -= 0.04D;

        this.move(MoverType.SELF, 0, motionY, 0);

        if (!world.isRemote) {
            IBlockStateRepose state = (IBlockStateRepose) fallTile;

            if (fallTime == 1) {
                world.setBlockToAir(posOrigin);
                if (state.canSpreadInAvalanche(world))
                    Repose.triggerNeighborSpread(posOrigin.up(), world);
            }

            if (state.canSpreadInAvalanche(world) && !Repose.isServerDelayed(world)) {
                AxisAlignedBB box = this.getEntityBoundingBox();

                int yTopCurrent = MathHelper.floor(box.maxY);
                int yTopPrevious = MathHelper.floor(box.maxY - motionY);

                if (yTopCurrent < yTopPrevious)
                    Repose.triggerNeighborSpread(new BlockPos(MathHelper.floor(posX), yTopPrevious, MathHelper.floor(posZ)), world);
            }
            if (this.onGround) {
                this.setDead();
                Repose.onLanding(state, new BlockPos((Entity) (Object) this), fallTile, this.getEntityData(), world);
            }
        }
    }
}
