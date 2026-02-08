package com.evandev.reliable_replacer.config;

import com.evandev.reliable_replacer.Constants;
import com.evandev.reliable_replacer.data.ReplacementResult;
import com.evandev.reliable_replacer.data.ReplacementRule;
import com.evandev.reliable_replacer.mixin.minecraft.ChunkMapAccessor;
import com.evandev.reliable_replacer.platform.Services;
import com.evandev.reliable_replacer.util.ChunkRuleCache;
import com.evandev.reliable_replacer.util.IProcessedChunk;
import com.evandev.reliable_replacer.util.RetrogenHandler;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.LevelData;
import org.jetbrains.annotations.Nullable;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class RuleManager {
    private static final Gson GSON = new GsonBuilder().setLenient().setPrettyPrinting().create();
    private static final Map<Block, Map<Integer, Property<?>>> PROPERTY_CACHE = new ConcurrentHashMap<>();
    public static volatile boolean HAS_LIVE_RULES = false;
    public static volatile Map<Block, List<ReplacementRule>> RULES_BY_BLOCK = Collections.emptyMap();

    public static void load(MinecraftServer server) {
        List<ReplacementRule> loadedRules = new ArrayList<>();
        Path configDir = Services.PLATFORM.getConfigDirectory().resolve("reliable_replacer");

        if (!Files.exists(configDir)) {
            try {
                Files.createDirectories(configDir);
                createExampleFile(configDir);
                createSwapperFile(configDir);
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

        Map<Block, List<ReplacementRule>> blockMap = new IdentityHashMap<>();
        boolean anyLiveRules = false;

        for (ReplacementRule rule : loadedRules) {
            rule.resolveBlocks();

            if (rule.shouldRunPlayerBlocks()) {
                anyLiveRules = true;
            }

            for (Block b : rule.getInputBlocks()) {
                blockMap.computeIfAbsent(b, k -> new ArrayList<>()).add(rule);
            }
        }

        RULES_BY_BLOCK = blockMap;
        HAS_LIVE_RULES = anyLiveRules;

        Constants.LOG.info("Loaded {} replacement rules. Live replacement active: {}", RULES_BY_BLOCK.size(), HAS_LIVE_RULES);

        if (server != null) {
            for (ServerLevel level : server.getAllLevels()) {
                ChunkMapAccessor map = (ChunkMapAccessor) level.getChunkSource().chunkMap;
                for (ChunkHolder holder : map.reliableReplacer$getChunks()) {
                    LevelChunk chunk = holder.getTickingChunk();
                    if (chunk != null) {
                        ((IProcessedChunk) chunk).reliableReplacer$setDirty(true);
                        RetrogenHandler.processChunk(chunk);
                    }
                }
            }
        }
    }

    private static void parseFile(Path path, List<ReplacementRule> list) {
        try (FileReader fileReader = new FileReader(path.toFile())) {
            JsonElement json = JsonParser.parseReader(fileReader);
            if (json.isJsonArray()) {
                for (JsonElement e : json.getAsJsonArray()) {
                    list.add(GSON.fromJson(e, ReplacementRule.class));
                }
            } else if (json.isJsonObject()) {
                if (json.getAsJsonObject().has("swapper")) {
                    json.getAsJsonObject().getAsJsonObject("swapper").entrySet().forEach(entry -> {
                        ReplacementRule rule = new ReplacementRule();
                        rule.inputs.add(entry.getKey());
                        rule.output = entry.getValue().getAsString();
                        list.add(rule);
                    });
                } else {
                    list.add(GSON.fromJson(json, ReplacementRule.class));
                }
            }
        } catch (Exception e) {
            Constants.LOG.error("Error parsing rule file: {}", path, e);
        }
    }

    private static void createExampleFile(Path dir) {
        Path exampleFile = dir.resolve("example_rules.json.disabled");
        String content = """
                [
                  {
                    "_comment_description": "BASIC SETTINGS: What to replace and what to replace it with.",
                    "inputs": [
                      "minecraft:cobblestone",
                      "minecraft:stone_bricks"
                    ],
                    "output": "minecraft:mossy_cobblestone",
                
                    "_comment_logic": "ADVANCED LOGIC: How the replacement behaves.",
                    "keep_states": true,
                    "retrogen": true,
                    "player_blocks": true,
                    "keep_nbt": true,
                
                    "_comment_filters": "FILTERS: The rule only runs if ALL these match.",
                    "biomes": [
                      "minecraft:jungle",
                      "minecraft:sparse_jungle"
                    ],
                    "dimensions": [
                      "minecraft:overworld"
                    ],
                    "structures": [
                      "minecraft:jungle_pyramid"
                    ]
                
                    "_comment_coords": "COORDINATES: Supports absolute numbers or worldspawn relative values.",
                    "min_y": "60",
                    "max_y": "100",
                    "min_x": "spawn-500",
                    "max_x": "spawn+500",
                    "min_z": "spawn-500",
                    "max_z": "spawn+500",
                
                    "_comment_exclusion": "EXCLUSIONS: If the 'not' block matches, the rule is SKIPPED.",
                    "not": {
                      "biomes": ["minecraft:river"]
                    }
                  }
                ]
                """;

        try {
            Files.writeString(exampleFile, content, StandardOpenOption.CREATE);
        } catch (Exception e) {
            Constants.LOG.error("Failed to generate example rule file", e);
        }
    }

    private static void createSwapperFile(Path dir) {
        Path swapperFile = dir.resolve("swapper.json");
        String content = """
                {
                  "swapper": {
                    "examplemod:input_block": "examplemod:output_block"
                  }
                }
                """;

        try {
            Files.writeString(swapperFile, content, StandardOpenOption.CREATE);
        } catch (Exception e) {
            Constants.LOG.error("Failed to generate swapper rule file", e);
        }
    }

    @Nullable
    public static ReplacementResult getReplacementResult(BlockState original, BlockPos pos, LevelAccessor level, boolean isRetrogen, boolean isLivePlacement, ChunkRuleCache cache) {
        LevelData levelData = level.getLevelData();
        BlockPos spawnPos = new BlockPos(levelData.getSpawnPos());
        if (cache == null) {
            cache = new ChunkRuleCache(level, new ChunkPos(pos));
        }
        RuleContext ctx = new RuleContext(level, pos, spawnPos, isRetrogen, null);
        return getReplacementResult(original, ctx, cache, isLivePlacement);
    }

    @Nullable
    public static ReplacementResult getReplacementResult(BlockState original, RuleContext ctx, ChunkRuleCache cache, boolean isLivePlacement) {
        if (!ModConfig.get().enabled || RULES_BY_BLOCK.isEmpty() || original == null)
            return null;

        List<ReplacementRule> candidates = RULES_BY_BLOCK.get(original.getBlock());
        if (candidates == null) {
            return null;
        }

        for (int i = 0; i < candidates.size(); i++) {
            ReplacementRule rule = candidates.get(i);

            if (ctx.isRetrogen && !rule.shouldRunRetrogen()) continue;

            if (isLivePlacement && !rule.shouldRunPlayerBlocks()) continue;

            if (!checkRule(rule, ctx, cache)) continue;
            if (rule.not != null && checkRule(rule.not, ctx, cache)) continue;

            if (original.is(rule.getOutputBlock())) {
                return null;
            }

            return new ReplacementResult(createReplacementState(original, rule), rule.keepNbt);
        }

        return null;
    }

    private static boolean checkRule(ReplacementRule rule, RuleContext ctx, ChunkRuleCache cache) {
        // Coordinate Checks
        int x = ctx.pos.getX();
        int y = ctx.pos.getY();
        int z = ctx.pos.getZ();
        int sx = ctx.spawnPos.getX();
        int sy = ctx.spawnPos.getY();
        int sz = ctx.spawnPos.getZ();

        if (!checkRange(x, rule.cachedMinX, rule.cachedMinXOffset, rule.minX,
                rule.cachedMaxX, rule.cachedMaxXOffset, rule.maxX, sx)) return false;

        if (!checkRange(y, rule.cachedMinY, rule.cachedMinYOffset, rule.minY,
                rule.cachedMaxY, rule.cachedMaxYOffset, rule.maxY, sy)) return false;

        if (!checkRange(z, rule.cachedMinZ, rule.cachedMinZOffset, rule.minZ,
                rule.cachedMaxZ, rule.cachedMaxZOffset, rule.maxZ, sz)) return false;

        // Dimension Check
        if (!rule.parsedDimensions.isEmpty()) {
            ResourceLocation dimId = ctx.getDimId();
            if (dimId != null && !rule.parsedDimensions.contains(dimId)) return false;
        }

        // Biome Check
        if (!rule.parsedBiomes.isEmpty()) {
            ResourceLocation biomeId = ctx.getBiomeId();
            if (biomeId == null || !rule.parsedBiomes.contains(biomeId)) return false;
        }

        // Structure Check
        if (!rule.parsedStructures.isEmpty()) {
            if (cache != null) {
                return cache.isPositionInStructure(rule, ctx.pos);
            }

            if (ctx.level instanceof ServerLevel sl) {
                StructureManager structureManager = sl.structureManager();
                Registry<Structure> structRegistry = ctx.level.registryAccess().registryOrThrow(Registries.STRUCTURE);

                for (ResourceLocation rl : rule.parsedStructures) {
                    if (structRegistry.containsKey(rl)) {
                        Structure structure = structRegistry.get(rl);
                        if (structure != null && structureManager.getStructureAt(ctx.pos, structure).isValid()) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        return true;
    }

    private static boolean checkRange(int pos, Integer cachedMin, Integer cachedMinOffset, String minStr,
                                      Integer cachedMax, Integer cachedMaxOffset, String maxStr, int spawn) {
        if (cachedMin != null) {
            if (pos < cachedMin) return false;
        } else if (cachedMinOffset != null) {
            if (pos < spawn + cachedMinOffset) return false;
        } else if (minStr != null) {
            int min = parseCoordinate(minStr, spawn);
            if (pos < min) return false;
        }

        if (cachedMax != null) {
            return pos <= cachedMax;
        } else if (cachedMaxOffset != null) {
            return pos <= spawn + cachedMaxOffset;
        } else if (maxStr != null) {
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
            return Integer.MIN_VALUE;
        }
    }

    private static BlockState createReplacementState(BlockState original, ReplacementRule rule) {
        Block outputBlock = rule.getOutputBlock();
        BlockState newState = outputBlock.defaultBlockState();

        if (rule.keepStates) {
            Map<Integer, Property<?>> targetProperties = PROPERTY_CACHE.computeIfAbsent(outputBlock, block -> {
                Map<Integer, Property<?>> map = new java.util.HashMap<>();
                for (Property<?> prop : block.defaultBlockState().getProperties()) {
                    map.put(prop.generateHashCode(), prop);
                }
                return map;
            });

            for (Property<?> prop : original.getProperties()) {
                Property<?> targetProp = targetProperties.get(prop.generateHashCode());
                if (targetProp != null) {
                    newState = copyProperty(original, newState, targetProp);
                }
            }
        }
        return newState;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to, Property<T> property) {
        return to.setValue(property, from.getValue(property));
    }

    public static class RuleContext {
        public final LevelAccessor level;
        public final BlockPos spawnPos;
        public final boolean isRetrogen;
        @Nullable
        public final ChunkAccess chunk;

        public BlockPos pos;

        private ResourceLocation biomeId;
        private ResourceLocation dimId;
        private boolean computedDim = false;

        private int lastBiomeX = Integer.MIN_VALUE;
        private int lastBiomeY = Integer.MIN_VALUE;
        private int lastBiomeZ = Integer.MIN_VALUE;

        public RuleContext(LevelAccessor level, BlockPos pos, BlockPos spawnPos, boolean isRetrogen, @Nullable ChunkAccess chunk) {
            this.level = level;
            this.pos = pos;
            this.spawnPos = spawnPos;
            this.isRetrogen = isRetrogen;
            this.chunk = chunk;
        }

        public void set(BlockPos pos) {
            this.pos = pos;
        }

        ResourceLocation getBiomeId() {
            int qX = pos.getX() >> 2;
            int qY = pos.getY() >> 2;
            int qZ = pos.getZ() >> 2;

            if (biomeId == null || qX != lastBiomeX || qY != lastBiomeY || qZ != lastBiomeZ) {
                Holder<Biome> biomeHolder;
                if (chunk != null) {
                    biomeHolder = chunk.getNoiseBiome(qX, qY, qZ);
                } else {
                    biomeHolder = level.getBiome(pos);
                }

                biomeId = biomeHolder.unwrapKey().map(ResourceKey::location).orElse(null);
                lastBiomeX = qX;
                lastBiomeY = qY;
                lastBiomeZ = qZ;
            }
            return biomeId;
        }

        ResourceLocation getDimId() {
            if (!computedDim) {
                if (level instanceof ServerLevel sl) {
                    dimId = sl.dimension().location();
                } else if (level instanceof WorldGenRegion wgr) {
                    dimId = wgr.getLevel().dimension().location();
                }
                computedDim = true;
            }
            return dimId;
        }
    }
}