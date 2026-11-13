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

    @net.minecraft.gametest.framework.GameTest(template = "empty")
    public static void missedTechniquesDamageScenery(net.minecraft.gametest.framework.GameTestHelper helper) {
        var level = helper.getLevel();
        var rule = level.getGameRules().getRule(com.evandouard.hxh.HxHGameRules.NEN_DESTRUCTION);
        boolean before = rule.get();
        rule.set(true, level.getServer());
        net.minecraft.core.BlockPos wall = new net.minecraft.core.BlockPos(4, 2, 1);
        helper.setBlock(wall, net.minecraft.world.level.block.Blocks.STONE);
        var husk = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.HUSK, new net.minecraft.core.BlockPos(1, 1, 1));
        husk.setNoGravity(true);
        husk.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(wall)));
        helper.assertTrue(com.evandouard.hxh.combat.Impacts.missedStrike(husk, 8, 5f) == 0
                && !level.getBlockState(helper.absolutePos(wall)).isAir(), "Un petit raté ne casse rien");
        int broken = com.evandouard.hxh.combat.Impacts.missedStrike(husk, 8, 20f);
        helper.assertTrue(broken >= 1 && level.getBlockState(helper.absolutePos(wall)).isAir(), "Un gros raté troue le mur (" + broken + " blocs)");
        rule.set(before, level.getServer());
        helper.succeed();
    }

    @net.minecraft.gametest.framework.GameTest(template = "empty")
    public static void perfectStrikeDigsAHalfSphere(net.minecraft.gametest.framework.GameTestHelper helper) {
        var level = helper.getLevel();
        // sol épais : 4 couches de pierre sous la surface, et un bloc posé sur le sol (ne doit pas partir)
        for (int x = 0; x <= 8; x++) for (int z = 0; z <= 8; z++) for (int y = 1; y <= 4; y++)
            helper.setBlock(new net.minecraft.core.BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.STONE);
        helper.setBlock(new net.minecraft.core.BlockPos(5, 5, 4), net.minecraft.world.level.block.Blocks.OAK_PLANKS);
        var husk = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.HUSK, new net.minecraft.core.BlockPos(4, 5, 4));
        var surface = net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new net.minecraft.core.BlockPos(4, 5, 4)));
        int broken = com.evandouard.hxh.combat.Impacts.bowl(level, surface, com.evandouard.hxh.combat.Impacts.BOWL_RADIUS, husk);
        helper.assertTrue(broken >= 30, "Cratère en demi-sphère : " + broken + " blocs");
        helper.assertTrue(!level.getBlockState(helper.absolutePos(new net.minecraft.core.BlockPos(5, 5, 4))).isAir(), "Rien au-dessus du sol ne part");
        helper.assertTrue(level.getBlockState(helper.absolutePos(new net.minecraft.core.BlockPos(4, 4, 4))).isAir()
                && level.getBlockState(helper.absolutePos(new net.minecraft.core.BlockPos(4, 2, 4))).isAir(), "Le centre est creusé en profondeur");
        helper.assertTrue(!level.getBlockState(helper.absolutePos(new net.minecraft.core.BlockPos(7, 2, 4))).isAir(), "Le bord du fond reste (forme ronde, pas une boîte)");
        helper.succeed();
    }
}
