package com.evandouard.hxh.gametest;

import com.evandouard.hxh.HxHConfig;
import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Tests des données Hunter et des stats. On utilise un zombie : les stats doivent
 * marcher sur n'importe quelle entité, pas seulement le joueur.
 */
@GameTestHolder(HxHMod.MODID)
@PrefixGameTestTemplate(false)
public final class HunterStatsTests {
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    private static Zombie zombie(GameTestHelper helper) {
        Zombie z = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, POS);
        z.setNoGravity(true);
        return z;
    }

    private static void assertNear(GameTestHelper helper, double expected, double actual, String what) {
        helper.assertTrue(Math.abs(expected - actual) < 0.001,
                what + " : attendu " + expected + ", obtenu " + actual);
    }

    @GameTest(template = "empty")
    public static void strengthAddsAttackDamage(GameTestHelper helper) {
        Zombie z = zombie(helper);
        double base = z.getAttributeValue(Attributes.ATTACK_DAMAGE);
        HunterStats.setLevel(z, Stat.STRENGTH, 50);
        assertNear(helper, base + 50 * HxHConfig.STRENGTH_DAMAGE_PER_LEVEL.get(),
                z.getAttributeValue(Attributes.ATTACK_DAMAGE), "Dégâts avec Force 50");
        HunterStats.setLevel(z, Stat.STRENGTH, 0);
        assertNear(helper, base, z.getAttributeValue(Attributes.ATTACK_DAMAGE), "Dégâts après retour à Force 0");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void speedAddsMovementSpeed(GameTestHelper helper) {
        Zombie z = zombie(helper);
        double base = z.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
        double before = z.getAttributeValue(Attributes.MOVEMENT_SPEED);
        HunterStats.setLevel(z, Stat.SPEED, 100);
        assertNear(helper, before + base * 100 * HxHConfig.SPEED_PERCENT_PER_LEVEL.get(),
                z.getAttributeValue(Attributes.MOVEMENT_SPEED), "Vitesse avec Vitesse 100");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vitalityAddsMaxHealth(GameTestHelper helper) {
        Zombie z = zombie(helper);
        double base = z.getMaxHealth();
        HunterStats.setLevel(z, Stat.VITALITY, 100);
        assertNear(helper, base + 100 * HxHConfig.VITALITY_HEALTH_PER_LEVEL.get(), z.getMaxHealth(),
                "Vie max avec Vitalité 100");
        HunterStats.setLevel(z, Stat.VITALITY, 0);
        helper.assertTrue(z.getHealth() <= z.getMaxHealth(), "La vie ne doit pas dépasser le max après baisse");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void enduranceReducesDamage(GameTestHelper helper) {
        // Réduction plate contre une attaque : on compare à un zombie témoin (même armure naturelle)
        Zombie attacker = zombie(helper);
        Zombie tough = zombie(helper);
        Zombie normal = zombie(helper);
        HunterStats.setLevel(tough, Stat.ENDURANCE, 100);
        float toughBefore = tough.getHealth();
        float normalBefore = normal.getHealth();
        tough.hurt(helper.getLevel().damageSources().mobAttack(attacker), 10f);
        normal.hurt(helper.getLevel().damageSources().mobAttack(attacker), 10f);
        double lostTough = toughBefore - tough.getHealth();
        double lostNormal = normalBefore - normal.getHealth();
        double flat = 100 * HxHConfig.ENDURANCE_FLAT_PER_LEVEL.get();
        helper.assertTrue(lostNormal > 0, "Le zombie témoin aurait dû prendre des dégâts");
        helper.assertTrue(lostTough < lostNormal - flat * 0.5,
                "Endurance 100 : environ " + flat + " PV de moins attendus (" + lostTough + " contre " + lostNormal + ")");
        // Les dégâts « internes » (noyade…) ne sont jamais réduits
        float before = tough.getHealth();
        tough.invulnerableTime = 0;
        tough.hurt(helper.getLevel().damageSources().drown(), 2f);
        helper.assertTrue(tough.getHealth() < before, "La noyade passe à travers l'Endurance et le Nen");
        // La chute, elle, est réduite (comme un coup)
        float beforeFall = tough.getHealth();
        tough.invulnerableTime = 0;
        tough.hurt(helper.getLevel().damageSources().fall(), 2f);
        helper.assertTrue(tough.getHealth() == beforeFall, "Endurance 100 : une petite chute ne fait rien");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void xpLevelsUpAndCarriesOver(GameTestHelper helper) {
        HunterData data = new HunterData();
        int needed = Stat.xpToNext(0) + Stat.xpToNext(1);
        int gained = data.addXp(Stat.SPEED, needed + 3);
        helper.assertTrue(gained == 2, "2 niveaux attendus, obtenu " + gained);
        helper.assertTrue(data.level(Stat.SPEED) == 2, "Niveau 2 attendu");
        helper.assertTrue(data.xp(Stat.SPEED) == 3, "3 XP restants attendus, obtenu " + data.xp(Stat.SPEED));
        data.setLevel(Stat.SPEED, Stat.MAX_LEVEL);
        helper.assertTrue(data.addXp(Stat.SPEED, 1_000_000) == 0, "Aucun gain au niveau max");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void saveAndPartialSync(GameTestHelper helper) {
        HunterData data = new HunterData();
        data.setLevel(Stat.STRENGTH, 42);
        data.setNenType(NenType.EMITTER);
        data.setNenUnlocked(true);

        // Sauvegarde complète puis rechargement
        HunterData loaded = new HunterData();
        loaded.read(data.writeAll());
        helper.assertTrue(loaded.level(Stat.STRENGTH) == 42, "Force non restaurée");
        helper.assertTrue(loaded.nenType() == NenType.EMITTER && loaded.nenUnlocked(), "Nen non restauré");

        // Synchro partielle : seules les clés modifiées partent
        data.writeDirtyAndClear();
        data.setLevel(Stat.SPEED, 7);
        CompoundTag partial = data.writeDirtyAndClear();
        helper.assertTrue(partial.size() == 1 && partial.contains("stat.speed"),
                "Le paquet partiel ne doit contenir que stat.speed : " + partial);
        loaded.read(partial);
        helper.assertTrue(loaded.level(Stat.SPEED) == 7 && loaded.level(Stat.STRENGTH) == 42,
                "La synchro partielle doit fusionner sans écraser le reste");
        helper.assertTrue(!data.isDirty(), "Plus rien à envoyer après l'envoi");
        helper.succeed();
    }
}
