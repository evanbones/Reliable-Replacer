package com.evandev.reliable_replacer.api;

import com.evandev.reliable_replacer.data.ReplacementRule;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface IReplacementContext {
    /**
     * @return The current position being evaluated.
     */
    BlockPos getPos();

    /**
     * @return The world spawn position (used for relative coordinate checks).
     */
    BlockPos getSpawnPos();

    /**
     * @return The ID of the dimension (e.g., "minecraft:overworld").
     */
    ResourceLocation getDimensionId();

    /**
     * @return The ID of the biome at the current position.
     */
    ResourceLocation getBiomeId();

    /**
     * Gets a block state at a specific position (usually for neighbor checks).
     */
    BlockState getBlockState(BlockPos pos);

    /**
     * Gets the NBT data of a block entity at a specific position.
     */
    @Nullable
    CompoundTag getBlockEntityNbt(BlockPos pos);

    /**
     * Checks if the current position is inside one of the structures defined by the rule.
     *
     * @param rule The rule containing the structure list (used for caching keys).
     */
    boolean matchesStructure(ReplacementRule rule);

    /**
     * @return True if this is running during retrogen/worldgen, False if player placed.
     */
    boolean isRetrogen();
}