package com.evandev.reliable_replacer.logic.impl;

import com.evandev.reliable_replacer.api.IReplacementContext;
import com.evandev.reliable_replacer.data.ReplacementRule;
import com.evandev.reliable_replacer.logic.ChunkRuleCache;
import com.evandev.reliable_replacer.logic.RuleManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.Nullable;

public class LiveReplacementContext implements IReplacementContext {
    private final LevelAccessor level;
    private final BlockPos spawnPos;
    private final boolean isRetrogen;
    @Nullable
    private final ChunkAccess chunk;
    @Nullable
    private final ChunkRuleCache ruleCache;
    private BlockPos pos;
    private ResourceLocation cachedBiomeId;
    private int lastBiomeX = Integer.MIN_VALUE;
    private int lastBiomeY = Integer.MIN_VALUE;
    private int lastBiomeZ = Integer.MIN_VALUE;

    private ResourceLocation cachedDimId;
    private boolean dimIdComputed = false;

    public LiveReplacementContext(LevelAccessor level, BlockPos pos, BlockPos spawnPos,
                                  boolean isRetrogen, @Nullable ChunkAccess chunk,
                                  @Nullable ChunkRuleCache ruleCache) {
        this.level = level;
        this.pos = pos;
        this.spawnPos = spawnPos;
        this.isRetrogen = isRetrogen;
        this.chunk = chunk;
        this.ruleCache = ruleCache;
    }

    @Override
    public BlockPos getPos() {
        return pos;
    }

    /**
     * Updates the cursor position. Used when iterating over chunks to avoid object allocation.
     */
    public void setPos(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public BlockPos getSpawnPos() {
        return spawnPos;
    }

    @Override
    public BlockState getBlockState(BlockPos targetPos) {
        if (chunk != null) {
            int cx = targetPos.getX() >> 4;
            int cz = targetPos.getZ() >> 4;
            if (cx == chunk.getPos().x && cz == chunk.getPos().z) {
                return chunk.getBlockState(targetPos);
            }
        }

        if (level instanceof ServerLevel sl) {
            if (!sl.isLoaded(targetPos)) {
                return Blocks.VOID_AIR.defaultBlockState();
            }
        } else if (level instanceof WorldGenRegion wgr) {
            int cx = targetPos.getX() >> 4;
            int cz = targetPos.getZ() >> 4;
            if (!wgr.hasChunk(cx, cz)) {
                return Blocks.VOID_AIR.defaultBlockState();
            }
        }

        return level.getBlockState(targetPos);
    }

    @Override
    public boolean isRetrogen() {
        return isRetrogen;
    }

    @Override
    public ResourceLocation getDimensionId() {
        if (!dimIdComputed) {
            if (level instanceof ServerLevel sl) {
                cachedDimId = sl.dimension().location();
            } else if (level instanceof WorldGenRegion wgr) {
                cachedDimId = wgr.getLevel().dimension().location();
            } else if (level instanceof Level l) {
                cachedDimId = l.dimension().location();
            }
            dimIdComputed = true;
        }
        return cachedDimId;
    }

    @Override
    @Nullable
    public CompoundTag getBlockEntityNbt(BlockPos targetPos) {
        if (chunk != null) {
            BlockEntity be = chunk.getBlockEntity(targetPos);
            if (be != null) return be.saveWithoutMetadata();

            // Fallback for chunks during generation that have deferred NBT
            return chunk.getBlockEntityNbtForSaving(targetPos);
        } else {
            int cx = targetPos.getX() >> 4;
            int cz = targetPos.getZ() >> 4;
            if (!level.hasChunk(cx, cz)) {
                return null;
            }

            BlockEntity be = level.getBlockEntity(targetPos);
            if (be != null) return be.saveWithoutMetadata();
            return null;
        }
    }

    @Override
    public ResourceLocation getBiomeId() {
        // If a tree/feature is generating, pretend all its blocks belong to the biome where it started
        if (this.level instanceof WorldGenRegion) {
            ResourceLocation featureBiome = RuleManager.ACTIVE_FEATURE_BIOME.get();
            if (featureBiome != null) {
                return featureBiome;
            }
        }

        int qX = pos.getX() >> 2;
        int qY = pos.getY() >> 2;
        int qZ = pos.getZ() >> 2;

        if (cachedBiomeId == null || qX != lastBiomeX || qY != lastBiomeY || qZ != lastBiomeZ) {
            Holder<Biome> biomeHolder;
            if (chunk != null) {
                biomeHolder = chunk.getNoiseBiome(qX, qY, qZ);
            } else {
                int cx = pos.getX() >> 4;
                int cz = pos.getZ() >> 4;
                if (!level.hasChunk(cx, cz)) {
                    return null;
                }
                biomeHolder = level.getBiome(pos);
            }

            var keyOpt = biomeHolder.unwrapKey();
            cachedBiomeId = keyOpt.map(ResourceKey::location).orElse(null);

            lastBiomeX = qX;
            lastBiomeY = qY;
            lastBiomeZ = qZ;
        }
        return cachedBiomeId;
    }

    @Override
    public boolean matchesStructure(ReplacementRule rule) {
        if (rule.parsedStructures == null || rule.parsedStructures.isEmpty()) return false;

        if (ruleCache != null) {
            return ruleCache.isPositionInStructure(rule, pos);
        }

        if (level instanceof ServerLevel sl) {
            StructureManager structureManager = sl.structureManager();
            Registry<Structure> structRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);

            for (ResourceLocation rl : rule.parsedStructures) {
                if (structRegistry.containsKey(rl)) {
                    Structure structure = structRegistry.get(rl);
                    if (structure != null && structureManager.getStructureAt(pos, structure).isValid()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}