package com.evandev.reliable_replacer.client.integration;

import com.evandev.reliable_replacer.config.ModConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClothConfigIntegration {

    public static Screen createScreen(Screen parent) {
        ModConfig config = ModConfig.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.reliable_replacer.title"));

        builder.setSavingRunnable(ModConfig::save);

        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("config.reliable_replacer.category.general"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_replacer.option.enabled"), config.enabled)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.reliable_replacer.option.enabled.tooltip"))
                .setSaveConsumer(newValue -> config.enabled = newValue)
                .build());

        List<String> currentMapAsList = new ArrayList<>();
        if (config.missingIdMap != null) {
            config.missingIdMap.forEach((k, v) -> currentMapAsList.add(k + "=" + v));
        }

        general.addEntry(entryBuilder.startStrList(Component.translatable("config.reliable_replacer.option.missing_id_map"), currentMapAsList)
                .setDefaultValue(new ArrayList<>())
                .setTooltip(Component.translatable("config.reliable_replacer.option.missing_id_map.tooltip"))
                .setSaveConsumer(list -> {
                    Map<String, String> newMap = new HashMap<>();
                    for (String entry : list) {
                        String[] parts = entry.split("=");
                        if (parts.length == 2) {
                            newMap.put(parts[0].trim(), parts[1].trim());
                        }
                    }
                    config.missingIdMap = newMap;
                })
                .build());

        return builder.build();
    }
}