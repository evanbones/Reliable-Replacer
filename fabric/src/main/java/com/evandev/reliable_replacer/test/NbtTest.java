package com.evandev.reliable_replacer.test;

import com.evandev.reliable_replacer.data.ReplacementRule;
import com.evandev.reliable_replacer.logic.RuleEvaluator;
import com.evandev.reliable_replacer.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;

public class NbtTest {

    @GameTest(template = "minecraft:empty_3x3x3", batch = "rr_nbt")
    public void testNbtPreservation(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);

        helper.setBlock(pos, Blocks.CHEST);
        RandomizableContainerBlockEntity chest = (RandomizableContainerBlockEntity) helper.getBlockEntity(pos);
        chest.setItem(0, new ItemStack(Items.DIAMOND));

        boolean success = BlockUtil.swapBlockWithNbt(
                helper.getLevel(),
                helper.absolutePos(pos),
                Blocks.BARREL.defaultBlockState(),
                true,
                null,
                3
        );

        if (!success) helper.fail("Block swap returned false");
        helper.assertBlockState(pos, state -> state.is(Blocks.BARREL), () -> "Block did not change to Barrel");

        RandomizableContainerBlockEntity barrel = (RandomizableContainerBlockEntity) helper.getBlockEntity(pos);
        if (barrel.getItem(0).getItem() != Items.DIAMOND) {
            helper.fail("NBT data (Inventory) was lost during swap");
        }

        helper.succeed();
    }

    @GameTest(template = "minecraft:empty_3x3x3", batch = "rr_nbt")
    public void testCustomNbtInjection(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);

        helper.setBlock(pos, Blocks.STONE);

        CompoundTag customNbt = new CompoundTag();
        customNbt.putString("CustomName", "{\"text\":\"Injected Barrel\"}");

        boolean success = BlockUtil.swapBlockWithNbt(
                helper.getLevel(),
                helper.absolutePos(pos),
                Blocks.BARREL.defaultBlockState(),
                false,
                customNbt,
                3
        );

        if (!success) helper.fail("Block swap returned false");
        helper.assertBlockState(pos, state -> state.is(Blocks.BARREL), () -> "Block did not change to Barrel");

        RandomizableContainerBlockEntity barrel = (RandomizableContainerBlockEntity) helper.getBlockEntity(pos);
        if (barrel == null || !barrel.hasCustomName() || !barrel.getCustomName().getString().equals("Injected Barrel")) {
            helper.fail("Custom NBT data (CustomName) was not injected during swap");
        }

        helper.succeed();
    }

    @GameTest(template = "minecraft:empty_3x3x3", batch = "rr_nbt")
    public void testInputNbtFiltering(GameTestHelper helper) {
        ReplacementRule rule = new ReplacementRule();
        rule.inputs.add("minecraft:chest");
        rule.output = "minecraft:barrel";
        rule.inputNbt = "{Items:[{id:\"minecraft:diamond\"}]}";
        rule.resolveBlocks();

        MockReplacementContext ctx = new MockReplacementContext();

        CompoundTag emptyChestNbt = new CompoundTag();
        ctx.setMockNbt(emptyChestNbt);
        if (RuleEvaluator.checkRule(rule, Blocks.CHEST.defaultBlockState(), ctx)) {
            helper.fail("Rule matched chest without the required NBT data");
        }

        CompoundTag validChestNbt = new CompoundTag();
        ListTag items = new ListTag();
        CompoundTag diamondItem = new CompoundTag();
        diamondItem.putString("id", "minecraft:diamond");
        items.add(diamondItem);
        validChestNbt.put("Items", items);

        ctx.setMockNbt(validChestNbt);
        if (!RuleEvaluator.checkRule(rule, Blocks.CHEST.defaultBlockState(), ctx)) {
            helper.fail("Rule failed to match chest with the correct NBT data");
        }

        helper.succeed();
    }
}