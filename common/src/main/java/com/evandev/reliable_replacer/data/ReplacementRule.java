package com.evandev.reliable_replacer.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

public class ReplacementRule {
    public Set<String> inputs = new HashSet<>();

    public String output;

    public Set<String> biomes = new HashSet<>();
    public Set<String> dimensions = new HashSet<>();

    @SerializedName("keep_states")
    public boolean keepStates = true;

    @SerializedName("retrogen")
    public boolean retrogen = true;

    private transient Block outputBlock;
    private transient Set<Block> inputBlocks;

    public boolean matches(BlockState state, ResourceLocation biomeId, ResourceLocation dimensionId) {
        if (!biomes.isEmpty() && biomeId != null) {
            if (!biomes.contains(biomeId.toString())) return false;
        }

        if (!dimensions.isEmpty() && dimensionId != null) {
            if (!dimensions.contains(dimensionId.toString())) return false;
        }

        if (inputBlocks == null) resolveBlocks();
        return inputBlocks.contains(state.getBlock());
    }

    public Block getOutputBlock() {
        if (outputBlock == null) {
            ResourceLocation id = ResourceLocation.tryParse(output);
            outputBlock = BuiltInRegistries.BLOCK.get(id);
            if (outputBlock == Blocks.AIR && !output.equals("minecraft:air")) {
                // TODO: log warning
            }
        }
        return outputBlock;
    }

    private void resolveBlocks() {
        inputBlocks = new HashSet<>();
        for (String id : inputs) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl != null && BuiltInRegistries.BLOCK.containsKey(rl)) {
                inputBlocks.add(BuiltInRegistries.BLOCK.get(rl));
            }
        }
    }
}