package com.evandouard.hxh.gametest;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.nen.NenMobAI;
import com.evandouard.hxh.nen.NenModes;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Comportement des mobs à Nen : techniques en combat, riposte des passifs, soin, persistance, puissance.
 */
@GameTestHolder(HxHMod.MODID)
@PrefixGameTestTemplate(false)
public final class NenMobTests {

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void nenMobUsesItsTechniques(GameTestHelper helper) {
        // Un husk avec le Hatsu de Gon (IA active) face à un husk immobile qu'il cible
        Zombie target = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(2, 1, 1));
        Zombie gon = helper.spawn(EntityType.HUSK, new BlockPos(0, 1, 1));
        HunterData d = HunterStats.get(gon);
        d.setNenType(NenType.ENHANCER);
        NenModes.unlock(gon);
        d.setNenRevealed(true);
        d.setKit("gon");
        gon.setTarget(target);
        float aura = d.aura();
        helper.succeedWhen(() -> helper.assertTrue(d.readyAt("gon_rock") > 0 || d.readyAt("gon_scissors") > 0
                        || d.readyAt("gon_paper") > 0,
                "Le mob Gon aurait dû lancer une technique (aura " + d.aura() + "/" + aura + ")"));
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void passiveMobBecomesNeutral(GameTestHelper helper) {
        Zombie attacker = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(2, 1, 1));
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 1));
        NenModes.unlock(pig);
        float before = attacker.getHealth();
        pig.hurt(helper.getLevel().damageSources().mobAttack(attacker), 1f);
        helper.succeedWhen(() -> {
            helper.assertTrue(pig.getTarget() == attacker, "Le cochon à Nen doit cibler son agresseur");
            helper.assertTrue(attacker.getHealth() < before, "Le cochon à Nen doit riposter");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void nenMobHealsAndPersists(GameTestHelper helper) {
        Zombie z = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(1, 1, 1));
        z.setNoGravity(true);
        NenModes.unlock(z);
        helper.assertTrue(z.isPersistenceRequired(), "Un mob à Nen ne doit plus disparaître");
        HunterData d = HunterStats.get(z);
        for (com.evandouard.hxh.data.Stat s : com.evandouard.hxh.data.Stat.values()) {
            d.setLevel(s, com.evandouard.hxh.HxHConfig.MOB_HEAL_MIN_LEVEL.get()); // assez fort pour savoir se soigner
        }
        HunterStats.applyAttributes(z);
        float aura = d.aura();
        z.setHealth(4f);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(z.getHealth() > 4f, "Le mob aurait dû se soigner avec le Nen");
            helper.assertTrue(d.aura() < aura, "Le soin aurait dû consommer de l'aura");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void weakNenMobDoesNotHeal(GameTestHelper helper) {
        Zombie z = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(1, 1, 1));
        z.setNoGravity(true);
        NenModes.unlock(z); // monde neuf : niveau 0
        z.setHealth(4f);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(z.getHealth() <= 4f, "Un mob à Nen de bas niveau ne doit pas se soigner");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void mobPowerGrowsWithPlayTime(GameTestHelper helper) {
        RandomSource r = RandomSource.create(7);
        double sumNew = 0, sum100 = 0;
        int max = 0;
        for (int i = 0; i < 2000; i++) {
            sumNew += NenMobAI.rollLevel(0.0, r);
            int l = NenMobAI.rollLevel(100.0, r);
            sum100 += l;
            max = Math.max(max, NenMobAI.rollLevel(1000.0, r));
        }
        helper.assertTrue(sumNew == 0, "Monde neuf : les mobs à Nen commencent au niveau 0");
        double avg100 = sum100 / 2000;
        helper.assertTrue(avg100 > 40 && avg100 < 75, "Après 100 h, niveau moyen attendu entre 40 et 75 : " + avg100);
        helper.assertTrue(max <= NenMobAI.MOB_MAX_LEVEL && max > 80,
                "Mobs très forts possibles mais toujours sous le max joueur : max " + max);
        helper.succeed();
    }
}
