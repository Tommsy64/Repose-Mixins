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
package com.tommsy.repose.mixin;

import javax.annotation.Nullable;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.tommsy.repose.IBlockStateRepose;
import com.tommsy.repose.Repose;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

@Mixin(EntityFallingBlock.class)
public abstract class MixinEntityFallingBlock extends MixinEntity {

    @Shadow
    private IBlockState fallTile;

    @Shadow
    public NBTTagCompound tileEntityData;

    @Unique
    private BlockPos prevBlockPos;

    @Inject(method = "onUpdate", at = @At(value = "HEAD"))
    public void setPrevBlockPos(CallbackInfo ci) {
        prevBlockPos = new BlockPos(this.prevPosX, this.prevPosY, this.prevPosZ);
    }

    @Redirect(method = "onUpdate", slice = @Slice(from = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/entity/item/EntityFallingBlock;fallTime:I", ordinal = 0)), at = @At(value = "NEW", target = "Lnet/minecraft/util/math/BlockPos;", ordinal = 0), allow = 1)
    public BlockPos useOriginBlockPos(Entity this$) {
        return prevBlockPos;
    }

    @Redirect(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockToAir(Lnet/minecraft/util/math/BlockPos;)Z"), allow = 1)
    public boolean onFallingStart(World world, BlockPos posOrigin) {
        boolean flag = world.setBlockToAir(posOrigin);
        if (!world.isRemote && ((IBlockStateRepose) fallTile).canSpreadInAvalanche(world))
            Repose.triggerNeighborSpread(posOrigin.up(), world);
        return flag;
    }

    /**
     * Returning false to drop item.
     */
    @Redirect(method = "onUpdate", slice = @Slice(from = @At(value = "INVOKE:LAST", target = "Lnet/minecraft/entity/item/EntityFallingBlock;setDead()V")), at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;mayPlace(Lnet/minecraft/block/Block;Lnet/minecraft/util/math/BlockPos;ZLnet/minecraft/util/EnumFacing;Lnet/minecraft/entity/Entity;)Z"), allow = 1)
    public boolean shouldDropItem(World world, Block block, BlockPos pos, boolean skipCollisionCheck, EnumFacing sidePlacedOn, @Nullable Entity placer) {
        IBlockStateRepose state = (IBlockStateRepose) this.world.getBlockState(pos);
        return !Repose.shouldDropAsItem(state, pos, world)
                && world.mayPlace(block, pos, skipCollisionCheck, sidePlacedOn, placer); // Default call
    }

    /**
     * We need to inject after the if statement handling the TileEntity, however there is no method there to use as an injection point. Therefore, disable that if statement and reimplement
     * it.
     *
     * @param this$
     * @return
     */
    @Redirect(method = "onUpdate", slice = @Slice(from = @At(value = "INVOKE:LAST", target = "Lnet/minecraft/entity/item/EntityFallingBlock;setDead()V")), at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/entity/item/EntityFallingBlock;tileEntityData:Lnet/minecraft/nbt/NBTTagCompound;", ordinal = 0), allow = 1)
    public NBTTagCompound disableTileEntityIfStatement(EntityFallingBlock this$) {
        if (tileEntityData != null && fallTile.getBlock().hasTileEntity(fallTile))
            Repose.copyTileEntityTags(new BlockPos(this$), tileEntityData, this.world); // Matches default behavior

        IBlockStateRepose reposeState = (IBlockStateRepose) fallTile;
        if (reposeState.canSpreadInAvalanche(world) && !Repose.isServerDelayed(world)) {
            AxisAlignedBB box = this.getEntityBoundingBox();

            int yTopCurrent = MathHelper.floor(box.maxY);
            int yTopPrevious = MathHelper.floor(box.maxY - motionY);

            if (yTopCurrent < yTopPrevious)
                Repose.triggerNeighborSpread(new BlockPos(MathHelper.floor(posX), yTopPrevious, MathHelper.floor(posZ)), world);
        }

        BlockPos pos = new BlockPos(this$);
        if (!Repose.isServerDelayed(world) && reposeState.canSpreadFrom(pos, world))
            reposeState.spreadFrom(pos, world);

        prevBlockPos = null;
        return null;
    }

}
