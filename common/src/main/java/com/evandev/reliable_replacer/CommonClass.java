package com.evandev.reliable_replacer;

import com.evandev.reliable_replacer.config.ModConfig;
import com.evandev.reliable_replacer.config.RuleManager;

public class CommonClass {
    public static void init() {
        ModConfig.load();
        RuleManager.load();
    }
}