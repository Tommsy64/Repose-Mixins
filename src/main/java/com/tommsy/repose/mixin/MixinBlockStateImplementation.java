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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.tommsy.repose.IBlockStateRepose;
import com.tommsy.repose.Repose;
import com.tommsy.repose.config.ReposeConfig;
import com.tommsy.repose.mixin.accessor.ChunkAccessor;

import net.minecraft.block.Block;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Mixin(BlockStateContainer.StateImplementation.class)
public abstract class MixinBlockStateImplementation implements IBlockState, IBlockStateRepose {

    @Shadow
    @Final
    private Block block;

    @Inject(method = "addCollisionBoxToList", at = @At(value = "HEAD"), cancellable = true)
    public void addCollisionBoxToList(World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity, boolean isActualState,
            CallbackInfo ci) {
        AxisAlignedBB collisionBB = getCollisionBoundingBox(world, pos);
        // Optimization
        if (collisionBB == Block.NULL_AABB) {
            ci.cancel();
            return;
        }

        // If Entity can use slope, block is configured to be sloped, collision box isn't too tall, and there is no collision box above it, apply our collision box calculation
        if (Repose.canEntityUseSlope(entity) && canSlope(collisionBB, pos, world)) {
            final double blockHeight = Repose.getBlockHeight(this, pos, world);
            final double stepHeight = blockHeight - 0.5;
            final boolean submerged = world.getBlockState(pos.up()).getMaterial().isLiquid();

            BlockPos.PooledMutableBlockPos posMut = BlockPos.PooledMutableBlockPos.retain();
            try {
                // Ordinal Directions
                // x, z
                // 1, -1
                // -1, -1
                // 1, 1
                // -1, 1
                final int pX = pos.getX(), pY = pos.getY(), pZ = pos.getZ(); // Stored to reduce method calls
                for (int z = -1; z <= 1; z += 2) {
                    for (int x = 1; x >= -1; x -= 2) {
                        double boundingBoxHeight;
                        if (Repose.canStepHigh(this, posMut.setPos(pX + x, pY, pZ), stepHeight, submerged, world) &&
                                Repose.canStepHigh(this, posMut.setPos(pX, pY, pZ + z), stepHeight, submerged, world) &&
                                Repose.canStepHigh(this, posMut.setPos(pX + x, pY, pZ + z), stepHeight, submerged, world))
                            boundingBoxHeight = blockHeight;
                        else
                            boundingBoxHeight = stepHeight;

                        AxisAlignedBB stepBox = new AxisAlignedBB(pX + Math.max(0, x * 0.5), pY, pZ + Math.max(0, z * 0.5),
                                pX + Math.max(0.5, x), pY + boundingBoxHeight, pZ + Math.max(0.5, z));

                        if (entityBox.intersects(stepBox))
                            collidingBoxes.add(stepBox);
                    }
                }
            } finally {
                posMut.release();
            }
            ci.cancel();
            return;
        }
        // Else, fall back to default implementation (i.e., don't cancel the method)
    }

    @Inject(method = "getCollisionBoundingBox", at = @At(value = "TAIL"), cancellable = true)
    public void getCollisionBoundingBox(CallbackInfoReturnable<AxisAlignedBB> ci) {
        AxisAlignedBB bb = ci.getReturnValue();
        // Minecraft's snow_layer with data 0 makes a 0-thickness box that still blocks sideways movement
        if (bb == null || bb.maxY == 0)
            ci.setReturnValue(null);
    }

    @Inject(method = "neighborChanged", at = @At(value = "HEAD"), cancellable = true)
    public void neighborChanged(World world, BlockPos pos, Block formerNeighbor, BlockPos neighborPos, CallbackInfo ci) {
        if (canFallFrom(pos, world)) {
            world.scheduleUpdate(pos, this.getBlock(), Repose.BLOCK_FALL_DELAY);
            ci.cancel();
        }
        // Else, fall back to default implementation
    }

    /**
     * @param collisionBB Equal to {@code getCollisionBoundingBox(world, pos);}
     */
    @Unique
    private boolean canSlope(AxisAlignedBB collisionBB, BlockPos pos, World world) {
        return Repose.doesBlockSlope(this) && collisionBB.maxY > 0.5 && world.getBlockState(pos.up()).getCollisionBoundingBox(world, pos.up()) == null;
    }

    @Override
    public boolean canSlope(BlockPos pos, World world) {
        AxisAlignedBB collisionBB = this.getCollisionBoundingBox(world, pos);
        return collisionBB == null ? false : canSlope(collisionBB, pos, world);
    }

    @Unique
    public boolean canFall(World world) {
        return ChunkAccessor.getPopulating() == null && !world.isRemote && ReposeConfig.granularBlocksFall && Repose.proxy.granularBlocks.contains(this.block);
    }

    @Override
    @Unique
    public boolean hasSolidTop(BlockPos pos, World world) {
        AxisAlignedBB topBox = new AxisAlignedBB(0, 0.99, 0, 1, 1, 1).offset(pos);
        ArrayList<AxisAlignedBB> intersectingBoxes = new ArrayList<>(1);
        addCollisionBoxToList(world, pos, topBox, intersectingBoxes, null, false);
        return !intersectingBoxes.isEmpty();
    }

    @Override
    @Unique
    public boolean canFallFrom(BlockPos pos, World world) {
        return canFall(world) && world.isBlockLoaded(pos.down()) && canFallThrough(pos.down(), world);
    }

    @Unique
    private boolean canFallThrough(BlockPos pos, World world) {
        IBlockState state = world.getBlockState(pos);
        if (ReposeConfig.breakOnPartialBlocks)
            return Repose.canDisplace(state) || !hasSolidTop(pos, world);
        return Repose.canDisplace(state) && !hasSolidTop(pos, world);
    }

    @Unique
    private boolean canSpread(World world) {
        return canFall(world) && ReposeConfig.blockSpread;
    }

    @Override
    @Unique
    public boolean canSpreadFrom(BlockPos pos, World world) {
        return canSpread(world) && world.isBlockLoaded(pos) && !canFallThrough(pos.down(), world);
    }

    @Unique
    private boolean canSpreadThrough(BlockPos pos, World world) {
        return Repose.canDisplace(world.getBlockState(pos)) && canFallThrough(pos.down(), world) && !Repose.isOccupiedByFallingBlock(pos, world);
    }

    @Override
    @Unique
    public void spreadFrom(BlockPos startPos, World world) {
        // TODO: Move this out of IBlockState impl...

        // Cardinal Directions
        // x, z
        // 0, -1
        // 0, 1
        // -1, 0
        // 1, 0

        int spreadablePosCount = 0;
        BlockPos[] spreadablePositions = new BlockPos[4];
        BlockPos pos;
        int negativeMultiplier = -1;
        for (int i = 0; i < 4; i++) {
            pos = startPos.add((i >> 1) * negativeMultiplier, 0, (~(i >> 1) & 1) * negativeMultiplier);
            negativeMultiplier *= -1;

            if (canSpreadThrough(pos, world))
                spreadablePositions[spreadablePosCount++] = pos;
        }

        if (spreadablePosCount > 0)
            fallFrom(spreadablePositions[world.rand.nextInt(spreadablePosCount)], startPos, world);
    }

    @Override
    @Unique
    public void fallFrom(BlockPos pos, BlockPos posOrigin, World world) {
        IBlockState origState = world.getBlockState(posOrigin);
        Block origBlock = origState.getBlock();

        // Hardcode?
        IBlockState state = origBlock == Blocks.GRASS || origBlock == Blocks.GRASS_PATH || origBlock == Blocks.FARMLAND ? Blocks.DIRT.getDefaultState() : origState;
        IBlockStateRepose stateRepose = (IBlockStateRepose) state;

        // Nullable
        TileEntity tileEntity = world.getTileEntity(posOrigin);
        if (!Repose.blocksFallInstantlyAt(pos, world) && !Repose.isServerDelayed(world)) {
            Repose.spawnFallingBlock(state, pos, posOrigin, tileEntity, world);
        } else {
            world.setBlockToAir(posOrigin);

            BlockPos.PooledMutableBlockPos posLanded = BlockPos.PooledMutableBlockPos.retain();
            try {
                // TODO: Replace 0 with getMinHeight to support cubic chunks.
                for (int y = pos.getY() - 1; y > 0; y--) {
                    posLanded.setPos(pos.getX(), y, pos.getZ());
                    if (!stateRepose.canFallFrom(posLanded, world)) {
                        NBTTagCompound tileEntityTags = null;
                        if (tileEntity != null)
                            tileEntity.writeToNBT(tileEntityTags);
                        Repose.onLanding(this, posLanded, state, tileEntityTags, world);
                        return;
                    }
                }

            } finally {
                posLanded.release();
            }
        }
    }

    @Override
    @Unique
    public boolean canSpreadInAvalanche(World world) {
        return ReposeConfig.avalanches && canSpread(world) && !Repose.isSoil(block);
    }
}
