package com.evandev.reliable_replacer.mixin.minecraft;

import com.evandev.reliable_replacer.data.ReplacementResult;
import com.evandev.reliable_replacer.logic.RuleManager;
import com.evandev.reliable_replacer.logic.impl.LiveReplacementContext;
import com.evandev.reliable_replacer.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RandomizableContainerBlockEntity.class)
public class RandomizableContainerBlockEntityMixin {

    @Inject(method = "unpackLootTable", at = @At("RETURN"))
    private void reliableReplacer$onLootUnpacked(Player player, CallbackInfo ci) {
        RandomizableContainerBlockEntity be = (RandomizableContainerBlockEntity) (Object) this;
        Level level = be.getLevel();

        if (level == null || level.isClientSide()) return;

        BlockPos pos = be.getBlockPos();
        BlockState state = be.getBlockState();
        LevelData levelData = level.getLevelData();
        BlockPos spawnPos = new BlockPos(levelData.getXSpawn(), levelData.getYSpawn(), levelData.getZSpawn());

        LiveReplacementContext ctx = new LiveReplacementContext(level, pos, spawnPos, false, null, null);
        ReplacementResult result = RuleManager.getReplacementResult(state, ctx, false);

        if (result != null && result.itemReplacements() != null && !result.itemReplacements().isEmpty()) {
            CompoundTag tag = be.saveWithoutMetadata();

            BlockUtil.applyItemReplacements(tag, result.itemReplacements());

            be.load(tag);
            be.setChanged();
        }
    }
}