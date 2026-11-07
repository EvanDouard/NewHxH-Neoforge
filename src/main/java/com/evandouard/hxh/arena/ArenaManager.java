package com.evandouard.hxh.arena;

import com.evandouard.hxh.network.HxHNetwork;
import com.evandouard.hxh.network.OpenElevatorPayload;
import com.evandouard.hxh.npc.NpcEntity;
import com.evandouard.hxh.quest.QuestData;
import com.evandouard.hxh.quest.QuestManager;
import com.evandouard.hxh.quest.QuestRegistry;
import com.evandouard.hxh.registry.ModBlocks;
import com.evandouard.hxh.registry.ModEntities;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * La Tour Céleste côté règles : inscription par tranche, ascenseur, combats sur le ring central du hall de sa
 * tranche (un seul combat à la fois par tranche, les autres attendent dans une file), compte à rebours immobile,
 * K.-O., sortie de ring, décision au temps, montée et descente d'étages, Maître d'étage, chambres.
 * On ne meurt pas dans la Tour : un coup fatal = K.-O.
 */
public final class ArenaManager {
    private static final int COUNTDOWN_TICKS = 60;
    /** Destination « ma chambre » dans l'ascenseur. */
    public static final int ROOM = -1;

    /** Un combat en cours (joueur → combat). */
    private record Fight(UUID player, UUID fighter, int bracket, int floor, ArenaWorld.Layout layout,
                         Vec3 start, long fightStart) {}

    private static final Map<UUID, Fight> FIGHTS = new HashMap<>();
    /** File d'attente du ring de chaque tranche. */
    private static final Map<Integer, Deque<UUID>> QUEUES = new HashMap<>();

    private ArenaManager() {}

    public static void register(IEventBus bus) {
        bus.addListener(ServerTickEvent.Post.class, e -> tick(e.getServer()));
        bus.addListener(EventPriority.HIGHEST, LivingDeathEvent.class, ArenaManager::onDeath);
        bus.addListener(EventPriority.HIGH, LivingIncomingDamageEvent.class, ArenaManager::onDamage);
        bus.addListener(net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Pre.class, e -> {
            if (e.getPlayer() instanceof ServerPlayer p && inFight(p)) e.setCanPickup(net.neoforged.neoforge.common.util.TriState.FALSE);
        });
        bus.addListener(PlayerEvent.PlayerLoggedInEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer p && !inFight(p)) restoreInventory(p);
        });
        bus.addListener(PlayerEvent.PlayerLoggedOutEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer p) onLogout(p);
        });
        // ---------- Protections (sauf dans sa propre chambre) ----------
        bus.addListener(BlockEvent.BreakEvent.class, e -> {
            if (e.getLevel() instanceof ServerLevel l && ArenaWorld.isArenaLevel(l) && !e.getPlayer().isCreative()
                    && !ArenaWorld.inOwnRoom(l, e.getPlayer().getUUID(), e.getPos())) e.setCanceled(true);
        });
        bus.addListener(BlockEvent.EntityPlaceEvent.class, e -> {
            if (!(e.getLevel() instanceof ServerLevel l) || !ArenaWorld.isArenaLevel(l)) return;
            if (e.getEntity() instanceof Player p && (p.isCreative() || ArenaWorld.inOwnRoom(l, p.getUUID(), e.getPos()))) return;
            e.setCanceled(true);
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
        if (!(player.level() instanceof ServerLevel l) || !ArenaWorld.isArenaLevel(l) || player.isCreative()) return false;
        if (ArenaWorld.inOwnRoom(l, player.getUUID(), player.blockPosition())) return false;
        return item instanceof BucketItem || item instanceof FlintAndSteelItem || item instanceof FireChargeItem;
    }

    private static boolean blockTeleport(Player player) {
        if (!ArenaWorld.isArenaLevel(player.level()) || player.isCreative()) return false;
        player.displayClientMessage(Component.translatable("message.hxh.arena_no_teleport").withStyle(ChatFormatting.RED), true);
        return true;
    }

    /** Pas de coups entre joueurs, et personne ne se mêle d'un combat qui n'est pas le sien. */
    private static void onDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || !ArenaWorld.isArenaLevel(victim.level())) return;
        Entity src = event.getSource().getEntity();
        if (src == null || src == victim) return;
        if (victim instanceof Player && src instanceof Player) event.setCanceled(true);
        else if (victim instanceof ArenaFighterEntity f && src instanceof Player p
                && f.opponent() != null && !p.getUUID().equals(f.opponent())) event.setCanceled(true);
        else if (victim instanceof Player p && src instanceof ArenaFighterEntity f
                && f.opponent() != null && !p.getUUID().equals(f.opponent())) event.setCanceled(true);
    }

    public static boolean isActiveFighter(ArenaFighterEntity fighter) {
        for (Fight f : FIGHTS.values()) if (f.fighter.equals(fighter.getUUID())) return true;
        return false;
    }

    public static boolean inFight(ServerPlayer player) {
        return FIGHTS.containsKey(player.getUUID());
    }

    private static void title(ServerPlayer player, Component text, @Nullable Component subtitle) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 30, 10));
        if (subtitle != null) player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        player.connection.send(new ClientboundSetTitleTextPacket(text));
    }

    private static void say(ServerPlayer player, String key, ChatFormatting color, Object... args) {
        player.sendSystemMessage(Component.translatable(key, args).withStyle(color));
    }

    // ---------- Entrer, sortir ----------

    /** Entre dans la Tour (portail ou commande) : on retient d'où l'on vient. */
    public static boolean enter(ServerPlayer player) {
        if (ArenaWorld.isArenaLevel(player.level())) {
            Fight fight = FIGHTS.get(player.getUUID());
            if (fight != null) finish(player.server, fight, player, false, "forfeit", false);
            toLobby(player);
            return true;
        }
        ServerLevel level = ArenaWorld.level(player.server);
        if (level == null) {
            say(player, "message.hxh.arena_missing", ChatFormatting.RED);
            return false;
        }
        QuestManager.data(player).setReturnPoint(player.level().dimension().location().toString(),
                player.getX(), player.getY(), player.getZ());
        toLobby(player);
        say(player, "message.hxh.arena_welcome", ChatFormatting.GOLD);
        return true;
    }

    /** Ramène au hall d'entrée (étage 0). */
    public static void toLobby(ServerPlayer player) {
        ServerLevel level = ArenaWorld.level(player.server);
        if (level == null) return;
        ArenaWorld.Layout lobby = ArenaWorld.ensureLobby(level);
        teleport(player, level, lobby.arrival(), lobby.arrivalYaw());
    }

    private static void teleport(ServerPlayer player, ServerLevel level, BlockPos pos, float yaw) {
        player.fallDistance = 0;
        player.teleportTo(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0f);
    }

    /** Quitte la Tour : retour à l'endroit d'où l'on est venu (ou au point d'apparition du monde). */
    public static void leave(ServerPlayer player) {
        Fight fight = FIGHTS.get(player.getUUID());
        if (fight != null) finish(player.server, fight, player, false, "forfeit", false);
        leaveQueues(player.getUUID());
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
        say(player, "message.hxh.arena_goodbye", ChatFormatting.GOLD);
    }

    // ---------- Ascenseur ----------

    /** Tranche d'étages du joueur. */
    public static ArenaBracket bracketOf(ServerPlayer player) {
        return ArenaBrackets.forFloor(QuestManager.data(player).arenaFloor());
    }

    /**
     * Lieu le plus haut accessible : tous les halls une fois inscrit (on peut aller regarder n'importe quel étage,
     * seul l'arbitre de sa tranche lance son combat) ; sans inscription, le hall d'entrée seulement.
     */
    public static int maxLocation(ServerPlayer player) {
        QuestData d = QuestManager.data(player);
        return d.arenaRegistered() ? ArenaWorld.locationOfBracket(ArenaBrackets.count() - 1) : ArenaWorld.ENTRY;
    }

    /**
     * Tranche où le joueur doit encore s'inscrire auprès de l'hôtesse avant que l'arbitre accepte un combat
     * (null = rien en attente). Sert aussi au suivi de quête, pour que l'objectif affiché soit le même que le refus.
     */
    @org.jetbrains.annotations.Nullable
    public static ArenaBracket pendingRegistration(QuestData d) {
        if (!d.arenaRegistered() || ArenaBrackets.count() == 0) return null;
        ArenaBracket mine = ArenaBrackets.forFloor(d.arenaFloor());
        if (d.registeredBracket() < mine.index()) return mine;
        if (d.arenaFloor() >= ArenaRules.NEN_FLOOR && !d.registered200()) return mine;
        return null;
    }

    /** Hall de la tranche du joueur. */
    public static int ownLocation(ServerPlayer player) {
        return ArenaWorld.locationOfBracket(bracketOf(player).index());
    }

    /** Clic droit sur un ascenseur : ouvre le choix de la destination. */
    public static void useElevator(ServerPlayer player) {
        if (!ArenaWorld.isArenaLevel(player.level())) {
            say(player, "message.hxh.arena_not_inside", ChatFormatting.RED);
            return;
        }
        if (inFight(player)) return;
        if (!QuestManager.data(player).arenaRegistered()) {
            say(player, "message.hxh.arena_not_registered", ChatFormatting.RED);
            return;
        }
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < ArenaBrackets.count(); i++) labels.add(ArenaBrackets.get(i).label());
        int here = ArenaWorld.inRoomZone(player.getX()) ? ROOM : Math.max(0, ArenaWorld.locationAt(player.blockPosition()));
        HxHNetwork.sendTo(player, new OpenElevatorPayload(labels, here, true));
    }

    /** Le joueur se tient près d'un ascenseur (vérification des demandes venues du client). */
    public static boolean nearElevator(ServerPlayer player) {
        BlockPos p = player.blockPosition();
        for (BlockPos q : BlockPos.betweenClosed(p.offset(-6, -2, -6), p.offset(6, 3, 6))) {
            if (player.level().getBlockState(q).is(ModBlocks.ARENA_ELEVATOR.get())) return true;
        }
        return false;
    }

    /**
     * Emmène le joueur : {@link #ROOM} = sa chambre, 0 = hall d'entrée, n = hall de la tranche n − 1.
     * @return null si c'est fait, sinon la clé du refus
     */
    @Nullable
    public static String goTo(ServerPlayer player, int destination) {
        if (!ArenaWorld.isArenaLevel(player.level())) return "message.hxh.arena_not_inside";
        if (inFight(player)) return "message.hxh.arena_already_fighting";
        ServerLevel level = (ServerLevel) player.level();
        if (destination == ROOM) {
            if (!QuestManager.data(player).arenaRegistered()) return "message.hxh.arena_not_registered";
            BlockPos room = ArenaWorld.ensureRoom(level, player.getUUID());
            teleport(player, level, room, 180f);
            title(player, Component.translatable("title.hxh.arena_room"), null);
            return null;
        }
        if (destination < 0 || destination > maxLocation(player)) return "message.hxh.arena_floor_locked";
        if (destination == ArenaWorld.ENTRY) {
            toLobby(player);
            title(player, Component.translatable("title.hxh.arena_lobby"), null);
            return null;
        }
        ArenaBracket bracket = ArenaBrackets.get(destination - 1);
        ArenaWorld.Layout layout = ArenaWorld.ensureHall(level, bracket);
        teleport(player, level, layout.arrival(), layout.arrivalYaw());
        title(player, Component.translatable(ArenaRules.isFloorMaster(bracket.from())
                ? "title.hxh.arena_top" : "title.hxh.arena_bracket", bracket.label()), null);
        return null;
    }

    /** Retour au hall de sa tranche (après un combat). */
    private static void returnToHall(ServerPlayer player) {
        if (goTo(player, ownLocation(player)) != null) toLobby(player);
    }

    // ---------- Hôtesse et arbitre ----------

    /** L'hôtesse : inscription au hall d'entrée, puis à chaque nouvelle tranche, et au 200e étage (Nen exigé). */
    public static void talkToReceptionist(ServerPlayer player, NpcEntity npc) {
        QuestData d = QuestManager.data(player);
        int loc = ArenaWorld.locationAt(npc.home());
        ArenaBracket mine = bracketOf(player);
        if (!d.arenaRegistered()) {
            d.setArenaRegistered(true);
            d.setRegisteredBracket(Math.max(d.registeredBracket(), 0));
            say(player, "message.hxh.arena_registered", ChatFormatting.GREEN, ArenaBrackets.get(0).label());
        } else if (loc == ArenaWorld.locationOfBracket(mine.index())) {
            int floor = d.arenaFloor();
            if (d.registeredBracket() < mine.index() && floor < ArenaRules.NEN_FLOOR) {
                d.setRegisteredBracket(mine.index());
                say(player, "message.hxh.arena_registered_bracket", ChatFormatting.GREEN, mine.label());
            } else if (floor >= ArenaRules.NEN_FLOOR && (!d.registered200() || d.registeredBracket() < mine.index())) {
                if (!HunterStats.get(player).nenUnlocked()) {
                    say(player, "message.hxh.arena_register_need_nen", ChatFormatting.RED);
                } else {
                    boolean first = !d.registered200();
                    d.setRegistered200(true);
                    d.setRegisteredBracket(mine.index());
                    say(player, first ? "message.hxh.arena_registered_200" : "message.hxh.arena_registered_bracket",
                            ChatFormatting.LIGHT_PURPLE, mine.label());
                }
            }
        }
        QuestManager.startArenaQuestIfNew(player);
        QuestManager.sync(player); // l'objectif « s'inscrire » disparaît du suivi
    }

    /** Réplique « au repos » des PNJ de la Tour, selon où en est le joueur (null = réplique par défaut). */
    @Nullable
    public static String idleDialog(ServerPlayer player, NpcEntity npc) {
        QuestData d = QuestManager.data(player);
        boolean master = ArenaRules.isFloorMaster(d.arenaFloor());
        if (npc.npcId().equals("receptionist")) {
            if (master) return "dialog.hxh.receptionist.master";
            int loc = ArenaWorld.locationAt(npc.home());
            if (loc > 0 && loc != ownLocation(player)) return "dialog.hxh.receptionist.other_floor";
            if (d.arenaFloor() >= ArenaRules.NEN_FLOOR && !d.registered200()) return "dialog.hxh.receptionist.need_nen";
        }
        return null;
    }

    /** L'arbitre du hall d'une tranche : lance le combat si c'est la tranche du joueur. */
    public static void talkToReferee(ServerPlayer player, NpcEntity npc) {
        QuestData d = QuestManager.data(player);
        if (ArenaRules.isFloorMaster(d.arenaFloor())) {
            player.sendSystemMessage(Component.translatable("dialog.hxh.referee.master").withStyle(ChatFormatting.GOLD));
            return;
        }
        int loc = ArenaWorld.locationAt(npc.home());
        ArenaBracket mine = bracketOf(player);
        if (loc != ArenaWorld.locationOfBracket(mine.index())) {
            say(player, "dialog.hxh.referee.wrong_floor", ChatFormatting.YELLOW, d.arenaFloor(), mine.label());
            return;
        }
        Component refusal = startFight(player);
        if (refusal != null) player.sendSystemMessage(refusal);
    }

    // ---------- Combat ----------

    /**
     * Lance le combat du joueur sur le ring de sa tranche, ou le met dans la file si le ring est pris.
     * @return null si le combat commence, sinon le message à afficher (refus ou place dans la file)
     */
    @Nullable
    public static Component startFight(ServerPlayer player) {
        if (!ArenaWorld.isArenaLevel(player.level())) return refusal("message.hxh.arena_not_inside");
        QuestData d = QuestManager.data(player);
        if (!d.arenaRegistered()) return refusal("message.hxh.arena_not_registered");
        if (inFight(player)) return refusal("message.hxh.arena_already_fighting");
        int floor = d.arenaFloor();
        if (ArenaRules.isFloorMaster(floor)) return Component.translatable("dialog.hxh.referee.master").withStyle(ChatFormatting.GOLD);
        ArenaBracket bracket = ArenaBrackets.forFloor(floor);
        if (floor >= ArenaRules.NEN_FLOOR && !d.registered200()) return refusal("message.hxh.arena_register_200");
        if (d.registeredBracket() < bracket.index()) {
            return Component.translatable("message.hxh.arena_register_bracket", bracket.label()).withStyle(ChatFormatting.RED);
        }
        if (floor >= ArenaRules.NEN_FLOOR && !HunterStats.get(player).nenUnlocked()) return refusal("message.hxh.arena_need_nen");

        // Un seul combat à la fois par tranche : les autres attendent leur tour
        Fight current = null;
        for (Fight f : FIGHTS.values()) if (f.bracket == bracket.index()) current = f;
        if (current != null) {
            Deque<UUID> queue = QUEUES.computeIfAbsent(bracket.index(), k -> new ArrayDeque<>());
            if (!queue.contains(player.getUUID())) queue.addLast(player.getUUID());
            int position = 1;
            for (UUID id : queue) {
                if (id.equals(player.getUUID())) break;
                position++;
            }
            ServerPlayer other = player.server.getPlayerList().getPlayer(current.player);
            return Component.translatable("message.hxh.arena_queued",
                    other == null ? "?" : other.getName().getString(), position).withStyle(ChatFormatting.YELLOW);
        }
        leaveQueues(player.getUUID());

        ServerLevel level = (ServerLevel) player.level();
        ArenaWorld.Layout layout = ArenaWorld.ensureHall(level, bracket);
        BlockPos ps = layout.playerStart();
        BlockPos os = layout.opponentStart();
        float playerYaw = (float) (Math.atan2(os.getZ() - ps.getZ(), os.getX() - ps.getX()) * 180 / Math.PI) - 90f;
        // Restes d'un ancien combat
        AABB ring = new AABB(layout.ringCenter()).inflate(layout.ringRadius() + 2, 6, layout.ringRadius() + 2);
        for (ArenaFighterEntity e : level.getEntitiesOfClass(ArenaFighterEntity.class, ring)) e.discard();

        ArenaFighterEntity fighter = ModEntities.ARENA_FIGHTER.get().create(level);
        if (fighter == null) return refusal("message.hxh.arena_missing");
        fighter.moveTo(os.getX() + 0.5, os.getY(), os.getZ() + 0.5, playerYaw + 180f, 0f);
        fighter.setYHeadRot(playerYaw + 180f);
        fighter.setYBodyRot(playerYaw + 180f);
        fighter.configure(floor, player.getUUID());
        fighter.setWaiting(true);
        level.addFreshEntity(fighter);

        Vec3 start = new Vec3(ps.getX() + 0.5, ps.getY(), ps.getZ() + 0.5);
        FIGHTS.put(player.getUUID(), new Fight(player.getUUID(), fighter.getUUID(), bracket.index(), floor, layout,
                start, level.getGameTime() + COUNTDOWN_TICKS));
        stashInventory(player);
        player.fallDistance = 0;
        player.teleportTo(level, start.x, start.y, start.z, playerYaw, 0f);
        player.setDeltaMovement(Vec3.ZERO);
        player.setHealth(player.getMaxHealth());
        say(player, floor >= ArenaRules.NEN_FLOOR ? "message.hxh.arena_fight_nen" : "message.hxh.arena_fight",
                ChatFormatting.GOLD, floor);
        return null;
    }

    /** Combat à mains nues : armes, armures et objets sont mis de côté et rendus à la fin. */
    private static void stashInventory(ServerPlayer player) {
        QuestData d = QuestManager.data(player);
        if (!d.arenaStash().isEmpty()) restoreInventory(player); // reste d'un combat interrompu
        net.minecraft.nbt.ListTag stash = player.getInventory().save(new net.minecraft.nbt.ListTag());
        if (stash.isEmpty()) return;
        d.setArenaStash(stash);
        player.getInventory().clearContent();
        player.containerMenu.broadcastChanges();
        player.displayClientMessage(Component.translatable("message.hxh.arena_items_stored").withStyle(ChatFormatting.GRAY), true);
    }

    /** Rend l'inventaire mis de côté (ce qui a été ramassé entre-temps est gardé ou lâché au sol). */
    public static void restoreInventory(ServerPlayer player) {
        QuestData d = QuestManager.data(player);
        if (d.arenaStash().isEmpty()) return;
        java.util.List<net.minecraft.world.item.ItemStack> extra = new java.util.ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty()) extra.add(s.copy());
        }
        player.getInventory().load(d.arenaStash());
        d.setArenaStash(new net.minecraft.nbt.ListTag());
        for (net.minecraft.world.item.ItemStack s : extra) {
            if (!player.getInventory().add(s)) player.drop(s, false);
        }
        player.containerMenu.broadcastChanges();
    }

    private static Component refusal(String key) {
        return Component.translatable(key).withStyle(ChatFormatting.RED);
    }

    private static void leaveQueues(UUID player) {
        for (Deque<UUID> q : QUEUES.values()) q.remove(player);
    }

    /** Le ring d'une tranche se libère : le premier de la file encore présent dans le hall combat. */
    private static void callNext(MinecraftServer server, int bracket) {
        Deque<UUID> queue = QUEUES.get(bracket);
        while (queue != null && !queue.isEmpty()) {
            ServerPlayer next = server.getPlayerList().getPlayer(queue.pollFirst());
            if (next == null || !ArenaWorld.isArenaLevel(next.level()) || inFight(next)
                    || ArenaWorld.locationAt(next.blockPosition()) != ArenaWorld.locationOfBracket(bracket)
                    || bracketOf(next).index() != bracket) continue;
            say(next, "message.hxh.arena_your_turn", ChatFormatting.GREEN);
            Component refusal = startFight(next);
            if (refusal == null) return;
            next.sendSystemMessage(refusal);
        }
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
            ejectSpectators(level, fight);
            long toStart = fight.fightStart - now;
            if (toStart > 0) {
                // Immobile pendant le compte à rebours
                if (player.position().distanceToSqr(fight.start) > 0.04) {
                    player.connection.teleport(fight.start.x, fight.start.y, fight.start.z, player.getYRot(), player.getXRot());
                }
                player.setDeltaMovement(Vec3.ZERO);
                if (toStart % 20 == 0) title(player, Component.literal(String.valueOf(toStart / 20)).withStyle(ChatFormatting.YELLOW), null);
                continue;
            }
            if (toStart == 0) {
                title(player, Component.translatable("title.hxh.arena_go").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), null);
                if (fighter != null) fighter.setWaiting(false);
            }
            ArenaWorld.Layout l = fight.layout;
            boolean playerOut = player.getY() < l.ringOutY() || !l.inRing(player.getX(), player.getZ());
            boolean fighterOut = fighter != null && fighter.isAlive() && (fighter.getY() < l.ringOutY() || !l.inRing(fighter.getX(), fighter.getZ()));
            if (fight.floor >= ArenaRules.NO_RING_OUT_FROM && (playerOut || fighterOut)) {
                // Au-delà du 200e, les sorties de ring ne comptent plus : retour sur le ring
                if (playerOut) {
                    player.connection.teleport(fight.start.x, fight.start.y, fight.start.z, player.getYRot(), player.getXRot());
                    player.displayClientMessage(Component.translatable("message.hxh.arena_back_in_ring").withStyle(ChatFormatting.YELLOW), true);
                }
                if (fighterOut) {
                    BlockPos os = l.opponentStart();
                    fighter.teleportTo(os.getX() + 0.5, os.getY(), os.getZ() + 0.5);
                }
                continue;
            }
            if (playerOut) {
                finish(server, fight, player, false, "ring_out", true);
            } else if (fighter == null || !fighter.isAlive()) {
                finish(server, fight, player, true, "ko", true);
            } else if (fighterOut) {
                finish(server, fight, player, true, "ring_out", true);
            } else if (now - fight.fightStart >= ArenaRules.FIGHT_TIME_LIMIT_TICKS) {
                boolean win = player.getHealth() / player.getMaxHealth() >= fighter.getHealth() / fighter.getMaxHealth();
                finish(server, fight, player, win, "decision", true);
            }
        }

        // Joueurs tombés : retour à l'arrivée du lieu où ils étaient
        for (ServerPlayer p : level.players()) {
            if (inFight(p) || p.isCreative() || p.isSpectator() || p.getY() >= ArenaWorld.FLOOR_Y - 14) continue;
            if (ArenaWorld.inRoomZone(p.getX())) goTo(p, ROOM);
            else {
                int loc = ArenaWorld.locationAt(p.blockPosition());
                if (loc > 0 && loc <= maxLocation(p)) goTo(p, loc);
                else toLobby(p);
            }
        }
        if (now % 100 == 0) ArenaWorld.cleanNpcs(level);
    }

    /** Le ring est réservé aux combattants : les spectateurs qui y entrent sont renvoyés au bord. */
    private static void ejectSpectators(ServerLevel level, Fight fight) {
        ArenaWorld.Layout l = fight.layout;
        for (ServerPlayer p : level.players()) {
            if (p.getUUID().equals(fight.player) || p.isCreative() || p.isSpectator()) continue;
            if (Math.abs(p.getY() - l.ringCenter().getY()) > 6 || !l.inRing(p.getX(), p.getZ())) continue;
            teleport(p, level, l.arrival(), l.arrivalYaw());
            p.displayClientMessage(Component.translatable("message.hxh.arena_ring_reserved").withStyle(ChatFormatting.RED), true);
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
        leaveQueues(player.getUUID());
        Fight fight = FIGHTS.get(player.getUUID());
        if (fight != null) finish(player.server, fight, player, false, "forfeit", false);
    }

    /**
     * Fin d'un combat : l'adversaire s'en va, l'étage et les Jenny sont mis à jour, retour au hall de sa tranche,
     * puis le suivant de la file monte sur le ring.
     */
    private static void finish(MinecraftServer server, Fight fight, @Nullable ServerPlayer player,
                               boolean win, String reason, boolean teleport) {
        FIGHTS.remove(fight.player);
        ServerLevel level = ArenaWorld.level(server);
        if (level != null && level.getEntity(fight.fighter) instanceof ArenaFighterEntity f) f.discard();
        if (player != null) {
            restoreInventory(player);
            long elapsed = level == null ? Long.MAX_VALUE : level.getGameTime() - fight.fightStart;
            boolean fast = win && elapsed <= ArenaRules.FAST_WIN_TICKS;
            say(player, "message.hxh.arena_" + (win ? "win" : "loss") + "_" + reason,
                    win ? ChatFormatting.GREEN : ChatFormatting.RED);
            applyResult(player, fight.floor, win, fast);
            if (teleport) {
                player.setHealth(player.getMaxHealth());
                player.clearFire();
                player.removeAllEffects();
                returnToHall(player);
            }
        }
        callNext(server, fight.bracket);
    }

    /**
     * Applique le résultat d'un combat à l'étage {@code floor} : nouvel étage, Jenny, changement de tranche,
     * Maître d'étage, quêtes du guichet. Séparé de la téléportation pour être testé seul.
     */
    public static int applyResult(ServerPlayer player, int floor, boolean win, boolean fast) {
        QuestData d = QuestManager.data(player);
        int newFloor = win ? ArenaRules.floorAfterWin(floor, fast) : ArenaRules.floorAfterLoss(floor);
        int jenny = win ? ArenaRules.jennyForWin(floor) : 0;
        d.recordArenaResult(win, newFloor, jenny);
        if (jenny > 0) say(player, "message.hxh.jenny_gained", ChatFormatting.YELLOW, jenny, d.jenny());
        if (newFloor != floor) {
            say(player, newFloor > floor ? "message.hxh.arena_floor_up" : "message.hxh.arena_floor_down",
                    ChatFormatting.AQUA, newFloor);
        }
        ArenaBracket before = ArenaBrackets.forFloor(floor);
        ArenaBracket after = ArenaBrackets.forFloor(newFloor);
        if (ArenaRules.isFloorMaster(newFloor)) {
            player.sendSystemMessage(Component.translatable("message.hxh.arena_floor_master").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            if (player.connection != null) {
                title(player, Component.translatable("title.hxh.arena_floor_master").withStyle(ChatFormatting.GOLD),
                        Component.translatable("subtitle.hxh.arena_floor_master"));
            }
        } else if (floor < ArenaRules.NEN_FLOOR && newFloor >= ArenaRules.NEN_FLOOR) {
            player.sendSystemMessage(Component.translatable("message.hxh.arena_reached_200").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        } else if (after.index() > before.index()) {
            say(player, "message.hxh.arena_new_bracket", ChatFormatting.LIGHT_PURPLE, after.label());
        } else if (after.index() < before.index()) {
            say(player, "message.hxh.arena_demoted", ChatFormatting.RED, after.label());
        }
        QuestManager.onArenaResult(player, win);
        return newFloor;
    }

    /** Pour les tests et les commandes : liste des combats en cours. */
    public static List<UUID> fightingPlayers() {
        return List.copyOf(FIGHTS.keySet());
    }
}
