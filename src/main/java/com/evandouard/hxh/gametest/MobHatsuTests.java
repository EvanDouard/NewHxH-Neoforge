package com.evandouard.hxh.gametest;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.hatsu.Characters;
import com.evandouard.hxh.hatsu.Hatsu;
import com.evandouard.hxh.hatsu.HatsuKits;
import com.evandouard.hxh.hatsu.HeldStrike;
import com.evandouard.hxh.hatsu.MobKits;
import com.evandouard.hxh.hatsu.MobTechniques;
import com.evandouard.hxh.hatsu.Technique;
import com.evandouard.hxh.hatsu.Techniques;
import com.evandouard.hxh.nen.NenHealing;
import com.evandouard.hxh.nen.NenMobAI;
import com.evandouard.hxh.nen.NenModes;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Hatsu des mobs, IA de combat au Nen, Ging, Silva, Uvogin, soin par niveaux. */
@GameTestHolder(HxHMod.MODID)
@PrefixGameTestTemplate(false)
public final class MobHatsuTests {

    private static HunterData awaken(Mob m) {
        HunterData d = HunterStats.get(m);
        NenModes.unlock(m);
        d.setNenLevel(60);
        d.setAura(d.auraMax());
        return d;
    }

    @GameTest(template = "empty")
    public static void eachMobGetsItsOwnHatsu(GameTestHelper helper) {
        Zombie z = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        HunterData d = awaken(z);
        NenMobAI.tick(z);
        helper.assertTrue("mob_zombie".equals(d.kit()), "Le zombie reçoit le Hatsu des zombies : " + d.kit());
        helper.assertTrue(HatsuKits.get("mob_zombie") != null && !HatsuKits.playable().contains(HatsuKits.get("mob_zombie")),
                "Kit de mob trouvable mais jamais proposé au joueur");
        helper.assertTrue(Hatsu.choose(helper.makeMockServerPlayerInLevel(), "mob_zombie") != null, "Un joueur ne peut pas le choisir");
        helper.assertTrue(MobKits.all().size() >= 12, "Un Hatsu pour chaque espèce courante");
        // chaque technique de mob part sans erreur
        Zombie target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 1));
        z.setTarget(target);
        StringBuilder fails = new StringBuilder();
        for (var kit : MobKits.all()) {
            for (String id : kit.techniques()) {
                Technique t = Techniques.get(id);
                try {
                    t.perform(z, 1f);
                    com.evandouard.hxh.hatsu.NenTasks.flushForTests();
                } catch (RuntimeException e) {
                    fails.append(id).append(" : ").append(e).append("; ");
                }
            }
        }
        helper.assertTrue(fails.isEmpty(), "Techniques de mob en erreur : " + fails);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nenZombiesLeaveNoLoot(GameTestHelper helper) {
        Zombie z = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        awaken(z);
        MobTechniques.NEN_HORDE.perform(z, 1f);
        var clones = helper.getLevel().getEntitiesOfClass(Zombie.class, z.getBoundingBox().inflate(6),
                c -> c.getTags().contains(MobTechniques.NEN_CLONE));
        helper.assertTrue(clones.size() == 2, "Deux zombies de Nen : " + clones.size());
        helper.assertTrue(!MobTechniques.NEN_HORDE.perform(clones.get(0), 1f), "Un zombie de Nen n'en crée pas d'autres");
        clones.forEach(Zombie::discard);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void gonMobsHaveNoUltimateAndWeakerJajanken(GameTestHelper helper) {
        Zombie z = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(1, 1, 1));
        HunterData d = awaken(z);
        d.setNenType(NenType.ENHANCER);
        d.setKit("gon");
        helper.assertTrue(Hatsu.powerFor(z, d, Techniques.GON_ROCK) < Hatsu.power(d, Techniques.GON_ROCK), "Jajanken de mob affaibli");
        Hatsu.select(z, "gon_rock_epic");
        Zombie target = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(3, 1, 1));
        // l'IA ne lance jamais l'ultime, même seule technique prête
        for (String id : HatsuKits.get("gon").techniques())
            if (!id.equals("gon_rock_epic")) d.setReadyAt(id, helper.getLevel().getGameTime() + 10000);
        NenMobAI.tryCast(z, target);
        helper.assertTrue(!com.evandouard.hxh.hatsu.NenCharge.isCharging(z), "Pas de Jajanken ultime pour un mob");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void gingCopyGoesIntoTheList(GameTestHelper helper) {
        Zombie ging = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(1, 1, 1));
        HunterData d = awaken(ging);
        d.setNenType(NenType.EMITTER);
        d.setKit("ging");
        d.setNenLevel(20);
        ging.getPersistentData().putString(Characters.GING_SEEN, "killua_godspeed");
        helper.assertTrue(Characters.GING_COPY.perform(ging, 1f), "Copie réussie");
        helper.assertTrue(Hatsu.techniqueIds(d).contains("killua_godspeed"), "La copie apparaît dans la liste");
        helper.assertTrue(Math.round(d.kitValue(Characters.GING_COPY_USES)) == 1, "Une utilisation au début");
        Hatsu.select(ging, "killua_godspeed");
        helper.assertTrue(Hatsu.cast(ging) == null, "La copie part");
        helper.assertTrue(!Hatsu.techniqueIds(d).contains("killua_godspeed"), "Plus d'utilisation : la copie disparaît");
        d.setNenLevel(80);
        Characters.GING_COPY.perform(ging, 1f);
        helper.assertTrue(Math.round(d.kitValue(Characters.GING_COPY_USES)) == 5, "Haut niveau : plusieurs utilisations");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void uvoginChargesBigBang(GameTestHelper helper) {
        Zombie uvo = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(1, 1, 1));
        HunterData d = awaken(uvo);
        d.setNenType(NenType.ENHANCER);
        d.setKit("uvogin");
        Hatsu.select(uvo, "uvogin_big_bang_impact");
        helper.assertTrue(Hatsu.cast(uvo) == null && HeldStrike.isHolding(uvo), "Big Bang Impact en charge");
        helper.runAfterDelay(20, () -> {
            float c = HeldStrike.charge(uvo);
            helper.assertTrue(c > 0.3f && c < 1f || !HeldStrike.isHolding(uvo), "La charge monte : " + c);
            Hatsu.releaseCast(uvo);
            helper.assertTrue(!HeldStrike.isHolding(uvo), "Relâché : le coup part");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void silvaHasAGreatOrb(GameTestHelper helper) {
        helper.assertTrue(HatsuKits.get("silva").techniques().contains("silva_great_orb"), "Grande boule de Silva");
        helper.assertTrue(Characters.SILVA_GREAT_ORB.isCharged() && !((Techniques.ChargedOrb) Characters.SILVA_GREAT_ORB).isEpic(),
                "Chargée vite, et sans destruction");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void healingSpeedsUpWithLevel(GameTestHelper helper) {
        helper.assertTrue(NenHealing.healPerSecond(100) >= 20, "Beaucoup de PV/s au niveau max : " + NenHealing.healPerSecond(100));
        helper.assertTrue(NenHealing.healPerSecond(0) <= 1.5, "Lent au début");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void illusionWallOpensThenCloses(GameTestHelper helper) {
        BlockPos a = new BlockPos(2, 1, 2), b = a.above();
        helper.setBlock(a, com.evandouard.hxh.registry.ModBlocks.ILLUSION_WALL.get());
        helper.setBlock(b, com.evandouard.hxh.registry.ModBlocks.ILLUSION_WALL.get());
        com.evandouard.hxh.block.IllusionWallBlock.open(helper.getLevel(), helper.absolutePos(a), 0);
        helper.assertTrue(helper.getBlockState(a).getValue(com.evandouard.hxh.block.IllusionWallBlock.OPEN)
                && helper.getBlockState(b).getValue(com.evandouard.hxh.block.IllusionWallBlock.OPEN), "Le passage s'ouvre d'un clic");
        helper.assertTrue(helper.getBlockState(a).getCollisionShape(helper.getLevel(), helper.absolutePos(a)).isEmpty(), "On passe au travers");
        helper.runAfterDelay(com.evandouard.hxh.block.IllusionWallBlock.OPEN_TICKS + 5, () -> {
            helper.assertTrue(!helper.getBlockState(a).getValue(com.evandouard.hxh.block.IllusionWallBlock.OPEN), "Le mur revient");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void spottingNoLongerUsesVanillaGlow(GameTestHelper helper) {
        // Chaîne radiesthésique : repérage dessiné pour le seul joueur, plus de Lueur de Minecraft visible par tous
        var player = helper.makeMockServerPlayerInLevel();
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        player.tickCount = 40;
        com.evandouard.hxh.hatsu.Passives.DOWSING_CHAIN.tick(player);
        helper.assertTrue(!zombie.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING), "Pas de Lueur vanilla");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void deadMobDissolvesBeforeVanillaPoof(GameTestHelper helper) {
        helper.assertTrue(com.evandouard.hxh.combat.DeathFx.handles(new Zombie(EntityType.ZOMBIE, helper.getLevel())), "Mob géré");
        helper.assertTrue(!com.evandouard.hxh.combat.DeathFx.handles(helper.makeMockServerPlayerInLevel()), "Joueur jamais retiré");
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        zombie.kill();
        // le corps disparaît au 19e tick de chute, avant le nuage de Minecraft (20e)
        helper.runAfterDelay(com.evandouard.hxh.combat.DeathFx.LAST_DEATH_TICK + 2, () -> {
            helper.assertTrue(zombie.isRemoved(), "Corps retiré");
            helper.assertTrue(zombie.deathTime <= com.evandouard.hxh.combat.DeathFx.LAST_DEATH_TICK, "Retiré avant le nuage");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void vanillaMobsOnlyGetWeakCopiesOfCharacterTechniques(GameTestHelper helper) {
        // Toute technique de personnage portée par un mob de Minecraft : affaiblie et sans ultime (décision du 12 nov)
        Zombie z = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        HunterData d = awaken(z);
        d.setNenType(NenType.SPECIALIST);
        d.setKit("zeno");
        helper.assertTrue(Hatsu.isCopy(z, d), "Zombie au kit de Zeno = copie");
        var dive = com.evandouard.hxh.hatsu.Techniques.get("zeno_dragon_dive");
        helper.assertTrue(dive != null && dive.isUltimate(), "Plongée du dragon = ultime");
        helper.assertTrue(Hatsu.powerFor(z, d, dive) < Hatsu.power(d, dive), "Copie affaiblie");
        d.setKit("mob_zombie");
        helper.assertTrue(!Hatsu.isCopy(z, d), "Le Hatsu de son espèce n'est pas une copie");
        var player = helper.makeMockServerPlayerInLevel();
        HunterData pd = HunterStats.get(player);
        pd.setKit("zeno");
        helper.assertTrue(!Hatsu.isCopy(player, pd), "Un joueur a le vrai kit");
        helper.assertTrue(Techniques.GON_ROCK_EPIC.isUltimate() && !Techniques.GON_ROCK.isUltimate(), "Seule la Pierre épique est ultime");
        helper.succeed();
    }
}
