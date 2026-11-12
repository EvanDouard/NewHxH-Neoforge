package com.evandouard.hxh.travel;

import com.evandouard.hxh.npc.NpcEntity;
import com.evandouard.hxh.quest.QuestData;
import com.evandouard.hxh.quest.QuestManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Voyages en dirigeable : on parle au guichetier d'un aéroport (PNJ « guichetier » posé avec le repère de PNJ).
 * Chaque aéroport visité devient une destination ; le billet se paie en Jenny selon la distance, gratuit avec la
 * licence de Hunter. Le vol se passe en fondu (écran noir, titre « En vol »), puis on descend devant le guichet
 * d'arrivée.
 */
public final class AirTravel {
    public static final String CLERK = "guichetier";
    /** Distance max au guichetier pour acheter un billet par le lien du chat. */
    public static final double DESK_RANGE = 8;
    public static final int MIN_PRICE = 50;
    /** Un Jenny par {@code BLOCKS_PER_JENNY} blocs de vol. */
    public static final int BLOCKS_PER_JENNY = 10;
    /** Durée du vol vue par le joueur (écran noir), en ticks. */
    public static final int FLIGHT_TICKS = 60;

    private static final Set<UUID> FLYING = new HashSet<>();

    private AirTravel() {}

    /** Prix du billet (0 avec la licence de Hunter, et entre deux dimensions : prix minimal). */
    public static int price(ServerPlayer player, Airports.Airport from, Airports.Airport to) {
        if (QuestManager.data(player).isHunter()) return 0;
        if (!from.dim.equals(to.dim)) return MIN_PRICE * 4;
        double d = Math.sqrt(from.blockPos().distSqr(to.blockPos()));
        return Math.max(MIN_PRICE, (int) Math.round(d / BLOCKS_PER_JENNY));
    }

    /** Clic droit sur le guichetier : l'aéroport s'ouvre au joueur, puis la liste des vols cliquables. */
    public static void talk(ServerPlayer player, NpcEntity clerk) {
        Airports all = Airports.get(player.server);
        String dim = player.level().dimension().location().toString();
        BlockPos home = clerk.home();
        Airports.Airport here = all.at(dim, home, Component.translatable("travel.hxh.default_name", home.getX(), home.getZ()).getString());
        if (all.visit(here, player.getUUID())) {
            com.evandouard.hxh.network.Hud.toast(player, Component.translatable("travel.hxh.toast.new"),
                    Component.literal(here.name), "✈", com.evandouard.hxh.network.Hud.GOLD);
        }
        say(player, Component.translatable("travel.hxh.welcome", here.name));
        List<Airports.Airport> dests = all.destinations(player.getUUID(), here);
        if (dests.isEmpty()) {
            say(player, Component.translatable("travel.hxh.no_destination"));
            return;
        }
        for (Airports.Airport to : dests) {
            int price = price(player, here, to);
            MutableComponent line = Component.literal("  ✈ ").append(Component.literal(to.name).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal("  "))
                    .append(price == 0 ? Component.translatable("travel.hxh.free") : Component.translatable("travel.hxh.price", price));
            player.sendSystemMessage(line.withStyle(s -> s.withUnderlined(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hunter fly " + to.id))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("travel.hxh.fly_hover", to.name)))));
        }
    }

    /** Lien « ✈ destination » : vérifie le guichet, la destination, le prix, puis décolle. */
    public static boolean fly(ServerPlayer player, int toId) {
        if (FLYING.contains(player.getUUID())) return false;
        Airports all = Airports.get(player.server);
        String dim = player.level().dimension().location().toString();
        Airports.Airport here = all.nearest(dim, player.blockPosition(), DESK_RANGE);
        Airports.Airport to = all.byId(toId);
        if (here == null) return refuse(player, "travel.hxh.not_at_desk");
        if (to == null || to == here || !to.visitedBy(player.getUUID())) return refuse(player, "travel.hxh.unknown_destination");
        ServerLevel target = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(to.dim)));
        if (target == null) return refuse(player, "travel.hxh.unknown_destination");
        int price = price(player, here, to);
        QuestData d = QuestManager.data(player);
        if (d.jenny() < price) return refuse(player, Component.translatable("travel.hxh.too_poor", price, d.jenny()));
        if (price > 0) {
            d.addJenny(-price);
            player.sendSystemMessage(Component.translatable("travel.hxh.paid", price, d.jenny()).withStyle(ChatFormatting.YELLOW));
        }
        FLYING.add(player.getUUID());
        // Embarquement : l'écran s'assombrit, titre « En vol », bruit des hélices
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, FLIGHT_TICKS + 30, 0, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FLIGHT_TICKS, 6, false, false, false));
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, FLIGHT_TICKS, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("travel.hxh.in_flight").withStyle(ChatFormatting.GOLD)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("travel.hxh.towards", to.name)));
        player.level().playSound(null, player.blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.6f, 0.7f);
        com.evandouard.hxh.hatsu.NenTasks.later(FLIGHT_TICKS, () -> land(player, target, to));
        return true;
    }

    private static void land(ServerPlayer player, ServerLevel target, Airports.Airport to) {
        FLYING.remove(player.getUUID());
        if (!player.isAlive() || player.hasDisconnected()) return;
        BlockPos desk = to.blockPos();
        target.getChunk(desk.getX() >> 4, desk.getZ() >> 4);
        // devant le guichet, sur un bloc libre
        BlockPos spot = desk.offset(2, 0, 0);
        for (int i = 0; i < 4 && !target.isEmptyBlock(spot); i++) spot = spot.above();
        player.teleportTo(target, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, player.getYRot(), 0);
        player.removeEffect(MobEffects.BLINDNESS);
        player.sendSystemMessage(Component.translatable("travel.hxh.landed", to.name).withStyle(ChatFormatting.GREEN));
        target.playSound(null, spot, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.4f, 1.6f);
    }

    public static boolean isFlying(ServerPlayer player) {
        return FLYING.contains(player.getUUID());
    }

    private static boolean refuse(ServerPlayer player, String key) {
        return refuse(player, Component.translatable(key));
    }

    private static boolean refuse(ServerPlayer player, Component msg) {
        player.sendSystemMessage(msg.copy().withStyle(ChatFormatting.RED));
        return false;
    }

    private static void say(ServerPlayer player, Component line) {
        player.sendSystemMessage(Component.translatable("dialog.hxh.format",
                Component.translatable("npc.hxh." + CLERK).withStyle(ChatFormatting.YELLOW), line));
    }

    /** Commande admin : renommer l'aéroport le plus proche. */
    public static boolean rename(ServerPlayer player, String name) {
        Airports all = Airports.get(player.server);
        Airports.Airport a = all.nearest(player.level().dimension().location().toString(), player.blockPosition(), 32);
        if (a == null) return refuse(player, "travel.hxh.not_at_desk");
        all.rename(a, name);
        player.sendSystemMessage(Component.translatable("travel.hxh.renamed", name).withStyle(ChatFormatting.GREEN));
        return true;
    }

    static boolean sameLevel(Level a, String dim) {
        return a.dimension().location().toString().equals(dim);
    }
}
