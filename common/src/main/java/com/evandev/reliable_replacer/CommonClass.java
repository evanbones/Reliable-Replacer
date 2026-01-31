package com.evandev.reliable_replacer;

import com.evandev.reliable_replacer.config.ModConfig;
import com.evandev.reliable_replacer.config.RuleManager;
import net.minecraft.server.MinecraftServer;

public class CommonClass {
    private static MinecraftServer currentServer;

    public static MinecraftServer getServer() {
        return currentServer;
    }

    public static void setServer(MinecraftServer server) {
        currentServer = server;
    }

    public static void init() {
        ModConfig.load();
        RuleManager.load(null);
    }
}