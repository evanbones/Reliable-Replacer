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

public class YACLIntegration {

    public static Screen createScreen(Screen parent) {
        ModConfig config = ModConfig.get();

        YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("config.reliable_replacer.title"))
                .save(ModConfig::save);

        ConfigCategory general = ConfigCategory.createBuilder()
                .name(Component.translatable("config.reliable_replacer.category.general"))
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("config.reliable_replacer.option.enabled"))
                        .description(OptionDescription.of(Component.translatable("config.reliable_replacer.option.enabled.tooltip")))
                        .binding(true, () -> config.enabled, val -> config.enabled = val)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("config.reliable_replacer.option.enable_retrogen"))
                        .description(OptionDescription.of(Component.translatable("config.reliable_replacer.option.enable_retrogen.tooltip")))
                        .binding(true, () -> config.enableRetrogen, val -> config.enableRetrogen = val)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                .option(ListOption.<String>createBuilder()
                        .name(Component.translatable("config.reliable_replacer.option.missing_id_map"))
                        .description(OptionDescription.of(Component.translatable("config.reliable_replacer.option.missing_id_map.tooltip")))
                        .binding(
                                new ArrayList<>(),
                                () -> {
                                    List<String> list = new ArrayList<>();
                                    if (config.missingIdMap != null) {
                                        config.missingIdMap.forEach((k, v) -> list.add(k + "=" + v));
                                    }
                                    return list;
                                },
                                list -> {
                                    Map<String, String> newMap = new HashMap<>();
                                    for (String entry : list) {
                                        String[] parts = entry.split("=");
                                        if (parts.length == 2) {
                                            newMap.put(parts[0].trim(), parts[1].trim());
                                        }
                                    }
                                    config.missingIdMap = newMap;
                                }
                        )
                        .controller(StringControllerBuilder::create)
                        .initial("")
                        .build())
                .build();

        return builder.category(general).build().generateScreen(parent);
    }
}
