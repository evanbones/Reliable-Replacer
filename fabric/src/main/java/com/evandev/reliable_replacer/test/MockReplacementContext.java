package com.evandev.reliable_replacer.test;

import com.evandev.reliable_replacer.api.IReplacementContext;
import com.evandev.reliable_replacer.data.ReplacementRule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MockReplacementContext implements IReplacementContext {

    private final Set<ResourceLocation> activeStructures = new HashSet<>();
    private final Set<String> activeFeatures = new HashSet<>();
    private final Map<Direction, BlockState> neighbors = new HashMap<>();
    private BlockPos pos = BlockPos.ZERO;
    private BlockPos spawnPos = BlockPos.ZERO;
    private ResourceLocation biomeId = new ResourceLocation("minecraft:plains");
    private ResourceLocation dimensionId = new ResourceLocation("minecraft:overworld");
    private boolean isRetrogen = false;
    private CompoundTag mockNbt = null;

    public MockReplacementContext setPos(int x, int y, int z) {
        this.pos = new BlockPos(x, y, z);
        return this;
    }

    public MockReplacementContext setSpawn(int x, int y, int z) {
        this.spawnPos = new BlockPos(x, y, z);
        return this;
    }

    public MockReplacementContext setBiome(String biome) {
        this.biomeId = new ResourceLocation(biome);
        return this;
    }

    public MockReplacementContext setDimension(String dim) {
        this.dimensionId = new ResourceLocation(dim);
        return this;
    }

    public MockReplacementContext setStructure(String structureId) {
        this.activeStructures.add(new ResourceLocation(structureId));
        return this;
    }

    public MockReplacementContext setFeature(String featureId) {
        this.activeFeatures.add(featureId);
        return this;
    }

    public MockReplacementContext setNeighbor(Direction dir, BlockState state) {
        this.neighbors.put(dir, state);
        return this;
    }

    public MockReplacementContext setMockNbt(CompoundTag tag) {
        this.mockNbt = tag;
        return this;
    }

    @Override
    @Nullable
    public CompoundTag getBlockEntityNbt(BlockPos targetPos) {
        return mockNbt;
    }

    @Override
    public BlockPos getPos() {
        return pos;
    }

    @Override
    public BlockPos getSpawnPos() {
        return spawnPos;
    }

    @Override
    public ResourceLocation getDimensionId() {
        return dimensionId;
    }

    @Override
    public ResourceLocation getBiomeId() {
        return biomeId;
    }

    @Override
    public BlockState getBlockState(BlockPos targetPos) {
        BlockPos diff = targetPos.subtract(this.pos);
        for (Direction dir : Direction.values()) {
            if (dir.getNormal().equals(diff)) {
                return neighbors.getOrDefault(dir, Blocks.AIR.defaultBlockState());
            }
        }
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public boolean matchesBiome(ReplacementRule rule) {
        if (rule.parsedBiomes == null || rule.parsedBiomes.isEmpty()) return true;
        return biomeId != null && rule.parsedBiomes.contains(biomeId);
    }

    @Override
    public boolean matchesFeature(ReplacementRule rule) {
        if (rule.features == null || rule.features.isEmpty()) return false;
        for (String needed : rule.features) {
            if (activeFeatures.contains(needed)) return true;
        }
        return false;
    }

    @Override
    public boolean matchesStructure(ReplacementRule rule) {
        if (rule.parsedStructures == null) return false;
        for (ResourceLocation needed : rule.parsedStructures) {
            if (activeStructures.contains(needed)) return true;
        }
        return false;
    }

    @Override
    public boolean isRetrogen() {
        return isRetrogen;
    }

    public MockReplacementContext setRetrogen(boolean isRetrogen) {
        this.isRetrogen = isRetrogen;
        return this;
    }
}