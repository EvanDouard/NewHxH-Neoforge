package com.evandouard.hxh.gametest;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.mob.MobPreset;
import com.evandouard.hxh.mob.MobPresets;
import com.evandouard.hxh.quest.QuestManager;
import com.evandouard.hxh.stats.HunterStats;
import com.evandouard.hxh.world.NenWildlife;
import com.evandouard.hxh.world.WorldEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Mobs naturels au Nen et événements aléatoires (primes de l'Association). */
@GameTestHolder(HxHMod.MODID)
@PrefixGameTestTemplate(false)
public final class WildlifeTests {

    @GameTest(template = "empty")
    public static void someNaturalZombiesMasterNen(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        RandomSource random = RandomSource.create(7);
        int picked = 0;
        for (int i = 0; i < 2000; i++) if (NenWildlife.pick(zombie, random, 100) != null) picked++;
        // zombies : rapide 5 %, renforceur 3 %, costaud 4 % (tirés l'un après l'autre) ≈ 11,5 %
        helper.assertTrue(picked > 120 && picked < 360, "Une petite part des zombies maîtrise le Nen : " + picked + "/2000");
        int none = 0;
        for (int i = 0; i < 500; i++) if (NenWildlife.pick(zombie, random, 0) != null) none++;
        helper.assertTrue(none == 0, "Règle à 0 % : aucun");
        MobPreset preset = MobPresets.get("zombie_renforceur");
        NenWildlife.convert(zombie, preset);
        HunterData data = HunterStats.get(zombie);
        // (les mobs posés par un test sont toujours persistants : on vérifie seulement la conversion)
        helper.assertTrue(data.nenLevel() > 0 && zombie.getTags().contains("hxh_natural_nen"), "Converti au Nen");
        helper.assertTrue(zombie.getPersistentData().getInt(NenWildlife.BOUNTY) == preset.bounty(), "Prime posée");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void eventSpawnsBountiesThatPay(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        WorldEvents.WorldEvent event = WorldEvents.get("manieur_errant");
        helper.assertTrue(event != null && WorldEvents.all().size() >= 4, "Événements chargés");
        List<LivingEntity> mobs = WorldEvents.trigger(event, player);
        helper.assertTrue(mobs.size() == 1 && mobs.get(0).getTags().contains("hxh_event"), "Le manieur errant apparaît");
        LivingEntity mob = mobs.get(0);
        double d = mob.position().subtract(player.position()).horizontalDistance();
        helper.assertTrue(d >= event.minDistance() - 2 && d <= event.maxDistance() + 3, "À bonne distance : " + d);
        int before = QuestManager.data(player).jenny();
        mob.hurt(player.damageSources().playerAttack(player), 10000f);
        helper.assertTrue(QuestManager.data(player).jenny() == before + event.bounty(), "La prime est payée au tueur");
        mob.discard();
        helper.succeed();
    }

    @net.minecraft.gametest.framework.GameTest(template = "empty")
    public static void foxbearActsLikeAPolarBear(net.minecraft.gametest.framework.GameTestHelper helper) {
        var bear = helper.spawn(com.evandouard.hxh.registry.ModEntities.FOXBEAR.get(), new net.minecraft.core.BlockPos(2, 1, 2));
        helper.assertTrue(bear.getMaxHealth() >= 60f, "Renard-ours : 60 PV (" + bear.getMaxHealth() + ")");
        helper.assertTrue(bear.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) >= 9, "Gros dégâts");
        var cub = bear.getBreedOffspring(helper.getLevel(), bear);
        helper.assertTrue(cub instanceof com.evandouard.hxh.beast.FoxbearEntity, "Les petits sont des renards-ours");
        helper.assertTrue(bear.getLootTable().location().getPath().equals("entities/foxbear"), "Butin : " + bear.getLootTable().location());
        // Comme l'ours polaire : il ne chasse pas un joueur de lui-même (neutre)
        helper.assertTrue(bear.getTarget() == null, "Neutre au départ");
        bear.discard();
        helper.succeed();
    }

    @net.minecraft.gametest.framework.GameTest(template = "empty")
    public static void greatStampHasAnArmoredSnout(net.minecraft.gametest.framework.GameTestHelper helper) {
        var pig = helper.spawnWithNoFreeWill(com.evandouard.hxh.registry.ModEntities.GREAT_STAMP.get(), new net.minecraft.core.BlockPos(3, 1, 3));
        pig.setYRot(-90f); // regarde vers +X
        pig.setYBodyRot(-90f);
        pig.setYHeadRot(-90f);
        helper.assertTrue(!pig.isConverting(), "Jamais de zoglin dans le monde normal");
        var front = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.HUSK, new net.minecraft.core.BlockPos(5, 1, 3));
        var back = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.HUSK, new net.minecraft.core.BlockPos(1, 1, 3));
        helper.assertTrue(pig.hitOnSnout(front) && !pig.hitOnSnout(back), "Devant = museau, derrière = non");
        float h0 = pig.getHealth();
        pig.hurt(pig.damageSources().mobAttack(front), 10f);
        float frontLoss = h0 - pig.getHealth();
        pig.invulnerableTime = 0;
        float h1 = pig.getHealth();
        pig.hurt(pig.damageSources().mobAttack(back), 10f);
        float backLoss = h1 - pig.getHealth();
        helper.assertTrue(frontLoss < backLoss * 0.5f, "Le museau blindé encaisse : face " + frontLoss + ", dos " + backLoss);
        helper.assertTrue(pig.getMaxHealth() >= 50f, "50 PV");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void spiderEagleLaysEggsAndStaysWild(GameTestHelper helper) {
        var eagle = helper.spawn(com.evandouard.hxh.registry.ModEntities.SPIDER_EAGLE.get(), new BlockPos(1, 2, 1));
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WHEAT_SEEDS, 8));
        helper.assertTrue(eagle.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND) == net.minecraft.world.InteractionResult.PASS
                && !eagle.isTame(), "Ne s'apprivoise pas avec des graines");
        helper.assertTrue(eagle.getMaxHealth() == 16f, "16 PV : " + eagle.getMaxHealth());
        eagle.setEggTime(2);
        helper.runAfterDelay(10, () -> {
            var eggs = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    eagle.getBoundingBox().inflate(6), i -> i.getItem().is(com.evandouard.hxh.registry.ModItems.SPIDER_EAGLE_EGG.get()));
            helper.assertTrue(!eggs.isEmpty(), "Un œuf d'aigle-araignée est pondu");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void numeleApeAndChimeraAntsHunt(GameTestHelper helper) {
        var ape = helper.spawn(com.evandouard.hxh.registry.ModEntities.MAN_FACED_APE.get(), new BlockPos(1, 2, 1));
        helper.assertTrue(ape.getMaxHealth() == 24f, "Singe : 24 PV");
        String line = ((net.minecraft.network.chat.TranslatableContents) com.evandouard.hxh.beast.ManFacedApeEntity
                .lureLine(helper.getLevel().getRandom()).getContents()).getKey();
        helper.assertTrue(line.startsWith("dialog.hxh.man_faced_ape.lure_"), "Réplique trompeuse : " + line);
        ape.discard();
        // Fourmi-Chimère : chasse sans attendre la nuit (ici un villageois)
        var ant = helper.spawn(com.evandouard.hxh.registry.ModEntities.CHIMERA_ANT.get(), new BlockPos(1, 2, 1));
        var villager = helper.spawn(EntityType.VILLAGER, new BlockPos(4, 2, 4));
        helper.assertTrue(ant.getMaxHealth() == 40f && ant.getArmorValue() >= 6, "Fourmi : 40 PV, armure 6");
        // Le mob d'exemple naturel lui donne le Nen
        MobPreset preset = MobPresets.get("fourmi_chimere_soldat");
        helper.assertTrue(preset != null && preset.naturalChance() > 0, "Fourmi au Nen : mob d'exemple naturel");
        var ant2 = helper.spawn(com.evandouard.hxh.registry.ModEntities.CHIMERA_ANT.get(), new BlockPos(6, 2, 1));
        NenWildlife.convert(ant2, preset);
        HunterData d = HunterStats.get(ant2);
        helper.assertTrue(d.nenUnlocked() && d.nenLevel() == 35, "Fourmi convertie : Nen niveau " + d.nenLevel());
        helper.succeedWhen(() -> helper.assertTrue(ant.getTarget() == villager, "La fourmi prend le villageois pour cible"));
    }
}
