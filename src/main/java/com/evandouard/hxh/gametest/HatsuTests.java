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
        d.setNenLevel(100); // toutes les techniques débloquées
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

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void castHitsTargetCostsAuraAndCoolsDown(GameTestHelper helper) {
        // Ciseaux : lame d'aura lancée tout droit, coût et recharge immédiats
        Zombie gon = husk(helper, 0);
        Zombie target = husk(helper, 2);
        HunterData d = hunter(gon, NenType.ENHANCER, "gon");
        d.setSelected(1);
        float aura = d.aura();
        float health = target.getHealth();
        helper.assertTrue(Hatsu.cast(gon) == null, "Ciseaux aurait dû partir");
        helper.assertTrue(Math.abs(d.aura() - (aura - Techniques.GON_SCISSORS.auraCost())) < 0.01, "Coût retiré");
        helper.assertTrue(d.readyAt("gon_scissors") > helper.getLevel().getGameTime(), "La recharge doit démarrer");
        helper.assertTrue("message.hxh.hatsu_cooldown".equals(Hatsu.cast(gon)), "Second lancement : en recharge");
        helper.succeedWhen(() -> helper.assertTrue(target.getHealth() < health, "La lame aurait dû toucher la cible"));
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void rockChargesInPlaceThenThrowsOrb(GameTestHelper helper) {
        Zombie gon = husk(helper, 0);
        Zombie target = husk(helper, 2);
        HunterData d = hunter(gon, NenType.ENHANCER, "gon");
        d.setSelected(0);
        float health = target.getHealth();
        helper.assertTrue(Hatsu.cast(gon) == null, "La Pierre commence à charger");
        helper.assertTrue(com.evandouard.hxh.hatsu.NenCharge.isCharging(gon), "En charge");
        helper.assertTrue(gon.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) <= 1e-6,
                "Figé sur place pendant la charge");
        float before = d.aura();
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(d.aura() < before - 20, "L'aura part en continu pendant la charge (" + d.aura() + ")");
            float spent = com.evandouard.hxh.hatsu.NenCharge.spent(gon);
            Hatsu.releaseCast(gon);
            helper.assertTrue(!com.evandouard.hxh.hatsu.NenCharge.isCharging(gon), "Relâchée");
            helper.assertTrue(spent >= 30, "Aura mise dans le coup : " + spent);
            helper.assertTrue(d.readyAt("gon_rock") > helper.getLevel().getGameTime(), "Recharge au relâchement");
            helper.assertTrue(gon.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) > 0,
                    "Peut de nouveau bouger");
            helper.assertTrue(!helper.getLevel().getEntitiesOfClass(com.evandouard.hxh.hatsu.NenProjectile.class,
                    gon.getBoundingBox().inflate(6)).isEmpty() || target.getHealth() < health, "La boule est partie");
            helper.succeedWhen(() -> helper.assertTrue(target.getHealth() < health, "La boule aurait dû toucher la cible"));
        });
    }

    @GameTest(template = "empty")
    public static void missStillCosts(GameTestHelper helper) {
        Zombie leorio = husk(helper, 1);
        leorio.setXRot(-90f); // regarde le ciel : personne (les tests voisins tournent en même temps)
        HunterData d = hunter(leorio, NenType.EMITTER, "leorio");
        float aura = d.aura();
        helper.assertTrue("message.hxh.hatsu_missed".equals(Hatsu.cast(leorio)), "Sans cible : raté");
        helper.assertTrue(d.aura() < aura, "Raté : l'aura doit quand même partir");
        helper.assertTrue(d.readyAt("leorio_remote_punch") > 0, "Raté : la recharge doit quand même démarrer");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ideaCharactersAreListedButNotChoosable(GameTestHelper helper) {
        helper.assertTrue(HatsuKits.playable().size() == 6, "6 personnages jouables attendus");
        helper.assertTrue(HatsuKits.all().size() > 30, "La liste d'idées doit être chargée");
        Zombie z = husk(helper, 1);
        hunter(z, NenType.ENHANCER, null);
        helper.assertTrue("message.hxh.hatsu_not_available".equals(Hatsu.choose(z, "netero")),
                "Netero (idée) ne doit pas être choisissable");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 150)
    public static void passivesWork(GameTestHelper helper) {
        // Neferpitou : pas de dégâts de chute
        Zombie pitou = husk(helper, 0);
        hunter(pitou, NenType.SPECIALIST, "neferpitou");
        float h0 = pitou.getHealth();
        pitou.hurt(helper.getLevel().damageSources().fall(), 6f);
        helper.assertTrue(pitou.getHealth() == h0, "Neferpitou ne doit pas prendre de dégâts de chute");

        // Killua : immunisé à la foudre
        Zombie killua = husk(helper, 1);
        hunter(killua, NenType.TRANSMUTER, "killua");
        float k0 = killua.getHealth();
        killua.hurt(helper.getLevel().damageSources().lightningBolt(), 6f);
        helper.assertTrue(killua.getHealth() == k0, "Killua doit être immunisé à la foudre");

        // En Zetsu, les passifs sont coupés
        NenModes.trySetMode(pitou, NenMode.ZETSU);
        pitou.invulnerableTime = 0;
        pitou.hurt(helper.getLevel().damageSources().fall(), 2f);
        helper.assertTrue(pitou.getHealth() < h0, "En Zetsu, le passif ne protège plus");

        // Leorio : récupère de la vie tout seul
        Zombie leorio = husk(helper, 2);
        hunter(leorio, NenType.EMITTER, "leorio");
        leorio.setHealth(10f);
        helper.runAfterDelay(110, () -> {
            helper.assertTrue(leorio.getHealth() > 10f, "Leorio (Médecin) aurait dû se soigner");
            helper.succeed();
        });
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
        helper.assertTrue(Hatsu.selectedTechnique(d) == Techniques.BASIC_ATTACK, "4 : attaque basique (coups de base à la suite)");
        Hatsu.cycle(gon);
        Hatsu.cycle(gon);
        helper.assertTrue(Hatsu.selectedTechnique(d) == Techniques.BASIC_CHARGED, "6 : frappe chargée");
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
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void baseMovesWorkWithoutNenAndEnduranceShortensCooldown(GameTestHelper helper) {
        Zombie fighter = husk(helper, 0);
        Zombie target = husk(helper, 2);
        HunterData d = HunterStats.get(fighter); // pas de Nen, pas de personnage
        helper.assertTrue(Hatsu.techniqueIds(d).equals(Techniques.BASE), "Sans personnage : seulement les coups de base");
        helper.assertTrue(Hatsu.selectedTechnique(d) == Techniques.BASIC_ATTACK, "Attaque basique sélectionnée");
        float health = target.getHealth();
        helper.assertTrue(Hatsu.cast(fighter) == null, "Attaque basique lancée sans Nen");
        helper.assertTrue(target.getHealth() < health, "La cible prend le coup");
        helper.assertTrue("message.hxh.hatsu_cooldown".equals(Hatsu.cast(fighter)), "Puis en recharge");
        int slow = Techniques.BASIC_KICK.cooldownFor(d);
        d.setLevel(com.evandouard.hxh.data.Stat.ENDURANCE, 100);
        int fast = Techniques.BASIC_KICK.cooldownFor(d);
        helper.assertTrue(fast < slow, "L'Endurance raccourcit la recharge du coup de pied (" + slow + " → " + fast + ")");
        // Frappe chargée : maintien puis relâchement
        helper.assertTrue(Hatsu.select(fighter, "basic_charged"), "Sélection par identifiant");
        helper.assertTrue(Hatsu.cast(fighter) == null && com.evandouard.hxh.combat.Combat.isCharging(fighter), "En charge");
        helper.runAfterDelay(10, () -> {
            Hatsu.releaseCast(fighter);
            helper.assertTrue(!com.evandouard.hxh.combat.Combat.isCharging(fighter), "Relâchée");
            helper.assertTrue(d.readyAt("basic_charged") > helper.getLevel().getGameTime(), "Recharge au relâchement");
            helper.succeed();
        });
    }
}
