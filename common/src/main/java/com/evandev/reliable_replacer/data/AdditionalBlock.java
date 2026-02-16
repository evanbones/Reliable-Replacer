package com.evandev.reliable_replacer.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdditionalBlock {
    public String output;
    public List<String> outputs = new ArrayList<>();
    @SerializedName("output_nbt")
    public String outputNbt;
    @SerializedName("x_offset")
    public int xOffset = 0;
    @SerializedName("y_offset")
    public int yOffset = 0;
    @SerializedName("z_offset")
    public int zOffset = 0;
    @SerializedName("output_state_properties")
    public Map<String, String> outputStateProperties = new HashMap<>();
    @SerializedName("randomize_properties")
    public List<String> randomizeProperties = new ArrayList<>();
    public boolean remove = false;

    private transient List<Block> outputBlocks;
    private transient CompoundTag parsedOutputNbt;
    private transient boolean nbtParsed = false;
    private transient boolean isOutputSelf = false;

    @Nullable
    public CompoundTag getParsedOutputNbt() {
        if (!nbtParsed) {
            nbtParsed = true;
            if (outputNbt != null && !outputNbt.trim().isEmpty()) {
                try {
                    parsedOutputNbt = TagParser.parseTag(outputNbt);
                } catch (Exception ignored) {
                }
            }
        }
        return parsedOutputNbt;
    }

    @Nullable
    public List<Block> getOutputBlocks() {
        if (outputBlocks == null && !isOutputSelf) {
            if (remove) {
                outputBlocks = List.of(Blocks.AIR);
            } else {
                List<String> combinedOutputs = new ArrayList<>();
                if (output != null) combinedOutputs.add(output);
                if (outputs != null && !outputs.isEmpty()) combinedOutputs.addAll(outputs);

                if (combinedOutputs.isEmpty()) {
                    isOutputSelf = true;
                    return null;
                }

                boolean isSelf = false;
                for (String out : combinedOutputs) {
                    if (out.equalsIgnoreCase("self") || out.equalsIgnoreCase("this")) {
                        isSelf = true;
                        break;
                    }
                }

                if (isSelf) {
                    isOutputSelf = true;
                    return null;
                } else {
                    outputBlocks = new ArrayList<>();
                    for (String outStr : combinedOutputs) {
                        ResourceLocation id = ResourceLocation.tryParse(outStr);
                        outputBlocks.add(id != null ? BuiltInRegistries.BLOCK.get(id) : Blocks.AIR);
                    }
                }
            }
        }
        return outputBlocks;
    }
}