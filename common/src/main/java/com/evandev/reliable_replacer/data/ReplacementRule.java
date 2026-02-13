package com.evandev.reliable_replacer.data;

import com.evandev.reliable_replacer.Constants;
import com.google.gson.annotations.SerializedName;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class ReplacementRule {
    public Set<String> inputs = new HashSet<>();
    public String output;

    public Set<String> biomes = new HashSet<>();
    public Set<String> dimensions = new HashSet<>();
    public Set<String> structures = new HashSet<>();

    @SerializedName("min_x")
    public String minX;
    @SerializedName("max_x")
    public String maxX;
    @SerializedName("min_y")
    public String minY;
    @SerializedName("max_y")
    public String maxY;
    @SerializedName("min_z")
    public String minZ;
    @SerializedName("max_z")
    public String maxZ;

    @SerializedName("keep_states")
    public boolean keepStates = true;
    @SerializedName("keep_nbt")
    public boolean keepNbt = true;
    @SerializedName("retrogen")
    public Boolean retrogen = null;
    @SerializedName("player_blocks")
    public Boolean playerBlocks = null;
    @SerializedName("not")
    public ReplacementRule not;

    @SerializedName("state_properties")
    public Map<String, String> stateProperties = new HashMap<>();

    @SerializedName("output_state_properties")
    public Map<String, String> outputStateProperties = new HashMap<>();

    @SerializedName("randomize_properties")
    public List<String> randomizeProperties = new ArrayList<>();

    @SerializedName("neighbors")
    public Map<String, String> neighbors = new HashMap<>();
    @SerializedName("probability")
    public Float probability = null;
    @SerializedName("remove")
    public boolean remove = false;

    public transient Integer cachedMinX, cachedMaxX;
    public transient Integer cachedMinY, cachedMaxY;
    public transient Integer cachedMinZ, cachedMaxZ;
    public transient Integer cachedMinXOffset, cachedMaxXOffset;
    public transient Integer cachedMinYOffset, cachedMaxYOffset;
    public transient Integer cachedMinZOffset, cachedMaxZOffset;

    public transient Set<ResourceLocation> parsedBiomes;
    public transient Set<ResourceLocation> parsedDimensions;
    public transient Set<ResourceLocation> parsedStructures;
    private transient Block outputBlock;
    private transient Set<Block> inputBlocks;
    private transient boolean isOutputSelf = false;

    /**
     * Returns the output block.
     *
     * @return The block to place, or NULL if the output is "Self" (keep original).
     */
    @Nullable
    public Block getOutputBlock() {
        if (outputBlock == null && !isOutputSelf) {
            if (remove) {
                outputBlock = Blocks.AIR;
            } else if (output == null) {
                isOutputSelf = true;
                return null;
            } else if (output.equalsIgnoreCase("self") || output.equalsIgnoreCase("this")) {
                isOutputSelf = true;
                return null;
            } else {
                ResourceLocation id = ResourceLocation.tryParse(output);
                outputBlock = id != null ? BuiltInRegistries.BLOCK.get(id) : Blocks.AIR;
            }
        }
        return outputBlock;
    }

    public Set<Block> getInputBlocks() {
        if (inputBlocks == null) {
            resolveBlocks();
        }
        return inputBlocks;
    }

    public void resolveBlocks() {
        inputBlocks = new HashSet<>();
        for (String id : inputs) {
            if (id.startsWith("#")) {
                ResourceLocation rl = ResourceLocation.tryParse(id.substring(1));
                if (rl != null) {
                    TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, rl);
                    BuiltInRegistries.BLOCK.getTagOrEmpty(tagKey)
                            .forEach(holder -> inputBlocks.add(holder.value()));
                }
            } else if (id.endsWith(":*")) {
                String namespace = id.split(":")[0];
                BuiltInRegistries.BLOCK.entrySet().stream()
                        .filter(entry -> entry.getKey().location().getNamespace().equals(namespace))
                        .map(java.util.Map.Entry::getValue)
                        .forEach(inputBlocks::add);
            } else {
                ResourceLocation rl = ResourceLocation.tryParse(id);
                if (rl != null && BuiltInRegistries.BLOCK.containsKey(rl)) {
                    inputBlocks.add(BuiltInRegistries.BLOCK.get(rl));
                }
            }
        }

        parsedBiomes = new HashSet<>();
        if (biomes != null) biomes.forEach(s -> {
            ResourceLocation rl = ResourceLocation.tryParse(s);
            if (rl != null) parsedBiomes.add(rl);
        });

        parsedDimensions = new HashSet<>();
        if (dimensions != null) dimensions.forEach(s -> {
            ResourceLocation rl = ResourceLocation.tryParse(s);
            if (rl != null) parsedDimensions.add(rl);
        });

        parsedStructures = new HashSet<>();
        if (structures != null) structures.forEach(s -> {
            ResourceLocation rl = ResourceLocation.tryParse(s);
            if (rl != null) parsedStructures.add(rl);
        });

        parseToCache(minX, v -> cachedMinX = v, v -> cachedMinXOffset = v);
        parseToCache(maxX, v -> cachedMaxX = v, v -> cachedMaxXOffset = v);
        parseToCache(minY, v -> cachedMinY = v, v -> cachedMinYOffset = v);
        parseToCache(maxY, v -> cachedMaxY = v, v -> cachedMaxYOffset = v);
        parseToCache(minZ, v -> cachedMinZ = v, v -> cachedMinZOffset = v);
        parseToCache(maxZ, v -> cachedMaxZ = v, v -> cachedMaxZOffset = v);

        if (not != null) not.resolveBlocks();
    }

    public boolean shouldRunRetrogen() {
        if (retrogen != null) return retrogen;
        return biomes.isEmpty() && structures.isEmpty();
    }

    public boolean shouldRunPlayerBlocks() {
        if (playerBlocks != null) return playerBlocks;
        return biomes.isEmpty() && structures.isEmpty();
    }

    private void parseToCache(String val, Consumer<Integer> absSetter, Consumer<Integer> offsetSetter) {
        if (val == null || val.trim().isEmpty()) return;
        try {
            String clean = val.replace(" ", "");
            if (clean.contains("spawn")) {
                String offsetStr = clean.replace("spawn", "");
                int offset = offsetStr.isEmpty() ? 0 : Integer.parseInt(offsetStr);
                offsetSetter.accept(offset);
            } else {
                absSetter.accept(Integer.parseInt(clean));
            }
        } catch (NumberFormatException e) {
            Constants.LOG.error("Invalid coordinate value in rule: {}", val);
        }
    }
}