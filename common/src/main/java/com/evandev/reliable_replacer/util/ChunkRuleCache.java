package com.evandev.reliable_replacer.util;

import com.evandev.reliable_replacer.data.ReplacementRule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.*;

public class ChunkRuleCache {
    private final Map<ReplacementRule, List<BoundingBox>> structureCache = new HashMap<>();
    private final LevelAccessor level;
    private final ChunkPos chunkPos;

    public ChunkRuleCache(LevelAccessor level, ChunkPos chunkPos) {
        this.level = level;
        this.chunkPos = chunkPos;
    }

    public boolean isPositionInStructure(ReplacementRule rule, BlockPos pos) {
        if (rule.structures.isEmpty()) return true;

        List<BoundingBox> boxes = structureCache.computeIfAbsent(rule, this::computeBoxes);
        if (boxes.isEmpty()) return false;

        for (BoundingBox box : boxes) {
            if (box.isInside(pos)) return true;
        }
        return false;
    }

    private List<BoundingBox> computeBoxes(ReplacementRule rule) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return Collections.emptyList();
        }

        List<BoundingBox> boxes = new ArrayList<>();
        Registry<Structure> structRegistry = serverLevel.registryAccess().registryOrThrow(Registries.STRUCTURE);

        Set<StructureStart> uniqueStarts = new HashSet<>();

        for (String structId : rule.structures) {
            ResourceLocation rl = ResourceLocation.tryParse(structId);
            if (rl != null && structRegistry.containsKey(rl)) {
                Structure structure = structRegistry.get(rl);
                if (structure != null) {
                    int minSection = serverLevel.getMinSection();
                    int maxSection = serverLevel.getMaxSection();

                    for (int y = minSection; y <= maxSection; y++) {
                        List<StructureStart> starts =
                                serverLevel.structureManager().startsForStructure(SectionPos.of(chunkPos, y), structure);
                        uniqueStarts.addAll(starts);
                    }
                }
            }
        }

        for (StructureStart start : uniqueStarts) {
            if (start.isValid()) {
                for (StructurePiece piece : start.getPieces()) {
                    boxes.add(piece.getBoundingBox());
                }
            }
        }

        return boxes;
    }
}