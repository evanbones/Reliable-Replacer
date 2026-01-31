package com.evandev.reliable_replacer.config;

import com.evandev.reliable_replacer.Constants;
import com.evandev.reliable_replacer.data.ReplacementRule;
import com.evandev.reliable_replacer.platform.Services;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class RuleManager {
    private static final Gson GSON = new GsonBuilder().setLenient().setPrettyPrinting().create();

    private static final Map<Block, List<ReplacementRule>> RULES_BY_BLOCK = new IdentityHashMap<>();
    private static final List<ReplacementRule> ALL_RULES = new ArrayList<>();

    public static void load() {
        RULES_BY_BLOCK.clear();
        ALL_RULES.clear();

        Path configDir = Services.PLATFORM.getConfigDirectory().resolve("reliable_replacer");

        if (!Files.exists(configDir)) {
            try {
                Files.createDirectories(configDir);
                generateDefaultConfig(configDir);
            } catch (Exception e) {
                Constants.LOG.error("Failed to create config directory", e);
            }
        }

        try (Stream<Path> paths = Files.walk(configDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(RuleManager::parseFile);
        } catch (Exception e) {
            Constants.LOG.error("Failed to load reliable replacer rules", e);
        }

        for (ReplacementRule rule : ALL_RULES) {
            for (String inputId : rule.inputs) {
                ResourceLocation rl = ResourceLocation.tryParse(inputId);
                if (rl != null && BuiltInRegistries.BLOCK.containsKey(rl)) {
                    Block block = BuiltInRegistries.BLOCK.get(rl);
                    RULES_BY_BLOCK.computeIfAbsent(block, k -> new ArrayList<>()).add(rule);
                }
            }
        }

        Constants.LOG.info("Loaded {} replacement rules.", ALL_RULES.size());
    }

    private static void parseFile(Path path) {
        try (FileReader fileReader = new FileReader(path.toFile())) {
            JsonElement json = JsonParser.parseReader(fileReader);
            if (json.isJsonArray()) {
                for (JsonElement e : json.getAsJsonArray()) {
                    ALL_RULES.add(GSON.fromJson(e, ReplacementRule.class));
                }
            } else if (json.isJsonObject()) {
                ALL_RULES.add(GSON.fromJson(json, ReplacementRule.class));
            }
        } catch (Exception e) {
            Constants.LOG.error("Error parsing rule file: {}", path, e);
        }
    }

    private static void generateDefaultConfig(Path configDir) {
        String example = """
                [
                  {
                    "inputs": ["minecraft:dirt", "minecraft:grass_block"],
                    "output": "minecraft:diamond_block",
                    "biomes": ["minecraft:plains"],
                    "keep_states": true,
                    "retrogen": true
                  }
                ]
                """;
        try {
            Files.writeString(configDir.resolve("example_replacements.json.disabled"), example);
        } catch (Exception ignored) {
        }
    }

    public static BlockState getReplacement(BlockState original, BlockPos pos, LevelAccessor level, boolean isRetrogen) {
        if (RULES_BY_BLOCK.isEmpty() || original == null || original.isAir()) return original;

        List<ReplacementRule> candidates = RULES_BY_BLOCK.get(original.getBlock());
        if (candidates == null || candidates.isEmpty()) {
            return original;
        }

        ResourceLocation biomeId = null;
        ResourceLocation dimId = null;

        for (ReplacementRule rule : candidates) {
            if (isRetrogen && !rule.retrogen) {
                continue;
            }

            if (!rule.dimensions.isEmpty()) {
                if (dimId == null && level != null) {
                    try {
                        dimId = level.dimensionType().effectsLocation();
                    } catch (Exception ignored) {
                    }
                }
                if (dimId == null || !rule.dimensions.contains(dimId.toString())) {
                    continue;
                }
            }

            if (!rule.biomes.isEmpty()) {
                if (biomeId == null && level != null) {
                    Holder<Biome> biomeHolder = level.getBiome(pos);
                    biomeId = biomeHolder.unwrapKey().map(ResourceKey::location).orElse(null);
                }
                if (biomeId == null || !rule.biomes.contains(biomeId.toString())) {
                    continue;
                }
            }

            return createReplacementState(original, rule);
        }

        return original;
    }

    private static BlockState createReplacementState(BlockState original, ReplacementRule rule) {
        BlockState newState = rule.getOutputBlock().defaultBlockState();

        if (rule.keepStates) {
            for (Property<?> prop : original.getProperties()) {
                if (newState.hasProperty(prop)) {
                    newState = copyProperty(original, newState, prop);
                }
            }
        }
        return newState;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to, Property<T> property) {
        return to.setValue(property, from.getValue(property));
    }
}