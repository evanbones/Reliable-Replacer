package com.evandev.reliable_replacer.test;

import com.evandev.reliable_replacer.data.ReplacementResult;
import com.evandev.reliable_replacer.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
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
        ReplacementResult result = new ReplacementResult(Blocks.BARREL.defaultBlockState(), true);

        boolean success = BlockUtil.swapBlockWithNbt(helper.getLevel(), helper.absolutePos(pos), result, 3);
        if (!success) helper.fail("Block swap returned false");
        helper.assertBlockState(pos, state -> state.is(Blocks.BARREL), () -> "Block did not change to Barrel");

        RandomizableContainerBlockEntity barrel = (RandomizableContainerBlockEntity) helper.getBlockEntity(pos);
        if (barrel.getItem(0).getItem() != Items.DIAMOND) {
            helper.fail("NBT data (Inventory) was lost during swap");
        }

        helper.succeed();
    }
}