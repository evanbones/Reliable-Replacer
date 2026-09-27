package com.evandev.reliable_replacer.systems;

import com.evandev.reliable_replacer.CommonClass;
import com.evandev.reliable_replacer.config.ModConfig;
import com.evandev.reliable_replacer.logic.RuleManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jetbrains.annotations.NotNull;

public class ReloadListener implements ResourceManagerReloadListener {
    @Override
    public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
        ModConfig.load();
        RuleManager.load(CommonClass.getServer());
    }
}