package com.evandev.reliable_replacer.client;

import com.evandev.reliable_replacer.client.integration.YaclConfigIntegration;
import com.evandev.reliable_replacer.platform.Services;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public class ClientConfigSetup {
    public static void register(ModContainer container) {
        if (Services.PLATFORM.isModLoaded("yet_another_config_lib_v3")) {
            container.registerExtensionPoint(IConfigScreenFactory.class, (c, parent) -> YaclConfigIntegration.createScreen(parent));
        }
    }
}