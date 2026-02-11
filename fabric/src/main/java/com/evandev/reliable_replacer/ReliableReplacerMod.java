package com.evandev.reliable_replacer;

import com.evandev.reliable_replacer.systems.ReloadListener;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

public class ReliableReplacerMod implements ModInitializer {

    @Override
    public void onInitialize() {
        CommonClass.init();

        ServerLifecycleEvents.SERVER_STARTING.register(CommonClass::setServer);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> CommonClass.setServer(null));

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new FabricReloadListener());
    }

    private static class FabricReloadListener extends ReloadListener implements IdentifiableResourceReloadListener {
        @Override
        public ResourceLocation getFabricId() {
            return new ResourceLocation("reliable_replacer", "reload_listener");
        }
    }
}