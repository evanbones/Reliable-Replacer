package com.evandev.reliable_replacer.forge;

//? if forge {
/*import com.evandev.reliable_replacer.CommonClass;
import com.evandev.reliable_replacer.Constants;
import com.evandev.reliable_replacer.config.ModConfig;
import com.evandev.reliable_replacer.forge.client.ClientConfigSetup;
import com.evandev.reliable_replacer.logic.RuleManager;
import com.evandev.reliable_replacer.platform.Services;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Constants.MOD_ID)
public class ReliableReplacerMod {

    public ReliableReplacerMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        forgeBus.addListener(this::onTagsUpdated);
        forgeBus.addListener(this::onServerAboutToStart);
        forgeBus.addListener(this::onServerStopped);

        if (Services.PLATFORM.isPhysicalClient()) {
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
*///?}
