package com.evandev.reliable_replacer.mixin.minecraft;

import com.evandev.reliable_replacer.util.RetrogenHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    @Inject(method = "tickChunk", at = @At("HEAD"))
    private void reliableReplacer$onTickChunk(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        RetrogenHandler.processChunk(chunk);
    }
}