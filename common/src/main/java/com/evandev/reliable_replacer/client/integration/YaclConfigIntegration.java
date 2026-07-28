package com.evandev.reliable_replacer.client.integration;

import com.evandev.reliable_replacer.config.ModConfig;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YaclConfigIntegration {

    public static Screen createScreen(Screen parent) {
        ModConfig config = ModConfig.get();

        YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("config.reliable_replacer.title"))
                .save(ModConfig::save);

        ConfigCategory.Builder generalCategory = ConfigCategory.createBuilder()
                .name(Component.translatable("config.reliable_replacer.category.general"));

        generalCategory.option(Option.<Boolean>createBuilder()
                .name(Component.translatable("config.reliable_replacer.option.enabled"))
                .description(OptionDescription.of(Component.translatable("config.reliable_replacer.option.enabled.tooltip")))
                .binding(true, () -> config.enabled, val -> config.enabled = val)
                .controller(TickBoxControllerBuilder::create)
                .build());

        generalCategory.group(ListOption.<String>createBuilder()
                .name(Component.translatable("config.reliable_replacer.option.missing_id_map"))
                .description(OptionDescription.of(Component.translatable("config.reliable_replacer.option.missing_id_map.tooltip")))
                .controller(StringControllerBuilder::create)
                .initial("")
                .binding(
                        new ArrayList<>(),
                        () -> mapToList(config.missingIdMap),
                        list -> config.missingIdMap = listToMap(list)
                )
                .build());

        return builder
                .category(generalCategory.build())
                .build()
                .generateScreen(parent);
    }

    private static List<String> mapToList(Map<String, String> map) {
        List<String> list = new ArrayList<>();
        if (map != null) {
            map.forEach((k, v) -> list.add(k + "=" + v));
        }
        return list;
    }

    private static Map<String, String> listToMap(List<String> list) {
        Map<String, String> map = new HashMap<>();
        for (String entry : list) {
            String[] parts = entry.split("=");
            if (parts.length == 2) {
                map.put(parts[0].trim(), parts[1].trim());
            }
        }
        return map;
    }
}
