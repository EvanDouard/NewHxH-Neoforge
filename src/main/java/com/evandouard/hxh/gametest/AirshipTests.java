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
    public static void travelChecksThenCharges(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        QuestData d = QuestManager.data(player);
        d.addJenny(-d.jenny());
        AirshipDestination dest = AirshipRegistry.get("hxh:kukuroo_mountain");
        helper.assertTrue("message.hxh.airship.unknown".equals(Airship.travel(player, "hxh:nulle_part", false)), "Destination inconnue refusée");
        helper.assertTrue("message.hxh.airship.no_terminal".equals(Airship.travel(player, dest.id().toString(), true)), "Sans borne à portée : refus");
        helper.assertTrue("message.hxh.airship.not_enough".equals(Airship.travel(player, dest.id().toString(), false)), "Sans Jenny : refus");
        helper.assertTrue(d.jenny() == 0, "Rien n'est prélevé en cas de refus");
        d.addJenny(dest.price() + 7);
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
}
