package com.evandev.reliable_replacer.logic;

import com.evandev.reliable_replacer.Constants;
import com.evandev.reliable_replacer.api.IProcessedChunk;
import com.evandev.reliable_replacer.api.IReplacementContext;
import com.evandev.reliable_replacer.config.ModConfig;
import com.evandev.reliable_replacer.data.AdditionalBlock;
import com.evandev.reliable_replacer.data.ReplacementResult;
import com.evandev.reliable_replacer.data.ReplacementRule;
import com.evandev.reliable_replacer.mixin.minecraft.ChunkMapAccessor;
import com.evandev.reliable_replacer.platform.Services;
import com.evandev.reliable_replacer.systems.RetrogenHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class RuleManager {
    public static final ThreadLocal<ResourceLocation> ACTIVE_FEATURE_BIOME = new ThreadLocal<>();
    public static final ThreadLocal<List<BlockPos>> LIVE_PLACEMENT_QUEUE = new ThreadLocal<>();
    private static final Gson GSON = new GsonBuilder().setLenient().setPrettyPrinting().create();
    private static final Map<Block, Map<String, Property<?>>> PROPERTY_CACHE = new ConcurrentHashMap<>();
    public static volatile boolean HAS_LIVE_RULES = false;
    public static volatile boolean HAS_AIR_RULES = false;
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
        boolean anyAirRules = false;

        for (ReplacementRule rule : loadedRules) {
            rule.resolveBlocks();

            for (Block b : rule.getInputBlocks()) {
                blockMap.computeIfAbsent(b, k -> new ArrayList<>()).add(rule);

                if (rule.shouldRunPlayerBlocks()) {
                    anyLiveRules = true;
                }

                // Check if this block is air
                if (b.defaultBlockState().isAir()) {
                    anyAirRules = true;
                }
            }
        }

        RULES_BY_BLOCK = blockMap;
        HAS_LIVE_RULES = anyLiveRules;
        HAS_AIR_RULES = anyAirRules;

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

    @Nullable
    public static ReplacementResult getReplacementResult(BlockState original, IReplacementContext ctx, boolean isLivePlacement) {
        if (!ModConfig.get().enabled || RULES_BY_BLOCK.isEmpty() || original == null)
            return null;

        List<ReplacementRule> candidates = RULES_BY_BLOCK.get(original.getBlock());
        if (candidates == null) {
            return null;
        }

        for (ReplacementRule rule : candidates) {
            if (ctx.isRetrogen() && !rule.shouldRunRetrogen()) continue;
            if (isLivePlacement && !rule.shouldRunPlayerBlocks()) continue;

            if (!RuleEvaluator.checkRule(rule, original, ctx)) continue;
            if (rule.not != null && RuleEvaluator.checkRule(rule.not, original, ctx)) continue;

            BlockState replacement = createReplacementState(original, rule, ctx.getPos());

            Map<BlockPos, BlockState> additionalBlocksMap = new HashMap<>();
            Map<BlockPos, CompoundTag> additionalNbtMap = new HashMap<>();
            if (rule.additionalBlocks != null && !rule.additionalBlocks.isEmpty()) {
                BlockPos pos = ctx.getPos();
                long seed = pos.getX() * 3129871L ^ pos.getY() * 116129781L ^ pos.getZ() * 3812423L;
                Random rand = new Random(seed);
                for (AdditionalBlock addBlock : rule.additionalBlocks) {
                    BlockPos offsetPos = ctx.getPos().offset(addBlock.xOffset, addBlock.yOffset, addBlock.zOffset);
                    BlockState addState = createAdditionalReplacementState(original, addBlock, rand);
                    if (addState != null) {
                        additionalBlocksMap.put(offsetPos, addState);
                        if (addBlock.getParsedOutputNbt() != null) {
                            additionalNbtMap.put(offsetPos, addBlock.getParsedOutputNbt());
                        }
                    }
                }
            }

            if (replacement.equals(original) && additionalBlocksMap.isEmpty() && rule.parsedOutputNbt == null && (rule.itemReplacements == null || rule.itemReplacements.isEmpty())) {
                return null;
            }

            return new ReplacementResult(replacement, rule.keepNbt, rule.parsedOutputNbt, rule.itemReplacements, additionalBlocksMap, additionalNbtMap);
        }

        return null;
    }

    private static BlockState createReplacementState(BlockState original, ReplacementRule rule, BlockPos pos) {
        List<Block> outputBlocks = rule.getOutputBlocks();

        if (outputBlocks == null || outputBlocks.isEmpty()) {
            BlockState newState = original;

            if (rule.outputStateProperties != null && !rule.outputStateProperties.isEmpty()) {
                for (Map.Entry<String, String> entry : rule.outputStateProperties.entrySet()) {
                    Property<?> prop = newState.getBlock().getStateDefinition().getProperty(entry.getKey());
                    if (prop != null) {
                        newState = setPropertyFromString(newState, prop, entry.getValue());
                    }
                }
            }

            if (rule.randomizeProperties != null && !rule.randomizeProperties.isEmpty()) {
                Random rand = new Random(pos.asLong());
                for (String propName : rule.randomizeProperties) {
                    Property<?> prop = newState.getBlock().getStateDefinition().getProperty(propName);
                    if (prop != null) {
                        newState = randomizeProperty(newState, prop, rand);
                    }
                }
            }
            return newState;
        }

        boolean needsRandom = outputBlocks.size() > 1 || (rule.randomizeProperties != null && !rule.randomizeProperties.isEmpty());
        Random rand = needsRandom ? new Random(pos.asLong()) : null;
        Block outputBlock = outputBlocks.size() == 1 ? outputBlocks.get(0) : outputBlocks.get(rand.nextInt(outputBlocks.size()));

        BlockState newState = outputBlock.defaultBlockState();

        if (rule.keepStates) {
            Map<String, Property<?>> targetProperties = PROPERTY_CACHE.computeIfAbsent(outputBlock, block -> {
                Map<String, Property<?>> map = new HashMap<>();
                for (Property<?> prop : block.defaultBlockState().getProperties()) {
                    map.put(prop.getName(), prop);
                }
                return map;
            });

            for (Property<?> prop : original.getProperties()) {
                Property<?> targetProp = targetProperties.get(prop.getName());
                if (targetProp != null) {
                    newState = copyProperty(original, newState, targetProp);
                }
            }
        }

        if (rule.outputStateProperties != null && !rule.outputStateProperties.isEmpty()) {
            for (Map.Entry<String, String> entry : rule.outputStateProperties.entrySet()) {
                Property<?> prop = outputBlock.getStateDefinition().getProperty(entry.getKey());
                if (prop != null) {
                    newState = setPropertyFromString(newState, prop, entry.getValue());
                }
            }
        }

        if (rule.randomizeProperties != null && !rule.randomizeProperties.isEmpty()) {
            for (String propName : rule.randomizeProperties) {
                Property<?> prop = outputBlock.getStateDefinition().getProperty(propName);
                if (prop != null) {
                    newState = randomizeProperty(newState, prop, rand);
                }
            }
        }

        return newState;
    }

    private static BlockState createAdditionalReplacementState(BlockState original, AdditionalBlock addBlock, Random rand) {
        List<Block> outputBlocks = addBlock.getOutputBlocks();
        Block outputBlock;

        if (outputBlocks == null || outputBlocks.isEmpty()) {
            outputBlock = original.getBlock();
        } else {
            outputBlock = outputBlocks.get(rand.nextInt(outputBlocks.size()));
        }

        BlockState newState = outputBlock.defaultBlockState();

        if (addBlock.outputStateProperties != null && !addBlock.outputStateProperties.isEmpty()) {
            for (Map.Entry<String, String> entry : addBlock.outputStateProperties.entrySet()) {
                Property<?> prop = outputBlock.getStateDefinition().getProperty(entry.getKey());
                if (prop != null) {
                    newState = setPropertyFromString(newState, prop, entry.getValue());
                }
            }
        }

        if (addBlock.randomizeProperties != null && !addBlock.randomizeProperties.isEmpty()) {
            for (String propName : addBlock.randomizeProperties) {
                Property<?> prop = outputBlock.getStateDefinition().getProperty(propName);
                if (prop != null) {
                    newState = randomizeProperty(newState, prop, rand);
                }
            }
        }

        return newState;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to, Property<T> targetProperty) {
        for (Property<?> originalProp : from.getProperties()) {
            if (originalProp.getName().equals(targetProperty.getName())) {
                return copyPropertyTyped(from, to, originalProp, targetProperty);
            }
        }
        return to;
    }

    private static <O extends Comparable<O>, T extends Comparable<T>> BlockState copyPropertyTyped(BlockState from, BlockState to, Property<O> originalProp, Property<T> targetProperty) {
        try {
            O originalValue = from.getValue(originalProp);
            String valueString = originalProp.getName(originalValue);
            Optional<T> parsedValue = targetProperty.getValue(valueString);

            if (parsedValue.isPresent()) {
                return to.setValue(targetProperty, parsedValue.get());
            }
        } catch (Exception ignored) {
        }
        return to;
    }

    private static <T extends Comparable<T>> BlockState setPropertyFromString(BlockState state, Property<T> property, String valueStr) {
        Optional<T> val = property.getValue(valueStr);
        return val.map(t -> state.setValue(property, t)).orElse(state);
    }

    private static <T extends Comparable<T>> BlockState randomizeProperty(BlockState state, Property<T> property, Random rand) {
        List<T> values = new ArrayList<>(property.getPossibleValues());
        if (values.isEmpty()) return state;
        T randomValue = values.get(rand.nextInt(values.size()));
        return state.setValue(property, randomValue);
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
                \s
                     "_comment_logic": "ADVANCED LOGIC: How the replacement behaves.",
                     "keep_states": true,
                     "retrogen": true,
                     "player_blocks": true,
                     "keep_nbt": true,
                     "probability": 0.5,
                \s
                    "_comment_input_nbt": "INPUT NBT: Only replace if the block entity has this NBT data (e.g., target blaze spawners)",
                    "input_nbt": "{SpawnData:{entity:{id:\\"minecraft:blaze\\"}}}",
                \s
                     "_comment_output_nbt": "NBT DATA: Add custom NBT to the output block",
                     "output_nbt": "{SpawnData:{entity:{id:\\"minecraft:zombie\\"}}}",
                \s
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
                     ],
                \s
                     "_comment_states": "STATE FILTERS: Only replace if input has these properties",
                     "state_properties": {
                         "half": "upper",
                         "axis": "y"
                     },
                    \s
                     "_comment_output_states": "OUTPUT STATES: Force the output to have these properties",
                     "output_state_properties": {
                         "axis": "y"
                     },
                    \s
                     "_comment_multiblock": "MULTI-BLOCK: Place secondary blocks (like the top half of a door or clearing the top half of tall grass)",
                     "additional_blocks": [
                       {
                         "output": "minecraft:mossy_cobblestone",
                         "x_offset": 0,
                         "y_offset": 1,
                         "z_offset": 0,
                         "output_state_properties": {
                             "half": "upper"
                         }
                       }
                     ],
                    \s
                     "_comment_random": "RANDOMIZATION: Randomly rotate the output block",
                     "randomize_properties": [
                         "facing",
                         "rotation"
                     ],
                \s
                     "_comment_conditions": "NEIGHBORS: Only replace if surroundings match",
                     "neighbors": {
                         "up": "minecraft:air",
                         "down": "minecraft:grass_block"
                     },
                \s
                     "_comment_coords": "COORDINATES: Supports absolute numbers or worldspawn relative values.",
                     "min_y": "60",
                     "max_y": "100",
                     "min_x": "spawn-500",
                     "max_x": "spawn+500",
                     "min_z": "spawn-500",
                     "max_z": "spawn+500",
                \s
                     "_comment_exclusion": "EXCLUSIONS: If the 'not' block matches, the rule is SKIPPED.",
                     "not": {
                       "biomes": ["minecraft:river"]
                     }
                   }
                 ]
                \s""";

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
}