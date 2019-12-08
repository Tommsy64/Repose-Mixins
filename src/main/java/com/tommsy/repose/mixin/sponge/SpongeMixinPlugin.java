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
package com.tommsy.repose.mixin.sponge;

import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class SpongeMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOG = LogManager.getLogger("Repose");

    private boolean spongePresent;

    @Override
    public void onLoad(String mixinPackage) {
        // The follow was copied from Phosphur by jellysquid3 https://github.com/jellysquid3/Phosphor/
        try {
            // This class will always be loaded by Forge prior to us (due to the tweak class ordering) and should have
            // no effect. On the off chance it isn't, early class loading shouldn't cause any issues as nobody seems to
            // transform core-mods themselves, or at least I hope they don't...
            Class.forName("org.spongepowered.mod.SpongeCoremod");

            this.spongePresent = true;
        } catch (Exception e) {
            this.spongePresent = false;
        }

        if (this.spongePresent) {
            LOG.info("Sponge has been detected on the classpath! Enabling Sponge specific patches.");
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return this.spongePresent;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}