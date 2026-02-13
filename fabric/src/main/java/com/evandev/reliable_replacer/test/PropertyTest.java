package com.evandev.reliable_replacer.test;

import com.evandev.reliable_replacer.data.ReplacementResult;
import com.evandev.reliable_replacer.data.ReplacementRule;
import com.evandev.reliable_replacer.logic.RuleManager;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;

import java.util.Collections;
import java.util.List;

public class PropertyTest {

    @GameTest(template = "minecraft:empty_3x3x3", batch = "rr_properties")
    public void testPropertyTransfer(GameTestHelper helper) {
        ReplacementRule rule = new ReplacementRule();
        rule.inputs.add("minecraft:oak_stairs");
        rule.output = "minecraft:stone_stairs";
        rule.keepStates = true;

        injectSingleRule(rule);

        BlockState inputState = Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.WEST)
                .setValue(StairBlock.HALF, Half.TOP);

        MockReplacementContext ctx = new MockReplacementContext();

        ReplacementResult result = RuleManager.getReplacementResult(inputState, ctx, true);
        if (result == null) helper.fail("Rule failed to match valid input");

        BlockState output = result.state();
        if (!output.is(Blocks.STONE_STAIRS)) helper.fail("Output block was correct type");

        if (output.getValue(StairBlock.FACING) != Direction.WEST)
            helper.fail("Facing property was not preserved");

        if (output.getValue(StairBlock.HALF) != Half.TOP)
            helper.fail("Half property was not preserved");

        cleanup();
        helper.succeed();
    }

    @GameTest(template = "minecraft:empty_3x3x3", batch = "rr_properties")
    public void testPropertyMismatchSafety(GameTestHelper helper) {
        ReplacementRule rule = new ReplacementRule();
        rule.inputs.add("minecraft:oak_stairs");
        rule.output = "minecraft:stone";
        rule.keepStates = true;

        injectSingleRule(rule);

        BlockState inputState = Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.EAST);

        MockReplacementContext ctx = new MockReplacementContext();

        try {
            ReplacementResult result = RuleManager.getReplacementResult(inputState, ctx, true);
            if (result == null || !result.state().is(Blocks.STONE)) {
                helper.fail("Failed to convert complex state to simple state");
            }
        } catch (Exception e) {
            helper.fail("Crash detected when transferring properties to incompatible block: " + e.getMessage());
        }

        cleanup();
        helper.succeed();
    }

    @GameTest(template = "minecraft:empty_3x3x3", batch = "rr_properties")
    public void testOutputStateEnforcement(GameTestHelper helper) {
        ReplacementRule rule = new ReplacementRule();
        rule.inputs.add("minecraft:stone");
        rule.output = "minecraft:oak_log";
        rule.outputStateProperties.put("axis", "y");

        injectSingleRule(rule);

        MockReplacementContext ctx = new MockReplacementContext();
        ReplacementResult result = RuleManager.getReplacementResult(Blocks.STONE.defaultBlockState(), ctx, true);

        if (result == null) helper.fail("Rule failed to match");

        if (result.state().getValue(BlockStateProperties.AXIS) != Direction.Axis.Y) {
            helper.fail("Output state property (axis=y) was not enforced");
        }

        cleanup();
        helper.succeed();
    }

    @GameTest(template = "minecraft:empty_3x3x3", batch = "rr_properties")
    public void testRandomizeProperties(GameTestHelper helper) {
        ReplacementRule rule = new ReplacementRule();
        rule.inputs.add("minecraft:stone");
        rule.output = "minecraft:furnace";
        rule.randomizeProperties.add("facing");

        injectSingleRule(rule);

        MockReplacementContext ctx = new MockReplacementContext();

        ctx.setPos(100, 64, 100);
        ReplacementResult result = RuleManager.getReplacementResult(Blocks.STONE.defaultBlockState(), ctx, true);

        if (result == null) helper.fail("Rule failed to match");

        if (!result.state().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            helper.fail("Output state missing randomized property");
        }

        cleanup();
        helper.succeed();
    }

    private void injectSingleRule(ReplacementRule rule) {
        rule.resolveBlocks();
        RuleManager.RULES_BY_BLOCK = Collections.singletonMap(
                rule.getInputBlocks().iterator().next(),
                List.of(rule)
        );
        RuleManager.HAS_LIVE_RULES = true;
    }

    private void cleanup() {
        RuleManager.RULES_BY_BLOCK = Collections.emptyMap();
        RuleManager.HAS_LIVE_RULES = false;
    }
}
