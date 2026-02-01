package com.evandev.reliable_replacer.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashSet;
import java.util.Set;

public class ReplacementRule {
    public Set<String> inputs = new HashSet<>();
    public String output;

    public Set<String> biomes = new HashSet<>();
    public Set<String> dimensions = new HashSet<>();

    public Set<String> structures = new HashSet<>();
    public Set<String> features = new HashSet<>();

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

    @SerializedName("retrogen")
    public boolean retrogen = true;

    @SerializedName("cancel_feature")
    public boolean cancelFeature = false;

    @SerializedName("apply_to_player_placement")
    public boolean applyToPlayerPlacement = true;

    @SerializedName("not")
    public ReplacementRule not;

    private transient Block outputBlock;
    private transient Set<Block> inputBlocks;

    public Block getOutputBlock() {
        if (outputBlock == null) {
            if (output == null) {
                outputBlock = Blocks.AIR;
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
            if (id.endsWith(":*")) {
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
    }
}