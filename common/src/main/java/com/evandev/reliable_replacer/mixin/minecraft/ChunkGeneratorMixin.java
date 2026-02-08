package com.evandev.reliable_replacer.mixin.minecraft;

import com.evandev.reliable_replacer.util.WorldgenHandler;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorMixin {

    @Inject(method = "applyBiomeDecoration", at = @At("RETURN"))
    private void reliableReplacer$onApplyDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager, CallbackInfo ci) {
        WorldgenHandler.processChunk(level, chunk);
    }
}