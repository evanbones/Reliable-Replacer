package com.evandev.reliable_replacer.config;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jetbrains.annotations.NotNull;

public class ReloadListener implements ResourceManagerReloadListener {
    @Override
    public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
        ModConfig.load();
        RuleManager.load();
    }
}