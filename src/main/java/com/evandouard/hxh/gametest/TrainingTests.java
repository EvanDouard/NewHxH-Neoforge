package com.evandouard.hxh.gametest;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.registry.ModBlocks;
import com.evandouard.hxh.registry.ModItems;
import com.evandouard.hxh.stats.HunterStats;
import com.evandouard.hxh.training.DumbbellItem;
import com.evandouard.hxh.training.TrainingEvents;
import com.evandouard.hxh.training.TreadmillBlock;
import com.evandouard.hxh.training.StrikingDummyBlock;
import com.evandouard.hxh.training.IceBathBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Objets d'entraînement : haltères (Force) et tapis de course (Vitesse ou Endurance), gammes I à IV. */
@GameTestHolder(HxHMod.MODID)
@PrefixGameTestTemplate(false)
public final class TrainingTests {

    @GameTest(template = "empty")
    public static void tiersTrainBetter(GameTestHelper helper) {
        double last = 0;
        for (var d : ModItems.DUMBBELLS) {
            double xp = d.get().xpPerRep();
            helper.assertTrue(xp > last, "Chaque gamme d'haltères rapporte plus que la précédente");
            last = xp;
        }
        last = 0;
        for (var t : ModBlocks.TREADMILLS) {
            double xp = t.get().xpPerSecond();
            helper.assertTrue(xp > last, "Chaque gamme de tapis rapporte plus que la précédente");
            last = xp;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dumbbellRepGivesStrengthXp(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        com.evandouard.hxh.family.Families.set(player, "");
        DumbbellItem dumbbell = ModItems.DUMBBELLS.get(1).get();
        ItemStack stack = new ItemStack(dumbbell);
        HunterData d = HunterStats.get(player);
        int before = d.xp(Stat.STRENGTH);
        int duration = dumbbell.getUseDuration(stack, player);
        dumbbell.onUseTick(helper.getLevel(), player, stack, duration - 10);
        helper.assertTrue(d.xp(Stat.STRENGTH) == before, "Pas d'XP avant la fin d'une répétition");
        dumbbell.onUseTick(helper.getLevel(), player, stack, duration - DumbbellItem.REP_TICKS);
        helper.assertTrue(d.xp(Stat.STRENGTH) - before == (int) dumbbell.xpPerRep(),
                "Une répétition d'haltères II : +" + dumbbell.xpPerRep() + " XP de Force, obtenu " + (d.xp(Stat.STRENGTH) - before));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void treadmillTrainsOneStatAtATime(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        TreadmillBlock block = ModBlocks.TREADMILLS.get(0).get();
        helper.setBlock(rel, block.defaultBlockState().setValue(TreadmillBlock.FACING, Direction.NORTH));
        BlockPos abs = helper.absolutePos(rel);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        com.evandouard.hxh.family.Families.set(player, ""); // pas de bonus de famille sur l'XP mesurée
        player.moveTo(abs.getX() + 0.5, abs.getY() + 0.375, abs.getZ() + 0.5);
        player.setOnGround(true);
        HunterData d = HunterStats.get(player);

        // Mode Vitesse : il faut sprinter
        player.setSprinting(true);
        int speed = d.xp(Stat.SPEED), endurance = d.xp(Stat.ENDURANCE);
        for (int i = 0; i < 20; i++) {
            player.moveTo(abs.getX() + 0.5, abs.getY() + 0.375, abs.getZ() + 0.5);
            TrainingEvents.tickTreadmill(player);
        }
        helper.assertTrue(d.xp(Stat.SPEED) - speed == (int) block.xpPerSecond(), "1 s de sprint : XP de Vitesse");
        helper.assertTrue(d.xp(Stat.ENDURANCE) == endurance, "Pas d'Endurance en mode Vitesse");
        helper.assertTrue(player.getDeltaMovement().z > 0, "Le tapis pousse vers l'arrière (sud quand on court au nord)");

        // Clic droit : mode Endurance, il faut marcher sans sprinter
        BlockState state = helper.getLevel().getBlockState(abs);
        state.useWithoutItem(helper.getLevel(), player, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
        helper.assertTrue(helper.getLevel().getBlockState(abs).getValue(TreadmillBlock.MODE) == TreadmillBlock.Mode.ENDURANCE,
                "Clic droit : passage en mode Endurance");
        speed = d.xp(Stat.SPEED);
        for (int i = 0; i < 20; i++) {
            player.moveTo(abs.getX() + 0.5, abs.getY() + 0.375, abs.getZ() + 0.5);
            TrainingEvents.tickTreadmill(player);
        }
        helper.assertTrue(d.xp(Stat.SPEED) == speed, "En mode Endurance, sprinter ne rapporte rien");
        player.setSprinting(false);
        for (int i = 0; i < 20; i++) {
            player.moveTo(abs.getX() + 0.5, abs.getY() + 0.375, abs.getZ() + 0.5);
            TrainingEvents.tickTreadmill(player);
        }
        helper.assertTrue(d.xp(Stat.ENDURANCE) - endurance == (int) block.xpPerSecond(), "1 s de marche : XP d'Endurance");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nutritiousDishGivesVitalityXp(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        com.evandouard.hxh.family.Families.set(player, "");
        var dish = ModItems.DISHES.get(0).get();
        HunterData d = HunterStats.get(player);
        player.getFoodData().setFoodLevel(4);
        int before = d.xp(Stat.VITALITY);
        dish.finishUsingItem(new ItemStack(dish), helper.getLevel(), player);
        helper.assertTrue(d.xp(Stat.VITALITY) > before, "Un plat nutritif rapporte de l'XP de Vitalité");
        helper.assertTrue(ModItems.DISHES.get(3).get().xpPerDish() > dish.xpPerDish(), "La gamme IV rapporte plus");
        helper.assertTrue(player.getFoodData().getFoodLevel() > 4, "Et il nourrit");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void meditationShrineMultipliesWillpower(GameTestHelper helper) {
        // Sanctuaire de niveau 3 : la pierre est au centre, méditer à côté multiplie la Volonté par 8
        var level = helper.getLevel();
        // loin de la zone des autres tests : le sanctuaire déborde du petit gabarit vide
        BlockPos centre = helper.absolutePos(new BlockPos(4, 1, 4)).offset(0, 0, 240);
        BlockPos stone = com.evandouard.hxh.meditation.MeditationShrines.build(level, centre, 3, false);
        helper.assertTrue(stone != null && level.getBlockState(stone).getBlock() instanceof com.evandouard.hxh.meditation.MeditationStoneBlock,
                "Pierre posée au centre");
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        com.evandouard.hxh.family.Families.set(player, "");
        player.moveTo(stone.getX() + 1.5, stone.getY(), stone.getZ() + 0.5);
        helper.assertTrue(com.evandouard.hxh.meditation.MeditationShrines.willMultiplier(player) == 8, "Volonté ×8 près d'une pierre III");
        player.moveTo(stone.getX() + 12.5, stone.getY(), stone.getZ() + 0.5);
        helper.assertTrue(com.evandouard.hxh.meditation.MeditationShrines.willMultiplier(player) == 1, "Loin de la pierre : ×1");
        helper.assertTrue(com.evandouard.hxh.meditation.MeditationShrines.Shrines.get(level).all().stream()
                .anyMatch(s -> s[0] == stone.asLong() && s[1] == 3), "Sanctuaire inscrit au registre (vendable par l'archéologue)");
        helper.assertTrue(!com.evandouard.hxh.meditation.MeditationShrines.buy(player, 5), "Niveau 5 jamais vendu");
        helper.succeed();
    }

    @net.minecraft.gametest.framework.GameTest(template = "empty")
    public static void armWrestlingRewardsSkillAndStrength(net.minecraft.gametest.framework.GameTestHelper helper) {
        // Un joueur qui suit la zone gagne ; un joueur qui ne pousse jamais perd
        var good = new com.evandouard.hxh.minigame.ArmWrestle.Match(40, 40, 100, 1L);
        for (int t = 0; t < 4000 && good.result == 0; t++) {
            good.holding = good.marker + good.velocity * 6 < good.zoneCenter;
            good.step();
        }
        helper.assertTrue(good.result == 1, "Suivre la zone : victoire (" + good.result + ", barre " + good.progress + ")");
        var lazy = new com.evandouard.hxh.minigame.ArmWrestle.Match(40, 40, 100, 2L);
        for (int t = 0; t < 4000 && lazy.result == 0; t++) lazy.step();
        helper.assertTrue(lazy.result == 2, "Ne jamais pousser : défaite");
        // Plus fort que l'adversaire : zone plus large
        var strong = new com.evandouard.hxh.minigame.ArmWrestle.Match(90, 20, 0, 3L);
        var weak = new com.evandouard.hxh.minigame.ArmWrestle.Match(20, 90, 0, 3L);
        helper.assertTrue(strong.zoneSize > weak.zoneSize, "La Force élargit la zone : " + strong.zoneSize + " / " + weak.zoneSize);
        // Mise : prise au départ
        net.minecraft.server.level.ServerPlayer p = helper.makeMockServerPlayerInLevel();
        var q = com.evandouard.hxh.quest.QuestManager.data(p);
        q.addJenny(-q.jenny());
        helper.assertTrue(!com.evandouard.hxh.minigame.ArmWrestle.start(p, 30, 100), "Sans Jenny : pas de défi");
        q.addJenny(100);
        helper.assertTrue(com.evandouard.hxh.minigame.ArmWrestle.start(p, 30, 100) && q.jenny() == 0, "Mise prise au départ");
        helper.assertTrue(com.evandouard.hxh.minigame.ArmWrestle.match(p) != null, "Manche en cours");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void strikingDummyHitsAnyCreatureButNeverKills(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        BlockState state = ModBlocks.STRIKING_DUMMY.get().defaultBlockState().setValue(StrikingDummyBlock.FACING, Direction.SOUTH);
        helper.setBlock(rel, state);
        net.minecraft.world.entity.monster.Zombie zombie = helper.spawnWithNoFreeWill(
                net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(1, 1, 2));
        BlockPos abs = helper.absolutePos(rel);
        float start = zombie.getHealth();
        for (int i = 0; i < 20; i++) {
            zombie.invulnerableTime = 0;
            zombie.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 1.5);
            StrikingDummyBlock.strike(helper.getLevel(), abs, state);
        }
        helper.assertTrue(zombie.getHealth() < start, "Le mannequin frappe aussi les mobs (vie " + zombie.getHealth() + ")");
        helper.assertTrue(zombie.isAlive() && zombie.getHealth() >= StrikingDummyBlock.MIN_HEALTH - StrikingDummyBlock.DAMAGE,
                "Le mannequin ne tue jamais : vie restante " + zombie.getHealth());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void strikingDummyDodgeTrainsSpeed(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        BlockState state = ModBlocks.STRIKING_DUMMY.get().defaultBlockState().setValue(StrikingDummyBlock.FACING, Direction.SOUTH);
        helper.setBlock(rel, state);
        BlockPos abs = helper.absolutePos(rel);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        com.evandouard.hxh.family.Families.set(player, "");
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 1.5);
        HunterData d = HunterStats.get(player);
        int speed = d.xp(Stat.SPEED);
        player.setShiftKeyDown(true);
        int n = StrikingDummyBlock.strike(helper.getLevel(), abs, state);
        helper.assertTrue(n == 1, "Le joueur devant le mannequin est visé (" + n + ")");
        helper.assertTrue(d.xp(Stat.SPEED) - speed == (int) StrikingDummyBlock.DODGE_XP,
                "Esquiver accroupi : +" + StrikingDummyBlock.DODGE_XP + " XP de Vitesse, obtenu " + (d.xp(Stat.SPEED) - speed));
        // derrière le mannequin : rien
        player.setShiftKeyDown(false);
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() - 1.5);
        helper.assertTrue(StrikingDummyBlock.strike(helper.getLevel(), abs, state) == 0, "Le mannequin ne frappe que devant lui");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void iceBathTrainsWillpowerWithoutFreezeDamage(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        com.evandouard.hxh.family.Families.set(player, "");
        HunterData d = HunterStats.get(player);
        int will = totalXp(d, Stat.WILLPOWER);
        for (int t = 1; t <= 400; t++) {
            player.tickCount = t;
            IceBathBlock.soak(player);
        }
        int gained = totalXp(d, Stat.WILLPOWER) - will;
        helper.assertTrue(gained == 20 * (int) IceBathBlock.XP_PER_SECOND,
                "20 s de bain glacé : +" + 20 * IceBathBlock.XP_PER_SECOND + " XP de Volonté, obtenu " + gained);
        // un mob aussi (le joueur de test est en créatif, donc insensible au gel)
        net.minecraft.world.entity.animal.Pig pig = helper.spawnWithNoFreeWill(
                net.minecraft.world.entity.EntityType.PIG, new BlockPos(2, 1, 2));
        for (int t = 1; t <= 400; t++) {
            pig.tickCount = t;
            IceBathBlock.soak(pig);
        }
        helper.assertTrue(pig.getTicksFrozen() > 0 && !pig.isFullyFrozen(),
                "Le givre monte sans atteindre les dégâts (" + pig.getTicksFrozen() + ")");
        helper.assertTrue(pig.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN), "Le bain ralentit aussi les mobs");
        helper.succeed();
    }

    /** XP cumulée depuis le niveau 0 (les montées de niveau remettent le compteur du niveau à zéro). */
    private static int totalXp(HunterData d, Stat stat) {
        int total = d.xp(stat);
        for (int l = 0; l < d.level(stat); l++) total += Stat.xpToNext(l);
        return total;
    }
}
