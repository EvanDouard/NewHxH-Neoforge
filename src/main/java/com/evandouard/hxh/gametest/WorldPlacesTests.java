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
}
