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
package com.tommsy.repose.config;

import com.tommsy.repose.Repose;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.RangeInt;

@Config(modid = Repose.MOD_ID)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReposeConfig {
    @Comment("Whether or not sneaking players can go up and down slopes.")
    public static boolean sneakingOnSlopes = false;

    @Comment("Whether or not blocks slope at the edge of liquids.")
    public static boolean slopingShores = true;

    @Comment("Whether or not all granular blocks should fall rather than only just sand/gravel.")
    public static boolean granularBlocksFall = true;

    public static boolean breakOnPartialBlocks = true;

    @Comment("Whether or not granular blocks will spread out into piles when they fall.")
    public static boolean blockSpread = true;

    @Comment("Whether or not mining solid blocks can trigger avalanches on granular blocks.")
    public static boolean avalanches = true;

    @Comment("0: Slope no blocks\r\n1: Slope granular blocks\r\n2: Slope natural stone blocks\r\n3: Slope both")
    @RangeInt(min = 0, max = 3)
    public static int slopingMode = 1;
}
