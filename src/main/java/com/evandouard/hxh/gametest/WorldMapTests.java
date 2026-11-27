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
        helper.assertTrue(map.widthBlocks() == 7968 * HxHWorldMap.SCALE && map.heightBlocks() == 5632 * HxHWorldMap.SCALE, "Taille de la carte : " + map.widthBlocks() + "×" + map.heightBlocks());
        helper.assertTrue(!HxHWorldMap.isLand(map.cellAt(0, 0)), "Le coin de la carte est de la mer");
        helper.assertTrue(!HxHWorldMap.isLand(map.cellAt(-500, 100)), "Hors de la carte : mer");
        for (AirshipDestination d : AirshipRegistry.all())
            helper.assertTrue(HxHWorldMap.isLand(map.cellAt(d.x(), d.z())), "Destination sur la terre : " + d.id());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hxhWorldHasItsOwnGenerator(GameTestHelper helper) {
        // Le serveur de GameTests n'a pas la dimension : on vérifie que le générateur sans structures vanilla est enregistré
        helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.CHUNK_GENERATOR.containsKey(HxHMod.id("world")),
                "Générateur hxh:world enregistré (pas de structures vanilla dans le monde HxH)");
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
        // Le serveur de GameTests n'a pas la dimension : on monte son générateur à la main avec les réglages lus dans les données
        var access = helper.getLevel().registryAccess();
        var biomes = access.lookupOrThrow(net.minecraft.core.registries.Registries.BIOME);
        java.util.function.Function<net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome>, net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>> b = biomes::getOrThrow;
        var source = new com.evandouard.hxh.world.HxHBiomeSource(b.apply(net.minecraft.world.level.biome.Biomes.OCEAN), b.apply(net.minecraft.world.level.biome.Biomes.DEEP_OCEAN),
                b.apply(net.minecraft.world.level.biome.Biomes.BEACH), b.apply(net.minecraft.world.level.biome.Biomes.PLAINS), b.apply(net.minecraft.world.level.biome.Biomes.TAIGA),
                b.apply(net.minecraft.world.level.biome.Biomes.SWAMP), b.apply(net.minecraft.world.level.biome.Biomes.FOREST), b.apply(net.minecraft.world.level.biome.Biomes.DARK_FOREST),
                b.apply(net.minecraft.world.level.biome.Biomes.JUNGLE), b.apply(net.minecraft.world.level.biome.Biomes.SAVANNA));
        var settings = access.lookupOrThrow(net.minecraft.core.registries.Registries.NOISE_SETTINGS)
                .getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.NOISE_SETTINGS, Airship.WORLD.location()));
        var gen = new net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator(source, settings);
        var rs = net.minecraft.world.level.levelgen.RandomState.create(settings.value(),
                access.lookupOrThrow(net.minecraft.core.registries.Registries.NOISE), 0L);
        AirshipDestination kukuroo = AirshipRegistry.get("hxh:kukuroo_mountain");
        int land = gen.getBaseHeight(kukuroo.x(), kukuroo.z(), Heightmap.Types.OCEAN_FLOOR_WG, helper.getLevel(), rs);
        helper.assertTrue(land >= 64, "Terre émergée à la montagne Kukuroo : " + land);
        int sea = gen.getBaseHeight(100, 100, Heightmap.Types.OCEAN_FLOOR_WG, helper.getLevel(), rs);
        helper.assertTrue(sea < HxHWorldMap.SEA_LEVEL - 5, "Fond marin profond au large : " + sea);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void placesOverrideRegionBiome(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME);
        var plains = biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS);
        var desert = biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.DESERT);
        var place = new com.evandouard.hxh.world.HxHBiomeSource.Place(1560, 810, 100, desert);
        var source = new com.evandouard.hxh.world.HxHBiomeSource(plains, plains, plains, plains, plains, plains, plains, plains, plains, plains,
                java.util.List.of(place));
        helper.assertTrue(source.placeBiome(1560, 810) == desert, "Biome du lieu en son centre");
        helper.assertTrue(source.placeBiome(1650, 810) == desert, "Biome du lieu dans le rayon");
        helper.assertTrue(source.placeBiome(1700, 810) == null, "Pas de biome de lieu hors du rayon");
        helper.assertTrue(source.possibleBiomes().contains(desert), "Le biome du lieu est déclaré au générateur");
        helper.succeed();
    }
}
