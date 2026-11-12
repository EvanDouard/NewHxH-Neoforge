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
        // Tous les personnages de la fiche sont maintenant jouables
        helper.assertTrue(HatsuKits.playable().size() == 43, "43 personnages jouables attendus : " + HatsuKits.playable().size());
        helper.assertTrue(HatsuKits.all().size() == 43, "Plus aucun personnage à l'état d'idée");
        Zombie z = husk(helper, 1);
        hunter(z, NenType.TRANSMUTER, null);
        helper.assertTrue(Hatsu.choose(z, "personnage_inconnu") != null, "Un personnage inconnu n'est pas choisissable");
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
        helper.assertTrue(Hatsu.selectedTechnique(d) == Techniques.GON_ROCK_EPIC, "4 : Pierre sans retenue");
        Hatsu.cycle(gon);
        helper.assertTrue(Hatsu.selectedTechnique(d) == Techniques.BASIC_ATTACK, "5 : attaque basique (coups de base à la suite)");
        Hatsu.cycle(gon);
        Hatsu.cycle(gon);
        helper.assertTrue(Hatsu.selectedTechnique(d) == Techniques.BASIC_CHARGED, "7 : frappe chargée");
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

    @GameTest(template = "empty")
    public static void ryuOnAttackGrowsSpecialMoves(GameTestHelper helper) {
        Zombie gon = husk(helper, 1);
        HunterData d = hunter(gon, NenType.ENHANCER, "gon");
        d.setRyuAttack(50);
        helper.assertTrue(com.evandouard.hxh.nen.NenDefense.ryuSpecialFactor(gon) == 1f, "Ryu équilibré : taille normale");
        d.setRyuAttack(100);
        helper.assertTrue(Math.abs(com.evandouard.hxh.nen.NenDefense.ryuSpecialFactor(gon) - 1.5f) < 1e-4, "Ryu 100 % : ×1,5");
        helper.assertTrue(((Techniques.ChargedOrb) Techniques.GON_ROCK_EPIC).isEpic()
                && !((Techniques.ChargedOrb) Techniques.GON_ROCK).isEpic(), "Pierre normale et sans retenue");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void paperConeHitsAllInFront(GameTestHelper helper) {
        Zombie gon = husk(helper, 0);
        HunterData d = hunter(gon, NenType.ENHANCER, "gon");
        Zombie a = husk(helper, 2);
        Zombie b = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(2, 1, 2));
        b.setNoGravity(true);
        Zombie behind = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(0, 1, 0));
        behind.setNoGravity(true);
        helper.runAfterDelay(3, () -> {
            java.util.List<net.minecraft.world.entity.LivingEntity> hit =
                    ((Techniques.PalmWave) Techniques.GON_PAPER).targets(gon);
            helper.assertTrue(hit.contains(a) && hit.contains(b), "Le cône touche les deux cibles devant");
            helper.assertFalse(hit.contains(behind), "Pas la cible sur le côté/derrière");
            float hp = a.getHealth();
            Techniques.GON_PAPER.perform(gon, 1f);
            helper.assertTrue(a.getHealth() < hp, "Dégâts");
            helper.assertTrue(a.getDeltaMovement().x > 0.5, "Projetée dans la direction du regard");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void bungeeGumAttachesThenSnaps(GameTestHelper helper) {
        Zombie hisoka = husk(helper, 0);
        hunter(hisoka, NenType.TRANSMUTER, "hisoka");
        Zombie target = husk(helper, 2);
        helper.runAfterDelay(3, () -> {
            // on replace les deux husks juste avant (rien ne doit les avoir bougés entre-temps)
            hisoka.setYRot(-90f);
            hisoka.setXRot(0f);
            target.teleportTo(hisoka.getX() + 2, hisoka.getY(), hisoka.getZ());
            target.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            helper.assertTrue(Techniques.HISOKA_BUNGEE_GUM.perform(hisoka, 1f), "Le Bungee Gum colle la cible devant");
            helper.assertTrue(com.evandouard.hxh.hatsu.LingeringNen.gumTarget(hisoka) == target, "Lien actif sur la cible");
            helper.assertTrue(Techniques.HISOKA_BUNGEE_GUM.perform(hisoka, 1f), "Relance : contraction");
            helper.assertTrue(com.evandouard.hxh.hatsu.LingeringNen.gumTarget(hisoka) == null, "Le lien est consommé");
            helper.assertTrue(target.getDeltaMovement().x < -0.1, "La cible est projetée vers Hisoka");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void doctorBlytheHealsPatientOverTime(GameTestHelper helper) {
        Zombie pitou = husk(helper, 0);
        hunter(pitou, NenType.SPECIALIST, "neferpitou");
        Zombie patient = husk(helper, 2);
        helper.runAfterDelay(3, () -> {
            patient.setHealth(4f);
            Techniques.PITOU_DOCTOR_BLYTHE.perform(pitou, 1f);
            helper.assertTrue(com.evandouard.hxh.hatsu.LingeringNen.isOperating(pitou), "La poupée opère");
            helper.assertTrue(patient.getHealth() <= 4.01f, "Le soin n'est pas instantané");
            helper.runAfterDelay(85, () -> {
                helper.assertTrue(patient.getHealth() >= 15f, "Le patient a récupéré ~16 PV, a " + patient.getHealth());
                helper.assertFalse(com.evandouard.hxh.hatsu.LingeringNen.isOperating(pitou), "Opération terminée");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty")
    public static void hisokaThrillScalesWithTargetHealth(GameTestHelper helper) {
        Zombie hisoka = husk(helper, 0);
        Zombie weak = husk(helper, 2);
        float base = com.evandouard.hxh.hatsu.Passives.HISOKA_THRILL.modifyOutgoingMelee(hisoka, weak, 10f);
        weak.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);
        float strong = com.evandouard.hxh.hatsu.Passives.HISOKA_THRILL.modifyOutgoingMelee(hisoka, weak, 10f);
        helper.assertTrue(base == 10f, "Cible à 20 PV : pas de bonus");
        helper.assertTrue(Math.abs(strong - 13f) < 1e-3, "Cible très robuste : +30 % max");
        helper.succeed();
    }
}
