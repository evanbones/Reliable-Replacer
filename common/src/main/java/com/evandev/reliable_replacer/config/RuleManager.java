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
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
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

    public static Map<Block, List<ReplacementRule>> getRulesByBlock() {
        return RULES_BY_BLOCK;
    }

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

        LevelData levelData = level.getLevelData();
        BlockPos spawnPos = new BlockPos(levelData.getXSpawn(), levelData.getYSpawn(), levelData.getZSpawn());

        RuleContext ctx = new RuleContext(level, pos, spawnPos, isRetrogen);

        for (ReplacementRule rule : candidates) {
            if (isRetrogen && !rule.retrogen) continue;

            if (!checkRule(rule, ctx)) continue;
            if (rule.not != null && checkRule(rule.not, ctx)) continue;

            return createReplacementState(original, rule);
        }

        return original;
    }

    private static boolean checkRule(ReplacementRule rule, RuleContext ctx) {
        // Coordinate Checks
        if (!checkRange(ctx.pos.getX(), rule.minX, rule.maxX, ctx.spawnPos.getX())) return false;
        if (!checkRange(ctx.pos.getY(), rule.minY, rule.maxY, ctx.spawnPos.getY())) return false;
        if (!checkRange(ctx.pos.getZ(), rule.minZ, rule.maxZ, ctx.spawnPos.getZ())) return false;

        // Dimension Check
        if (!rule.dimensions.isEmpty()) {
            ResourceLocation dimId = ctx.getDimId();
            if (dimId != null && !rule.dimensions.contains(dimId.toString())) return false;
        }

        // Biome Check
        if (!rule.biomes.isEmpty()) {
            ResourceLocation biomeId = ctx.getBiomeId();
            if (biomeId == null || !rule.biomes.contains(biomeId.toString())) return false;
        }

        // Feature Check
        if (!rule.features.isEmpty()) {
            if (ctx.isRetrogen) return false;

            Registry<PlacedFeature> placedRegistry = ctx.level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
            Registry<ConfiguredFeature<?, ?>> configuredRegistry = ctx.level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);

            boolean anyMatch = FeatureContext.getFeatureStack().anyMatch(activeFeature -> {
                String activeStr = activeFeature.toString();
                if (rule.features.contains(activeStr)) return true;

                for (String f : rule.features) {
                    if (f.endsWith(":*") && activeFeature.getNamespace().equals(f.split(":")[0])) {
                        return true;
                    }

                    if (f.startsWith("#")) {
                        ResourceLocation tagId = ResourceLocation.tryParse(f.substring(1));
                        if (tagId != null) {
                            if (hasTag(placedRegistry, activeFeature, tagId)) return true;
                            if (hasTag(configuredRegistry, activeFeature, tagId)) return true;
                        }
                    }
                }
                return false;
            });

            if (!anyMatch) return false;
        }

        // Structure Check
        if (!rule.structures.isEmpty()) {
            if (ctx.level instanceof ServerLevel serverLevel) {
                boolean inStructure = false;
                Registry<Structure> structRegistry = serverLevel.registryAccess().registryOrThrow(Registries.STRUCTURE);

                for (String structId : rule.structures) {
                    ResourceLocation rl = ResourceLocation.tryParse(structId);
                    if (rl != null && structRegistry.containsKey(rl)) {
                        Structure structure = structRegistry.get(rl);
                        if (structure != null && serverLevel.structureManager().getStructureAt(ctx.pos, structure).isValid()) {
                            inStructure = true;
                            break;
                        }
                    }
                }
                return inStructure;
            } else {
                return false;
            }
        }

        return true;
    }

    private static <T> boolean hasTag(Registry<T> registry, ResourceLocation id, ResourceLocation tagId) {
        TagKey<T> key = TagKey.create(registry.key(), tagId);
        return registry.getHolder(ResourceKey.create(registry.key(), id))
                .map(holder -> holder.is(key))
                .orElse(false);
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

    private static class RuleContext {
        final LevelAccessor level;
        final BlockPos pos;
        final BlockPos spawnPos;
        final boolean isRetrogen;

        private ResourceLocation biomeId;
        private ResourceLocation dimId;
        private boolean computedBiome = false;
        private boolean computedDim = false;

        RuleContext(LevelAccessor level, BlockPos pos, BlockPos spawnPos, boolean isRetrogen) {
            this.level = level;
            this.pos = pos;
            this.spawnPos = spawnPos;
            this.isRetrogen = isRetrogen;
        }

        ResourceLocation getBiomeId() {
            if (!computedBiome) {
                Holder<Biome> biomeHolder = level.getBiome(pos);
                biomeId = biomeHolder.unwrapKey().map(ResourceKey::location).orElse(null);
                computedBiome = true;
            }
            return biomeId;
        }

        ResourceLocation getDimId() {
            if (!computedDim) {
                if (level instanceof ServerLevel sl) {
                    dimId = sl.dimension().location();
                }
                computedDim = true;
            }
            return dimId;
        }
    }
}