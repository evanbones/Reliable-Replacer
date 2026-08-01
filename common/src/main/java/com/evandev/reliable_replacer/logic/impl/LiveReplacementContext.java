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
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
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
            if (be != null) return be.saveWithoutMetadata(level.registryAccess());
            return chunk.getBlockEntityNbtForSaving(targetPos, level.registryAccess());
        } else {
            int cx = targetPos.getX() >> 4;
            int cz = targetPos.getZ() >> 4;
            if (!level.hasChunk(cx, cz)) {
                return null;
            }

            BlockEntity be = level.getBlockEntity(targetPos);
            if (be != null) return be.saveWithoutMetadata(level.registryAccess());
            return null;
        }
    }

    @Override
    public ResourceLocation getBiomeId() {
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

    public ResourceLocation getBiomeIdAt(int x, int y, int z) {
        int qX = x >> 2;
        int qY = y >> 2;
        int qZ = z >> 2;

        Holder<Biome> biomeHolder;
        int cx = x >> 4;
        int cz = z >> 4;

        if (chunk != null && cx == chunk.getPos().x && cz == chunk.getPos().z) {
            biomeHolder = chunk.getNoiseBiome(qX, qY, qZ);
        } else {
            if (!hasChunk(cx, cz)) {
                return null;
            }
            if (chunk != null && level instanceof WorldGenRegion wgr) {
                biomeHolder = wgr.getChunk(cx, cz).getNoiseBiome(qX, qY, qZ);
            } else if (level instanceof ServerLevel sl) {
                biomeHolder = sl.getChunk(cx, cz).getNoiseBiome(qX, qY, qZ);
            } else {
                biomeHolder = level.getBiome(new BlockPos(x, y, z));
            }
        }

        var keyOpt = biomeHolder.unwrapKey();
        return keyOpt.map(ResourceKey::location).orElse(null);
    }

    private boolean hasChunk(int cx, int cz) {
        if (level instanceof WorldGenRegion wgr) {
            return wgr.hasChunk(cx, cz);
        } else if (level instanceof ServerLevel sl) {
            return sl.getChunkSource().hasChunk(cx, cz);
        }
        return level != null && level.hasChunk(cx, cz);
    }

    @Override
    public boolean matchesBiome(ReplacementRule rule) {
        if (rule.parsedBiomes == null || rule.parsedBiomes.isEmpty()) return true;

        int radius = rule.getBiomeRadius();
        if (radius <= 0) {
            ResourceLocation biomeId = getBiomeId();
            return biomeId != null && rule.parsedBiomes.contains(biomeId);
        }

        int quartRadius = (int) Math.ceil(radius / 4.0);
        long radiusSq = (long) radius * radius;

        int px = pos.getX();
        int py = pos.getY();
        int pz = pos.getZ();

        int centerQX = px >> 2;
        int centerQY = py >> 2;
        int centerQZ = pz >> 2;

        for (int dqx = -quartRadius; dqx <= quartRadius; dqx++) {
            int minX = (centerQX + dqx) << 2;
            int maxX = minX + 3;
            int dx = 0;
            if (px < minX) dx = minX - px;
            else if (px > maxX) dx = px - maxX;

            for (int dqz = -quartRadius; dqz <= quartRadius; dqz++) {
                int minZ = (centerQZ + dqz) << 2;
                int maxZ = minZ + 3;
                int dz = 0;
                if (pz < minZ) dz = minZ - pz;
                else if (pz > maxZ) dz = pz - maxZ;

                if ((long) dx * dx + (long) dz * dz > radiusSq) continue;

                for (int dqy = -quartRadius; dqy <= quartRadius; dqy++) {
                    int minY = (centerQY + dqy) << 2;
                    int maxY = minY + 3;
                    int dy = 0;
                    if (py < minY) dy = minY - py;
                    else if (py > maxY) dy = py - maxY;

                    if ((long) dx * dx + (long) dy * dy + (long) dz * dz > radiusSq) continue;

                    ResourceLocation sampleBiome = getBiomeIdAt(minX, minY, minZ);
                    if (sampleBiome != null && rule.parsedBiomes.contains(sampleBiome)) {
                        return true;
                    }
                }
            }
        }
        return false;
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
            int radius = rule.getStructureRadius();

            for (ResourceLocation rl : rule.parsedStructures) {
                if (structRegistry.containsKey(rl)) {
                    Structure structure = structRegistry.get(rl);
                    if (structure == null) continue;

                    if (radius <= 0) {
                        if (structureManager.getStructureAt(pos, structure).isValid()) {
                            return true;
                        }
                    } else {
                        ChunkPos currentChunk = new ChunkPos(pos);
                        int chunkRadius = (int) Math.ceil(radius / 16.0);
                        long radiusSq = (long) radius * radius;

                        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
                            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                                int cx = currentChunk.x + dx;
                                int cz = currentChunk.z + dz;
                                if (!hasChunk(cx, cz)) continue;

                                SectionPos secPos = SectionPos.of(cx, cz, 0);
                                for (StructureStart start : structureManager.startsForStructure(secPos, structure)) {
                                    if (start.isValid()) {
                                        for (StructurePiece piece : start.getPieces()) {
                                            if (ChunkRuleCache.distanceSqToBox(pos, piece.getBoundingBox()) <= radiusSq) {
                                                return true;
                                            }
                                        }
                                        if (start.getPieces().isEmpty() && ChunkRuleCache.distanceSqToBox(pos, start.getBoundingBox()) <= radiusSq) {
                                            return true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean matchesFeature(ReplacementRule rule) {
        if (rule.features == null || rule.features.isEmpty()) return false;
        return RuleManager.matchesFeature(rule, pos, level != null ? level.registryAccess() : null);
    }
}