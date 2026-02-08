package com.evandev.reliable_replacer.mixin.minecraft;

import com.evandev.reliable_replacer.config.ModConfig;
import com.evandev.reliable_replacer.util.IProcessedChunk;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(ChunkSerializer.class)
public class ChunkSerializerMixin {
    @Inject(method = "read", at = @At("HEAD"))
    private static void onReadChunk(ServerLevel level, PoiManager poiManager, RegionStorageInfo regionStorageInfo, ChunkPos pos, CompoundTag tag, CallbackInfoReturnable<ProtoChunk> cir) {
        Map<String, String> remapper = ModConfig.get().missingIdMap;
        if (remapper == null || remapper.isEmpty() || !tag.contains("sections", Tag.TAG_LIST)) return;

        ListTag sections = tag.getList("sections", Tag.TAG_COMPOUND);
        for (int i = 0; i < sections.size(); i++) {
            CompoundTag section = sections.getCompound(i);
            if (section.contains("block_states", Tag.TAG_COMPOUND)) {
                CompoundTag blockStates = section.getCompound("block_states");
                if (blockStates.contains("palette", Tag.TAG_LIST)) {
                    ListTag palette = blockStates.getList("palette", Tag.TAG_COMPOUND);

                    for (int j = 0; j < palette.size(); j++) {
                        CompoundTag entry = palette.getCompound(j);
                        String name = entry.getString("Name");
                        if (remapper.containsKey(name)) {
                            entry.putString("Name", remapper.get(name));
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "write", at = @At("RETURN"))
    private static void onWrite(ServerLevel level, ChunkAccess chunk, CallbackInfoReturnable<CompoundTag> cir) {
        if (chunk instanceof IProcessedChunk processedChunk) {
            CompoundTag tag = cir.getReturnValue();

            if (processedChunk.reliableReplacer$hasBeenProcessed()) {
                tag.putBoolean("ReliableReplacerProcessed", true);
            }
            if (processedChunk.reliableReplacer$isDirty()) {
                tag.putBoolean("ReliableReplacerDirty", true);
            }
        }
    }

    @Inject(method = "read", at = @At("RETURN"))
    private static void onReadReturn(ServerLevel level, PoiManager poiManager, RegionStorageInfo regionStorageInfo, ChunkPos pos, CompoundTag tag, CallbackInfoReturnable<ProtoChunk> cir) {
        ChunkAccess chunk = cir.getReturnValue();

        if (chunk instanceof IProcessedChunk processedChunk) {
            if (tag.contains("ReliableReplacerProcessed") && tag.getBoolean("ReliableReplacerProcessed")) {
                processedChunk.reliableReplacer$markProcessed();
            }

            if (tag.contains("ReliableReplacerDirty") && tag.getBoolean("ReliableReplacerDirty")) {
                processedChunk.reliableReplacer$setDirty(true);
            }
        }
    }
}