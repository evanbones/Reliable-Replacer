package com.evandev.reliable_replacer.test;

import com.evandev.reliable_replacer.data.ReplacementRule;
import com.evandev.reliable_replacer.logic.RuleEvaluator;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

public class WildcardTest {

    @GameTest(template = "minecraft:empty_3x3x3", batch = "rr_wildcard")
    public void testNamespaceWildcard(GameTestHelper helper) {
        ReplacementRule rule = new ReplacementRule();
        rule.inputs.add("minecraft:*");
        rule.output = "minecraft:gold_block";
        rule.resolveBlocks();

        MockReplacementContext ctx = new MockReplacementContext();

        if (!RuleEvaluator.checkRule(rule, Blocks.STONE.defaultBlockState(), ctx)) {
            helper.fail("Wildcard 'minecraft:*' failed to match minecraft:stone");
        }
        if (!RuleEvaluator.checkRule(rule, Blocks.DIRT.defaultBlockState(), ctx)) {
            helper.fail("Wildcard 'minecraft:*' failed to match minecraft:dirt");
        }

        if (rule.getInputBlocks().isEmpty()) {
            helper.fail("Wildcard resolution resulted in empty input list");
        }

        helper.succeed();
    }
}