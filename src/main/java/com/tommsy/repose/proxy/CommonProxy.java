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
package com.tommsy.repose.proxy;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.tommsy.repose.Repose;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.oredict.OreDictionary;

public class CommonProxy {

    public final Set<Block> granularBlocks = new HashSet<>(), naturalStoneBlocks = new HashSet<>();
    public final Set<Block> soilBlocks = new HashSet<>(), sedimentaryBlocks = new HashSet<>();

    public void preInit(FMLPreInitializationEvent event) {}

    public void init(FMLInitializationEvent event) {}

    public void postInit(FMLPostInitializationEvent event) {

        Block.REGISTRY.spliterator().forEachRemaining((block) -> {
            Material material = getBlockMaterial(block);

            if (material.blocksMovement() && isHarvestedBy(block, "shovel")) {
                granularBlocks.add(block);
                if (isSoil(material))
                    soilBlocks.add(block);
                if (isSediment(material))
                    sedimentaryBlocks.add(block);
            }
        });

        Stream.of(OreDictionary.getOreNames()).filter(s -> s.startsWith("ore") || s.equals("stone") || s.equals("sandstone"))
                .map(name -> OreDictionary.getOres(name, false))
                .flatMap(l -> l.stream()).map(i -> Block.getBlockFromItem(i.getItem()))
                .collect(Collectors.toCollection(() -> naturalStoneBlocks));

        naturalStoneBlocks.add(Blocks.HARDENED_CLAY);
        naturalStoneBlocks.add(Blocks.STAINED_HARDENED_CLAY);
        naturalStoneBlocks.add(Blocks.NETHERRACK);
        naturalStoneBlocks.add(Blocks.END_STONE);

        Repose.getLogger().info(granularBlocks.stream().map(b -> b.getRegistryName().toString()).collect(Collectors.joining(", ", "Granular Blocks: [", "]")));
        Repose.getLogger().info(soilBlocks.stream().map(b -> b.getRegistryName().toString()).collect(Collectors.joining(", ", "Soil Blocks: [", "]")));
        Repose.getLogger().info(sedimentaryBlocks.stream().map(b -> b.getRegistryName().toString()).collect(Collectors.joining(", ", "Sedimentary Blocks: [", "]")));
    }

    private boolean isHarvestedBy(Block block, String tool) {
        String harvestTool = block.getHarvestTool(block.getDefaultState());
        return harvestTool != null && harvestTool.equals(tool);
    }

    private boolean isSoil(Material material) {
        return Material.GROUND.equals(material) || Material.GRASS.equals(material);
    }

    private boolean isSediment(Material material) {
        return Material.SAND.equals(material) || Material.CLAY.equals(material);
    }

    @SuppressWarnings("deprecation")
    private Material getBlockMaterial(Block block) {
        return block.getMaterial(block.getDefaultState());
    }
}
