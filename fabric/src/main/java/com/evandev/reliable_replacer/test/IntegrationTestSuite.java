package com.evandev.reliable_replacer.test;

import com.evandev.reliable_replacer.config.ModConfig;
import com.evandev.reliable_replacer.logic.RuleManager;
import com.evandev.reliable_replacer.data.ReplacementRule;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.*;

public class IntegrationTestSuite {

    /**
     * Verifies that modifying a block inside a replacement doesn't cause an infinite loop.
     */
    @GameTest(template = "minecraft:empty_3x3x3", batch = "rr_integration")
    public void testRecursionSafety(GameTestHelper helper) {
        ReplacementRule ruleA = new ReplacementRule();
        ruleA.inputs.add("minecraft:stone");
        ruleA.output = "minecraft:dirt";
        ruleA.playerBlocks = true;

        ReplacementRule ruleB = new ReplacementRule();
        ruleB.inputs.add("minecraft:dirt");
        ruleB.output = "minecraft:glass";
        ruleB.playerBlocks = true;

        injectRules(Arrays.asList(ruleA, ruleB));

        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Blocks.STONE);

        helper.assertBlockState(pos, s -> s.is(Blocks.DIRT),
                () -> "Recursion protection failed! Block mutated twice in one tick."
        );

        cleanup();
        helper.succeed();
    }

    private void injectRules(List<ReplacementRule> rules) {
        ModConfig.get().enabled = true;
        Map<Block, List<ReplacementRule>> map = new IdentityHashMap<>();
        for (ReplacementRule r : rules) {
            r.resolveBlocks();
            for (Block b : r.getInputBlocks()) map.computeIfAbsent(b, k -> new ArrayList<>()).add(r);
        }
        RuleManager.RULES_BY_BLOCK = map;
        RuleManager.HAS_LIVE_RULES = true;
    }

    private void cleanup() {
        RuleManager.RULES_BY_BLOCK = Collections.emptyMap();
        RuleManager.HAS_LIVE_RULES = false;
    }
}