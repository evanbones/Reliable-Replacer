package com.evandev.reliable_replacer;

import com.evandev.reliable_replacer.client.ClientConfigSetup;
import com.evandev.reliable_replacer.logic.RuleManager;
import com.evandev.reliable_replacer.systems.ReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@Mod(Constants.MOD_ID)
public class ReliableReplacerMod {

    public ReliableReplacerMod(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);

        IEventBus forgeBus = NeoForge.EVENT_BUS;
        forgeBus.addListener(this::addReloadListener);
        forgeBus.addListener(this::onServerAboutToStart);
        forgeBus.addListener(this::onServerStopped);

        if (FMLEnvironment.dist.isClient()) {
            ClientConfigSetup.register(ModLoadingContext.get().getActiveContainer());
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(CommonClass::init);
    }

    private void onServerAboutToStart(ServerAboutToStartEvent event) {
        CommonClass.setServer(event.getServer());
        RuleManager.load(event.getServer());
    }

    private void onServerStopped(ServerStoppedEvent event) {
        CommonClass.setServer(null);
    }

    private void addReloadListener(final AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }
}