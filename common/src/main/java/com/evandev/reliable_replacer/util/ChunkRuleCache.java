package com.evandev.reliable_replacer.util;

import com.evandev.reliable_replacer.data.ReplacementRule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.IdentityHashMap;
import java.util.Map;

public class ChunkRuleCache {
    private final Map<ReplacementRule, Boolean> ruleValidity = new IdentityHashMap<>();
    private final LevelAccessor level;
    private final BlockPos centerPos;

    public ChunkRuleCache(LevelAccessor level, ChunkPos chunkPos) {
        this.level = level;
        this.centerPos = chunkPos.getMiddleBlockPosition(64);
    }

    public boolean isRuleValid(ReplacementRule rule) {
        return ruleValidity.computeIfAbsent(rule, this::computeValidity);
    }

    private boolean computeValidity(ReplacementRule rule) {
        if (!rule.dimensions.isEmpty()) {
            if (level instanceof ServerLevel serverLevel) {
                ResourceLocation dimId = serverLevel.dimension().location();
                if (!rule.dimensions.contains(dimId.toString())) {
                    return false;
                }
            }
        }

        if (!rule.structures.isEmpty()) {
            if (level instanceof ServerLevel serverLevel) {
                Registry<Structure> structRegistry = serverLevel.registryAccess().registryOrThrow(Registries.STRUCTURE);
                boolean inStructure = false;

                for (String structId : rule.structures) {
                    ResourceLocation rl = ResourceLocation.tryParse(structId);
                    if (rl != null && structRegistry.containsKey(rl)) {
                        Structure structure = structRegistry.get(rl);
                        if (structure != null && serverLevel.structureManager().getStructureAt(centerPos, structure).isValid()) {
                            inStructure = true;
                            break;
                        }
                    }
                }

                return inStructure;
            } else {
                return false;
            }
        }

        return true;
    }
}