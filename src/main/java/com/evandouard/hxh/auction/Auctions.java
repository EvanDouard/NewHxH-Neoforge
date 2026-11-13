package com.evandouard.hxh.auction;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.network.HxHNetwork;
import com.evandouard.hxh.network.OpenAuctionPayload;
import com.evandouard.hxh.quest.QuestData;
import com.evandouard.hxh.quest.QuestManager;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * Ventes aux enchères (York Shin) : chaque jour de jeu, quelques lots tirés au sort ; on enchérit contre des
 * enchérisseurs PNJ. Chaque mise du joueur est suivie de leur réponse : s'ils montent, on peut surenchérir ; si la
 * mise dépasse leur limite, le lot est adjugé tout de suite (Jenny débités, objet donné). Le serveur décide de tout.
 * État par joueur et par jour, dans ses données persistantes.
 */
public final class Auctions extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, Auction> AUCTIONS = Map.of();
    private static final String TAG = "hxh_auction";

    public enum Result { OUTBID, WON, NOT_ENOUGH_JENNY, SOLD, INVALID }

    /** Statut d'un lot pour un joueur. */
    public static final int OPEN = 0, RIVAL_LEADS = 1, WON_BY_YOU = 2;

    private Auctions() {
        super(new Gson(), "auctions");
    }

    public static void register(AddReloadListenerEvent event) {
        event.addListener(new Auctions());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, Auction> loaded = new TreeMap<>();
        files.forEach((id, json) -> {
            try {
                loaded.put(id, Auction.parse(id, json.getAsJsonObject()));
            } catch (Exception e) {
                HxHMod.LOGGER.error("Vente aux enchères {} invalide : {}", id, e.getMessage());
            }
        });
        AUCTIONS = Map.copyOf(loaded);
        HxHMod.LOGGER.info("{} salle(s) des ventes chargée(s)", loaded.size());
    }

    @Nullable
    public static Auction get(ResourceLocation id) {
        return AUCTIONS.get(id);
    }

    @Nullable
    public static Auction forNpc(String npcId) {
        for (Auction a : AUCTIONS.values()) if (a.npc().equals(npcId)) return a;
        return null;
    }

    public static Map<ResourceLocation, Auction> all() {
        return AUCTIONS;
    }

    // ---------- Lots du jour ----------

    public static long day(ServerPlayer player) {
        return player.serverLevel().getDayTime() / 24000L;
    }

    /** Indices (dans {@code auction.lots()}) des lots du jour : tirage pondéré, sans doublon, le même pour tout le monde. */
    public static List<Integer> todaysLots(Auction a, long day, long seed) {
        Random rnd = new Random(seed ^ (day * 0x9E3779B97F4A7C15L) ^ a.id().hashCode());
        List<Integer> pool = new ArrayList<>();
        for (int i = 0; i < a.lots().size(); i++) pool.add(i);
        List<Integer> out = new ArrayList<>();
        while (out.size() < a.lotsPerDay() && !pool.isEmpty()) {
            int total = 0;
            for (int i : pool) total += a.lots().get(i).weight();
            int roll = rnd.nextInt(total);
            for (int k = 0; k < pool.size(); k++) {
                roll -= a.lots().get(pool.get(k)).weight();
                if (roll < 0) {
                    out.add(pool.remove(k));
                    break;
                }
            }
        }
        return out;
    }

    /** Limite des enchérisseurs PNJ pour ce lot, ce jour, ce joueur : 75 à 125 % de la valeur estimée. */
    public static int ceiling(Auction.Lot lot, long day, long seed, int slot) {
        Random rnd = new Random(seed * 31 + day * 7919 + slot * 104729L + lot.value());
        return Math.max(lot.start(), Math.round(lot.value() * (0.75f + 0.5f * rnd.nextFloat())));
    }

    /** Pas d'enchère : 10 % du prix, arrondi à 50 Jenny, au moins 50. */
    public static int step(int price) {
        return Math.max(50, Math.round(price * 0.1f / 50f) * 50);
    }

    // ---------- État du joueur ----------

    private static CompoundTag state(ServerPlayer player, Auction a) {
        CompoundTag root = player.getPersistentData().getCompound(TAG);
        String key = a.id().toString();
        CompoundTag s = root.getCompound(key);
        if (s.getLong("day") != day(player) || !s.contains("day")) {
            s = new CompoundTag(); // nouveau jour : nouvelle vente
            s.putLong("day", day(player));
        }
        root.put(key, s);
        player.getPersistentData().put(TAG, root);
        return s;
    }

    private static long seed(ServerPlayer player) {
        return player.serverLevel().getSeed() ^ player.getUUID().getLeastSignificantBits();
    }

    /** Prix actuel du lot n° {@code slot} du jour (mise de départ s'il n'y a pas encore d'enchère). */
    public static int price(ServerPlayer player, Auction a, int slot) {
        CompoundTag s = state(player, a);
        int lot = todaysLots(a, day(player), player.serverLevel().getSeed()).get(slot);
        return s.contains("p" + slot) ? s.getInt("p" + slot) : a.lots().get(lot).start();
    }

    public static int status(ServerPlayer player, Auction a, int slot) {
        CompoundTag s = state(player, a);
        if (s.getBoolean("w" + slot)) return WON_BY_YOU;
        return s.contains("p" + slot) ? RIVAL_LEADS : OPEN;
    }

    /** Prochaine mise du joueur : la mise de départ, ou le prix actuel + un pas. */
    public static int nextBid(ServerPlayer player, Auction a, int slot) {
        int p = price(player, a, slot);
        return status(player, a, slot) == OPEN ? p : p + step(p);
    }

    /** Le joueur enchérit sur le lot n° {@code slot} du jour. */
    public static Result bid(ServerPlayer player, Auction a, int slot) {
        List<Integer> lots = todaysLots(a, day(player), player.serverLevel().getSeed());
        if (slot < 0 || slot >= lots.size()) return Result.INVALID;
        if (status(player, a, slot) == WON_BY_YOU) return Result.SOLD;
        Auction.Lot lot = a.lots().get(lots.get(slot));
        int bid = nextBid(player, a, slot);
        QuestData d = QuestManager.data(player);
        if (d.jenny() < bid) {
            player.displayClientMessage(Component.translatable("auction.hxh.not_enough", bid - d.jenny()), true);
            return Result.NOT_ENOUGH_JENNY;
        }
        CompoundTag s = state(player, a);
        int limit = ceiling(lot, day(player), seed(player), slot);
        if (bid >= limit) {
            // Plus personne : adjugé !
            d.addJenny(-bid);
            s.putInt("p" + slot, bid);
            s.putBoolean("w" + slot, true);
            ItemStack stack = lot.stack().copy();
            if (!player.getInventory().add(stack)) player.drop(stack, false);
            player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 0.5f, 1.6f);
            player.displayClientMessage(Component.translatable("auction.hxh.won", stack.getHoverName(), bid)
                    .withStyle(ChatFormatting.GOLD), false);
            return Result.WON;
        }
        // Un enchérisseur monte
        int rival = Math.min(limit, bid + step(bid));
        s.putInt("p" + slot, rival);
        int bidder = 2 + new Random(seed(player) + slot * 13L + rival).nextInt(98);
        player.displayClientMessage(Component.translatable("auction.hxh.outbid", bidder, rival).withStyle(ChatFormatting.YELLOW), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.NEUTRAL, 0.6f, 1.4f);
        return Result.OUTBID;
    }

    /** Ouvre (ou rafraîchit) l'écran de la vente chez le joueur. */
    public static void open(ServerPlayer player, Auction a) {
        List<Integer> lots = todaysLots(a, day(player), player.serverLevel().getSeed());
        List<OpenAuctionPayload.Line> lines = new ArrayList<>();
        for (int slot = 0; slot < lots.size(); slot++) {
            Auction.Lot lot = a.lots().get(lots.get(slot));
            lines.add(new OpenAuctionPayload.Line(lot.stack().copy(), price(player, a, slot), nextBid(player, a, slot), status(player, a, slot)));
        }
        HxHNetwork.sendTo(player, new OpenAuctionPayload(a.id().toString(), a.npc(), QuestManager.data(player).jenny(), lines));
    }
}
