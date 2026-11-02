package com.evandouard.hxh.gametest;

import com.evandouard.hxh.HxHConfig;
import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.NenMode;
import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.nen.NenAwakening;
import com.evandouard.hxh.nen.NenHealing;
import com.evandouard.hxh.nen.NenModes;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Tests du soin par le Nen et des trois voies d'éveil (maître, méditation, accident).
 */
@GameTestHolder(HxHMod.MODID)
@PrefixGameTestTemplate(false)
public final class AwakeningAndHealingTests {
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    /** Des « husks » : des zombies qui ne brûlent pas au soleil (le feu fausserait les tests). */
    private static Zombie zombie(GameTestHelper helper) {
        Zombie z = helper.spawnWithNoFreeWill(EntityType.HUSK, POS);
        z.setNoGravity(true);
        return z;
    }

    // ---------- Soin ----------

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void healingTurnsAuraIntoHealth(GameTestHelper helper) {
        Zombie z = zombie(helper);
        NenModes.unlock(z);
        HunterData data = HunterStats.get(z);
        data.setHealUnlocked(true);
        z.setHealth(10);
        float auraBefore = data.aura();
        helper.assertTrue(NenHealing.setHealing(z, true) == null, "Le soin aurait dû démarrer");
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(z.getHealth() > 11, "La vie aurait dû remonter, obtenue " + z.getHealth());
            helper.assertTrue(data.aura() < auraBefore, "L'aura aurait dû baisser : " + data.aura());
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void healingNeedsUnlockAndNoZetsu(GameTestHelper helper) {
        Zombie z = zombie(helper);
        helper.assertTrue(NenHealing.setHealing(z, true) != null, "Sans Nen : refus");
        NenModes.unlock(z);
        helper.assertTrue(NenHealing.setHealing(z, true) != null, "Sans maîtrise du soin : refus");
        HunterStats.get(z).setHealUnlocked(true);
        NenModes.trySetMode(z, NenMode.ZETSU);
        helper.assertTrue(NenHealing.setHealing(z, true) != null, "En Zetsu : refus");
        NenModes.trySetMode(z, NenMode.TEN);
        helper.assertTrue(NenHealing.setHealing(z, true) == null, "En Ten : accepté");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void healingMasteryIsEfficientButSlowToLevel(GameTestHelper helper) {
        helper.assertTrue(NenHealing.healPerSecond(100) > NenHealing.healPerSecond(0) * 5,
                "Au niveau 100, le soin doit être bien plus rapide");
        helper.assertTrue(NenHealing.auraPerHealth(100) < NenHealing.auraPerHealth(0),
                "Au niveau 100, le soin doit coûter moins cher");
        for (int level : new int[]{0, 10, 50}) {
            helper.assertTrue(HunterData.healXpToNext(level) >= 3 * Stat.xpToNext(level),
                    "L'XP de soin doit être au moins 3x celle d'une stat (niveau " + level + ")");
        }
        HunterData data = new HunterData();
        helper.assertTrue(data.addHealXp(HunterData.healXpToNext(0)) == 1 && data.healLevel() == 1,
                "Passage au niveau 1 de soin");
        helper.succeed();
    }

    // ---------- Éveil ----------

    @GameTest(template = "empty")
    public static void masterOpensPores(GameTestHelper helper) {
        Zombie master = zombie(helper);
        Zombie student = zombie(helper);
        Zombie other = zombie(helper);

        helper.assertTrue(NenAwakening.openPores(master, student) != null, "Un maître sans Nen ne peut rien ouvrir");
        NenModes.unlock(master);
        float masterAura = HunterStats.get(master).aura();

        helper.assertTrue(NenAwakening.openPores(master, student) == null, "L'ouverture aurait dû réussir");
        HunterData s = HunterStats.get(student);
        helper.assertTrue(s.nenUnlocked(), "L'élève doit avoir le Nen");
        helper.assertTrue(s.aura() == 0, "Éveil forcé : l'aura de l'élève s'échappe (0)");
        helper.assertTrue(student.hasEffect(MobEffects.WEAKNESS), "Éveil forcé : l'élève est affaibli");
        helper.assertTrue(HunterStats.get(master).aura() <= masterAura - HxHConfig.OPEN_PORES_AURA_COST.get() + 0.01,
                "Le maître doit avoir dépensé de l'aura");
        helper.assertTrue(NenAwakening.openPores(master, other) != null, "Temps de recharge : refus");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void meditationAwakens(GameTestHelper helper) {
        Zombie monk = zombie(helper);
        HunterData data = HunterStats.get(monk);
        data.setMeditationTicks(HxHConfig.MEDITATION_SECONDS_TO_AWAKEN.get() * 20 - 10);
        NenAwakening.toggleMeditation(monk);
        helper.assertTrue(data.meditating(), "La méditation aurait dû commencer");
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(data.nenUnlocked(), "La méditation aurait dû éveiller le Nen");
            helper.assertTrue(!data.meditating(), "La méditation s'arrête à l'éveil");
            helper.assertTrue(data.aura() > 0, "Éveil par méditation : l'aura n'est pas perdue");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void nenMobCanAwakenByAccident(GameTestHelper helper) {
        Zombie attacker = zombie(helper);
        Zombie victim = zombie(helper);
        RandomSource random = RandomSource.create(42);

        for (int i = 0; i < 200; i++) {
            helper.assertTrue(!NenAwakening.tryAccidentalAwakening(attacker, victim, random),
                    "Un attaquant sans Nen ne doit jamais éveiller");
        }
        NenModes.unlock(attacker);
        int tries = 0;
        while (!HunterStats.get(victim).nenUnlocked() && tries < 2000) {
            NenAwakening.tryAccidentalAwakening(attacker, victim, random);
            tries++;
        }
        helper.assertTrue(HunterStats.get(victim).nenUnlocked(), "Aucun éveil en 2000 coups");
        helper.succeed();
    }
}
