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
        float fresh = NenDefense.outgoingFactor(striker, false);
        d.setKoUntil(d.koUntil() - NenDefense.KO_CHARGE_TICKS); // le Ko a eu le temps de se charger
        float charged = NenDefense.outgoingFactor(striker, false);
        helper.assertTrue(charged >= 2f && fresh < charged, "Chargé, le Ko au moins double le coup : " + fresh + " → " + charged);
        helper.assertTrue("message.hxh.ko_cancelled".equals(NenDefense.ko(striker)) && !NenDefense.koActive(striker),
                "Second appui : le Ko est relâché");
        d.setAura(d.auraMax());
        helper.assertTrue(NenDefense.ko(striker) == null, "Le Ko se relance");

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

    @GameTest(template = "empty")
    public static void talentRollsMatchTheTable(GameTestHelper helper) {
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(42);
        java.util.Map<com.evandouard.hxh.data.NenTalent, Integer> counts = new java.util.EnumMap<>(com.evandouard.hxh.data.NenTalent.class);
        int n = 100_000;
        for (int i = 0; i < n; i++) counts.merge(com.evandouard.hxh.data.NenTalent.roll(random), 1, Integer::sum);
        int total = 0;
        for (com.evandouard.hxh.data.NenTalent t : com.evandouard.hxh.data.NenTalent.values()) {
            total += t.weightPerMille();
            double share = counts.getOrDefault(t, 0) * 1000.0 / n;
            helper.assertTrue(Math.abs(share - t.weightPerMille()) <= Math.max(3, t.weightPerMille() * 0.1),
                    t.id() + " : " + share + " ‰ au lieu de " + t.weightPerMille());
        }
        helper.assertTrue(total == 1000, "Les chances font 100 %");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void talentMultipliesNenXp(GameTestHelper helper) {
        net.minecraft.world.entity.monster.Zombie zombie = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new net.minecraft.core.BlockPos(1, 1, 1));
        com.evandouard.hxh.data.HunterData d = com.evandouard.hxh.stats.HunterStats.get(zombie);
        com.evandouard.hxh.nen.NenModes.unlock(zombie);
        d.setTalent(com.evandouard.hxh.data.NenTalent.GENIUS);
        int before = d.nenXp();
        com.evandouard.hxh.nen.NenProgress.addNenXp(zombie, 10);
        helper.assertTrue(d.nenXp() - before == 30, "Génie du Nen : 10 XP deviennent 30, obtenu " + (d.nenXp() - before));
        d.setTalent(com.evandouard.hxh.data.NenTalent.ORDINARY);
        before = d.nenXp();
        com.evandouard.hxh.nen.NenProgress.addNenXp(zombie, 10);
        helper.assertTrue(d.nenXp() - before == 8, "Talent ordinaire : 10 XP deviennent 8, obtenu " + (d.nenXp() - before));
        zombie.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void exorcistRemovesForeignAuras(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer p = helper.makeMockServerPlayerInLevel();
        var q = com.evandouard.hxh.quest.QuestManager.data(p);
        helper.assertTrue(com.evandouard.hxh.nen.Exorcism.curses(p).isEmpty(), "Au départ : aucune malédiction");
        // Malédiction de Nen : le lait ne l'enlève pas
        com.evandouard.hxh.nen.Exorcism.curse(p);
        p.removeEffectsCuredBy(net.neoforged.neoforge.common.EffectCures.MILK);
        helper.assertTrue(p.hasEffect(com.evandouard.hxh.effect.ModEffects.NEN_CURSE), "Le lait n'enlève pas la malédiction");
        // Dette du Hakoware et bombe de Genthru
        p.getPersistentData().putFloat(com.evandouard.hxh.hatsu.Specials.HAKO_DEBT, 12f);
        p.getPersistentData().putLong(com.evandouard.hxh.nen.Exorcism.BOMB, p.level().getGameTime() + 100);
        helper.assertTrue(com.evandouard.hxh.nen.Exorcism.curses(p).size() == 3, "Trois malédictions : " + com.evandouard.hxh.nen.Exorcism.curses(p));
        // Pas assez de Jenny : rien ne change
        q.addJenny(-q.jenny());
        com.evandouard.hxh.nen.Exorcism.talk(p);
        helper.assertTrue(com.evandouard.hxh.nen.Exorcism.curses(p).size() == 3, "Sans Jenny, l'exorciste ne fait rien");
        int price = com.evandouard.hxh.nen.Exorcism.price(p, 3);
        q.addJenny(price + 7);
        com.evandouard.hxh.nen.Exorcism.talk(p);
        helper.assertTrue(com.evandouard.hxh.nen.Exorcism.curses(p).isEmpty(), "Tout est exorcisé");
        helper.assertTrue(q.jenny() == 7, "Prix payé (" + price + "), reste " + q.jenny());
        helper.assertTrue(!p.hasEffect(com.evandouard.hxh.effect.ModEffects.NEN_CURSE), "Malédiction retirée");
        // Le sceau d'un serment n'est pas une aura étrangère
        com.evandouard.hxh.stats.HunterStats.get(p).setSealedUntil(p.level().getGameTime() + 1000);
        com.evandouard.hxh.nen.Exorcism.exorcise(p);
        helper.assertTrue(com.evandouard.hxh.stats.HunterStats.get(p).sealedUntil() > p.level().getGameTime(), "Le sceau du serment reste");
        com.evandouard.hxh.stats.HunterStats.get(p).setSealedUntil(0);
        helper.succeed();
    }
}
