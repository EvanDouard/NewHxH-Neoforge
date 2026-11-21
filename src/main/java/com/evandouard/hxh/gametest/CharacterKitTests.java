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

/** Personnages ajoutés depuis la fiche docs/hatsu-et-techniques.md. */
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
        helper.assertTrue(Math.abs(d.kitValue(KitBars.PRAYER) - 40f) < 0.01f, "10 de Prière dépensés");
        helper.assertTrue(target.getHealth() < hp, "La cible devant est frappée");
        // Main zéro : seulement prière pleine
        d.setAura(d.auraMax());
        helper.assertTrue("message.hxh.no_prayer".equals(Hatsu.castRefusal(d, Netero.ZERO_HAND, now)), "Main zéro : prière pleine requise");
        d.setKitValue(KitBars.PRAYER, KitBars.PRAYER_MAX);
        helper.assertTrue(Hatsu.castRefusal(d, Netero.ZERO_HAND, now) == null, "Prière pleine : Main zéro prête");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void zoldyckAssassinHitsIsolatedTargetsHarder(GameTestHelper helper) {
        Zombie silva = husk(helper, 0);
        Zombie target = husk(helper, 2);
        hunter(silva, NenType.EMITTER, "silva");
        float alone = Hatsu.modifyOutgoingMelee(silva, target, 10f);
        helper.assertTrue(alone >= 12.4f, "Cible isolée : +25 % : " + alone);
        Zombie other = husk(helper, 4);
        float crowded = Hatsu.modifyOutgoingMelee(silva, target, 10f);
        helper.assertTrue(crowded <= 10.01f, "Un autre être vivant à côté : pas de bonus : " + crowded);
        other.discard();
        silva.discard();
        Zombie killua = husk(helper, 1);
        hunter(killua, NenType.TRANSMUTER, "killua");
        helper.assertTrue(Hatsu.modifyOutgoingMelee(killua, target, 10f) <= 10.01f, "Killua n'a pas encore le bonus");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void botobaiVeteranIsStrongerWhenHealthy(GameTestHelper helper) {
        Zombie botobai = husk(helper, 0);
        hunter(botobai, NenType.ENHANCER, "botobai");
        botobai.setHealth(botobai.getMaxHealth());
        botobai.tickCount = 20;
        com.evandouard.hxh.hatsu.Characters.BOTOBAI_VETERAN.tick(botobai);
        helper.assertTrue(botobai.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST)
                && botobai.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE), "En forme : puissance et résistance");
        botobai.removeAllEffects();
        botobai.setHealth(botobai.getMaxHealth() * 0.3f);
        com.evandouard.hxh.hatsu.Characters.BOTOBAI_VETERAN.tick(botobai);
        helper.assertTrue(!botobai.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST), "Blessé : pas de bonus");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void zeroHandSpendsAllAura(GameTestHelper helper) {
        Zombie netero = husk(helper, 0);
        HunterData d = hunter(netero, NenType.ENHANCER, "netero");
        d.setAura(d.auraMax());
        helper.assertTrue(Netero.ZERO_HAND.perform(netero, 1f), "La Main zéro part");
        helper.assertTrue(d.aura() < 0.01f, "Toute l'aura est dépensée : " + d.aura());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void godspeedSlowsTheWorldExceptFastOnes(GameTestHelper helper) {
        Zombie killua = husk(helper, 0);
        hunter(killua, NenType.TRANSMUTER, "killua");
        Zombie slow = husk(helper, 5);
        Zombie fast = husk(helper, 6);
        com.evandouard.hxh.stats.HunterStats.setLevel(fast, com.evandouard.hxh.data.Stat.SPEED, 90);
        helper.assertTrue(com.evandouard.hxh.hatsu.Techniques.KILLUA_GODSPEED.perform(killua, 1f), "Le Godspeed part");
        helper.assertTrue(killua.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED), "Killua file");
        helper.assertTrue(!killua.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN), "Killua n'est pas ralenti");
        helper.assertTrue(slow.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN), "Le monde autour est ralenti");
        helper.assertTrue(!fast.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN), "Vitesse 90 : pas ralenti");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void memoryBombFiresSixBulletsThenReloads(GameTestHelper helper) {
        Zombie pakunoda = husk(helper, 0);
        Zombie target = husk(helper, 8);
        HunterData d = hunter(pakunoda, NenType.SPECIALIST, "pakunoda");
        var bomb = com.evandouard.hxh.hatsu.Characters.PAKUNODA_MEMORY_BOMB;
        for (int i = 1; i <= 5; i++) {
            bomb.perform(pakunoda, 1f); // la balle part, touchée ou non
            helper.assertTrue(bomb.cooldownFor(d) == 0, "Balle " + i + " : pas de recharge");
        }
        bomb.perform(pakunoda, 1f);
        helper.assertTrue(bomb.cooldownFor(d) > 0, "Après la 6e : rechargement");
        bomb.perform(pakunoda, 1f);
        helper.assertTrue(bomb.cooldownFor(d) == 0, "Rechargé : la série repart");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void risingSunGrowsWithDamageTaken(GameTestHelper helper) {
        Zombie feitan = husk(helper, 0);
        HunterData d = hunter(feitan, NenType.TRANSMUTER, "feitan");
        float calm = com.evandouard.hxh.hatsu.Specials.sunDamage(feitan);
        com.evandouard.hxh.hatsu.Specials.FEITAN_PAIN.modifyIncoming(feitan, feitan.damageSources().generic(), 12f);
        float hurt = com.evandouard.hxh.hatsu.Specials.sunDamage(feitan);
        helper.assertTrue(hurt >= calm + 11.9f, "Le Soleil levant monte avec les dégâts reçus : " + calm + " → " + hurt);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void countdownBombIsRandomAndDefusedByContact(GameTestHelper helper) {
        Zombie genthru = husk(helper, 0);
        Zombie target = husk(helper, 4);
        hunter(genthru, NenType.CONJURER, "genthru");
        helper.assertTrue(com.evandouard.hxh.hatsu.Characters.GENTHRU_COUNTDOWN.perform(genthru, 1f), "La bombe est posée");
        long now = helper.getLevel().getGameTime();
        long at = target.getPersistentData().getLong(com.evandouard.hxh.nen.Exorcism.BOMB);
        helper.assertTrue(at - now >= 20 && at - now <= 100, "Délai entre 1 et 5 s : " + (at - now));
        var hit = genthru.damageSources().mobAttack(target);
        for (int i = 0; i < com.evandouard.hxh.hatsu.Kinds.BOMB_DEFUSE_CONTACTS; i++) {
            helper.assertTrue(target.getPersistentData().contains(com.evandouard.hxh.nen.Exorcism.BOMB), "Contact " + i + " : encore armée");
            com.evandouard.hxh.hatsu.Characters.GENTHRU_BOMBER.modifyIncoming(genthru, hit, 1f);
        }
        helper.assertTrue(!target.getPersistentData().contains(com.evandouard.hxh.nen.Exorcism.BOMB), "Trois contacts : désamorcée");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lot5RoyalPressureStanceAndFocusedShot(GameTestHelper helper) {
        Zombie meruem = husk(helper, 0);
        Zombie near = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 1));
        near.setNoGravity(true);
        hunter(meruem, NenType.EMITTER, "meruem");
        helper.assertTrue(com.evandouard.hxh.hatsu.Characters.MERUEM_PRESSURE.perform(meruem, 1f), "Pression royale part");
        helper.assertTrue(near.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN), "L'ennemi proche est ralenti");
        helper.assertTrue(near.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS), "L'ennemi proche est affaibli");
        Zombie nobunaga = husk(helper, 6);
        hunter(nobunaga, NenType.ENHANCER, "nobunaga");
        helper.assertTrue(com.evandouard.hxh.hatsu.Characters.NOBUNAGA_STANCE.perform(nobunaga, 1f), "Posture du samouraï part");
        helper.assertTrue(nobunaga.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE), "Résistance en posture");
        helper.assertTrue(nobunaga.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST), "Force en posture");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lot6HeatWaveBurnsAndPhantomStepHides(GameTestHelper helper) {
        Zombie feitan = husk(helper, 0);
        Zombie near = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 1));
        near.setNoGravity(true);
        hunter(feitan, NenType.TRANSMUTER, "feitan");
        helper.assertTrue(com.evandouard.hxh.hatsu.Characters.FEITAN_HEAT_WAVE.perform(feitan, 1f), "Vague de chaleur part");
        helper.assertTrue(near.getRemainingFireTicks() > 0 || near.isDeadOrDying(), "L'ennemi proche brûle");
        helper.assertTrue(com.evandouard.hxh.hatsu.Characters.FEITAN_PHANTOM_STEP.perform(feitan, 1f), "Pas fantôme part");
        helper.assertTrue(feitan.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY), "Feitan est invisible");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lot7ElectricFistSlowsAndSmashHits(GameTestHelper helper) {
        Zombie killua = husk(helper, 0);
        hunter(killua, NenType.TRANSMUTER, "killua");
        Zombie close = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 1));
        close.setNoGravity(true);
        close.setPos(close.getX(), killua.getY(), close.getZ());
        com.evandouard.hxh.hatsu.Characters.KILLUA_ELECTRIC_FIST.perform(killua, 1f);
        helper.assertTrue(close.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN) || close.isDeadOrDying()
                || close.getHealth() < close.getMaxHealth(), "Le poing électrique touche");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lot8CloudBlindsAndGuardResists(GameTestHelper helper) {
        Zombie morel = husk(helper, 0);
        Zombie near = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 1));
        near.setNoGravity(true);
        hunter(morel, NenType.MANIPULATOR, "morel");
        helper.assertTrue(com.evandouard.hxh.hatsu.Characters.MOREL_BLINDING_CLOUD.perform(morel, 1f), "Le nuage part");
        helper.assertTrue(near.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS), "L'ennemi proche est aveuglé");
        Zombie guard = husk(helper, 6);
        hunter(guard, NenType.ENHANCER, "tsezguerra");
        helper.assertTrue(com.evandouard.hxh.hatsu.Characters.TSEZGUERRA_INSTANT_GUARD.perform(guard, 1f), "La défense part");
        helper.assertTrue(guard.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE), "Résistance active");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lot9ChargeBuffsUvogin(GameTestHelper helper) {
        Zombie uvogin = husk(helper, 0);
        hunter(uvogin, NenType.ENHANCER, "uvogin");
        helper.assertTrue(com.evandouard.hxh.hatsu.Characters.UVOGIN_CHARGE.perform(uvogin, 1f), "La charge part");
        helper.assertTrue(uvogin.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED), "Vitesse active");
        helper.assertTrue(uvogin.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST), "Force active");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lot10UltrasoundRevealsAndPounceLeaps(GameTestHelper helper) {
        Zombie leorio = husk(helper, 0);
        Zombie near = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(5, 1, 1));
        near.setNoGravity(true);
        hunter(leorio, NenType.EMITTER, "leorio");
        helper.assertTrue(com.evandouard.hxh.hatsu.Characters.LEORIO_ULTRASOUND.perform(leorio, 1f), "Les ultrasons partent");
        helper.assertTrue(near.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING), "L'ennemi proche est révélé");
        Zombie pitou = husk(helper, 10);
        hunter(pitou, NenType.SPECIALIST, "neferpitou");
        helper.assertTrue(com.evandouard.hxh.hatsu.Characters.PITOU_POUNCE.perform(pitou, 1f), "Le bond part");
        helper.assertTrue(pitou.getDeltaMovement().x > 0.5, "Pitou bondit vers l'avant (est) : " + pitou.getDeltaMovement().x);
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
