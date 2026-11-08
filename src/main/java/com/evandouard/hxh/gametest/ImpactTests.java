package com.evandouard.hxh.gametest;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.combat.Combat;
import com.evandouard.hxh.combat.Impacts;
import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Paliers d'impact selon la Force (en % du niveau max).
 */
@GameTestHolder(HxHMod.MODID)
@PrefixGameTestTemplate(false)
public final class ImpactTests {

    private static Zombie husk(GameTestHelper helper, int x, boolean gravity) {
        Zombie z = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(x, 1, 1));
        z.setNoGravity(!gravity);
        z.setYRot(-90f);
        z.setYHeadRot(-90f);
        z.setXRot(0f);
        return z;
    }

    private static void strength(Zombie z, int level) {
        HunterStats.get(z).setLevel(Stat.STRENGTH, level);
        HunterStats.applyAttributes(z);
    }

    @GameTest(template = "empty")
    public static void tierFollowsStrengthPercent(GameTestHelper helper) {
        helper.assertTrue(Impacts.tierFor(0.10f) == Impacts.Tier.WEAK, "10 % : faible");
        helper.assertTrue(Impacts.tierFor(0.25f) == Impacts.Tier.MEDIUM, "25 % : moyen");
        helper.assertTrue(Impacts.tierFor(0.60f) == Impacts.Tier.HEAVY, "60 % : lourd");
        helper.assertTrue(Impacts.tierFor(0.75f) == Impacts.Tier.DEVASTATING, "75 % : dévastateur");
        helper.assertTrue(Impacts.Tier.WEAK.down() == Impacts.Tier.WEAK && Impacts.Tier.HEAVY.down() == Impacts.Tier.MEDIUM,
                "Un palier de moins, sans passer sous faible");
        helper.assertTrue(Impacts.Tier.DEVASTATING.atMost(Impacts.Tier.MEDIUM) == Impacts.Tier.MEDIUM, "Plafond");
        Zombie z = husk(helper, 0, false);
        strength(z, 80);
        helper.assertTrue(Impacts.tierOf(z) == Impacts.Tier.DEVASTATING, "Force 80/100 : dévastateur");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void devastatingStrikeFreezesThenCraters(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 0, 1), Blocks.DIRT);
        Zombie a = husk(helper, 0, false);
        Zombie v = husk(helper, 2, true);
        strength(a, 100);
        helper.runAfterDelay(5, () -> {
            float before = v.getHealth();
            helper.assertTrue(Combat.chargedStrike(a, 1.0f) > 0, "La frappe parfaite touche");
            helper.assertTrue(v.getHealth() < before, "Les dégâts tombent tout de suite");
            helper.assertTrue(Impacts.isFrozen(v) && Impacts.isFrozen(a), "Image d'impact : les deux sont figés");
            helper.assertBlockNotPresent(Blocks.DIRT, new BlockPos(2, 0, 1));
            helper.runAfterDelay(Impacts.FREEZE_TICKS + 3, () -> {
                helper.assertFalse(Impacts.isFrozen(v), "La cible repart après l'image d'impact");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty")
    public static void notPerfectOrWeakDoesNotFreeze(GameTestHelper helper) {
        Zombie a = husk(helper, 0, false);
        Zombie v = husk(helper, 2, false);
        strength(a, 100);
        Combat.chargedStrike(a, 0.5f); // pas parfaite : un palier de moins (lourd)
        helper.assertFalse(Impacts.isFrozen(v), "Frappe pas parfaite : pas d'image d'impact");
        strength(a, 10);
        Combat.chargedStrike(a, 1.0f);
        helper.assertFalse(Impacts.isFrozen(v), "Force faible : pas d'image d'impact");
        helper.succeed();
    }
}
