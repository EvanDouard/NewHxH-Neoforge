package com.evandouard.hxh.gametest;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.NenMode;
import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.hatsu.Hatsu;
import com.evandouard.hxh.hatsu.HatsuKits;
import com.evandouard.hxh.hatsu.Techniques;
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
 * Tests du Hatsu : choix du personnage, lancement, recharge, technique suivante, affinité.
 * Le lanceur est un mob : un mob Gon doit pouvoir faire exactement ce que fait un joueur Gon.
 */
@GameTestHolder(HxHMod.MODID)
@PrefixGameTestTemplate(false)
public final class HatsuTests {

    /** Husk immobile, regardant vers +X. */
    private static Zombie husk(GameTestHelper helper, int x) {
        Zombie z = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(x, 1, 1));
        z.setNoGravity(true);
        z.setYRot(-90f);
        z.setYHeadRot(-90f);
        z.setXRot(0f);
        return z;
    }

    /** Donne le Nen, un type révélé et éventuellement un personnage. */
    private static HunterData hunter(Zombie z, NenType type, String kit) {
        HunterData d = HunterStats.get(z);
        d.setNenType(type);
        NenModes.unlock(z);
        d.setNenRevealed(true);
        if (kit != null) d.setKit(kit);
        return d;
    }

    @GameTest(template = "empty")
    public static void chooseKitRules(GameTestHelper helper) {
        Zombie z = husk(helper, 1);
        HunterData d = HunterStats.get(z);
        d.setNenType(NenType.ENHANCER);
        NenModes.unlock(z);
        helper.assertTrue(Hatsu.choose(z, "gon") != null, "Sans divination : refus");
        d.setNenRevealed(true);
        helper.assertTrue(Hatsu.choose(z, "killua") != null, "Killua n'est pas Renforcement : refus");
        helper.assertTrue(Hatsu.choose(z, "gon") == null, "Gon aurait dû être accepté");
        helper.assertTrue(d.kit().equals("gon"), "Le kit doit être Gon");
        helper.assertTrue(Hatsu.choose(z, "gon") != null, "Le choix est définitif : second choix refusé");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void castHitsTargetCostsAuraAndCoolsDown(GameTestHelper helper) {
        Zombie gon = husk(helper, 0);
        Zombie target = husk(helper, 2);
        HunterData d = hunter(gon, NenType.ENHANCER, "gon");
        float aura = d.aura();
        float health = target.getHealth();

        helper.assertTrue(Hatsu.cast(gon) == null, "Jajanken Pierre aurait dû partir");
        helper.assertTrue(target.getHealth() < health, "La cible aurait dû prendre des dégâts");
        helper.assertTrue(Math.abs(d.aura() - (aura - Techniques.GON_ROCK.auraCost())) < 0.01,
                "Le coût en aura doit être retiré");
        helper.assertTrue(d.readyAt("gon_rock") > helper.getLevel().getGameTime(), "La recharge doit démarrer");
        helper.assertTrue("message.hxh.hatsu_cooldown".equals(Hatsu.cast(gon)), "Second lancement : en recharge");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void noTargetCostsNothing(GameTestHelper helper) {
        Zombie leorio = husk(helper, 1);
        leorio.setYRot(90f); // regarde vers -X : personne
        HunterData d = hunter(leorio, NenType.EMITTER, "leorio");
        float aura = d.aura();
        helper.assertTrue("message.hxh.hatsu_no_target".equals(Hatsu.cast(leorio)), "Sans cible : refus");
        helper.assertTrue(d.aura() == aura && d.readyAt("leorio_remote_punch") == 0, "Rien ne doit être consommé");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nextTechniqueLoops(GameTestHelper helper) {
        Zombie gon = husk(helper, 1);
        HunterData d = hunter(gon, NenType.ENHANCER, "gon");
        helper.assertTrue(Hatsu.selectedTechnique(d) == Techniques.GON_ROCK, "1 : Pierre");
        Hatsu.cycle(gon);
        helper.assertTrue(Hatsu.selectedTechnique(d) == Techniques.GON_SCISSORS, "2 : Ciseaux");
        Hatsu.cycle(gon);
        helper.assertTrue(Hatsu.selectedTechnique(d) == Techniques.GON_PAPER, "3 : Papier");
        Hatsu.cycle(gon);
        helper.assertTrue(Hatsu.selectedTechnique(d) == Techniques.GON_ROCK, "Retour à 1 : Pierre");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void affinityScalesPower(GameTestHelper helper) {
        Zombie gon = husk(helper, 1);
        HunterData d = hunter(gon, NenType.ENHANCER, "gon");
        helper.assertTrue(Math.abs(Hatsu.power(d, Techniques.GON_ROCK) - 1.0f) < 1e-4, "Pierre (Renforcement) : 100 %");
        helper.assertTrue(Math.abs(Hatsu.power(d, Techniques.GON_PAPER) - 0.8f) < 1e-4, "Papier (Émission) : 80 %");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void zetsuBlocksCasting(GameTestHelper helper) {
        Zombie killua = husk(helper, 1);
        HunterData d = hunter(killua, NenType.TRANSMUTER, "killua");
        Hatsu.cycle(killua); // Godspeed (sur soi, pas besoin de cible)
        NenModes.trySetMode(killua, NenMode.ZETSU);
        helper.assertTrue("message.hxh.hatsu_zetsu".equals(Hatsu.cast(killua)), "En Zetsu : refus");
        NenModes.trySetMode(killua, NenMode.NONE);
        helper.assertTrue(Hatsu.cast(killua) == null, "Hors Zetsu, Godspeed doit partir");
        helper.assertTrue(killua.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED), "Godspeed donne de la vitesse");
        helper.assertTrue(HatsuKits.all().size() == 6, "6 personnages attendus dans ce premier lot");
        helper.succeed();
    }
}
