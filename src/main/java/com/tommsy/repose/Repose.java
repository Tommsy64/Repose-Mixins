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

import javax.annotation.Nullable;

import org.apache.logging.log4j.Logger;

import com.tommsy.repose.config.ReposeConfig;
import com.tommsy.repose.mixin.core.accessor.ChunkAccessor;
import com.tommsy.repose.proxy.ClientProxy;
import com.tommsy.repose.proxy.CommonProxy;

import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod(modid = Repose.MOD_ID, name = Repose.MOD_NAME, version = Repose.VERSION)
public class Repose {
    public static final String MOD_ID = "@MOD_ID@";
    public static final String MOD_NAME = "@MOD_NAME@";
    public static final String VERSION = "@VERSION@";

    @Getter
    private static Logger logger;

    @Instance
    @Getter
    private static Repose instance;

    @SidedProxy(serverSide = "com.tommsy.@MOD_ID@.proxy.ServerProxy", clientSide = "com.tommsy.@MOD_ID@.proxy.ClientProxy")
    public static CommonProxy proxy;

    /**
     * Utility field so that casting to {@linkplain ClientProxy} isn't needed every time.
     */
    @SideOnly(Side.CLIENT)
    public static ClientProxy clientProxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        proxy.preInit(event);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    public static final int BLOCK_FALL_DELAY = 2;

    public static boolean canEntityUseSlope(Entity entity) {
        if (entity instanceof EntityPlayer) {
            return ReposeConfig.sneakingOnSlopes || !((EntityPlayer) entity).isSneaking();
        } else if (entity instanceof EntityCreature) {
            return ((EntityCreature) entity).getEyeHeight() > 0.5f;
        }
        return false;
    }

    public static boolean canStepHigh(IBlockState state, BlockPos pos, double stepHeight, boolean submerged, World world) {
        IBlockState neighbor = world.getBlockState(pos);
        return (!ReposeConfig.slopingShores && !submerged && neighbor.getMaterial().isLiquid()) ||
                (neighbor.getMaterial().blocksMovement() && getBlockHeight(neighbor, pos, world) >= stepHeight);
    }

    public static double getBlockHeight(IBlockState state, BlockPos pos, World world) {
        AxisAlignedBB collisionBB = state.getCollisionBoundingBox(world, pos);
        return collisionBB == null ? 0 : collisionBB.maxY;
    }

    public static boolean doesBlockSlope(IBlockState state) {
        switch (ReposeConfig.slopingMode) {
        case 1:
            return proxy.granularBlocks.contains(state.getBlock());
        case 2:
            return proxy.naturalStoneBlocks.contains(state.getBlock());
        case 3:
            return proxy.granularBlocks.contains(state.getBlock()) && proxy.naturalStoneBlocks.contains(state.getBlock());
        }
        return false;
    }

    public static boolean isSoil(Block block) {
        return proxy.soilBlocks.contains(block);
    }

    public static boolean blocksFallInstantlyAt(BlockPos pos, World world) {
        return BlockFalling.fallInstantly || !world.isAreaLoaded(pos.add(-32, -32, -32), pos.add(32, 32, 32));
    }

    public static boolean isServerDelayed(World world) {
        return MinecraftServer.getCurrentTimeMillis() - world.getMinecraftServer().currentTime > 2000L;
    }

    public static void spawnFallingBlock(IBlockState state, BlockPos pos, BlockPos posOrigin, @Nullable TileEntity tileEntity, World world) {
        EntityFallingBlock entityFallingBlock = new EntityFallingBlock(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, state);
        entityFallingBlock.prevPosX = posOrigin.getX() + 0.5;
        entityFallingBlock.prevPosY = posOrigin.getY();
        entityFallingBlock.prevPosZ = posOrigin.getZ() + 0.5;
        NBTTagCompound entityData = entityFallingBlock.getEntityData();
        for (String key : entityData.getKeySet())
            entityData.removeTag(key);

        if (tileEntity != null)
            tileEntity.writeToNBT(entityData);

        world.spawnEntity(entityFallingBlock);
    }

    public static boolean isOccupiedByFallingBlock(BlockPos pos, World world) {
        ClassInheritanceMultiMap<Entity>[] entityLists = world.getChunk(pos).getEntityLists();
        final AxisAlignedBB fullBlockBox = Block.FULL_BLOCK_AABB.offset(pos);

        int indexBelow = MathHelper.clamp(0, MathHelper.floor((fullBlockBox.minY - 1) / 16.0), entityLists.length - 1);
        for (EntityFallingBlock fallingBlock : entityLists[indexBelow].getByClass(EntityFallingBlock.class)) {
            if (fallingBlock.getEntityBoundingBox().intersects(fullBlockBox))
                return true;
        }
        // Probably optimizable by using modulo to compute indexAbove
        int indexAbove = MathHelper.clamp(0, MathHelper.floor((fullBlockBox.minY + 1) / 16.0), entityLists.length - 1);
        if (indexAbove != indexBelow)
            for (EntityFallingBlock fallingBlock : entityLists[indexAbove].getByClass(EntityFallingBlock.class)) {
                if (fallingBlock.getEntityBoundingBox().intersects(fullBlockBox))
                    return true;
            }

        return false;
    }

    public static boolean canDisplace(IBlockState state) {
        return !state.getMaterial().blocksMovement();
    }

    public static boolean shouldDropAsItem(IBlockState state, BlockPos pos, World world) {
        return ReposeConfig.breakOnPartialBlocks &&
                (!Repose.canDisplace(state) || // ex.: landing on a slab
                        Repose.canDisplace(world.getBlockState(pos.down())) || // ex.: landing on a ladder
                        ((IBlockStateRepose) state).hasSolidTop(pos, world)); // ex. landing IN a ladder (falling instantly)
    }

    /**
     * Used when doing instant-fall.
     */
    public static void onLanding(IBlockStateRepose reposeState, BlockPos collisionPos, IBlockState state, @Nullable NBTTagCompound entityTags, World world) {
        Block block = state.getBlock();
        BlockPos pos = ReposeConfig.breakOnPartialBlocks || Repose.canDisplace(world.getBlockState(collisionPos)) ? collisionPos : collisionPos.up();
        IBlockState stateHere = world.getBlockState(pos);

        if (shouldDropAsItem(stateHere, pos, world)) {
            block.dropBlockAsItem(world, pos, state, 0);
        } else {
            if (!world.isAirBlock(pos))
                stateHere.getBlock().dropBlockAsItem(world, pos, world.getBlockState(pos), 0);

            world.setBlockState(pos, state);
            if (block instanceof BlockFalling) {
                BlockFalling blockFalling = (BlockFalling) block;
                blockFalling.onEndFalling(world, pos, state, stateHere);
            }

            if (entityTags != null && block.hasTileEntity(state))
                copyTileEntityTags(pos, entityTags, world);

            if (!Repose.isServerDelayed(world) && reposeState.canSpreadFrom(pos, world))
                reposeState.spreadFrom(pos, world);

        }
        SoundType sound = block.getSoundType(state, world, pos, null);
        world.playSound(null, pos, sound.getBreakSound(), SoundCategory.BLOCKS, sound.getVolume(), sound.getPitch());
    }

    public static void copyTileEntityTags(BlockPos pos, NBTTagCompound tagCompound, World world) {
        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity != null) {
            NBTTagCompound newTagCompound = new NBTTagCompound();
            tileEntity.writeToNBT(newTagCompound);

            for (String key : tagCompound.getKeySet())
                if (key != "x" && key != "y" && key != "z")
                    newTagCompound.setTag(key, tagCompound.getTag(key));

            tileEntity.readFromNBT(newTagCompound);
            tileEntity.markDirty();
        }
    }

    public static void triggerNeighborSpread(BlockPos pos, World world) {
        // Prevent beach destruction
        if (!(ChunkAccessor.getPopulating() != null || world.isRemote || world.getBlockState(pos).getMaterial().isLiquid())) {
            BlockPos.PooledMutableBlockPos posMut = BlockPos.PooledMutableBlockPos.retain();
            try {
                // Cardinal Directions
                // x, z
                // 0, -1
                // 0, 1
                // -1, 0
                // 1, 0

                // Neighbor spread 1 block above the one broken
                final int pX = pos.getX(), pY = pos.getY() + 1, pZ = pos.getZ(); // Stored to reduce method calls
                int negativeMultiplier = -1;
                for (int i = 0; i < 4; i++) {
                    posMut.setPos(pX + (i >> 1) * negativeMultiplier, pY, pZ + (~(i >> 1) & 1) * negativeMultiplier);
                    negativeMultiplier *= -1;
                    IBlockStateRepose stateRepose = (IBlockStateRepose) world.getBlockState(posMut);
                    if (stateRepose.canSpreadInAvalanche(world) && !Repose.isOccupiedByFallingBlock(posMut, world) && stateRepose.canSpreadFrom(posMut, world))
                        stateRepose.spreadFrom(posMut, world);
                }
            } finally {
                posMut.release();
            }
        }
    }
}
