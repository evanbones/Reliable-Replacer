package com.evandev.reliable_replacer.mixin.minecraft;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Objects;

@Mixin(StructureTemplate.class)
public class StructureTemplateMixin {

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