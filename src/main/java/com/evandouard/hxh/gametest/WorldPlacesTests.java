package com.evandouard.hxh.gametest;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.world.WorldPlaces;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Lieux du monde HxH décrits en données : le village de l'île de la Baleine se bâtit avec ses maisons, portes et lits. */
@GameTestHolder(HxHMod.MODID)
@PrefixGameTestTemplate(false)
public final class WorldPlacesTests {

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void whaleIslandVillageIsBuilt(GameTestHelper helper) {
        var place = WorldPlaces.get("hxh:whale_island_village");
        helper.assertTrue(place != null, "Le village de l'île de la Baleine est chargé");
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        int ground = WorldPlaces.build(helper.getLevel(), place, origin.getX(), origin.getZ());
        var level = helper.getLevel();
        BlockState door = level.getBlockState(new BlockPos(origin.getX() + 4, ground, origin.getZ()));
        helper.assertTrue(door.is(Blocks.OAK_DOOR), "La maison de Mito a sa porte : " + door);
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 1, ground, origin.getZ() + 1)).is(Blocks.RED_BED), "Un lit dans la maison de Mito");
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 4, ground + 1, origin.getZ() + 3)).isAir(), "L'intérieur est dégagé");
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 11, ground, origin.getZ() + 11)).is(Blocks.WATER), "Le puits contient de l'eau");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void kukurooEstateHasGateKeeperAndMike(GameTestHelper helper) {
        var place = WorldPlaces.get("hxh:kukuroo_domaine_zoldyck");
        helper.assertTrue(place != null, "Le domaine Zoldyck est chargé");
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        var level = helper.getLevel();
        int ground = WorldPlaces.build(level, place, origin.getX(), origin.getZ());
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() - 9, ground, origin.getZ() + 1)).is(Blocks.IRON_DOOR), "Porte des visiteurs");
        for (int n = 1; n <= 7; n++) {
            BlockState door = level.getBlockState(new BlockPos(origin.getX(), ground + 1, origin.getZ() + 2 * n - 1));
            helper.assertTrue(door.is(com.evandouard.hxh.registry.ModBlocks.HEAVY_DOOR.get()) && door.getValue(com.evandouard.hxh.block.HeavyDoorBlock.DOOR) == n, "Porte n°" + n + " : " + door);
        }
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 19, ground, origin.getZ() - 11)).is(Blocks.BOOKSHELF), "Maison de Zebro meublée");
        var box = new net.minecraft.world.phys.AABB(origin.getX() - 40, ground - 5, origin.getZ() - 60, origin.getX() + 70, ground + 20, origin.getZ() + 130);
        var npcs = level.getEntitiesOfClass(com.evandouard.hxh.npc.NpcEntity.class, box);
        for (String id : new String[]{"zebro", "gotoh", "canary", "majordome_zoldyck"})
            helper.assertTrue(npcs.stream().anyMatch(n -> n.npcId().equals(id)), "PNJ présent : " + id);
        var mike = level.getEntitiesOfClass(net.minecraft.world.entity.animal.Wolf.class, box).stream()
                .filter(w -> w.hasCustomName() && w.getCustomName().getString().equals("Mike")).findFirst();
        helper.assertTrue(mike.isPresent(), "Mike, le chien géant, est là");
        helper.assertTrue(mike.get().getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SCALE) > 2.0, "Mike est géant : échelle " + mike.get().getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SCALE) + ", PV max " + mike.get().getMaxHealth());
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void heavyDoorOpensOnlyWithEnoughStrengthThenCloses(GameTestHelper helper) {
        var level = helper.getLevel();
        var block = com.evandouard.hxh.registry.ModBlocks.HEAVY_DOOR.get();
        BlockPos base = helper.absolutePos(new BlockPos(2, 1, 2));
        for (int x = 0; x < 3; x++) for (int y = 0; y < 4; y++)
            level.setBlock(base.offset(x, y, 0), block.defaultBlockState().setValue(com.evandouard.hxh.block.HeavyDoorBlock.DOOR, 5), 3);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(base), net.minecraft.core.Direction.NORTH, base, false);
        var weak = helper.makeMockServerPlayerInLevel();
        com.evandouard.hxh.stats.HunterStats.setLevel(weak, com.evandouard.hxh.data.Stat.STRENGTH, 10);
        level.getBlockState(base).useWithoutItem(level, weak, hit);
        helper.assertTrue(!level.getBlockState(base).getValue(com.evandouard.hxh.block.HeavyDoorBlock.OPEN), "Force 10 : la porte n°5 (32 t) ne bouge pas");
        var strong = helper.makeMockServerPlayerInLevel();
        com.evandouard.hxh.stats.HunterStats.setLevel(strong, com.evandouard.hxh.data.Stat.STRENGTH, 60);
        level.getBlockState(base).useWithoutItem(level, strong, hit);
        for (int x = 0; x < 3; x++) for (int y = 0; y < 4; y++)
            helper.assertTrue(level.getBlockState(base.offset(x, y, 0)).getValue(com.evandouard.hxh.block.HeavyDoorBlock.OPEN), "Toute la plaque s'ouvre (" + x + "," + y + ")");
        helper.runAfterDelay(160, () -> {
            for (int x = 0; x < 3; x++) for (int y = 0; y < 4; y++)
                helper.assertTrue(!level.getBlockState(base.offset(x, y, 0)).getValue(com.evandouard.hxh.block.HeavyDoorBlock.OPEN), "La plaque se referme seule");
            helper.succeed();
        });
    }
}
