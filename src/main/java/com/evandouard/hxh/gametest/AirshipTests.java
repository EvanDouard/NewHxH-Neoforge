package com.evandouard.hxh.gametest;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.airship.Airship;
import com.evandouard.hxh.airship.AirshipDestination;
import com.evandouard.hxh.airship.AirshipRegistry;
import com.evandouard.hxh.quest.QuestData;
import com.evandouard.hxh.quest.QuestManager;
import com.evandouard.hxh.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Dirigeable : destinations chargées, refus (inconnue, sans borne, sans Jenny), voyage payant vers une plateforme avec borne. */
@GameTestHolder(HxHMod.MODID)
@PrefixGameTestTemplate(false)
public final class AirshipTests {

    @GameTest(template = "empty")
    public static void destinationsAreLoaded(GameTestHelper helper) {
        helper.assertTrue(AirshipRegistry.all().size() >= 10, "Au moins 10 destinations : " + AirshipRegistry.all().size());
        AirshipDestination whale = AirshipRegistry.get("hxh:whale_island");
        helper.assertTrue(whale != null && whale.price() > 0, "Île de la Baleine présente avec un prix");
        helper.assertTrue(AirshipRegistry.get("hxh:nulle_part") == null, "Destination inconnue → null");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void arrivalIsFreeAndWelcomeHappensOnce(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        QuestData d = QuestManager.data(player);
        int before = d.jenny();
        Airship.arrive(player, AirshipRegistry.get("hxh:whale_island"));
        helper.assertTrue(d.jenny() == before, "L'arrivée automatique ne coûte rien");
        helper.assertTrue(helper.getLevel().getGameRules().getBoolean(com.evandouard.hxh.HxHGameRules.SPAWN_IN_HXH_WORLD), "Règle hxhSpawnInWorld active par défaut");
        player.getPersistentData().putBoolean(Airship.WELCOMED_TAG, true);
        helper.assertTrue(!Airship.welcome(player), "Un joueur déjà accueilli n'est pas téléporté de nouveau");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void switchWorldRefusesWithoutHxhDimension(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        if (helper.getLevel().getServer().getLevel(Airship.WORLD) == null) {
            helper.assertTrue("message.hxh.world.missing".equals(Airship.switchWorld(player, true)), "Sans dimension HxH : refus");
        } else {
            helper.assertTrue("message.hxh.world.already_normal".equals(Airship.switchWorld(player, false)) || player.level().dimension().equals(Airship.WORLD), "Déjà dans le monde normal : refus");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ticketPriceGrowsWithDistance(GameTestHelper helper) {
        int near = Airship.farePrice(100 * 20, 1000), mid = Airship.farePrice(2000 * 20, 1000), far = Airship.farePrice(8000 * 20, 1000);
        helper.assertTrue(near < mid && mid < far, "Plus c'est loin, plus c'est cher : " + near + " < " + mid + " < " + far);
        helper.assertTrue(Airship.farePrice(0, 0) >= 10 && Airship.farePrice(500 * 20, 0) % 10 == 0, "Prix minimal et arrondi à 10");
        helper.assertTrue(Airship.farePrice(2000 * 20, 3000) > Airship.farePrice(2000 * 20, 0), "Le prestige du lieu s'ajoute");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void travelChecksThenCharges(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        QuestData d = QuestManager.data(player);
        d.addJenny(-d.jenny());
        AirshipDestination dest = AirshipRegistry.get("hxh:kukuroo_mountain");
        helper.assertTrue("message.hxh.airship.unknown".equals(Airship.travel(player, "hxh:nulle_part", false)), "Destination inconnue refusée");
        helper.assertTrue("message.hxh.airship.no_terminal".equals(Airship.travel(player, dest.id().toString(), true)), "Sans borne à portée : refus");
        helper.assertTrue("message.hxh.airship.not_enough".equals(Airship.travel(player, dest.id().toString(), false)), "Sans Jenny : refus");
        helper.assertTrue(d.jenny() == 0, "Rien n'est prélevé en cas de refus");
        int fare = Airship.ticketPrice(player, dest);
        d.addJenny(fare + 7);
        helper.assertTrue(Airship.travel(player, dest.id().toString(), false) == null, "Voyage accepté avec assez de Jenny");
        helper.assertTrue(d.jenny() == 7, "Le billet est prélevé, reste " + d.jenny());
        helper.assertTrue(Math.abs(player.getX() - (dest.x() + 0.5)) < 1 && Math.abs(player.getZ() - (dest.z() + 0.5)) < 1,
                "Arrivée à la destination : " + player.position());
        // Le serveur de GameTests n'instancie pas les dimensions des données : repli sur le monde de test
        helper.assertTrue(player.level().dimension().equals(Airship.WORLD) || helper.getLevel().getServer().getLevel(Airship.WORLD) == null, "Arrivée dans la dimension du monde HxH : " + player.level().dimension().location());
        BlockPos terminal = BlockPos.containing(dest.x() + 2, player.getY(), dest.z());
        helper.assertTrue(player.level().getBlockState(terminal).is(ModBlocks.AIRSHIP_TERMINAL.get()), "Une borne attend à l'arrivée pour repartir");
        helper.assertTrue(player.level().getBlockState(player.blockPosition().below()).isSolid(), "Le joueur arrive sur une plateforme solide");
        helper.succeed();
    }

    /** Bouton dev de la carte : téléportation gratuite n'importe où dans le monde, refusée hors des bornes. */
    @GameTest(template = "empty")
    public static void devTeleportIsFreeAndBounded(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        QuestData d = QuestManager.data(player);
        d.addJenny(-d.jenny());
        helper.assertTrue(Airship.devTeleport(player, 30000, 20000), "TP accepté dans le monde");
        helper.assertTrue(Math.abs(player.getX() - 30000.5) < 1 && Math.abs(player.getZ() - 20000.5) < 1, "Arrivée au point demandé : " + player.position());
        helper.assertTrue(d.jenny() == 0, "Aucun Jenny prélevé");
        double x = player.getX();
        helper.assertTrue(!Airship.devTeleport(player, -5, 100), "Hors du monde (x<0) refusé");
        helper.assertTrue(!Airship.devTeleport(player, 100, 10_000_000), "Hors du monde (z trop grand) refusé");
        helper.assertTrue(player.getX() == x, "Pas de déplacement après un refus");
        helper.succeed();
    }
}
