package com.evandev.reliable_replacer.mixin.minecraft;

import com.evandev.reliable_replacer.config.ModConfig;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Objects;

@Mixin(StructureTemplate.class)
public class StructureTemplateMixin {

    @Inject(method = "load", at = @At("HEAD"))
    private void reliableReplacer$remapStructurePalette(HolderGetter<Block> blockGetter, CompoundTag tag, CallbackInfo ci) {
        Map<String, String> remapper = ModConfig.get().missingIdMap;
        if (remapper == null || remapper.isEmpty()) return;

        if (tag.contains("palette", Tag.TAG_LIST)) {
            ListTag palette = tag.getList("palette", Tag.TAG_COMPOUND);
            for (int i = 0; i < palette.size(); i++) {
                CompoundTag entry = palette.getCompound(i);
                String name = entry.getString("Name");
                if (remapper.containsKey(name)) {
                    entry.putString("Name", remapper.get(name));
                }
            }
        }

        if (tag.contains("palettes", Tag.TAG_LIST)) {
            ListTag palettes = tag.getList("palettes", Tag.TAG_LIST);
            for (int i = 0; i < palettes.size(); i++) {
                ListTag palette = palettes.getList(i);
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

    @ModifyArg(
            method = "placeInWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/BlockEntity;load(Lnet/minecraft/nbt/CompoundTag;)V"
            ),
            index = 0
    )
    private CompoundTag reliableReplacer$fixNbtId(CompoundTag tag, @Local BlockEntity instance) {
        if (instance == null) return tag;

        String worldTileId = Objects.requireNonNull(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(instance.getType())).toString();
        String nbtTileId = tag.getString("id");

        if (!worldTileId.equals(nbtTileId)) {
            CompoundTag newTag = tag.copy();
            newTag.putString("id", worldTileId);
            return newTag;
        }

        return tag;
    }
}