package com.evandev.reliable_replacer.logic;

import com.evandev.reliable_replacer.data.ReplacementRule;
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
import net.minecraft.world.level.chunk.ChunkStatus;
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
        ServerLevel sl = null;
        if (level instanceof ServerLevel s) {
            sl = s;
        } else if (level instanceof WorldGenRegion wgr) {
            sl = wgr.getLevel();
        }

        if (sl == null) {
            return Collections.emptyList();
        }

        StructureManager structureManager = sl.structureManager();
        Registry<Structure> structRegistry = sl.registryAccess().registryOrThrow(Registries.STRUCTURE);

        ChunkAccess chunk = level.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.STRUCTURE_REFERENCES);

        List<BoundingBox> boxes = new ArrayList<>();
        Map<Structure, LongSet> references = chunk.getAllReferences();

        for (ResourceLocation rl : rule.parsedStructures) {
            if (structRegistry.containsKey(rl)) {
                Structure structure = structRegistry.get(rl);
                if (structure != null && references.containsKey(structure)) {
                    LongSet refs = references.get(structure);

                    for (long packedChunkPos : refs) {
                        ChunkPos structChunkPos = new ChunkPos(packedChunkPos);
                        SectionPos startPos = SectionPos.of(structChunkPos, 0);

                        StructureStart start = structureManager.getStartForStructure(
                                startPos,
                                structure,
                                level.getChunk(structChunkPos.x, structChunkPos.z, ChunkStatus.STRUCTURE_STARTS)
                        );

                        if (start != null && start.isValid()) {
                            for (StructurePiece piece : start.getPieces()) {
                                boxes.add(piece.getBoundingBox());
                            }
                        }
                    }
                }
            }
        }
        return boxes;
    }
}