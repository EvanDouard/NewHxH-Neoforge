package com.evandouard.hxh.gametest;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.mob.MobPreset;
import com.evandouard.hxh.mob.MobPresets;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Mobs d'exemple décrits en JSON (data/hxh/mob_presets). */
@GameTestHolder(HxHMod.MODID)
@PrefixGameTestTemplate(false)
public class MobPresetTests {

    private static LivingEntity spawn(GameTestHelper helper, String name, int x) {
        MobPreset preset = MobPresets.get(name);
        helper.assertTrue(preset != null, "Mob d'exemple chargé : " + name);
        LivingEntity e = preset.spawn(helper.getLevel(), helper.absoluteVec(new Vec3(x + 0.5, 1, 1.5)), 0f);
        helper.assertTrue(e != null, "Le mob apparaît : " + name);
        return e;
    }

    @GameTest(template = "empty")
    public static void presetsLoadAndApplyStats(GameTestHelper helper) {
        helper.assertTrue(MobPresets.all().size() >= 8, "Au moins 8 mobs d'exemple (" + MobPresets.all().size() + ")");
        // Tous les exemples apparaissent sans erreur
        int x = 0;
        for (var id : MobPresets.all().keySet()) {
            LivingEntity e = spawn(helper, id.getPath(), x++ % 6);
            e.discard();
        }

        LivingEntity brute = spawn(helper, "zombie_costaud", 0);
        HunterData d = HunterStats.get(brute);
        helper.assertTrue(d.level(Stat.STRENGTH) == 50 && d.level(Stat.SPEED) == 10, "Stats du zombie costaud");
        helper.assertTrue(brute.getMainHandItem().isEmpty(), "Mains nues par défaut");
        helper.assertTrue(brute.getHealth() == brute.getMaxHealth(), "Vie pleine");

        LivingEntity nen = spawn(helper, "zombie_renforceur", 2);
        HunterData n = HunterStats.get(nen);
        helper.assertTrue(n.nenUnlocked() && n.nenLevel() == 30, "Nen éveillé au niveau 30");
        helper.assertTrue("gon".equals(n.kit()), "Kit de Gon (" + n.kit() + ")");
        helper.assertTrue(n.level(Stat.STRENGTH) == 35, "Stats réglées, pas tirées au hasard");

        LivingEntity dummy = spawn(helper, "mannequin", 4);
        helper.assertTrue(dummy instanceof Mob m && m.isNoAi(), "Le mannequin ne bouge pas");
        helper.assertTrue(dummy.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) >= 0.6, "Le mannequin résiste au recul");

        LivingEntity archer = spawn(helper, "squelette_archer", 5);
        helper.assertTrue(archer.getMainHandItem().is(net.minecraft.world.item.Items.BOW), "Arc en main");

        helper.assertTrue(MobPresets.get("hxh:mannequin") != null, "Identifiant complet accepté");
        helper.assertTrue(MobPresets.get("inexistant") == null, "Nom inconnu refusé");
        brute.discard();
        nen.discard();
        dummy.discard();
        archer.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dummyNeverDiesButKillCommandWorks(GameTestHelper helper) {
        LivingEntity dummy = spawn(helper, "mannequin", 1);
        dummy.hurt(helper.getLevel().damageSources().generic(), 10000f);
        helper.assertTrue(dummy.isAlive() && dummy.getHealth() > 0, "Le mannequin encaisse sans mourir");
        dummy.kill(); // /kill : on peut quand même le retirer
        helper.assertTrue(!dummy.isAlive(), "/kill le retire");
        // PNJ de quête : aucun effet ne les touche
        com.evandouard.hxh.npc.NpcEntity npc = helper.spawn(com.evandouard.hxh.registry.ModEntities.NPC.get(), new net.minecraft.core.BlockPos(2, 1, 1));
        npc.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.evandouard.hxh.effect.ModEffects.FEAR, 100));
        helper.assertTrue(!npc.hasEffect(com.evandouard.hxh.effect.ModEffects.FEAR), "PNJ insensible à la peur");
        npc.discard();
        helper.succeed();
    }
}
