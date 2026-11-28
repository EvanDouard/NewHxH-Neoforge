package com.evandouard.hxh.gametest;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.mob.CharacterEntity;
import com.evandouard.hxh.mob.HeldPoses;
import com.evandouard.hxh.nen.NenAwakening;
import com.evandouard.hxh.registry.ModEntities;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Poses tenues des personnages GeckoLib : méditation en tailleur, prière de Netero en seiza. */
@GameTestHolder(HxHMod.MODID)
@PrefixGameTestTemplate(false)
public final class PoseTests {

    private static String read(String path) {
        try (InputStream in = HxHMod.class.getResourceAsStream(path)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    /** Un personnage qui médite prend la pose de méditation ; un Netero prie ; s'arrêter rend la pose normale. */
    @GameTest(template = "empty")
    public static void meditatingCharacterTakesHeldPose(GameTestHelper helper) {
        CharacterEntity gon = helper.spawn(ModEntities.GON.get(), new BlockPos(2, 2, 2));
        gon.setNoAi(true);
        helper.assertTrue(HeldPoses.of(gon) == null, "Pas de pose avant de méditer");
        NenAwakening.toggleMeditation(gon);
        helper.assertTrue(HeldPoses.of(gon) == HeldPoses.MEDITATE, "En méditation : pose en tailleur");
        HunterStats.get(gon).setKit("netero");
        helper.assertTrue(HeldPoses.of(gon) == HeldPoses.PRAY, "Un Netero qui médite prie (seiza, gasshō)");
        NenAwakening.toggleMeditation(gon);
        helper.assertTrue(HeldPoses.of(gon) == null, "Fin de la méditation : retour à la pose normale");
        helper.succeed();
    }

    /** Tout personnage humanoïde (genoux et coudes) a ses animations meditate et pray. */
    @GameTest(template = "empty")
    public static void humanoidModelsHaveMeditationAnimations(GameTestHelper helper) {
        int checked = 0;
        for (var key : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            if (!HxHMod.MODID.equals(key.getNamespace())) continue;
            String id = key.getPath();
            String geo = read("/assets/hxh/geo/entity/npc/" + id + ".geo.json");
            String anim = read("/assets/hxh/animations/entity/npc/" + id + ".animation.json");
            if (geo == null || anim == null || !geo.contains("\"right_shin\"") || !geo.contains("\"right_forearm\"")) continue;
            helper.assertTrue(anim.contains("\"meditate\""), id + " : animation meditate absente");
            helper.assertTrue(anim.contains("\"pray\""), id + " : animation pray absente");
            checked++;
        }
        helper.assertTrue(checked >= 20, "Au moins 20 personnages vérifiés (" + checked + ")");
        helper.succeed();
    }
}
