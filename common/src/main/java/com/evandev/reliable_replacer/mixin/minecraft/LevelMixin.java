package com.evandev.reliable_replacer.mixin.minecraft;

import com.evandev.reliable_replacer.config.ModConfig;
import com.evandev.reliable_replacer.config.RuleManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {

    @Unique
    private static final ThreadLocal<Boolean> reliableReplacer$isReplacing = ThreadLocal.withInitial(() -> false);

    @Shadow
    public abstract boolean setBlock(BlockPos pos, BlockState state, int flags);

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z", at = @At("HEAD"), cancellable = true)
    private void onSetBlock(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<Boolean> cir) {
        Level level = (Level) (Object) this;

        if (level.isClientSide) {
            return;
        }

        if (!ModConfig.get().enabled) {
            return;
        }

        if (reliableReplacer$isReplacing.get()) {
            return;
        }

        BlockState replacement = RuleManager.getReplacement(state, pos, level, false);

        if (replacement != state) {
            reliableReplacer$isReplacing.set(true);
            try {
                cir.setReturnValue(this.setBlock(pos, replacement, flags));
            } finally {
                reliableReplacer$isReplacing.set(false);
            }
        }
    }
}