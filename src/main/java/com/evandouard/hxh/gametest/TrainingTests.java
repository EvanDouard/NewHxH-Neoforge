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
}
