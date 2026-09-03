package com.evandev.reliable_replacer.test;

import com.evandev.reliable_replacer.data.ReplacementRule;
import com.evandev.reliable_replacer.logic.RuleEvaluator;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;
import java.util.Set;

public class LogicTestSuite {

    @GameTest(template = "minecraft:empty_3x3x3", batch = "rr_logic")
    public void testStructureFilter(GameTestHelper helper) {
        ReplacementRule rule = new ReplacementRule();
        rule.structures = Set.of("minecraft:jungle_pyramid");
        rule.resolveBlocks();

        MockReplacementContext ctx = new MockReplacementContext();

        boolean matches = RuleEvaluator.checkRule(rule, Blocks.STONE.defaultBlockState(), ctx);
        if (matches) helper.fail("Rule matched without structure present");

        ctx.setStructure("minecraft:jungle_pyramid");
        if (!RuleEvaluator.checkRule(rule, Blocks.STONE.defaultBlockState(), ctx)) {
            helper.fail("Rule failed despite correct structure");
        }

        helper.succeed();
    }

    @GameTest(template = "minecraft:empty_3x3x3", batch = "rr_logic")
    public void testBiomeAndDimension(GameTestHelper helper) {
        ReplacementRule rule = new ReplacementRule();
        rule.biomes = Set.of("minecraft:desert");
        rule.dimensions = Set.of("minecraft:overworld");
        rule.resolveBlocks();

        MockReplacementContext ctx = new MockReplacementContext();

        ctx.setDimension("minecraft:overworld").setBiome("minecraft:plains");
        if (RuleEvaluator.checkRule(rule, Blocks.SAND.defaultBlockState(), ctx))
            helper.fail("Rule matched wrong biome");

        ctx.setDimension("minecraft:the_nether").setBiome("minecraft:desert");
        if (RuleEvaluator.checkRule(rule, Blocks.SAND.defaultBlockState(), ctx))
            helper.fail("Rule matched wrong dimension");

        ctx.setDimension("minecraft:overworld").setBiome("minecraft:desert");
        if (!RuleEvaluator.checkRule(rule, Blocks.SAND.defaultBlockState(), ctx))
            helper.fail("Rule failed correct biome/dimension");

        helper.succeed();
    }

    @GameTest(template = "minecraft:empty_3x3x3", batch = "rr_logic")
    public void testCoordinates(GameTestHelper helper) {
        ReplacementRule rule = new ReplacementRule();
        rule.minY = "60";
        rule.maxY = "70";
        rule.minX = "spawn-10";
        rule.resolveBlocks();

        MockReplacementContext ctx = new MockReplacementContext().setSpawn(100, 64, 100);

        if (RuleEvaluator.checkRule(rule, Blocks.STONE.defaultBlockState(), ctx.setPos(95, 59, 100)))
            helper.fail("Rule matched below minY");

        if (RuleEvaluator.checkRule(rule, Blocks.STONE.defaultBlockState(), ctx.setPos(89, 65, 100)))
            helper.fail("Rule matched outside relative minX");

        if (!RuleEvaluator.checkRule(rule, Blocks.STONE.defaultBlockState(), ctx.setPos(95, 65, 100)))
            helper.fail("Rule failed valid coordinates");

        helper.succeed();
    }

    @GameTest(template = "minecraft:empty_3x3x3", batch = "rr_logic")
    public void testNeighbors(GameTestHelper helper) {
        ReplacementRule rule = new ReplacementRule();
        rule.neighbors.put("up", List.of("minecraft:water"));
        rule.resolveBlocks();

        MockReplacementContext ctx = new MockReplacementContext();

        if (RuleEvaluator.checkRule(rule, Blocks.DIRT.defaultBlockState(), ctx))
            helper.fail("Rule matched missing neighbor");

        ctx.setNeighbor(Direction.UP, Blocks.WATER.defaultBlockState());
        if (!RuleEvaluator.checkRule(rule, Blocks.DIRT.defaultBlockState(), ctx))
            helper.fail("Rule failed matching neighbor");

        helper.succeed();
    }

    @GameTest(template = "minecraft:empty_3x3x3", batch = "rr_logic")
    public void testInputStateProperties(GameTestHelper helper) {
        ReplacementRule rule = new ReplacementRule();
        rule.inputs.add("minecraft:oak_log");
        rule.stateProperties.put("axis", "x");
        rule.resolveBlocks();

        MockReplacementContext ctx = new MockReplacementContext();
        BlockState logY = Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y);
        BlockState logX = Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X);

        if (RuleEvaluator.checkRule(rule, logY, ctx)) helper.fail("Rule matched wrong axis (Y)");
        if (!RuleEvaluator.checkRule(rule, logX, ctx)) helper.fail("Rule failed correct axis (X)");

        helper.succeed();
    }

    @GameTest(template = "minecraft:empty_3x3x3", batch = "rr_logic")
    public void testExclusionLogic(GameTestHelper helper) {
        ReplacementRule rule = new ReplacementRule();
        ReplacementRule notRule = new ReplacementRule();
        notRule.biomes = Set.of("minecraft:plains");

        rule.not = notRule;
        rule.resolveBlocks();

        MockReplacementContext ctx = new MockReplacementContext();

        ctx.setBiome("minecraft:plains");
        if (RuleEvaluator.checkRule(rule.not, Blocks.STONE.defaultBlockState(), ctx)) {
            boolean notMatch = RuleEvaluator.checkRule(rule.not, Blocks.STONE.defaultBlockState(), ctx);

            if (!notMatch) {
                helper.fail("The 'not' rule failed to match the exclusion context");
            }
        }

        ctx.setBiome("minecraft:desert");
        if (RuleEvaluator.checkRule(rule.not, Blocks.STONE.defaultBlockState(), ctx)) {
            helper.fail("Not rule matched incorrectly");
        }

        helper.succeed();
    }

    @GameTest(template = "minecraft:empty_3x3x3", batch = "rr_logic")
    public void testStructureExclusion(GameTestHelper helper) {
        ReplacementRule rule = new ReplacementRule();
        rule.inputs = Set.of("minecraft:sculk_shrieker");

        ReplacementRule notRule = new ReplacementRule();
        notRule.structures = Set.of("minecraft:ancient_city");

        rule.not = notRule;
        rule.resolveBlocks();

        BlockState shrieker = Blocks.SCULK_SHRIEKER.defaultBlockState();

        MockReplacementContext outside = new MockReplacementContext();
        if (!RuleEvaluator.checkRule(rule, shrieker, outside))
            helper.fail("Rule failed outside the excluded structure");
        if (RuleEvaluator.checkRule(rule.not, shrieker, outside))
            helper.fail("Exclusion matched outside the structure");

        MockReplacementContext inside = new MockReplacementContext().setStructure("minecraft:ancient_city");
        if (!RuleEvaluator.checkRule(rule.not, shrieker, inside))
            helper.fail("Exclusion failed to match inside the structure");

        helper.succeed();
    }

    @GameTest(template = "minecraft:empty_3x3x3", batch = "rr_logic")
    public void testNeighborLists(GameTestHelper helper) {
        ReplacementRule rule = new ReplacementRule();
        rule.neighbors.put("down", List.of("minecraft:grass_block", "minecraft:dirt"));
        rule.resolveBlocks();

        MockReplacementContext ctx = new MockReplacementContext();
        BlockState stone = Blocks.STONE.defaultBlockState();

        ctx.setNeighbor(Direction.DOWN, Blocks.STONE.defaultBlockState());
        if (RuleEvaluator.checkRule(rule, stone, ctx)) helper.fail("Rule matched a block outside the neighbor list");

        ctx.setNeighbor(Direction.DOWN, Blocks.DIRT.defaultBlockState());
        if (!RuleEvaluator.checkRule(rule, stone, ctx)) helper.fail("Rule failed a block inside the neighbor list");

        helper.succeed();
    }

    @GameTest(template = "minecraft:empty_3x3x3", batch = "rr_logic")
    public void testNeighborTagsAndKeywords(GameTestHelper helper) {
        ReplacementRule tagRule = new ReplacementRule();
        tagRule.neighbors.put("up", List.of("#minecraft:logs"));
        tagRule.resolveBlocks();

        MockReplacementContext ctx = new MockReplacementContext();
        BlockState stone = Blocks.STONE.defaultBlockState();

        ctx.setNeighbor(Direction.UP, Blocks.STONE.defaultBlockState());
        if (RuleEvaluator.checkRule(tagRule, stone, ctx)) helper.fail("Tag neighbor matched a block outside the tag");

        ctx.setNeighbor(Direction.UP, Blocks.OAK_LOG.defaultBlockState());
        if (!RuleEvaluator.checkRule(tagRule, stone, ctx)) helper.fail("Tag neighbor failed a block inside the tag");

        ReplacementRule anyRule = new ReplacementRule();
        anyRule.neighbors.put("any", List.of("minecraft:water"));
        anyRule.resolveBlocks();

        MockReplacementContext anyCtx = new MockReplacementContext();
        if (RuleEvaluator.checkRule(anyRule, stone, anyCtx)) helper.fail("'any' matched with no water neighbor");

        anyCtx.setNeighbor(Direction.NORTH, Blocks.WATER.defaultBlockState());
        if (!RuleEvaluator.checkRule(anyRule, stone, anyCtx)) helper.fail("'any' failed with one water neighbor");

        ReplacementRule allRule = new ReplacementRule();
        allRule.neighbors.put("all_horizontal", List.of("minecraft:water"));
        allRule.resolveBlocks();

        MockReplacementContext allCtx = new MockReplacementContext();
        allCtx.setNeighbor(Direction.NORTH, Blocks.WATER.defaultBlockState());
        if (RuleEvaluator.checkRule(allRule, stone, allCtx))
            helper.fail("'all_horizontal' matched with one water neighbor");

        for (Direction d : Direction.Plane.HORIZONTAL) {
            allCtx.setNeighbor(d, Blocks.WATER.defaultBlockState());
        }
        if (!RuleEvaluator.checkRule(allRule, stone, allCtx))
            helper.fail("'all_horizontal' failed with all water neighbors");

        helper.succeed();
    }

    @GameTest(template = "minecraft:empty_3x3x3", batch = "rr_logic")
    public void testSingularFilterAliases(GameTestHelper helper) {
        ReplacementRule rule = new ReplacementRule();
        rule.biome = "minecraft:desert";
        rule.resolveBlocks();

        BlockState stone = Blocks.STONE.defaultBlockState();

        if (RuleEvaluator.checkRule(rule, stone, new MockReplacementContext().setBiome("minecraft:plains")))
            helper.fail("Singular biome alias matched the wrong biome");
        if (!RuleEvaluator.checkRule(rule, stone, new MockReplacementContext().setBiome("minecraft:desert")))
            helper.fail("Singular biome alias failed the correct biome");

        helper.succeed();
    }
}
