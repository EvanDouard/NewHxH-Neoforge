package com.evandouard.hxh.gametest;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.hatsu.Hatsu;
import com.evandouard.hxh.hatsu.KitBars;
import com.evandouard.hxh.hatsu.Netero;
import com.evandouard.hxh.nen.NenModes;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Personnages ajoutés depuis la fiche docs/kits-personnages.md. */
@GameTestHolder(HxHMod.MODID)
@PrefixGameTestTemplate(false)
public final class CharacterKitTests {

    /** Husk immobile, regardant vers +X. */
    static Zombie husk(GameTestHelper helper, int x) {
        Zombie z = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(x, 1, 1));
        z.setNoGravity(true);
        z.setYRot(-90f);
        z.setYHeadRot(-90f);
        z.setXRot(0f);
        return z;
    }

    static HunterData hunter(Zombie z, NenType type, String kit) {
        HunterData d = HunterStats.get(z);
        d.setNenType(type);
        NenModes.unlock(z);
        d.setNenRevealed(true);
        d.setNenLevel(100);
        d.setAura(d.auraMax());
        if (kit != null) d.setKit(kit);
        return d;
    }

    @GameTest(template = "empty")
    public static void neteroSpendsPrayerOnKannon(GameTestHelper helper) {
        Zombie netero = husk(helper, 0);
        Zombie target = husk(helper, 2);
        HunterData d = hunter(netero, NenType.ENHANCER, "netero");
        long now = helper.getLevel().getGameTime();
        helper.assertTrue(Hatsu.select(netero, "netero_kannon"), "Kannon sélectionnable");
        helper.assertTrue("message.hxh.no_prayer".equals(Hatsu.castRefusal(d, Netero.KANNON, now)), "Sans Prière : refus");
        // Prier remplit la jauge
        for (int t = 10; t <= 200; t += 10) Netero.prayTick(netero, t);
        helper.assertTrue(Math.abs(d.kitValue(KitBars.PRAYER) - 50f) < 0.01f, "10 s de prière : 50 → " + d.kitValue(KitBars.PRAYER));
        float hp = target.getHealth();
        helper.assertTrue(Hatsu.cast(netero) == null, "La paume de Kannon part");
        helper.assertTrue(Math.abs(d.kitValue(KitBars.PRAYER) - 20f) < 0.01f, "30 de Prière dépensés");
        helper.assertTrue(target.getHealth() < hp, "La cible devant est frappée");
        // Poings de Kannon : coups renforcés
        helper.assertTrue(Hatsu.select(netero, "netero_kannon_fists"), "Poings sélectionnables");
        helper.assertTrue(Hatsu.cast(netero) == null, "Les poings de Kannon s'allument");
        float out = Hatsu.modifyOutgoingMelee(netero, target, 10f);
        helper.assertTrue(out >= 15.9f, "Coups ×1,6 avec les poings de Kannon : " + out);
        // Main zéro : seulement prière pleine
        d.setAura(d.auraMax());
        helper.assertTrue("message.hxh.no_prayer".equals(Hatsu.castRefusal(d, Netero.ZERO_HAND, now)), "Main zéro : prière pleine requise");
        d.setKitValue(KitBars.PRAYER, KitBars.PRAYER_MAX);
        helper.assertTrue(Hatsu.castRefusal(d, Netero.ZERO_HAND, now) == null, "Prière pleine : Main zéro prête");
        helper.succeed();
    }

    /** Chaque technique de chaque personnage jouable part sans erreur, lancée par un mob sur une cible devant lui. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void everyTechniqueOfEveryKitRuns(GameTestHelper helper) {
        Zombie caster = husk(helper, 0);
        Zombie target = husk(helper, 2);
        StringBuilder failures = new StringBuilder();
        int cast = 0;
        for (var kit : com.evandouard.hxh.hatsu.HatsuKits.playable()) {
            HunterData d = hunter(caster, kit.type(), kit.id());
            for (String id : kit.techniques()) {
                var t = com.evandouard.hxh.hatsu.Techniques.get(id);
                if (t == null) {
                    failures.append(kit.id()).append(": technique inconnue ").append(id).append("; ");
                    continue;
                }
                if (!t.implemented()) {
                    failures.append(kit.id()).append(": technique non jouable ").append(id).append("; ");
                    continue;
                }
                if (t.isCharged()) continue;
                d.setAura(d.auraMax());
                for (var bar : KitBars.of(kit.id())) d.setKitValue(bar.key(), bar.max());
                target.setHealth(target.getMaxHealth());
                try {
                    t.perform(caster, 1f);
                    com.evandouard.hxh.hatsu.NenTasks.flushForTests();
                    cast++;
                } catch (RuntimeException e) {
                    failures.append(kit.id()).append("/").append(id).append(" : ").append(e).append("; ");
                }
            }
            for (var p : kit.passives()) if (!p.implemented()) failures.append(kit.id()).append(": passif non jouable ").append(p.id()).append("; ");
        }
        helper.assertTrue(failures.length() == 0, failures.toString());
        helper.assertTrue(cast > 50, "Techniques lancées : " + cast);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void specialMechanicsWork(GameTestHelper helper) {
        Zombie a = husk(helper, 0);
        Zombie b = husk(helper, 2);
        long now = helper.getLevel().getGameTime();
        // Biscuit : deux formes, la taille suit, et disparaît quand on change de personnage
        HunterData d = hunter(a, NenType.TRANSMUTER, "biscuit");
        float small = com.evandouard.hxh.hatsu.Hatsu.modifyOutgoingMelee(a, b, 10f);
        com.evandouard.hxh.hatsu.Specials.BISCUIT_SWITCH_FORM.perform(a, 1f);
        a.tickCount = 20;
        com.evandouard.hxh.hatsu.Hatsu.tickPassives(a);
        float big = com.evandouard.hxh.hatsu.Hatsu.modifyOutgoingMelee(a, b, 10f);
        helper.assertTrue(small < 10f && big > 14.9f, "Petite forme ×0,8, grande ×1,5 : " + small + " / " + big);
        helper.assertTrue(a.getScale() > 1.3f, "Grande forme : plus grande (" + a.getScale() + ")");
        d.setKit("gon");
        com.evandouard.hxh.hatsu.Hatsu.tickPassives(a);
        helper.assertTrue(Math.abs(a.getScale() - 1f) < 0.01f, "Autre personnage : taille normale (" + a.getScale() + ")");
        // Phinks : tours limités par le Nen
        d = hunter(a, NenType.ENHANCER, "phinks");
        d.setNenLevel(25);
        int ok = 0;
        for (int i = 0; i < 4; i++) if (com.evandouard.hxh.hatsu.Specials.PHINKS_RIPPER_CYCLOTRON.perform(a, 1f)) ok++;
        helper.assertTrue(ok == 2, "Nen 25 : 2 tours au plus (obtenu " + ok + ")");
        float punch = com.evandouard.hxh.hatsu.Hatsu.modifyOutgoingMelee(a, b, 4f);
        helper.assertTrue(Math.abs(punch - 9f) < 0.01f, "Le coup libère les tours : 4 + 2 × 2,5 = 9 (" + punch + ")");
        // Feitan : la douleur monte avec les coups reçus
        d = hunter(a, NenType.TRANSMUTER, "feitan");
        a.hurt(helper.getLevel().damageSources().generic(), 4f);
        helper.assertTrue(d.kitValue(KitBars.PAIN) >= 19f, "4 PV perdus : douleur 20 (" + d.kitValue(KitBars.PAIN) + ")");
        helper.assertTrue("message.hxh.no_pain".equals(Hatsu.castRefusal(d, com.evandouard.hxh.hatsu.Specials.FEITAN_RISING_SUN, now)),
                "Soleil levant : douleur pleine requise");
        // Shalnark : pas de contrôle au-delà de 10 niveaux d'écart
        d = hunter(a, NenType.MANIPULATOR, "shalnark");
        d.setNenLevel(20);
        HunterStats.get(b).setNenLevel(35);
        helper.assertTrue(!com.evandouard.hxh.hatsu.Specials.canControl(a, b), "Écart de 15 : l'antenne ne prend pas");
        HunterStats.get(b).setNenLevel(28);
        helper.assertTrue(com.evandouard.hxh.hatsu.Specials.canControl(a, b), "Écart de 8 : contrôle possible");
        // Knuckle : 12 dégâts = 6 de dette ; banqueroute = Zetsu forcé
        d = hunter(a, NenType.CONJURER, "knuckle");
        HunterData bd = hunter(b, NenType.ENHANCER, null);
        helper.assertTrue(com.evandouard.hxh.hatsu.Specials.KNUCKLE_POT_CLEAN.perform(a, 1f), "Pot-Clean s'attache");
        com.evandouard.hxh.hatsu.Hatsu.modifyOutgoingMelee(a, b, 12f);
        float debt = b.getPersistentData().getFloat(com.evandouard.hxh.hatsu.Specials.HAKO_DEBT);
        helper.assertTrue(Math.abs(debt - 6f) < 0.01f, "12 dégâts → dette 6 (" + debt + ")");
        b.getPersistentData().putFloat(com.evandouard.hxh.hatsu.Specials.HAKO_DEBT, bd.auraMax() - 1);
        com.evandouard.hxh.hatsu.Hatsu.modifyOutgoingMelee(a, b, 4f);
        helper.assertTrue(bd.mode() == com.evandouard.hxh.data.NenMode.ZETSU, "Banqueroute : Zetsu");
        helper.assertTrue("message.hxh.bankrupt".equals(com.evandouard.hxh.nen.NenModes.trySetMode(b, com.evandouard.hxh.data.NenMode.REN)),
                "Impossible de quitter le Zetsu en banqueroute");
        // Chrollo : vole puis utilise
        d = hunter(a, NenType.SPECIALIST, "chrollo");
        b.getPersistentData().putString(com.evandouard.hxh.hatsu.Specials.LAST_TECHNIQUE, "leorio_shockwave");
        helper.assertTrue(com.evandouard.hxh.hatsu.Specials.CHROLLO_SKILL_HUNTER.perform(a, 1f), "Le livre vole la technique");
        helper.assertTrue("leorio_shockwave".equals(a.getPersistentData().getString(com.evandouard.hxh.hatsu.Specials.BOOK)), "Page remplie");
        com.evandouard.hxh.hatsu.Specials.CHROLLO_SKILL_HUNTER.perform(a, 1f);
        helper.assertTrue(a.getPersistentData().getString(com.evandouard.hxh.hatsu.Specials.BOOK).isEmpty(), "Page utilisée puis effacée");
        helper.succeed();
    }
}
