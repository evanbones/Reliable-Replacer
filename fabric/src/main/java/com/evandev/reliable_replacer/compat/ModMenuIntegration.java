package com.evandev.reliable_replacer.compat;

import com.evandev.reliable_replacer.client.integration.YACLIntegration;
import com.evandev.reliable_replacer.platform.Services;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (Services.PLATFORM.isModLoaded("yet_another_config_lib_v3")) {
            return YACLIntegration::createScreen;
        }
        return null;
    }
}