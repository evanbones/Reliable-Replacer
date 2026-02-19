package com.evandev.reliable_replacer;

import com.evandev.reliable_replacer.config.ModConfig;
import com.evandev.reliable_replacer.logic.RuleManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

public class ReliableReplacerMod implements ModInitializer {

    @Override
    public void onInitialize() {
        CommonClass.init();

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            CommonClass.setServer(server);
            ModConfig.load();
            RuleManager.load(server);
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> CommonClass.setServer(null));
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new FabricReloadListener());
    }

    private static class FabricReloadListener implements SimpleSynchronousResourceReloadListener {

        @Override
        public ResourceLocation getFabricId() {
            return new ResourceLocation("reliable_replacer", "reload_listener");
        }

        @Override
        public void onResourceManagerReload(@NotNull ResourceManager manager) {
            if (CommonClass.getServer() == null) {
                return;
            }

            ModConfig.load();
            RuleManager.load(CommonClass.getServer());
        }

        @Override
        public Collection<ResourceLocation> getFabricDependencies() {
            return Collections.singletonList(ResourceReloadListenerKeys.TAGS);
        }
    }
}