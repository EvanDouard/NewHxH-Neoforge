package com.evandouard.hxh.gametest;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.Mastery;
import com.evandouard.hxh.data.NenMode;
import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.hatsu.Hatsu;
import com.evandouard.hxh.nen.NenDefense;
import com.evandouard.hxh.nen.NenModes;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Nen avancé : niveau et XP de Nen, maîtrises plafonnées, dégâts plats par posture, Ko, Hatsu à niveau requis.
 */
@GameTestHolder(HxHMod.MODID)
@PrefixGameTestTemplate(false)
public final class NenAdvancedTests {

    private static Zombie husk(GameTestHelper helper, int x) {
        Zombie z = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(x, 1, 1));
        z.setNoGravity(true);
        return z;
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void practicingTenGivesXp(GameTestHelper helper) {
        Zombie z = husk(helper, 1);
        NenModes.unlock(z);
        HunterData d = HunterStats.get(z);
        NenModes.trySetMode(z, NenMode.TEN);
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(d.masteryXp(Mastery.TEN) > 0 || d.masteryLevel(Mastery.TEN) > 0, "Le Ten aurait dû progresser");
            helper.assertTrue(d.nenXp() > 0 || d.nenLevel() > 0, "Le Nen aurait dû progresser");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void masteryIsCappedByNenLevel(GameTestHelper helper) {
        HunterData d = new HunterData();
        d.setNenUnlocked(true);
        d.addMasteryXp(Mastery.EN, 10_000_000);
        helper.assertTrue(d.masteryLevel(Mastery.EN) == Mastery.MIN_CAP,
                "Niveau de Nen 0 : la maîtrise plafonne à " + Mastery.MIN_CAP + ", obtenu " + d.masteryLevel(Mastery.EN));
        d.setNenLevel(40);
        d.addMasteryXp(Mastery.EN, 10_000_000);
        helper.assertTrue(d.masteryLevel(Mastery.EN) == 40, "Niveau de Nen 40 : plafond à 40, obtenu " + d.masteryLevel(Mastery.EN));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void flatDamageDependsOnStance(GameTestHelper helper) {
        // Même attaque de 5 PV : Zetsu > Neutre > Ten > Ken en dégâts subis
        Zombie attacker = husk(helper, 0);
        NenMode[] stances = {NenMode.ZETSU, NenMode.NONE, NenMode.TEN, NenMode.KEN};
        float[] lost = new float[stances.length];
        for (int i = 0; i < stances.length; i++) {
            Zombie v = husk(helper, 2);
            NenModes.unlock(v);
            HunterStats.get(v).setMasteryLevel(Mastery.KEN, 50);
            NenModes.trySetMode(v, stances[i]);
            float before = v.getHealth();
            v.hurt(helper.getLevel().damageSources().mobAttack(attacker), 5f);
            lost[i] = before - v.getHealth();
        }
        helper.assertTrue(lost[0] > lost[1] && lost[1] > lost[2] && lost[2] > lost[3],
                "Dégâts attendus Zetsu > Neutre > Ten > Ken : " + java.util.Arrays.toString(lost));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void koChargesNextHitAndExposesBody(GameTestHelper helper) {
        Zombie striker = husk(helper, 0);
        NenModes.unlock(striker);
        HunterData d = HunterStats.get(striker);
        float aura = d.aura();
        helper.assertTrue(NenDefense.ko(striker) == null, "Le Ko aurait dû s'activer");
        helper.assertTrue(d.aura() < aura, "Le Ko consomme de l'aura d'un coup");
        helper.assertTrue(NenDefense.nenResistance(striker) == 0f, "Sous Ko, le corps est découvert (0 de résistance)");
        float charged = NenDefense.outgoingFactor(striker, false);
        helper.assertTrue(charged >= 2f, "Sous Ko, la prochaine attaque est au moins doublée : " + charged);

        // Le coup part : le Ko est dépensé
        Zombie victim = husk(helper, 1);
        striker.doHurtTarget(victim);
        helper.assertTrue(!NenDefense.koActive(striker), "Le Ko est dépensé après le coup");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hatsuNeedsNenLevel(GameTestHelper helper) {
        Zombie gon = husk(helper, 1);
        HunterData d = HunterStats.get(gon);
        d.setNenType(NenType.ENHANCER);
        NenModes.unlock(gon);
        d.setNenRevealed(true);
        d.setKit("gon");
        d.setNenLevel(0);
        Hatsu.cycle(gon);
        Hatsu.cycle(gon); // Papier : niveau 10 requis
        helper.assertTrue("message.hxh.hatsu_nen_level".equals(Hatsu.cast(gon)), "Papier doit être bloqué au niveau 0");
        d.setNenLevel(10);
        helper.assertTrue(!"message.hxh.hatsu_nen_level".equals(Hatsu.cast(gon)), "Au niveau 10, Papier est débloqué");
        helper.succeed();
    }
}
