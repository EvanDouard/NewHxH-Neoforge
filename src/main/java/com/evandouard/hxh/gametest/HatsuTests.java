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
        helper.assertTrue(Math.abs(Hatsu.power(d, Techniques.GON_PAPER) - 0.9f) < 1e-4, "Papier (Émission + Renforcement) : 90 %");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void techniqueTypesPhysicalAndScarletEyes(GameTestHelper helper) {
        // toutes les techniques à deux types existent
        for (String id : com.evandouard.hxh.hatsu.NenAffinity.secondaryIds()) {
            helper.assertTrue(Techniques.get(id) != null, "Technique à deux types inconnue : " + id);
        }
        Zombie z = husk(helper, 1);
        HunterData d = hunter(z, NenType.SPECIALIST, null);
        // Physique : coups de base et coups secrets (Main-griffe comprise) à 100 % pour tout le monde
        for (var t : new com.evandouard.hxh.hatsu.Technique[]{Techniques.BASIC_KICK, Techniques.BASIC_ATTACK,
                com.evandouard.hxh.hatsu.FamilyMoves.ZOLDYCK_CLAW, com.evandouard.hxh.hatsu.FamilyMoves.KURTA_SCARLET}) {
            helper.assertTrue(com.evandouard.hxh.hatsu.NenAffinity.physical(t), t.id() + " devrait être Physique");
            helper.assertTrue(com.evandouard.hxh.hatsu.NenAffinity.of(z, d, t) == 1f, t.id() + " : 100 % pour un Spécialiste");
        }
        // Spécialiste contre Renforcement (opposé) : 40 %, zone à 70 %
        helper.assertTrue(Math.abs(com.evandouard.hxh.hatsu.NenAffinity.of(z, d, Techniques.GON_ROCK) - 0.4f) < 1e-4, "Pierre : 40 % pour un Spécialiste");
        helper.assertTrue(Math.abs(com.evandouard.hxh.hatsu.NenAffinity.sizeFactor(z, Techniques.GON_ROCK) - 0.7f) < 1e-4, "Zone à 70 % à l'opposé");
        // deux types : moyenne (Foudre de Killua = Transformation + Émission, pour un Spécialiste : 60 % et 60 %)
        helper.assertTrue(com.evandouard.hxh.hatsu.NenAffinity.types(Techniques.KILLUA_THUNDERBOLT).size() == 2, "Foudre : deux types");
        helper.assertTrue(Math.abs(com.evandouard.hxh.hatsu.NenAffinity.of(z, d, Techniques.KILLUA_THUNDERBOLT) - 0.6f) < 1e-4,
                "Foudre : 60 % et 60 %");
        // Yeux écarlates : Spécialisation, toutes les catégories à 100 %
        d.setNenType(NenType.ENHANCER);
        z.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.evandouard.hxh.effect.ModEffects.SCARLET_EYES, 200));
        helper.assertTrue(com.evandouard.hxh.hatsu.NenAffinity.typeOf(z, d) == NenType.SPECIALIST, "Yeux écarlates : Spécialisation");
        for (var t : new com.evandouard.hxh.hatsu.Technique[]{Techniques.GON_ROCK, Techniques.PITOU_TERPSICHORA, Techniques.KILLUA_THUNDERBOLT}) {
            helper.assertTrue(com.evandouard.hxh.hatsu.NenAffinity.of(z, d, t) == 1f, t.id() + " à 100 % sous les yeux écarlates");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void kurtaClanLevelLengthensScarletEyes(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        com.evandouard.hxh.family.Families.set(player, "kurta");
        HunterData d = HunterStats.get(player);
        com.evandouard.hxh.family.Families.setClanLevel(player, 0);
        int will0 = d.level(com.evandouard.hxh.data.Stat.WILLPOWER);
        helper.assertTrue(com.evandouard.hxh.hatsu.FamilyMoves.KURTA_SCARLET.perform(player, 1f), "Colère écarlate lancée");
        var eyes = player.getEffect(com.evandouard.hxh.effect.ModEffects.SCARLET_EYES);
        helper.assertTrue(eyes != null && eyes.getDuration() == 300, "Clan 0 : 15 s de yeux écarlates, obtenu " + (eyes == null ? 0 : eyes.getDuration()));
        helper.assertFalse(player.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST)
                || player.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE), "Plus de Force ni de Résistance");
        player.removeAllEffects();
        com.evandouard.hxh.family.Families.setClanLevel(player, 100);
        helper.assertTrue(d.clanLevel() == 100, "Niveau de clan 100");
        com.evandouard.hxh.hatsu.FamilyMoves.KURTA_SCARLET.perform(player, 1f);
        eyes = player.getEffect(com.evandouard.hxh.effect.ModEffects.SCARLET_EYES);
        helper.assertTrue(eyes != null && eyes.getDuration() == 6000, "Clan 100 : 5 min de yeux écarlates");
        helper.assertTrue(com.evandouard.hxh.hatsu.FamilyMoves.KURTA_SCARLET.cooldownFor(d) >= 6000, "La recharge part après les yeux");
        // les stats de clan : Volonté +10 au clan 100 (xp_bonus 0,2 × 50)
        helper.assertTrue(d.clanBonus(com.evandouard.hxh.data.Stat.WILLPOWER) == 10
                        && d.level(com.evandouard.hxh.data.Stat.WILLPOWER) == Math.min(100, will0 + 10),
                "Clan 100 : Volonté +10 (" + will0 + " → " + d.level(com.evandouard.hxh.data.Stat.WILLPOWER) + ")");
        // le carnet (onglet Personnage) montre la famille, le niveau de clan et l'héritage
        String sheet = com.evandouard.hxh.stats.ProfileText.statsAndNen(player).stream()
                .map(c -> c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t ? t.getKey() : "")
                .reduce("", (a, b) -> a + " " + b);
        helper.assertTrue(sheet.contains("profile.hxh.family") && sheet.contains("profile.hxh.clan_level_max")
                && sheet.contains("profile.hxh.family_heritage"), "Carnet : famille, clan et héritage dans Personnage (" + sheet + ")");
        // l'entraînement nourrit le clan ; sans famille, rien
        com.evandouard.hxh.family.Families.setClanLevel(player, 0);
        HunterStats.addXp(player, com.evandouard.hxh.data.Stat.STRENGTH, 500);
        helper.assertTrue(d.clanXp() > 0, "L'XP de stat donne de l'XP de clan");
        com.evandouard.hxh.family.Families.set(player, "");
        helper.assertTrue(d.clanBonus(com.evandouard.hxh.data.Stat.WILLPOWER) == 0, "Sans famille : aucun bonus de clan");
        player.removeAllEffects();
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
        // les techniques de zone des tests voisins (qui tournent en même temps) ne doivent pas la tuer
        target.setInvulnerable(true);
        helper.runAfterDelay(3, () -> {
            // on replace les deux husks juste avant (rien ne doit les avoir bougés entre-temps)
            hisoka.setYRot(-90f);
            hisoka.setXRot(0f);
            target.teleportTo(hisoka.getX() + 2, hisoka.getY(), hisoka.getZ());
            target.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            helper.assertTrue(Techniques.HISOKA_BUNGEE_GUM.perform(hisoka, 1f), "Le Bungee Gum colle la cible devant (cible vivante : "
                    + target.isAlive() + ", retirée : " + target.isRemoved() + ", lanceur vivant : " + hisoka.isAlive()
                    + ", regard " + hisoka.getLookAngle() + ", dernier coup : "
                    + (target.getLastDamageSource() == null ? "aucun" : target.getLastDamageSource().getMsgId()
                    + " par " + target.getLastDamageSource().getEntity()) + ", PV " + target.getHealth() + ")");
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
        HunterData pd = HunterStats.get(pitou);
        pd.setAuraMax(500);
        pd.setAura(500);
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

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void doctorBlytheIsHeldAndDrainsAura(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer pitou = helper.makeMockServerPlayerInLevel();
        HunterData d = HunterStats.get(pitou);
        d.setNenType(NenType.SPECIALIST);
        d.setAuraMax(500);
        d.setAura(500);
        helper.assertTrue(Techniques.PITOU_DOCTOR_BLYTHE.isCharged(), "Docteur Blythe se maintient");
        pitou.setHealth(4f);
        // (patient = Pitou lui-même : en jeu c'est la cible devant lui, ici d'autres tests tournent à côté)
        var doll = (Techniques.DollHeal) Techniques.PITOU_DOCTOR_BLYTHE;
        com.evandouard.hxh.hatsu.LingeringNen.startHeldDoll(pitou, pitou, doll, 2f, doll.drain());
        helper.assertTrue(com.evandouard.hxh.hatsu.LingeringNen.isHoldingDoll(pitou), "La poupée opère tant qu'on tient");
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(com.evandouard.hxh.hatsu.LingeringNen.isHoldingDoll(pitou), "Toujours là après 2 s (pas de durée fixe)");
            helper.assertTrue(d.aura() < 485f, "L'aura baisse en continu : " + d.aura());
            helper.assertTrue(pitou.getHealth() > 8f, "Pitou se soigne : " + pitou.getHealth());
            com.evandouard.hxh.hatsu.Hatsu.releaseCast(pitou);
            helper.assertFalse(com.evandouard.hxh.hatsu.LingeringNen.isHoldingDoll(pitou), "Relâché : la poupée disparaît");
            helper.assertTrue(d.readyAt(Techniques.PITOU_DOCTOR_BLYTHE.id()) > pitou.level().getGameTime(), "La recharge part au relâchement");
            float left = d.aura();
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(d.aura() >= left - 0.01f, "Plus de consommation après le relâchement");
                d.setReadyAt(Techniques.PITOU_DOCTOR_BLYTHE.id(), 0);
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void characterVowsHaveTheirOwnRules(GameTestHelper helper) {
        com.evandouard.hxh.hatsu.Vows.Vow CHAR = com.evandouard.hxh.hatsu.Vows.Vow.CHARACTER;
        // Kurapika : Chaîne-prison ×3 contre la Brigade seulement, sinon la mort
        Zombie kurapika = husk(helper, 0);
        HunterData d = hunter(kurapika, NenType.CONJURER, "kurapika");
        helper.assertTrue(com.evandouard.hxh.hatsu.Vows.take(kurapika, "character") == null, "Serment de Kurapika prêté");
        helper.assertTrue(com.evandouard.hxh.hatsu.Vows.powerMultiplier(d, Techniques.KURAPIKA_CHAIN_JAIL) == 3f, "Chaîne-prison ×3");
        helper.assertTrue(com.evandouard.hxh.hatsu.Vows.statMultiplier(d) == 1f, "Les stats ne bougent pas");
        Zombie nobunaga = husk(helper, 2);
        hunter(nobunaga, NenType.ENHANCER, "nobunaga");
        com.evandouard.hxh.combat.AreaScaling.casting(kurapika, Techniques.KURAPIKA_CHAIN_JAIL);
        float before = nobunaga.getHealth();
        nobunaga.invulnerableTime = 0;
        nobunaga.hurt(Techniques.damageFrom(kurapika), 5f);
        helper.assertTrue(nobunaga.getHealth() < before && kurapika.isAlive(), "Contre un membre de la Brigade : ça porte");
        net.minecraft.world.entity.animal.Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 1, 3));
        pig.hurt(Techniques.damageFrom(kurapika), 5f);
        helper.assertTrue(pig.getHealth() == pig.getMaxHealth(), "Hors de la Brigade : le coup ne porte pas");
        helper.assertTrue(!kurapika.isAlive() && d.vow().isEmpty(), "Serment rompu et Kurapika meurt");
        // Feitan : Soleil levant seulement blessé (refus, pas de rupture)
        Zombie feitan = husk(helper, 4);
        HunterData f = hunter(feitan, NenType.TRANSMUTER, "feitan");
        helper.assertTrue(com.evandouard.hxh.hatsu.Vows.take(feitan, "character") == null, "Serment de Feitan prêté");
        helper.assertTrue("message.hxh.vow_character_hurt".equals(com.evandouard.hxh.hatsu.Vows.entityRefusal(feitan, f,
                com.evandouard.hxh.hatsu.Specials.FEITAN_RISING_SUN)), "En pleine forme : refusé");
        feitan.setHealth(feitan.getMaxHealth() * 0.4f);
        helper.assertTrue(com.evandouard.hxh.hatsu.Vows.entityRefusal(feitan, f, com.evandouard.hxh.hatsu.Specials.FEITAN_RISING_SUN) == null,
                "Blessé : autorisé");
        helper.assertTrue(com.evandouard.hxh.hatsu.Vows.of(f) == CHAR, "Toujours sous serment");
        // Un personnage sans serment propre
        Zombie gon = husk(helper, 6);
        hunter(gon, NenType.ENHANCER, "gon");
        helper.assertTrue("message.hxh.vow_no_character".equals(com.evandouard.hxh.hatsu.Vows.take(gon, "character")), "Gon n'en a pas");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dayAndNightVowsSwitchWithTheSun(GameTestHelper helper) {
        Zombie z = husk(helper, 0);
        HunterData d = hunter(z, NenType.ENHANCER, "gon");
        d.setLevel(com.evandouard.hxh.data.Stat.STRENGTH, 50);
        helper.assertTrue(com.evandouard.hxh.hatsu.Vows.take(z, "overtime") == null, "Heure supplémentaire prêtée");
        d.setVowNight(false);
        helper.assertTrue(com.evandouard.hxh.hatsu.Vows.statMultiplier(d) == 0.7f, "Le jour : ×0,7");
        helper.assertTrue(d.level(com.evandouard.hxh.data.Stat.STRENGTH) == 35, "Force 50 → 35 le jour : " + d.level(com.evandouard.hxh.data.Stat.STRENGTH));
        d.setVowNight(true);
        helper.assertTrue(com.evandouard.hxh.hatsu.Vows.statMultiplier(d) == 1.3f, "La nuit : ×1,3");
        helper.assertTrue(com.evandouard.hxh.hatsu.Vows.powerMultiplier(d, Techniques.KURAPIKA_CHAIN_JAIL) == 1.3f, "Techniques ×1,3 la nuit");
        helper.assertTrue("message.hxh.vow_already".equals(com.evandouard.hxh.hatsu.Vows.take(z, "day_shift")),
                "Service de jour refusé tant qu'Heure supplémentaire est prise");
        com.evandouard.hxh.hatsu.Vows.breakVow(z, "vow.hxh.reason.renounced");
        d.setSealedUntil(0);
        helper.assertTrue(com.evandouard.hxh.hatsu.Vows.take(z, "day_shift") == null, "Service de jour prêté");
        d.setVowNight(true);
        helper.assertTrue(com.evandouard.hxh.hatsu.Vows.statMultiplier(d) == 0.7f, "Service de jour : ×0,7 la nuit");
        d.setVowNight(false);
        helper.assertTrue(com.evandouard.hxh.hatsu.Vows.statMultiplier(d) == 1.3f, "Service de jour : ×1,3 le jour");
        helper.succeed();
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

    @GameTest(template = "empty")
    public static void vowsStrengthenAndSeal(GameTestHelper helper) {
        Zombie z = husk(helper, 1);
        HunterData d = hunter(z, NenType.EMITTER, "leorio");
        long now = z.level().getGameTime();
        d.setAura(d.auraMax());
        var punch = Techniques.get("leorio_remote_punch");
        float base = Hatsu.powerFor(z, d, punch);
        // Une seule technique : celle sélectionnée au serment
        Hatsu.select(z, "leorio_remote_punch");
        helper.assertTrue(com.evandouard.hxh.hatsu.Vows.take(z, "single_technique") == null, "Serment de technique unique prêté");
        helper.assertTrue(Math.abs(Hatsu.powerFor(z, d, punch) - base * 1.6f) < 1e-3, "Technique visée ×1,6");
        helper.assertTrue("message.hxh.vow_single".equals(Hatsu.castRefusal(d, Techniques.get("leorio_shockwave"), now)), "Autre technique refusée");
        helper.assertTrue(Hatsu.castRefusal(d, Techniques.BASIC_ATTACK, now) == null, "Coups de base toujours permis");
        helper.assertTrue(com.evandouard.hxh.hatsu.Vows.take(z, "life_price") != null, "Un seul serment à la fois");
        // Renoncer = rompre : Hatsu scellé
        com.evandouard.hxh.hatsu.Vows.breakVow(z, "vow.hxh.reason.renounced");
        helper.assertTrue(d.vow().isEmpty(), "Serment effacé");
        helper.assertTrue("message.hxh.vow_sealed".equals(Hatsu.castRefusal(d, punch, now)), "Hatsu scellé après la rupture");
        helper.assertTrue(com.evandouard.hxh.hatsu.Vows.take(z, "life_price") != null, "Pas de nouveau serment pendant le sceau");
        d.setSealedUntil(0);
        // Payer de sa vie : 2 PV par technique
        helper.assertTrue(com.evandouard.hxh.hatsu.Vows.take(z, "life_price") == null, "Serment de vie prêté");
        z.setHealth(20f);
        d.setAura(d.auraMax());
        Hatsu.cast(z);
        helper.assertTrue(z.getHealth() <= 18.01f, "La technique a coûté 2 PV (" + z.getHealth() + ")");
        d.setVow("", "", 0L);
        // Seulement contre les monstres : toucher un cochon rompt le serment
        net.minecraft.world.entity.animal.Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 1, 1));
        pig.setNoGravity(true);
        z.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, pig.getBoundingBox().getCenter());
        helper.assertTrue(com.evandouard.hxh.hatsu.Vows.take(z, "monsters_only") == null, "Serment des monstres prêté");
        d.setAura(d.auraMax());
        d.setReadyAt("leorio_remote_punch", 0);
        float pigHp = pig.getHealth();
        Hatsu.cast(z);
        helper.assertTrue(pig.getHealth() == pigHp, "Monstres seulement : le coup sur un animal ne porte pas");
        helper.assertTrue("monsters_only".equals(d.vow()), "Le serment n'est pas rompu");
        // Stats multipliées sous serment, et pause
        d.setLevel(com.evandouard.hxh.data.Stat.STRENGTH, 20);
        helper.assertTrue(d.level(com.evandouard.hxh.data.Stat.STRENGTH) == 30 && d.baseLevel(com.evandouard.hxh.data.Stat.STRENGTH) == 20, "Force ×1,5 sous serment");
        helper.assertTrue("message.hxh.vow_paused".equals(com.evandouard.hxh.hatsu.Vows.toggle(z)) && d.level(com.evandouard.hxh.data.Stat.STRENGTH) == 20, "En pause : plus de bonus");
        com.evandouard.hxh.hatsu.Vows.toggle(z);
        d.setVow("", "", 0L);
        d.setSealedUntil(0);
        // Tout sacrifier : ×3 pendant 60 s, puis scellé une journée
        helper.assertTrue(com.evandouard.hxh.hatsu.Vows.take(z, "ultimate") == null, "Serment ultime prêté");
        helper.assertTrue(Math.abs(Hatsu.powerFor(z, d, punch) - base * 3f) < 1e-3, "Serment ultime ×3");
        d.setVow("ultimate", "", z.level().getGameTime() - 1);
        com.evandouard.hxh.hatsu.Vows.tick(z);
        helper.assertTrue(d.vow().isEmpty() && d.nenSealedUntil() - z.level().getGameTime() > 200000, "Après le serment ultime : tout le Nen scellé une dizaine de jours");
        d.setReadyAt("leorio_remote_punch", 0);
        d.setAura(d.auraMax());
        helper.assertTrue("message.hxh.nen_sealed".equals(Hatsu.castRefusal(d, punch, z.level().getGameTime())), "Plus de technique");
        d.setNenSealedUntil(0);
        d.setSealedUntil(0);
        helper.succeed();
    }
}
