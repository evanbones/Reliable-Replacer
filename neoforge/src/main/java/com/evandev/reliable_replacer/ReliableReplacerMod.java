package com.evandev.reliable_replacer;

import com.evandev.reliable_replacer.client.ClientConfigSetup;
import com.evandev.reliable_replacer.config.ModConfig;
import com.evandev.reliable_replacer.logic.RuleManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@Mod(Constants.MOD_ID)
public class ReliableReplacerMod {

    public ReliableReplacerMod(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);

        IEventBus neoBus = NeoForge.EVENT_BUS;
        neoBus.addListener(this::onTagsUpdated);
        neoBus.addListener(this::onServerAboutToStart);
        neoBus.addListener(this::onServerStopped);

        if (FMLEnvironment.getDist().isClient()) {
            ClientConfigSetup.register(ModLoadingContext.get().getActiveContainer());
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(CommonClass::init);
    }

    private void onServerAboutToStart(ServerAboutToStartEvent event) {
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