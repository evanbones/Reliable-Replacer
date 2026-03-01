package com.evandev.reliable_replacer.mixin.minecraft;

import com.evandev.reliable_replacer.config.ModConfig;
import com.evandev.reliable_replacer.data.ReplacementResult;
import com.evandev.reliable_replacer.logic.RuleManager;
import com.evandev.reliable_replacer.logic.impl.LiveReplacementContext;
import com.evandev.reliable_replacer.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(BlockItem.class)
public class BlockItemMixin {

    @Inject(method = "place", at = @At("HEAD"))
    private void reliableReplacer$onPlaceStart(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        RuleManager.LIVE_PLACEMENT_QUEUE.set(new ArrayList<>());
    }

    @Inject(method = "place", at = @At("RETURN"))
    private void reliableReplacer$onBlockPlaced(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        List<BlockPos> positions = RuleManager.LIVE_PLACEMENT_QUEUE.get();
        RuleManager.LIVE_PLACEMENT_QUEUE.remove();

        if (cir.getReturnValue() != InteractionResult.FAIL && RuleManager.HAS_LIVE_RULES && ModConfig.get().enabled) {
            Level level = context.getLevel();
            LevelData levelData = level.getLevelData();
            BlockPos spawnPos = new BlockPos(levelData.getSpawnPos());

            if (positions != null && !positions.isEmpty()) {
                for (BlockPos pos : positions) {
                    BlockState state = level.getBlockState(pos);
                    LiveReplacementContext ctx = new LiveReplacementContext(level, pos, spawnPos, false, null, null);
                    ReplacementResult result = RuleManager.getReplacementResult(state, ctx, true);

                    if (result != null) {
                        BlockState replacement = result.state();
                        boolean hasAdditionalBlocks = result.additionalBlocks() != null && !result.additionalBlocks().isEmpty();
                        boolean hasCustomNbt = result.customNbt() != null;
                        boolean hasItemReplacements = result.itemReplacements() != null && !result.itemReplacements().isEmpty();

                        if (!replacement.equals(state) || hasAdditionalBlocks || hasCustomNbt || hasItemReplacements) {
                            if (!replacement.equals(state) || hasCustomNbt || hasItemReplacements) {
                                BlockUtil.swapBlockWithNbt(level, pos, replacement, result.keepNbt(), result.customNbt(), result.itemReplacements(), 3);
                            }

                            if (hasAdditionalBlocks) {
                                for (var entry : result.additionalBlocks().entrySet()) {
                                    BlockPos addPos = entry.getKey();
                                    CompoundTag addNbt = result.additionalNbt().get(addPos);
                                    BlockUtil.swapBlockWithNbt(level, addPos, entry.getValue(), false, addNbt, null, 3);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}