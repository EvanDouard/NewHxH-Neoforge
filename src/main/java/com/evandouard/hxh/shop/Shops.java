package com.evandouard.hxh.shop;

import com.evandouard.hxh.network.HxHNetwork;
import com.evandouard.hxh.network.OpenShopPayload;
import com.evandouard.hxh.quest.QuestData;
import com.evandouard.hxh.quest.QuestManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Achat et vente chez un marchand, côté serveur (le client n'envoie que « acheter l'offre n » / « vendre la ligne n »).
 * Les Jenny sont ceux du carnet (gagnés à la Tour Céleste et en quêtes).
 */
public final class Shops {
    private Shops() {}

    public enum Result { OK, NOT_ENOUGH_JENNY, NOTHING_TO_SELL, INVALID, LOCKED }

    /** L'offre est-elle achetable par ce joueur (quête requise terminée) ? */
    public static boolean unlocked(ServerPlayer player, Shop.Offer o) {
        return o.requires().isEmpty() || QuestManager.data(player).isCompleted(o.requires());
    }

    /** Ouvre (ou rafraîchit) l'écran de la boutique chez le joueur. */
    public static void open(ServerPlayer player, Shop shop) {
        QuestData d = QuestManager.data(player);
        List<OpenShopPayload.Line> sells = new ArrayList<>();
        // owned = -1 : offre verrouillée (quête pas encore faite)
        for (Shop.Offer o : shop.sells()) sells.add(new OpenShopPayload.Line(o.stack().copy(), o.price(), unlocked(player, o) ? 0 : -1));
        List<OpenShopPayload.Line> buys = new ArrayList<>();
        for (Shop.Buy b : shop.buys()) buys.add(new OpenShopPayload.Line(new ItemStack(b.item()), b.price(), count(player.getInventory(), b.item())));
        HxHNetwork.sendTo(player, new OpenShopPayload(shop.id().toString(), shop.npc(), d.jenny(), sells, buys));
    }

    public static Result buy(ServerPlayer player, Shop shop, int index) {
        if (index < 0 || index >= shop.sells().size()) return Result.INVALID;
        Shop.Offer o = shop.sells().get(index);
        QuestData d = QuestManager.data(player);
        if (!unlocked(player, o)) {
            player.displayClientMessage(Component.translatable("shop.hxh.locked"), true);
            return Result.LOCKED;
        }
        if (d.jenny() < o.price()) {
            player.displayClientMessage(Component.translatable("shop.hxh.not_enough", o.price() - d.jenny()), true);
            return Result.NOT_ENOUGH_JENNY;
        }
        d.addJenny(-o.price());
        ItemStack stack = o.stack().copy();
        if (!player.getInventory().add(stack)) player.drop(stack, false);
        player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.8f, 1.1f);
        return Result.OK;
    }

    /** Vend une unité (ou tout le stock si {@code all}) d'une ligne de rachat. */
    public static Result sell(ServerPlayer player, Shop shop, int index, boolean all) {
        if (index < 0 || index >= shop.buys().size()) return Result.INVALID;
        Shop.Buy b = shop.buys().get(index);
        Inventory inv = player.getInventory();
        int have = count(inv, b.item());
        if (have <= 0) return Result.NOTHING_TO_SELL;
        int amount = all ? have : 1;
        int removed = remove(inv, b.item(), amount);
        QuestManager.data(player).addJenny(removed * b.price());
        player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 0.6f, 1.2f);
        return Result.OK;
    }

    static int count(Inventory inv, Item item) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item) && !s.isDamaged() && !s.isEnchanted()) n += s.getCount();
        }
        return n;
    }

    private static int remove(Inventory inv, Item item, int amount) {
        int left = amount;
        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.is(item) || s.isDamaged() || s.isEnchanted()) continue;
            int take = Math.min(left, s.getCount());
            s.shrink(take);
            left -= take;
        }
        inv.setChanged();
        return amount - left;
    }
}
