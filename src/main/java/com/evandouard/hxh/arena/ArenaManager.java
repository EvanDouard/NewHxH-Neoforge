package com.evandouard.hxh.arena;

import com.evandouard.hxh.quest.QuestData;
import com.evandouard.hxh.quest.QuestManager;
import com.evandouard.hxh.registry.ModEntities;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Combats de la Tour Céleste : entrée/sortie, inscription, compte à rebours, K.-O., sortie de ring,
 * décision au temps, étages et Jenny. On ne meurt pas dans la Tour : un coup fatal = K.-O.
 * Protections de la dimension : ni casse, ni pose, ni perle, ni chorus, ni explosions qui détruisent.
 */
public final class ArenaManager {
    private static final int COUNTDOWN_TICKS = 60;

    /** Un combat en cours (joueur → combat). */
    private record Fight(UUID player, UUID fighter, int slot, int floor, long fightStart) {}

    private static final Map<UUID, Fight> FIGHTS = new HashMap<>();

    private ArenaManager() {}

    public static void register(IEventBus bus) {
        bus.addListener(ServerTickEvent.Post.class, e -> tick(e.getServer()));
        bus.addListener(EventPriority.HIGHEST, LivingDeathEvent.class, ArenaManager::onDeath);
        bus.addListener(PlayerEvent.PlayerLoggedOutEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer p) onLogout(p);
        });
        // ---------- Protections ----------
        bus.addListener(BlockEvent.BreakEvent.class, e -> {
            if (e.getLevel() instanceof Level l && ArenaWorld.isArenaLevel(l) && !e.getPlayer().isCreative()) e.setCanceled(true);
        });
        bus.addListener(BlockEvent.EntityPlaceEvent.class, e -> {
            if (e.getLevel() instanceof Level l && ArenaWorld.isArenaLevel(l)
                    && !(e.getEntity() instanceof Player p && p.isCreative())) e.setCanceled(true);
        });
        bus.addListener(PlayerInteractEvent.RightClickItem.class, e -> {
            if (forbiddenItem(e.getEntity(), e.getItemStack().getItem())) e.setCanceled(true);
        });
        bus.addListener(PlayerInteractEvent.RightClickBlock.class, e -> {
            if (forbiddenItem(e.getEntity(), e.getItemStack().getItem())) e.setCanceled(true);
        });
        bus.addListener(EntityTeleportEvent.EnderPearl.class, e -> {
            if (blockTeleport(e.getPlayer())) e.setCanceled(true);
        });
        bus.addListener(EntityTeleportEvent.ChorusFruit.class, e -> {
            if (e.getEntityLiving() instanceof Player p && blockTeleport(p)) e.setCanceled(true);
        });
        bus.addListener(ExplosionEvent.Detonate.class, e -> {
            if (ArenaWorld.isArenaLevel(e.getLevel())) e.getAffectedBlocks().clear();
        });
    }

    private static boolean forbiddenItem(Player player, Item item) {
        return ArenaWorld.isArenaLevel(player.level()) && !player.isCreative()
                && (item instanceof BucketItem || item instanceof FlintAndSteelItem || item instanceof FireChargeItem);
    }

    private static boolean blockTeleport(Player player) {
        if (!ArenaWorld.isArenaLevel(player.level()) || player.isCreative()) return false;
        player.displayClientMessage(Component.translatable("message.hxh.arena_no_teleport").withStyle(ChatFormatting.RED), true);
        return true;
    }

    public static boolean isActiveFighter(ArenaFighterEntity fighter) {
        for (Fight f : FIGHTS.values()) if (f.fighter.equals(fighter.getUUID())) return true;
        return false;
    }

    public static boolean inFight(ServerPlayer player) {
        return FIGHTS.containsKey(player.getUUID());
    }

    // ---------- Entrer, sortir ----------

    /** Entre dans la Tour (portail ou commande) : on retient d'où l'on vient. */
    public static boolean enter(ServerPlayer player) {
        if (ArenaWorld.isArenaLevel(player.level())) {
            toLobby(player);
            return true;
        }
        ServerLevel level = ArenaWorld.level(player.server);
        if (level == null) {
            player.sendSystemMessage(Component.translatable("message.hxh.arena_missing").withStyle(ChatFormatting.RED));
            return false;
        }
        QuestManager.data(player).setReturnPoint(player.level().dimension().location().toString(),
                player.getX(), player.getY(), player.getZ());
        toLobby(player);
        player.sendSystemMessage(Component.translatable("message.hxh.arena_welcome").withStyle(ChatFormatting.GOLD));
        return true;
    }

    /** Ramène au hall d'accueil (en le construisant au besoin). */
    public static void toLobby(ServerPlayer player) {
        ServerLevel level = ArenaWorld.level(player.server);
        if (level == null) return;
        ArenaWorld.ensureLobby(level);
        BlockPos s = ArenaWorld.LOBBY_SPAWN;
        player.fallDistance = 0;
        // Regard vers le nord, sur le guichet
        player.teleportTo(level, s.getX() + 0.5, s.getY(), s.getZ() + 0.5, 180f, 0f);
    }

    /** Quitte la Tour : retour à l'endroit d'où l'on est venu (ou au point d'apparition du monde). */
    public static void leave(ServerPlayer player) {
        Fight fight = FIGHTS.get(player.getUUID());
        if (fight != null) finish(player.server, fight, player, false, "forfeit", false);
        QuestData d = QuestManager.data(player);
        ServerLevel target = null;
        ResourceLocation dim = ResourceLocation.tryParse(d.returnDim());
        if (dim != null) target = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, dim));
        if (target == null || ArenaWorld.isArenaLevel(target)) {
            target = player.server.overworld();
            BlockPos spawn = target.getSharedSpawnPos();
            player.teleportTo(target, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, player.getYRot(), 0f);
        } else {
            Vec3 p = d.returnPos();
            player.teleportTo(target, p.x, p.y, p.z, player.getYRot(), 0f);
        }
        player.sendSystemMessage(Component.translatable("message.hxh.arena_goodbye").withStyle(ChatFormatting.GOLD));
    }

    // ---------- Combat ----------

    /**
     * Prend l'ascenseur : combat contre un adversaire de son étage.
     * @return null si le combat commence, sinon la clé du refus
     */
    @Nullable
    public static String startFight(ServerPlayer player) {
        if (!ArenaWorld.isArenaLevel(player.level())) return "message.hxh.arena_not_inside";
        QuestData d = QuestManager.data(player);
        if (!d.arenaRegistered()) return "message.hxh.arena_not_registered";
        if (inFight(player)) return "message.hxh.arena_already_fighting";
        int floor = d.arenaFloor();
        if (floor >= ArenaRules.NEN_FLOOR && !HunterStats.get(player).nenUnlocked()) return "message.hxh.arena_need_nen";

        ServerLevel level = (ServerLevel) player.level();
        int slot = freeSlot();
        ArenaWorld.buildArena(level, slot);
        BlockPos c = ArenaWorld.arenaCenter(slot);
        // Restes d'un ancien combat
        AABB box = new AABB(c).inflate(ArenaWorld.ARENA_HALF + 2, 8, ArenaWorld.ARENA_HALF + 2);
        for (Entity e : level.getEntitiesOfClass(Entity.class, box, e -> !(e instanceof Player))) e.discard();

        ArenaFighterEntity fighter = ModEntities.ARENA_FIGHTER.get().create(level);
        if (fighter == null) return "message.hxh.arena_missing";
        fighter.moveTo(c.getX() + 6.5, c.getY(), c.getZ() + 0.5, 90f, 0f);
        fighter.setYHeadRot(90f);
        fighter.setYBodyRot(90f);
        fighter.configure(floor, player.getUUID());
        fighter.setWaiting(true);
        level.addFreshEntity(fighter);

        FIGHTS.put(player.getUUID(), new Fight(player.getUUID(), fighter.getUUID(), slot, floor,
                level.getGameTime() + COUNTDOWN_TICKS));
        player.fallDistance = 0;
        player.teleportTo(level, c.getX() - 5.5, c.getY(), c.getZ() + 0.5, -90f, 0f);
        player.setHealth(player.getMaxHealth());
        player.sendSystemMessage(Component.translatable(floor >= ArenaRules.NEN_FLOOR
                ? "message.hxh.arena_fight_nen" : "message.hxh.arena_fight", floor).withStyle(ChatFormatting.GOLD));
        return null;
    }

    private static int freeSlot() {
        for (int slot = 0; ; slot++) {
            boolean used = false;
            for (Fight f : FIGHTS.values()) if (f.slot == slot) used = true;
            if (!used) return slot;
        }
    }

    private static void title(ServerPlayer player, Component text) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 20, 5));
        player.connection.send(new ClientboundSetTitleTextPacket(text));
    }

    private static void tick(MinecraftServer server) {
        ServerLevel level = ArenaWorld.level(server);
        if (level == null) return;
        long now = level.getGameTime();

        for (Fight fight : new ArrayList<>(FIGHTS.values())) {
            ServerPlayer player = server.getPlayerList().getPlayer(fight.player);
            Entity e = level.getEntity(fight.fighter);
            ArenaFighterEntity fighter = e instanceof ArenaFighterEntity f ? f : null;
            if (player == null || player.level() != level) {
                finish(server, fight, player, false, "forfeit", false);
                continue;
            }
            // Compte à rebours 3, 2, 1, puis « Combat ! »
            long toStart = fight.fightStart - now;
            if (toStart > 0) {
                if (toStart % 20 == 0) title(player, Component.literal(String.valueOf(toStart / 20)).withStyle(ChatFormatting.YELLOW));
                continue;
            }
            if (toStart == 0) {
                title(player, Component.translatable("title.hxh.arena_go").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                if (fighter != null) fighter.setWaiting(false);
            }
            if (player.getY() < ArenaWorld.RING_OUT_Y) {
                finish(server, fight, player, false, "ring_out", true);
            } else if (fighter == null || !fighter.isAlive()) {
                finish(server, fight, player, true, "ko", true);
            } else if (fighter.getY() < ArenaWorld.RING_OUT_Y) {
                finish(server, fight, player, true, "ring_out", true);
            } else if (now - fight.fightStart >= ArenaRules.FIGHT_TIME_LIMIT_TICKS) {
                // Décision des juges : la meilleure santé restante l'emporte
                boolean win = player.getHealth() / player.getMaxHealth() >= fighter.getHealth() / fighter.getMaxHealth();
                finish(server, fight, player, win, "decision", true);
            }
        }

        // Joueurs tombés du hall ou restés dans la zone des arènes sans combat (reconnexion…)
        for (ServerPlayer p : level.players()) {
            if (inFight(p) || p.isCreative() || p.isSpectator()) continue;
            if (p.getY() < ArenaWorld.FLOOR_Y - 14 || ArenaWorld.inArenaZone(p.getX())) toLobby(p);
        }
    }

    /** On ne meurt pas dans la Tour : le joueur est mis K.-O., l'adversaire vaincu quitte l'arène. */
    private static void onDeath(LivingDeathEvent event) {
        if (!ArenaWorld.isArenaLevel(event.getEntity().level())) return;
        MinecraftServer server = event.getEntity().getServer();
        if (server == null) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            event.setCanceled(true);
            player.setHealth(1f);
            Fight fight = FIGHTS.get(player.getUUID());
            if (fight != null) finish(server, fight, player, false, "ko", true);
            else toLobby(player);
        } else if (event.getEntity() instanceof ArenaFighterEntity fighter) {
            event.setCanceled(true);
            fighter.setHealth(1f);
            for (Fight fight : new ArrayList<>(FIGHTS.values())) {
                if (!fight.fighter.equals(fighter.getUUID())) continue;
                finish(server, fight, server.getPlayerList().getPlayer(fight.player), true, "ko", true);
                return;
            }
            fighter.discard();
        }
    }

    private static void onLogout(ServerPlayer player) {
        Fight fight = FIGHTS.get(player.getUUID());
        if (fight != null) finish(player.server, fight, player, false, "forfeit", false);
    }

    /**
     * Fin d'un combat : l'adversaire s'en va, l'étage et les Jenny sont mis à jour, retour au hall.
     * @param teleport false quand le joueur n'est plus là (déconnexion, autre dimension)
     */
    private static void finish(MinecraftServer server, Fight fight, @Nullable ServerPlayer player,
                               boolean win, String reason, boolean teleport) {
        FIGHTS.remove(fight.player);
        ServerLevel level = ArenaWorld.level(server);
        if (level != null && level.getEntity(fight.fighter) instanceof ArenaFighterEntity f) f.discard();
        if (player == null) return;
        long elapsed = level == null ? Long.MAX_VALUE : level.getGameTime() - fight.fightStart;
        boolean fast = win && elapsed <= ArenaRules.FAST_WIN_TICKS;
        player.sendSystemMessage(Component.translatable("message.hxh.arena_" + (win ? "win" : "loss") + "_" + reason)
                .withStyle(win ? ChatFormatting.GREEN : ChatFormatting.RED));
        applyResult(player, fight.floor, win, fast);
        if (teleport) {
            player.setHealth(player.getMaxHealth());
            player.clearFire();
            player.removeAllEffects();
            toLobby(player);
        }
    }

    /**
     * Applique le résultat d'un combat à l'étage {@code floor} : nouvel étage, Jenny, quêtes du guichet.
     * Séparé de la téléportation pour être testé seul.
     */
    public static int applyResult(ServerPlayer player, int floor, boolean win, boolean fast) {
        QuestData d = QuestManager.data(player);
        int newFloor = win ? ArenaRules.floorAfterWin(floor, fast) : ArenaRules.floorAfterLoss(floor);
        int jenny = win ? ArenaRules.jennyForWin(floor) : 0;
        d.recordArenaResult(win, newFloor, jenny);
        if (jenny > 0) {
            player.sendSystemMessage(Component.translatable("message.hxh.jenny_gained", jenny, d.jenny())
                    .withStyle(ChatFormatting.YELLOW));
        }
        if (newFloor != floor) {
            player.sendSystemMessage(Component.translatable(newFloor > floor ? "message.hxh.arena_floor_up"
                    : "message.hxh.arena_floor_down", newFloor).withStyle(ChatFormatting.AQUA));
        }
        if (floor < ArenaRules.NEN_FLOOR && newFloor >= ArenaRules.NEN_FLOOR) {
            player.sendSystemMessage(Component.translatable("message.hxh.arena_reached_200")
                    .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        }
        QuestManager.onArenaResult(player, win);
        return newFloor;
    }

    /** Pour les tests et les commandes : liste des combats en cours. */
    public static List<UUID> fightingPlayers() {
        return List.copyOf(FIGHTS.keySet());
    }
}
