package com.evandev.reliable_replacer.forge.client;

//? if forge {
/*import com.evandev.reliable_replacer.client.integration.YACLIntegration;
import com.evandev.reliable_replacer.platform.Services;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModContainer;

public class ClientConfigSetup {
    public static void register(ModContainer container) {
        if (Services.PLATFORM.isModLoaded("yet_another_config_lib_v3")) {
            container.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> YACLIntegration.createScreen(parent)));
        }
    }
}
*///?}
