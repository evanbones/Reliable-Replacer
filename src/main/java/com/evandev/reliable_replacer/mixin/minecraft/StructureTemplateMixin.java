package com.evandev.reliable_replacer.mixin.minecraft;

import com.evandev.reliable_replacer.config.ModConfig;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if <1.21.2 {
/*import net.minecraft.nbt.Tag;
*///?}

import java.util.Map;
import java.util.Objects;

@Mixin(StructureTemplate.class)
public class StructureTemplateMixin {

    @Inject(method = "load", at = @At("HEAD"))
    private void reliableReplacer$remapStructurePalette(HolderGetter<Block> blockGetter, CompoundTag tag, CallbackInfo ci) {
        Map<String, String> remapper = ModConfig.get().missingIdMap;
        if (remapper == null || remapper.isEmpty()) return;

        //? if <1.21.2 {
        /*if (tag.contains("palette", Tag.TAG_LIST)) {
            reliableReplacer$remapPalette(tag.getList("palette", Tag.TAG_COMPOUND), remapper);
        }

        if (tag.contains("palettes", Tag.TAG_LIST)) {
            ListTag palettes = tag.getList("palettes", Tag.TAG_LIST);
            for (int i = 0; i < palettes.size(); i++) {
                reliableReplacer$remapPalette(palettes.getList(i), remapper);
            }
        }
        *///?} else {
        if (tag.contains("palette")) {
            reliableReplacer$remapPalette(tag.getListOrEmpty("palette"), remapper);
        }

        if (tag.contains("palettes")) {
            ListTag palettes = tag.getListOrEmpty("palettes");
            for (int i = 0; i < palettes.size(); i++) {
                palettes.getList(i).ifPresent(palette -> reliableReplacer$remapPalette(palette, remapper));
            }
        }
        //?}
    }

    @Unique
    private static void reliableReplacer$remapPalette(ListTag palette, Map<String, String> remapper) {
        for (int i = 0; i < palette.size(); i++) {
            //? if <1.21.2 {
            /*CompoundTag entry = palette.getCompound(i);
            String name = entry.getString("Name");
            *///?} else {
            CompoundTag entry = palette.getCompoundOrEmpty(i);
            String name = entry.getStringOr("Name", "");
            //?}
            if (remapper.containsKey(name)) {
                entry.putString("Name", remapper.get(name));
            }
        }
    }

    //? if <1.21.2 {
    /*@ModifyArg(
            method = "placeInWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/BlockEntity;loadWithComponents(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)V"
            ),
            index = 0
    )
    *///?} else {
    @ModifyArg(
            method = "placeInWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/storage/TagValueInput;create(Lnet/minecraft/util/ProblemReporter;Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/level/storage/ValueInput;"
            ),
            index = 2
    )
    //?}
    private CompoundTag reliableReplacer$fixNbtId(CompoundTag tag, @Local BlockEntity instance) {
        if (instance == null) return tag;

        String worldTileId = Objects.requireNonNull(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(instance.getType())).toString();
        //? if <1.21.2 {
        /*String nbtTileId = tag.getString("id");
        *///?} else {
        String nbtTileId = tag.getStringOr("id", "");
        //?}

        if (!worldTileId.equals(nbtTileId)) {
            CompoundTag newTag = tag.copy();
            newTag.putString("id", worldTileId);
            return newTag;
        }

        return tag;
    }
}
