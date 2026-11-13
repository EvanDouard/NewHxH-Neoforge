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

    @GameTest(template = "empty")
    public static void auctionsOutbidThenSell(GameTestHelper helper) {
        var a = com.evandouard.hxh.auction.Auctions.forNpc("commissaire_priseur");
        helper.assertTrue(a != null, "La vente de York Shin est chargée");
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        QuestData d = QuestManager.data(player);
        d.addJenny(-d.jenny());
        long day = com.evandouard.hxh.auction.Auctions.day(player);
        long seed = player.serverLevel().getSeed();
        var lots = com.evandouard.hxh.auction.Auctions.todaysLots(a, day, seed);
        helper.assertTrue(lots.size() == a.lotsPerDay() && new java.util.HashSet<>(lots).size() == lots.size(), "Lots du jour sans doublon");
        helper.assertTrue(lots.equals(com.evandouard.hxh.auction.Auctions.todaysLots(a, day, seed)), "Même tirage pour tout le monde le même jour");
        // Gamme IV : l'haltère IV est bien parmi les lots possibles
        helper.assertTrue(a.lots().stream().anyMatch(l -> l.stack().getItem().toString().contains("dumbbell_4")), "Haltère IV aux enchères");
        helper.assertTrue(com.evandouard.hxh.auction.Auctions.bid(player, a, 0) == com.evandouard.hxh.auction.Auctions.Result.NOT_ENOUGH_JENNY, "Sans Jenny : refus");
        d.addJenny(1_000_000);
        var item = a.lots().get(lots.get(0)).stack().getItem();
        int outbids = 0;
        com.evandouard.hxh.auction.Auctions.Result r;
        int last = 0;
        do {
            int next = com.evandouard.hxh.auction.Auctions.nextBid(player, a, 0);
            helper.assertTrue(next > last, "Chaque mise monte (" + last + " -> " + next + ")");
            last = next;
            r = com.evandouard.hxh.auction.Auctions.bid(player, a, 0);
            if (r == com.evandouard.hxh.auction.Auctions.Result.OUTBID) {
                outbids++;
                helper.assertTrue(player.getInventory().countItem(item) == 0 && d.jenny() == 1_000_000, "Surenchéri : rien payé, rien reçu");
            }
        } while (r == com.evandouard.hxh.auction.Auctions.Result.OUTBID && outbids < 50);
        helper.assertTrue(r == com.evandouard.hxh.auction.Auctions.Result.WON, "Le lot finit adjugé (" + r + ")");
        helper.assertTrue(player.getInventory().countItem(item) >= 1, "Objet reçu");
        helper.assertTrue(d.jenny() == 1_000_000 - last, "Prix payé : la dernière mise (" + last + ")");
        helper.assertTrue(com.evandouard.hxh.auction.Auctions.bid(player, a, 0) == com.evandouard.hxh.auction.Auctions.Result.SOLD, "Un lot adjugé ne se revend pas");
        helper.assertTrue(com.evandouard.hxh.auction.Auctions.bid(player, a, 99) == com.evandouard.hxh.auction.Auctions.Result.INVALID, "Lot inexistant");
        helper.succeed();
    }
}
