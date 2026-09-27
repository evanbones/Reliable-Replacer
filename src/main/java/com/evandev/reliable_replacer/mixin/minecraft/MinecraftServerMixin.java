package com.evandev.reliable_replacer.mixin.minecraft;

import com.evandev.reliable_replacer.CommonClass;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(method = "runServer", at = @At("HEAD"))
    private void reliableReplacer$setServer(CallbackInfo ci) {
        CommonClass.setServer((MinecraftServer) (Object) this);
    }
}