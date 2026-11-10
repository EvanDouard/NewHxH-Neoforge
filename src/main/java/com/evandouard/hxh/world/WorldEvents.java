package com.evandouard.hxh.world;

import com.evandouard.hxh.HxHGameRules;
import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.mob.MobPreset;
import com.evandouard.hxh.mob.MobPresets;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Événements aléatoires annoncés dans le chat ({@code data/<namespace>/world_events/<nom>.json}) : de temps en temps,
 * l'Association Hunter signale une menace près d'un joueur de l'Overworld (un manieur de Nen errant, une horde…), avec
 * ses coordonnées et une prime pour qui l'abat. Les mobs repartent au bout de {@code lifetime_seconds}.
 * <pre>
 * { "weight": 3, "preset": "elite_manipulateur", "count": 1, "bounty": 300,
 *   "night_only": false, "min_distance": 40, "max_distance": 80, "lifetime_seconds": 600 }
 * </pre>
 * Le message est la traduction {@code event.<namespace>.<nom>} (arguments : x, z, prime). Règle hxhRandomEvents.
 */
public final class WorldEvents extends SimpleJsonResourceReloadListener {
    public record WorldEvent(ResourceLocation id, int weight, String preset, int count, int bounty, boolean nightOnly,
                             int minDistance, int maxDistance, int lifetimeSeconds) {
        String messageKey() {
            return "event." + id.getNamespace() + "." + id.getPath();
        }
    }

    private static volatile Map<ResourceLocation, WorldEvent> EVENTS = Map.of();
    /** Entre deux événements : 15 à 25 minutes de jeu. */
    static final int MIN_GAP = 20 * 60 * 15, MAX_GAP = 20 * 60 * 25;
    private static long nextAt = -1;
    /** Mobs d'événement en cours : identifiant → tick de départ. */
    private static final Map<UUID, Long> ALIVE = new HashMap<>();

    private WorldEvents() {
        super(new Gson(), "world_events");
    }

    public static void register(AddReloadListenerEvent event) {
        event.addListener(new WorldEvents());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, WorldEvent> map = new TreeMap<>();
        files.forEach((id, json) -> {
            try {
                JsonObject o = json.getAsJsonObject();
                map.put(id, new WorldEvent(id, GsonHelper.getAsInt(o, "weight", 1), GsonHelper.getAsString(o, "preset"),
                        GsonHelper.getAsInt(o, "count", 1), GsonHelper.getAsInt(o, "bounty", 0),
                        GsonHelper.getAsBoolean(o, "night_only", false), GsonHelper.getAsInt(o, "min_distance", 40),
                        GsonHelper.getAsInt(o, "max_distance", 80), GsonHelper.getAsInt(o, "lifetime_seconds", 600)));
            } catch (RuntimeException e) {
                HxHMod.LOGGER.error("[HxH] Événement invalide {}", id, e);
            }
        });
        EVENTS = Map.copyOf(map);
        HxHMod.LOGGER.info("[HxH] {} événement(s) aléatoire(s) chargé(s)", map.size());
    }

    public static Map<ResourceLocation, WorldEvent> all() {
        return new TreeMap<>(EVENTS);
    }

    @Nullable
    public static WorldEvent get(String name) {
        ResourceLocation id = ResourceLocation.tryParse(name.contains(":") ? name : HxHMod.MODID + ":" + name);
        return id == null ? null : EVENTS.get(id);
    }

    /** Chaque tick serveur : départ des mobs expirés, et prochain événement quand l'heure est venue. */
    public static void tick(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        long now = overworld.getGameTime();
        if (now % 100 != 0) return;
        ALIVE.entrySet().removeIf(en -> {
            if (now < en.getValue()) return false;
            var entity = overworld.getEntity(en.getKey());
            if (entity != null) entity.discard();
            return true;
        });
        if (!overworld.getGameRules().getBoolean(HxHGameRules.RANDOM_EVENTS) || EVENTS.isEmpty()
                || server instanceof net.minecraft.gametest.framework.GameTestServer) return;
        if (nextAt < 0) nextAt = now + gap(overworld);
        if (now < nextAt) return;
        nextAt = now + gap(overworld);
        List<ServerPlayer> players = overworld.players().stream().filter(p -> !p.isSpectator()).toList();
        if (players.isEmpty()) return;
        ServerPlayer target = players.get(overworld.getRandom().nextInt(players.size()));
        WorldEvent event = pickEvent(overworld);
        if (event != null) trigger(event, target);
    }

    private static int gap(ServerLevel level) {
        return MIN_GAP + level.getRandom().nextInt(MAX_GAP - MIN_GAP + 1);
    }

    @Nullable
    private static WorldEvent pickEvent(ServerLevel level) {
        boolean night = !level.isDay();
        List<WorldEvent> pool = new ArrayList<>();
        for (WorldEvent e : EVENTS.values()) if (!e.nightOnly() || night) pool.add(e);
        int total = pool.stream().mapToInt(WorldEvent::weight).sum();
        if (total <= 0) return null;
        int r = level.getRandom().nextInt(total);
        for (WorldEvent e : pool) {
            r -= e.weight();
            if (r < 0) return e;
        }
        return null;
    }

    /** Déclenche l'événement près du joueur : mobs, primes, annonce à tout le monde. Renvoie les mobs posés. */
    public static List<LivingEntity> trigger(WorldEvent event, ServerPlayer near) {
        ServerLevel level = near.serverLevel();
        MobPreset preset = MobPresets.get(event.preset());
        List<LivingEntity> out = new ArrayList<>();
        if (preset == null) return out;
        double angle = level.getRandom().nextDouble() * Math.PI * 2;
        int dist = event.minDistance() + level.getRandom().nextInt(Math.max(1, event.maxDistance() - event.minDistance() + 1));
        int x = near.getBlockX() + (int) Math.round(Math.cos(angle) * dist);
        int z = near.getBlockZ() + (int) Math.round(Math.sin(angle) * dist);
        level.getChunk(x >> 4, z >> 4);
        BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        long until = level.getGameTime() + event.lifetimeSeconds() * 20L;
        for (int i = 0; i < event.count(); i++) {
            Vec3 at = Vec3.atBottomCenterOf(ground).add((i % 3) - 1, 0, (i / 3) - 1);
            LivingEntity mob = preset.spawn(level, at, level.getRandom().nextFloat() * 360f);
            if (mob == null) continue;
            if (event.bounty() > 0) mob.getPersistentData().putInt(NenWildlife.BOUNTY, event.bounty());
            mob.addTag("hxh_event");
            if (level == level.getServer().overworld()) ALIVE.put(mob.getUUID(), until);
            out.add(mob);
        }
        Component msg = Component.translatable("event.hxh.header").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal(" "))
                .append(Component.translatable(event.messageKey(), ground.getX(), ground.getZ(), event.bounty()).withStyle(ChatFormatting.YELLOW));
        for (ServerPlayer p : level.players()) p.sendSystemMessage(msg);
        return out;
    }
}
