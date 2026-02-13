package com.evandev.reliable_replacer;

import com.evandev.reliable_replacer.client.ClientConfigSetup;
import com.evandev.reliable_replacer.config.ModConfig;
import com.evandev.reliable_replacer.logic.RuleManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(ReliableReplacerMod.MOD_ID)
public class ReliableReplacerMod {
    public static final String MOD_ID = "reliable_replacer";

    public ReliableReplacerMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.addListener(this::onTagsUpdated);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopped);
        if (FMLEnvironment.dist.isClient()) {
            ClientConfigSetup.register(ModLoadingContext.get().getActiveContainer());
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        CommonClass.init();
    }

    private void onServerStarting(ServerStartingEvent event) {
        CommonClass.setServer(event.getServer());
    }

    private void onServerStopped(ServerStoppedEvent event) {
        CommonClass.setServer(null);
    }

    private void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            ModConfig.load();
            RuleManager.load(CommonClass.getServer());
        }
    }
}