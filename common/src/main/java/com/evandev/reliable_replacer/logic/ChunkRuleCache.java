package com.evandev.reliable_replacer.logic;

import com.evandev.reliable_replacer.Constants;
import com.evandev.reliable_replacer.data.ReplacementRule;
import com.evandev.reliable_replacer.mixin.minecraft.WorldGenRegionAccessor;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkDependencies;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkRuleCache {
    private static final int MAX_CHUNK_RADIUS = 8;
    private static final Set<ReplacementRule> WARNED_RADIUS_RULES = ConcurrentHashMap.newKeySet();

    private final Map<ReplacementRule, List<BoundingBox>> structureCache = new HashMap<>();
    private final LevelAccessor level;
    private final ChunkPos chunkPos;

    public ChunkRuleCache(LevelAccessor level, ChunkPos chunkPos) {
        this.level = level;
        this.chunkPos = chunkPos;
    }

    public static long distanceSqToBox(BlockPos pos, BoundingBox box) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        int dx = 0;
        if (x < box.minX()) {
            dx = box.minX() - x;
        } else if (x > box.maxX()) {
            dx = x - box.maxX();
        }

        int dy = 0;
        if (y < box.minY()) {
            dy = box.minY() - y;
        } else if (y > box.maxY()) {
            dy = y - box.maxY();
        }

        int dz = 0;
        if (z < box.minZ()) {
            dz = box.minZ() - z;
        } else if (z > box.maxZ()) {
            dz = z - box.maxZ();
        }

        return (long) dx * dx + (long) dy * dy + (long) dz * dz;
    }

    private static void addStart(StructureStart start, Set<StructureStart> visitedStarts, List<BoundingBox> boxes) {
        if (start == null || !start.isValid() || !visitedStarts.add(start)) return;

        for (StructurePiece piece : start.getPieces()) {
            boxes.add(piece.getBoundingBox());
        }
        if (start.getPieces().isEmpty()) {
            boxes.add(start.getBoundingBox());
        }
    }

    public boolean isPositionInStructure(ReplacementRule rule, BlockPos pos) {
        if (rule.structures.isEmpty()) return true;

        List<BoundingBox> boxes = structureCache.computeIfAbsent(rule, this::computeBoxes);
        if (boxes.isEmpty()) return false;

        int radius = rule.getStructureRadius();
        if (radius <= 0) {
            for (BoundingBox box : boxes) {
                if (box.isInside(pos)) return true;
            }
        } else {
            long radiusSq = (long) radius * radius;
            for (BoundingBox box : boxes) {
                if (distanceSqToBox(pos, box) <= radiusSq) return true;
            }
        }
        return false;
    }

    private boolean canAccess(int cx, int cz, ChunkStatus status) {
        if (level instanceof WorldGenRegion wgr) {
            ChunkStep step = ((WorldGenRegionAccessor) wgr).reliableReplacer$getGeneratingStep();
            ChunkDependencies dependencies = step.directDependencies();
            int distance = wgr.getCenter().getChessboardDistance(cx, cz);
            return distance < dependencies.size() && status.isOrBefore(dependencies.get(distance));
        } else if (level instanceof ServerLevel sl) {
            return sl.getChunkSource().hasChunk(cx, cz);
        }
        return level.hasChunk(cx, cz);
    }

    private List<BoundingBox> computeBoxes(ReplacementRule rule) {
        ServerLevel serverLevel = null;
        if (level instanceof ServerLevel currentLevel) {
            serverLevel = currentLevel;
        } else if (level instanceof WorldGenRegion wgr) {
            serverLevel = wgr.getLevel();
        }

        if (serverLevel == null) {
            return Collections.emptyList();
        }

        StructureManager structureManager = serverLevel.structureManager();
        Registry<Structure> structRegistry = serverLevel.registryAccess().registryOrThrow(Registries.STRUCTURE);

        List<BoundingBox> boxes = new ArrayList<>();
        int chunkRadius = rule.getStructureRadius() > 0 ? (int) Math.ceil(rule.getStructureRadius() / 16.0) : 0;

        if (chunkRadius > MAX_CHUNK_RADIUS) {
            chunkRadius = MAX_CHUNK_RADIUS;
            if (WARNED_RADIUS_RULES.add(rule)) {
                Constants.LOG.warn("structure_radius of {} exceeds the {} block limit reachable during world generation; clamping the structure search to {} chunks.",
                        rule.getStructureRadius(), MAX_CHUNK_RADIUS * 16, MAX_CHUNK_RADIUS);
            }
        }

        Set<StructureStart> visitedStarts = new HashSet<>();

        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                int cx = chunkPos.x + dx;
                int cz = chunkPos.z + dz;

                if (canAccess(cx, cz, ChunkStatus.STRUCTURE_REFERENCES)) {
                    collectFromReferences(level.getChunk(cx, cz, ChunkStatus.STRUCTURE_REFERENCES),
                            rule, structRegistry, structureManager, visitedStarts, boxes);
                } else if (canAccess(cx, cz, ChunkStatus.STRUCTURE_STARTS)) {
                    collectFromStarts(level.getChunk(cx, cz, ChunkStatus.STRUCTURE_STARTS),
                            rule, structRegistry, visitedStarts, boxes);
                }
            }
        }
        return boxes;
    }

    private void collectFromReferences(ChunkAccess chunk, ReplacementRule rule, Registry<Structure> structRegistry,
                                       StructureManager structureManager, Set<StructureStart> visitedStarts, List<BoundingBox> boxes) {
        Map<Structure, LongSet> references = chunk.getAllReferences();
        if (references.isEmpty()) return;

        for (ResourceLocation rl : rule.parsedStructures) {
            Structure structure = structRegistry.get(rl);
            if (structure == null) continue;

            LongSet refs = references.get(structure);
            if (refs == null) continue;

            for (long packedChunkPos : refs) {
                ChunkPos structChunkPos = new ChunkPos(packedChunkPos);
                if (!canAccess(structChunkPos.x, structChunkPos.z, ChunkStatus.STRUCTURE_STARTS)) continue;

                StructureStart start = structureManager.getStartForStructure(
                        SectionPos.of(structChunkPos, 0),
                        structure,
                        level.getChunk(structChunkPos.x, structChunkPos.z, ChunkStatus.STRUCTURE_STARTS)
                );

                addStart(start, visitedStarts, boxes);
            }
        }
    }

    private void collectFromStarts(ChunkAccess chunk, ReplacementRule rule, Registry<Structure> structRegistry,
                                   Set<StructureStart> visitedStarts, List<BoundingBox> boxes) {
        Map<Structure, StructureStart> starts = chunk.getAllStarts();
        if (starts.isEmpty()) return;

        for (ResourceLocation rl : rule.parsedStructures) {
            Structure structure = structRegistry.get(rl);
            if (structure == null) continue;

            addStart(starts.get(structure), visitedStarts, boxes);
        }
    }
}