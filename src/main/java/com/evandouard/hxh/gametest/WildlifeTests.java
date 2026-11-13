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
}
