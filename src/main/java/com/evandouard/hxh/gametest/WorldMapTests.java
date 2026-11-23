package com.evandouard.hxh.gametest;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.airship.Airship;
import com.evandouard.hxh.airship.AirshipDestination;
import com.evandouard.hxh.airship.AirshipRegistry;
import com.evandouard.hxh.world.HxHTerrainDensity;
import com.evandouard.hxh.world.HxHWorldMap;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Monde HxH : carte en grille, relief déduit de la carte, dimension générée (terre et mer aux bons endroits). */
@GameTestHolder(HxHMod.MODID)
@PrefixGameTestTemplate(false)
public final class WorldMapTests {

    @GameTest(template = "empty")
    public static void mapHasExpectedSizeAndLandAtDestinations(GameTestHelper helper) {
        HxHWorldMap map = HxHWorldMap.get();
        helper.assertTrue(map.widthBlocks() == 7968 && map.heightBlocks() == 5632, "Taille de la carte : " + map.widthBlocks() + "×" + map.heightBlocks());
        helper.assertTrue(!HxHWorldMap.isLand(map.cellAt(0, 0)), "Le coin de la carte est de la mer");
        helper.assertTrue(!HxHWorldMap.isLand(map.cellAt(-500, 100)), "Hors de la carte : mer");
        for (AirshipDestination d : AirshipRegistry.all())
            helper.assertTrue(HxHWorldMap.isLand(map.cellAt(d.x(), d.z())), "Destination sur la terre : " + d.id());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void terrainDensityFollowsTheMap(GameTestHelper helper) {
        HxHWorldMap map = HxHWorldMap.get();
        AirshipDestination whale = AirshipRegistry.get("hxh:whale_island");
        double land = map.height(whale.x(), whale.z());
        helper.assertTrue(land > HxHWorldMap.SEA_LEVEL, "L'île est au-dessus de la mer : " + land);
        double sea = map.height(100, 100);
        helper.assertTrue(sea < HxHWorldMap.SEA_LEVEL - 5, "Le large est sous la mer : " + sea);
        var density = HxHTerrainDensity.INSTANCE;
        helper.assertTrue(density.compute(new DensityFunction.SinglePointContext(whale.x(), (int) land - 10, whale.z())) > 0, "Densité positive sous le sol");
        helper.assertTrue(density.compute(new DensityFunction.SinglePointContext(whale.x(), (int) land + 10, whale.z())) < 0, "Densité négative dans l'air");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void dimensionGeneratesLandAndSea(GameTestHelper helper) {
        // Le serveur de GameTests n'instancie pas la dimension : on interroge directement son générateur (lu dans les données)
        var access = helper.getLevel().registryAccess();
        var stem = access.registryOrThrow(net.minecraft.core.registries.Registries.LEVEL_STEM)
                .get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LEVEL_STEM, Airship.WORLD.location()));
        helper.assertTrue(stem != null, "La dimension du monde HxH est déclarée");
        helper.assertTrue(stem.generator() instanceof net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator, "Générateur à bruit");
        var gen = (net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator) stem.generator();
        helper.assertTrue(gen.getBiomeSource() instanceof com.evandouard.hxh.world.HxHBiomeSource, "Biomes tirés de la carte");
        var rs = net.minecraft.world.level.levelgen.RandomState.create(gen.generatorSettings().value(),
                access.lookupOrThrow(net.minecraft.core.registries.Registries.NOISE), 0L);
        AirshipDestination kukuroo = AirshipRegistry.get("hxh:kukuroo_mountain");
        int land = gen.getBaseHeight(kukuroo.x(), kukuroo.z(), Heightmap.Types.OCEAN_FLOOR_WG, helper.getLevel(), rs);
        helper.assertTrue(land >= 64, "Terre émergée à la montagne Kukuroo : " + land);
        int sea = gen.getBaseHeight(100, 100, Heightmap.Types.OCEAN_FLOOR_WG, helper.getLevel(), rs);
        helper.assertTrue(sea < HxHWorldMap.SEA_LEVEL - 5, "Fond marin profond au large : " + sea);
        helper.succeed();
    }
}
