package com.evandev.reliable_replacer.fabric;

//? if fabric {

import com.evandev.reliable_replacer.CommonClass;
import com.evandev.reliable_replacer.Constants;
import com.evandev.reliable_replacer.config.ModConfig;
import com.evandev.reliable_replacer.logic.RuleManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;
//? if <1.21.2 {
/*import net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys;
*///?} else {
import net.fabricmc.fabric.api.resource.v1.reloader.ResourceReloaderKeys;
//?}

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
        public Identifier getFabricId() {
            return Identifier.fromNamespaceAndPath(Constants.MOD_ID, "reload_listener");
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
        public Collection<Identifier> getFabricDependencies() {
            //? if <1.21.2 {
            /*return Collections.singletonList(ResourceReloadListenerKeys.TAGS);
            *///?} else {
            return Collections.singletonList(ResourceReloaderKeys.AFTER_VANILLA);
            //?}
        }
    }
}
//?}
