package com.evandev.reliable_replacer.logic;

import com.evandev.reliable_replacer.Constants;
import com.evandev.reliable_replacer.api.IReplacementContext;
import com.evandev.reliable_replacer.data.ReplacementRule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Map;
import java.util.Random;

public class RuleEvaluator {
    public static boolean checkRule(ReplacementRule rule, BlockState original, IReplacementContext ctx) {
        if (!rule.getInputBlocks().isEmpty() && !rule.getInputBlocks().contains(original.getBlock())) {
            return false;
        }

        BlockPos pos = ctx.getPos();
        BlockPos spawn = ctx.getSpawnPos();

        // Coordinate Checks
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        if (!RuleEvaluator.checkRange(x, rule.cachedMinX, rule.cachedMinXOffset, rule.minX,
                rule.cachedMaxX, rule.cachedMaxXOffset, rule.maxX, spawn.getX())) return false;
        if (!RuleEvaluator.checkRange(y, rule.cachedMinY, rule.cachedMinYOffset, rule.minY,
                rule.cachedMaxY, rule.cachedMaxYOffset, rule.maxY, spawn.getY())) return false;
        if (!RuleEvaluator.checkRange(z, rule.cachedMinZ, rule.cachedMinZOffset, rule.minZ,
                rule.cachedMaxZ, rule.cachedMaxZOffset, rule.maxZ, spawn.getZ())) return false;

        // Dimension Check
        if (rule.parsedDimensions != null && !rule.parsedDimensions.isEmpty()) {
            var dimId = ctx.getDimensionId();
            if (dimId != null && !rule.parsedDimensions.contains(dimId)) return false;
        }

        // Biome Check
        if (rule.parsedBiomes != null && !rule.parsedBiomes.isEmpty()) {
            var biomeId = ctx.getBiomeId();
            if (biomeId == null || !rule.parsedBiomes.contains(biomeId)) return false;
        }

        // Structure Check
        if (rule.parsedStructures != null && !rule.parsedStructures.isEmpty()) {
            if (!ctx.matchesStructure(rule)) return false;
        }

        // Probability Check
        if (rule.probability != null) {
            long seed = pos.getX() * 3129871L ^ pos.getY() * 116129781L ^ pos.getZ() * 3812423L;
            float rng = new Random(seed).nextFloat();
            if (rng > rule.probability) return false;
        }

        // State Properties Check
        if (!rule.stateProperties.isEmpty()) {
            for (Map.Entry<String, String> entry : rule.stateProperties.entrySet()) {
                Property<?> prop = original.getBlock().getStateDefinition().getProperty(entry.getKey());
                if (prop == null) return false;
                String actualValue = original.getValue(prop).toString();
                if (!actualValue.equals(entry.getValue())) return false;
            }
        }

        // Neighbor Check
        if (!rule.neighbors.isEmpty()) {
            for (Map.Entry<String, String> entry : rule.neighbors.entrySet()) {
                Direction dir = Direction.byName(entry.getKey());
                if (dir == null) continue;

                BlockPos neighborPos = ctx.getPos().relative(dir);
                BlockState neighborState = ctx.getBlockState(neighborPos);
                String reqId = entry.getValue();
                var neighborId = BuiltInRegistries.BLOCK.getKey(neighborState.getBlock());

                if (!neighborId.toString().equals(reqId)) {
                    if (reqId.endsWith(":*")) {
                        String namespace = reqId.split(":")[0];
                        if (!neighborId.getNamespace().equals(namespace)) return false;
                    } else {
                        return false;
                    }
                }
            }
        }

        // Input NBT Check
        if (rule.parsedInputNbt != null) {
            CompoundTag actualNbt = ctx.getBlockEntityNbt(ctx.getPos());
            return actualNbt != null && NbtUtils.compareNbt(rule.parsedInputNbt, actualNbt, true);
        }

        return true;
    }

    private static boolean checkRange(int pos, Integer cachedMin, Integer cachedMinOffset, String minStr,
                                      Integer cachedMax, Integer cachedMaxOffset, String maxStr, int spawn) {
        if (cachedMin != null) {
            if (pos < cachedMin) return false;
        } else if (cachedMinOffset != null) {
            if (pos < spawn + cachedMinOffset) return false;
        } else if (minStr != null) {
            int min = RuleEvaluator.parseCoordinate(minStr, spawn);
            if (pos < min) return false;
        }

        if (cachedMax != null) {
            return pos <= cachedMax;
        } else if (cachedMaxOffset != null) {
            return pos <= spawn + cachedMaxOffset;
        } else if (maxStr != null) {
            int max = RuleEvaluator.parseCoordinate(maxStr, spawn);
            return pos <= max;
        }
        return true;
    }

    private static int parseCoordinate(String val, int spawn) {
        try {
            val = val.replace(" ", "");
            if (val.contains("spawn")) {
                String offsetStr = val.replace("spawn", "");
                int offset = 0;
                if (!offsetStr.isEmpty()) {
                    offset = Integer.parseInt(offsetStr);
                }
                return spawn + offset;
            }
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            Constants.LOG.error("Invalid coordinate value in rule: {}", val);
            return Integer.MIN_VALUE;
        }
    }
}