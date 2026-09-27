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
import com.evandev.reliable_replacer.util.CompatUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.jetbrains.annotations.Nullable;
//? if <26.3 {
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
//?} else {
/*import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
*///?}
//? if >=1.21.2 && <26.3 {
import net.minecraft.world.level.WorldGenLevel;
//?}

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class RuleManager {
    public static final ThreadLocal<Deque<ActiveFeatureContext>> ACTIVE_FEATURE_STACK = ThreadLocal.withInitial(ArrayDeque::new);
    public static final ThreadLocal<List<BlockPos>> LIVE_PLACEMENT_QUEUE = new ThreadLocal<>();
    private static final Gson GSON = new GsonBuilder().setLenient().setPrettyPrinting().create();
    private static final Map<Block, Map<String, Property<?>>> PROPERTY_CACHE = new ConcurrentHashMap<>();
    private static final Map<Object, Identifier> TRUNK_BLOCK_CACHE = new ConcurrentHashMap<>();
    //? if <26.3 {
    private static final ResourceKey<? extends Registry<?>> CONFIGURED_FEATURE_REGISTRY = Registries.CONFIGURED_FEATURE;
    private static final ResourceKey<? extends Registry<?>> FEATURE_TYPE_REGISTRY = Registries.FEATURE;
    //?} else {
    /*private static final ResourceKey<? extends Registry<?>> CONFIGURED_FEATURE_REGISTRY = Registries.FEATURE;
    private static final ResourceKey<? extends Registry<?>> FEATURE_TYPE_REGISTRY = Registries.FEATURE_TYPE;
    *///?}
    private static final Identifier NO_TRUNK_BLOCK = Identifier.withDefaultNamespace("__no_trunk__");
    public static volatile boolean HAS_LIVE_RULES = false;
    public static volatile boolean HAS_AIR_RULES = false;
    public static volatile boolean HAS_RETROGEN_RULES = false;
    public static volatile boolean HAS_FEATURE_RULES = false;
    public static volatile int RETROGEN_RULES_HASH = 0;
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
        boolean anyRetrogenRules = false;
        boolean anyFeatureRules = false;
        List<ReplacementRule> retrogenRules = new ArrayList<>();

        for (ReplacementRule rule : loadedRules) {
            rule.resolveBlocks();

            if (rule.shouldRunRetrogen()) {
                anyRetrogenRules = true;
                retrogenRules.add(rule);
            }

            if (rule.features != null && !rule.features.isEmpty()) {
                anyFeatureRules = true;
            }
            if (rule.not != null && rule.not.features != null && !rule.not.features.isEmpty()) {
                anyFeatureRules = true;
            }

            for (Block block : rule.getInputBlocks()) {
                blockMap.computeIfAbsent(block, k -> new ArrayList<>()).add(rule);

                if (rule.shouldRunPlayerBlocks()) {
                    anyLiveRules = true;
                }

                if (block.defaultBlockState().isAir()) {
                    anyAirRules = true;
                }
            }
        }

        RULES_BY_BLOCK = blockMap;
        HAS_LIVE_RULES = anyLiveRules;
        HAS_AIR_RULES = anyAirRules;
        HAS_RETROGEN_RULES = anyRetrogenRules;
        HAS_FEATURE_RULES = anyFeatureRules;
        RETROGEN_RULES_HASH = GSON.toJson(retrogenRules).hashCode();

        Constants.LOG.info("Loaded {} replacement rules. Live replacement active: {}, Retrogen active: {}", RULES_BY_BLOCK.size(), HAS_LIVE_RULES, HAS_RETROGEN_RULES && ModConfig.get().enableRetrogen);

        if (server != null && ModConfig.get().enableRetrogen && HAS_RETROGEN_RULES) {
            for (ServerLevel level : server.getAllLevels()) {
                ChunkMapAccessor map = (ChunkMapAccessor) level.getChunkSource().chunkMap;
                for (ChunkHolder holder : map.reliableReplacer$getVisibleChunkMap().values()) {
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

    //? if <26.3 {
    private static Identifier extractTrunkOrFoliageBlockId(FeatureConfiguration config, @Nullable LevelAccessor level) {
        if (!(config instanceof TreeConfiguration treeConfig)) return null;
    //?} else {
    /*private static Identifier extractTrunkOrFoliageBlockId(Feature feature, @Nullable LevelAccessor level) {
        if (!(feature instanceof TreeFeature treeFeature)) return null;
    *///?}
        //? if >=1.21.2 && <26.3 {
        if (!(level instanceof WorldGenLevel worldGenLevel)) return null;
        //?} else if >=26.3 {
        /*if (level == null) return null;
        *///?}

        //? if <26.3 {
        Identifier cached = TRUNK_BLOCK_CACHE.computeIfAbsent(config, cfg -> {
        //?} else {
        /*Identifier cached = TRUNK_BLOCK_CACHE.computeIfAbsent(feature, cfg -> {
        *///?}
            try {
                //? if <1.21.2 {
                /*BlockState state = treeConfig.trunkProvider.getState(RandomSource.create(42L), BlockPos.ZERO);
                *///?} else if <26.3 {
                BlockState state = treeConfig.trunkProvider.getState(worldGenLevel, RandomSource.create(42L), BlockPos.ZERO);
                //?} else {
                /*BlockState state = treeFeature.trunkProvider().value().getState(level, RandomSource.create(42L), BlockPos.ZERO);
                *///?}
                return BuiltInRegistries.BLOCK.getKey(state.getBlock());
            } catch (Exception ignored) {
                return NO_TRUNK_BLOCK;
            }
        });
        return cached == NO_TRUNK_BLOCK ? null : cached;
    }

    public static void pushActivePlacedFeature(PlacedFeature placedFeature, @Nullable LevelAccessor level, @Nullable BlockPos origin) {
        if (!HAS_FEATURE_RULES || placedFeature == null) return;
        RegistryAccess registryAccess = level != null ? level.registryAccess() : null;

        Identifier placedFeatureId = null;
        Holder<PlacedFeature> placedFeatureHolder = null;

        if (registryAccess != null) {
            Optional<Registry<PlacedFeature>> placedFeatureRegistryOpt = CompatUtil.registry(registryAccess, Registries.PLACED_FEATURE);
            if (placedFeatureRegistryOpt.isPresent()) {
                Registry<PlacedFeature> placedFeatureRegistry = placedFeatureRegistryOpt.get();
                placedFeatureId = placedFeatureRegistry.getResourceKey(placedFeature).map(CompatUtil::keyId).orElseGet(() -> placedFeatureRegistry.getKey(placedFeature));
                if (placedFeatureId != null) {
                    placedFeatureHolder = CompatUtil.getHolder(placedFeatureRegistry, ResourceKey.create(Registries.PLACED_FEATURE, placedFeatureId)).orElse(null);
                }
            }
        }

        //? if <26.3 {
        Holder<ConfiguredFeature<?, ?>> configuredFeatureHolder = placedFeature.feature();
        Feature<?> featureInstance = configuredFeatureHolder.value().feature();
        Identifier trunkId = extractTrunkOrFoliageBlockId(configuredFeatureHolder.value().config(), level);
        //?} else {
        /*Holder<Feature> configuredFeatureHolder = placedFeature.feature();
        Feature featureInstance = configuredFeatureHolder.value();
        Identifier trunkId = extractTrunkOrFoliageBlockId(featureInstance, level);
        *///?}
        Identifier configuredFeatureId = configuredFeatureHolder.unwrapKey().map(CompatUtil::keyId).orElse(null);

        FeatureType featureType = resolveFeatureType(featureInstance);

        ActiveFeatureContext ctx = new ActiveFeatureContext(
                placedFeature, placedFeatureId, placedFeatureHolder,
                configuredFeatureHolder, configuredFeatureId,
                featureInstance, featureType.id(), featureType.holder(),
                origin, trunkId
        );
        ACTIVE_FEATURE_STACK.get().push(ctx);
    }

    //? if <26.3 {
    public static void pushActiveFeature(Feature<?> featureInstance, @Nullable FeatureConfiguration config, @Nullable LevelAccessor level, @Nullable BlockPos origin) {
        if (!HAS_FEATURE_RULES || featureInstance == null) return;
        Identifier trunkId = config != null ? extractTrunkOrFoliageBlockId(config, level) : null;
    //?} else {
    /*public static void pushActiveFeature(Feature featureInstance, @Nullable LevelAccessor level, @Nullable BlockPos origin) {
        if (!HAS_FEATURE_RULES || featureInstance == null) return;
        Identifier trunkId = extractTrunkOrFoliageBlockId(featureInstance, level);
    *///?}
        FeatureType featureType = resolveFeatureType(featureInstance);

        ActiveFeatureContext ctx = new ActiveFeatureContext(
                null, null, null,
                null, null,
                featureInstance, featureType.id(), featureType.holder(),
                origin, trunkId
        );
        ACTIVE_FEATURE_STACK.get().push(ctx);
    }

    //? if <26.3 {
    private static FeatureType resolveFeatureType(Feature<?> featureInstance) {
        Identifier id = BuiltInRegistries.FEATURE.getKey(featureInstance);
        Holder<?> holder = id != null ? CompatUtil.getHolder(BuiltInRegistries.FEATURE, ResourceKey.create(Registries.FEATURE, id)).orElse(null) : null;
        return new FeatureType(id, holder);
    }
    //?} else {
    /*private static FeatureType resolveFeatureType(Feature featureInstance) {
        MapCodec<? extends Feature> codec = featureInstance.codec();
        Identifier id = BuiltInRegistries.FEATURE_TYPE.getKey(codec);
        Holder<?> holder = id != null ? CompatUtil.getHolder(BuiltInRegistries.FEATURE_TYPE, ResourceKey.create(Registries.FEATURE_TYPE, id)).orElse(null) : null;
        return new FeatureType(id, holder);
    }
    *///?}

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean isInTag(@Nullable Holder<?> holder, ResourceKey<? extends Registry<?>> registry, Identifier tagLocation) {
        return holder != null && ((Holder) holder).is(TagKey.create((ResourceKey) registry, tagLocation));
    }

    public static void popActiveFeature() {
        Deque<ActiveFeatureContext> stack = ACTIVE_FEATURE_STACK.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    public static boolean matchesFeature(ReplacementRule rule, @Nullable BlockPos currentPos, @Nullable RegistryAccess registryAccess) {
        if (rule.features == null || rule.features.isEmpty()) return false;
        Deque<ActiveFeatureContext> stack = ACTIVE_FEATURE_STACK.get();
        if (stack.isEmpty()) return false;

        int featureRadius = rule.getFeatureRadius();
        long radiusSq = (long) featureRadius * featureRadius;

        for (ActiveFeatureContext activeFeatureContext : stack) {
            if (featureRadius > 0 && currentPos != null && activeFeatureContext.origin() != null) {
                if (currentPos.distSqr(activeFeatureContext.origin()) > radiusSq) {
                    continue;
                }
            }

            for (String featureRequirement : rule.features) {
                if (featureRequirement == null || featureRequirement.isEmpty()) continue;

                if (featureRequirement.startsWith("#")) {
                    Identifier tagLocation = Identifier.tryParse(featureRequirement.substring(1));
                    if (tagLocation != null) {
                        if (isInTag(activeFeatureContext.placedFeatureHolder, Registries.PLACED_FEATURE, tagLocation)) {
                            return true;
                        }
                        if (isInTag(activeFeatureContext.configuredFeatureHolder, CONFIGURED_FEATURE_REGISTRY, tagLocation)) {
                            return true;
                        }
                        if (isInTag(activeFeatureContext.featureHolder, FEATURE_TYPE_REGISTRY, tagLocation)) {
                            return true;
                        }
                    }
                } else if (featureRequirement.contains("*")) {
                    Pattern pattern = rule.compiledFeaturePatterns != null ? rule.compiledFeaturePatterns.get(featureRequirement) : null;
                    if (pattern == null) continue;
                    if (activeFeatureContext.placedFeatureId != null && pattern.matcher(activeFeatureContext.placedFeatureId.toString()).matches())
                        return true;
                    if (activeFeatureContext.configuredFeatureId != null && pattern.matcher(activeFeatureContext.configuredFeatureId.toString()).matches())
                        return true;
                    if (activeFeatureContext.featureId != null && pattern.matcher(activeFeatureContext.featureId.toString()).matches())
                        return true;
                    if (activeFeatureContext.trunkOrFoliageBlockId != null && pattern.matcher(activeFeatureContext.trunkOrFoliageBlockId.toString()).matches())
                        return true;
                } else {
                    Identifier requirementLocation = Identifier.tryParse(featureRequirement);
                    if (requirementLocation != null) {
                        if (activeFeatureContext.placedFeatureId != null && activeFeatureContext.placedFeatureId.equals(requirementLocation))
                            return true;
                        if (activeFeatureContext.configuredFeatureId != null && activeFeatureContext.configuredFeatureId.equals(requirementLocation))
                            return true;
                        if (activeFeatureContext.featureId != null && activeFeatureContext.featureId.equals(requirementLocation))
                            return true;
                        if (activeFeatureContext.trunkOrFoliageBlockId != null && activeFeatureContext.trunkOrFoliageBlockId.equals(requirementLocation))
                            return true;

                        String path = requirementLocation.getPath();
                        if (activeFeatureContext.placedFeatureId != null && (activeFeatureContext.placedFeatureId.getPath().equals(path) || activeFeatureContext.placedFeatureId.getPath().contains(path)))
                            return true;
                        if (activeFeatureContext.configuredFeatureId != null && (activeFeatureContext.configuredFeatureId.getPath().equals(path) || activeFeatureContext.configuredFeatureId.getPath().contains(path)))
                            return true;
                        if (activeFeatureContext.featureId != null && (activeFeatureContext.featureId.getPath().equals(path) || activeFeatureContext.featureId.getPath().contains(path)))
                            return true;
                        if (activeFeatureContext.trunkOrFoliageBlockId != null && (activeFeatureContext.trunkOrFoliageBlockId.getPath().equals(path) || activeFeatureContext.trunkOrFoliageBlockId.getPath().contains(path)))
                            return true;
                    }
                }
            }
        }
        return false;
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
        Block outputBlock = outputBlocks.size() == 1 ? outputBlocks.getFirst() : outputBlocks.get(rand.nextInt(outputBlocks.size()));

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
                     "features": [
                       "minecraft:ore_diamond_buried"
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
                     "_comment_conditions": "NEIGHBORS: Only replace if surroundings match (single block ID, list of IDs, block tag #tag, or wildcard)",
                     "neighbors": {
                         "up": "minecraft:air",
                         "down": [
                             "minecraft:grass_block",
                             "minecraft:dirt"
                         ]
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

    private record FeatureType(@Nullable Identifier id, @Nullable Holder<?> holder) {
    }

    public record ActiveFeatureContext(
            @Nullable PlacedFeature placedFeature,
            @Nullable Identifier placedFeatureId,
            @Nullable Holder<PlacedFeature> placedFeatureHolder,
            @Nullable Holder<?> configuredFeatureHolder,
            @Nullable Identifier configuredFeatureId,
            @Nullable Object feature,
            @Nullable Identifier featureId,
            @Nullable Holder<?> featureHolder,
            @Nullable BlockPos origin,
            @Nullable Identifier trunkOrFoliageBlockId
    ) {
    }
}
