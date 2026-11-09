package com.evandouard.hxh.gametest;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.quest.QuestData;
import com.evandouard.hxh.quest.QuestManager;
import com.evandouard.hxh.shop.Shop;
import com.evandouard.hxh.shop.ShopRegistry;
import com.evandouard.hxh.shop.Shops;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Marchands : achat avec les Jenny, refus sans assez de Jenny, rachat des objets. */
@GameTestHolder(HxHMod.MODID)
@PrefixGameTestTemplate(false)
public final class ShopTests {

    private static Shop towerShop(GameTestHelper helper) {
        Shop shop = ShopRegistry.forNpc("merchant");
        helper.assertTrue(shop != null, "La boutique du marchand de la Tour est chargée");
        return shop;
    }

    @GameTest(template = "empty")
    public static void buyingCostsJennyAndGivesItem(GameTestHelper helper) {
        Shop shop = towerShop(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        QuestData d = QuestManager.data(player);
        d.addJenny(-d.jenny());
        Shop.Offer dumbbell = shop.sells().get(0);
        helper.assertTrue(dumbbell.stack().getItem().toString().contains("dumbbell_3"), "La première offre est l'haltère III");
        helper.assertTrue(Shops.buy(player, shop, 0) == Shops.Result.NOT_ENOUGH_JENNY, "Sans Jenny, pas d'achat");
        helper.assertFalse(player.getInventory().contains(dumbbell.stack()), "Rien reçu");
        d.addJenny(dumbbell.price() + 5);
        helper.assertTrue(Shops.buy(player, shop, 0) == Shops.Result.OK, "Achat accepté");
        helper.assertTrue(d.jenny() == 5, "Le prix est retiré, reste " + d.jenny());
        helper.assertTrue(player.getInventory().countItem(dumbbell.stack().getItem()) == 1, "L'haltère III est dans l'inventaire");
        helper.assertTrue(Shops.buy(player, shop, 99) == Shops.Result.INVALID, "Offre inexistante refusée");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sellingGivesJenny(GameTestHelper helper) {
        Shop shop = towerShop(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        QuestData d = QuestManager.data(player);
        d.addJenny(-d.jenny());
        int boneLine = -1;
        for (int i = 0; i < shop.buys().size(); i++) if (shop.buys().get(i).item() == Items.BONE) boneLine = i;
        helper.assertTrue(boneLine >= 0, "Le marchand rachète les os");
        int price = shop.buys().get(boneLine).price();
        helper.assertTrue(Shops.sell(player, shop, boneLine, false) == Shops.Result.NOTHING_TO_SELL, "Rien à vendre");
        player.getInventory().add(new ItemStack(Items.BONE, 10));
        helper.assertTrue(Shops.sell(player, shop, boneLine, false) == Shops.Result.OK, "Vente d'un os");
        helper.assertTrue(player.getInventory().countItem(Items.BONE) == 9 && d.jenny() == price, "Un os contre son prix");
        Shops.sell(player, shop, boneLine, true);
        helper.assertTrue(player.getInventory().countItem(Items.BONE) == 0 && d.jenny() == price * 10, "Tout vendu : " + d.jenny());
        helper.succeed();
    }
}
