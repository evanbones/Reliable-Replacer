package com.evandev.reliable_replacer.test;

import com.evandev.reliable_replacer.data.ReplacementRule;
import com.evandev.reliable_replacer.logic.RuleManager;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

import java.util.Collections;
import java.util.List;

public class ContextTest {

    @GameTest(template = "minecraft:empty_3x3x3", batch = "rr_context")
    public void testRetrogenOnlyRule(GameTestHelper helper) {
        ReplacementRule rule = new ReplacementRule();
        rule.inputs.add("minecraft:stone");
        rule.output = "minecraft:glass";
        rule.retrogen = true;
        rule.playerBlocks = false;

        injectSingleRule(rule);
        MockReplacementContext ctx = new MockReplacementContext();

        if (RuleManager.getReplacementResult(Blocks.STONE.defaultBlockState(), ctx.setRetrogen(false), true) != null) {
            helper.fail("Retrogen-only rule triggered during player placement");
        }

        if (RuleManager.getReplacementResult(Blocks.STONE.defaultBlockState(), ctx.setRetrogen(true), false) == null) {
            helper.fail("Retrogen-only rule failed to trigger during retrogen");
        }

        cleanup();
        helper.succeed();
    }

    @GameTest(template = "minecraft:empty_3x3x3", batch = "rr_context")
    public void testPlayerOnlyRule(GameTestHelper helper) {
        ReplacementRule rule = new ReplacementRule();
        rule.inputs.add("minecraft:stone");
        rule.output = "minecraft:glass";
        rule.retrogen = false;
        rule.playerBlocks = true;

        injectSingleRule(rule);
        MockReplacementContext ctx = new MockReplacementContext();

        if (RuleManager.getReplacementResult(Blocks.STONE.defaultBlockState(), ctx.setRetrogen(true), false) != null) {
            helper.fail("Player-only rule triggered during retrogen");
        }

        if (RuleManager.getReplacementResult(Blocks.STONE.defaultBlockState(), ctx.setRetrogen(false), true) == null) {
            helper.fail("Player-only rule failed to trigger during player placement");
        }

        cleanup();
        helper.succeed();
    }

    private void injectSingleRule(ReplacementRule rule) {
        rule.resolveBlocks();
        RuleManager.RULES_BY_BLOCK = Collections.singletonMap(Blocks.STONE, List.of(rule));
        RuleManager.HAS_LIVE_RULES = true;
    }

    private void cleanup() {
        RuleManager.RULES_BY_BLOCK = Collections.emptyMap();
        RuleManager.HAS_LIVE_RULES = false;
    }
}