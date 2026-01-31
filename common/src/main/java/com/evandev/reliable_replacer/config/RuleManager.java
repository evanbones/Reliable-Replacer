//
package com.evandev.reliable_replacer.config;

import com.evandev.reliable_replacer.Constants;
import com.evandev.reliable_replacer.data.ReplacementRule;
import com.evandev.reliable_replacer.platform.Services;
import com.evandev.reliable_replacer.util.FeatureContext;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.LevelData;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class RuleManager {
    private static final Gson GSON = new GsonBuilder().setLenient().setPrettyPrinting().create();

    private static volatile Map<Block, List<ReplacementRule>> RULES_BY_BLOCK = Collections.emptyMap();
    private static volatile List<ReplacementRule> ALL_RULES = Collections.emptyList();
    private static volatile List<ReplacementRule> FEATURE_CANCEL_RULES = Collections.emptyList();

    public static void load() {
        List<ReplacementRule> loadedRules = new ArrayList<>();

        Path configDir = Services.PLATFORM.getConfigDirectory().resolve("reliable_replacer");

        if (!Files.exists(configDir)) {
            try {
                Files.createDirectories(configDir);
            } catch (Exception e) {
                Constants.LOG.error("Failed to create config directory", e);
            }
        }

        try (Stream<Path> paths = Files.walk(configDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(p -> parseFile(p, loadedRules));
        } catch (Exception e) {
            Constants.LOG.error("Failed to load reliable replacer rules", e);
        }

        List<ReplacementRule> cancelRules = new ArrayList<>();
        Map<Block, List<ReplacementRule>> blockMap = new IdentityHashMap<>();

        for (ReplacementRule rule : loadedRules) {
            rule.resolveBlocks();

            if (rule.cancelFeature) {
                cancelRules.add(rule);
            }

            for (Block b : rule.getInputBlocks()) {
                blockMap.computeIfAbsent(b, k -> new ArrayList<>()).add(rule);
            }
        }

        ALL_RULES = loadedRules;
        FEATURE_CANCEL_RULES = cancelRules;
        RULES_BY_BLOCK = blockMap;

        Constants.LOG.info("Loaded {} replacement rules.", ALL_RULES.size());
    }

    private static void parseFile(Path path, List<ReplacementRule> list) {
        try (FileReader fileReader = new FileReader(path.toFile())) {
            JsonElement json = JsonParser.parseReader(fileReader);
            if (json.isJsonArray()) {
                for (JsonElement e : json.getAsJsonArray()) {
                    list.add(GSON.fromJson(e, ReplacementRule.class));
                }
            } else if (json.isJsonObject()) {
                list.add(GSON.fromJson(json, ReplacementRule.class));
            }
        } catch (Exception e) {
            Constants.LOG.error("Error parsing rule file: {}", path, e);
        }
    }

    public static boolean shouldCancelFeature(ResourceLocation featureId, LevelAccessor level) {
        if (FEATURE_CANCEL_RULES.isEmpty()) return false;

        for (ReplacementRule rule : FEATURE_CANCEL_RULES) {
            if (rule.features.contains(featureId.toString())) {
                return true;
            }
            for (String f : rule.features) {
                if (f.endsWith(":*") && featureId.getNamespace().equals(f.split(":")[0])) {
                    return true;
                }
            }
        }
        return false;
    }

    public static BlockState getReplacement(BlockState original, BlockPos pos, LevelAccessor level, boolean isRetrogen) {
        if (RULES_BY_BLOCK.isEmpty() || original == null || original.isAir()) return original;

        List<ReplacementRule> candidates = RULES_BY_BLOCK.get(original.getBlock());
        if (candidates == null) {
            return original;
        }

        ResourceLocation biomeId = null;
        ResourceLocation dimId = null;

        // Retrieve spawn position from LevelData since LevelAccessor doesn't have getSharedSpawnPos()
        LevelData levelData = level.getLevelData();
        BlockPos spawnPos = new BlockPos(levelData.getXSpawn(), levelData.getYSpawn(), levelData.getZSpawn());

        for (ReplacementRule rule : candidates) {
            if (isRetrogen && !rule.retrogen) continue;

            // Coordinate Checks
            if (!checkRange(pos.getX(), rule.minX, rule.maxX, spawnPos.getX())) continue;
            if (!checkRange(pos.getY(), rule.minY, rule.maxY, spawnPos.getY())) continue;
            if (!checkRange(pos.getZ(), rule.minZ, rule.maxZ, spawnPos.getZ())) continue;

            // Dimension Check
            if (!rule.dimensions.isEmpty()) {
                if (dimId == null && level instanceof ServerLevel sl) {
                    dimId = sl.dimension().location();
                }
                if (dimId != null && !rule.dimensions.contains(dimId.toString())) continue;
            }

            // Biome Check
            if (!rule.biomes.isEmpty()) {
                if (biomeId == null) {
                    Holder<Biome> biomeHolder = level.getBiome(pos);
                    biomeId = biomeHolder.unwrapKey().map(ResourceKey::location).orElse(null);
                }
                if (biomeId == null || !rule.biomes.contains(biomeId.toString())) continue;
            }

            // Feature Context Check
            if (!rule.features.isEmpty()) {
                if (isRetrogen) continue;

                ResourceLocation currentFeature = FeatureContext.getCurrentFeature();
                if (currentFeature == null) continue;

                boolean match = rule.features.contains(currentFeature.toString());
                if (!match) {
                    for (String f : rule.features) {
                        if (f.endsWith(":*") && currentFeature.getNamespace().equals(f.split(":")[0])) {
                            match = true;
                            break;
                        }
                    }
                }
                if (!match) continue;
            }

            // Structure Check
            if (!rule.structures.isEmpty()) {
                if (level instanceof ServerLevel serverLevel) {
                    boolean inStructure = false;
                    Registry<Structure> structRegistry = serverLevel.registryAccess().registryOrThrow(Registries.STRUCTURE);

                    for (String structId : rule.structures) {
                        ResourceLocation rl = ResourceLocation.tryParse(structId);
                        if (rl != null && structRegistry.containsKey(rl)) {
                            Structure structure = structRegistry.get(rl);
                            if (structure != null && serverLevel.structureManager().getStructureAt(pos, structure).isValid()) {
                                inStructure = true;
                                break;
                            }
                        }
                    }
                    if (!inStructure) continue;
                } else {
                    continue;
                }
            }

            return createReplacementState(original, rule);
        }

        return original;
    }

    private static boolean checkRange(int pos, String minStr, String maxStr, int spawn) {
        if (minStr != null) {
            int min = parseCoordinate(minStr, spawn);
            if (pos < min) return false;
        }
        if (maxStr != null) {
            int max = parseCoordinate(maxStr, spawn);
            return pos <= max;
        }
        return true;
    }

    private static int parseCoordinate(String val, int spawn) {
        try {
            val = val.replace(" ", "");
            if (val.contains("spawn")) {
                String offsetStr = val.replace("spawn", "");
                int offset = 0;
                if (!offsetStr.isEmpty()) {
                    offset = Integer.parseInt(offsetStr);
                }
                return spawn + offset;
            }
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            Constants.LOG.error("Invalid coordinate value in rule: {}", val);
            return 0;
        }
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