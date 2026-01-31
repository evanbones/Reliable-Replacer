package com.evandev.reliable_replacer.mixin.minecraft;

import com.evandev.reliable_replacer.util.RetrogenHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {

    @Inject(method = "startTickingChunk", at = @At("RETURN"))
    private void reliableReplacer$onStartTicking(LevelChunk chunk, CallbackInfo ci) {
        RetrogenHandler.processChunk(chunk);
    }

    @Inject(method = "onStructureStartsAvailable", at = @At("RETURN"))
    private void reliableReplacer$onStructureLoad(ChunkAccess chunk, CallbackInfo ci) {
        if (chunk instanceof LevelChunk levelChunk) {
            RetrogenHandler.processChunk(levelChunk);
        }
    }
}