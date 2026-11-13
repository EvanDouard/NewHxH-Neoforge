package com.evandouard.hxh.contract;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.mob.MobPreset;
import com.evandouard.hxh.mob.MobPresets;
import com.evandouard.hxh.quest.QuestManager;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Contrats Zoldyck ({@code data/<namespace>/contracts/<id>.json}) : le majordome des Zoldyck (PNJ
 * {@code majordome_zoldyck}) confie une cible nommée, posée loin du joueur, à éliminer avant la fin du délai. Seul le
 * titulaire du contrat touche la récompense (+50 % pour un Zoldyck). Un contrat à la fois ; délai dépassé ou cible
 * tuée par un autre = contrat raté, la cible disparaît.
 * <pre>
 * { "name": {"fr": "Un usurier de York Shin", "en": "A York Shin loan shark"}, "preset": "elite_manipulateur",
 *   "reward": 800, "min_distance": 120, "max_distance": 250, "time_seconds": 1200, "weight": 3 }
 * </pre>
 */
public final class Contracts extends SimpleJsonResourceReloadListener {
    /**
     * @param board {@code zoldyck} (majordome) ou {@code blacklist} (traques de l'Association, licence exigée)
     * @param minStars étoiles du rang Hunter nécessaires (Blacklist)
     */
    public record Contract(ResourceLocation id, String nameFr, String nameEn, String preset, int reward, int minDistance,
                           int maxDistance, int timeSeconds, int weight, String board, int minStars) {
        public boolean blacklist() {
            return BLACKLIST.equals(board);
        }

        public String name(String lang) {
            return lang != null && lang.startsWith("fr") ? nameFr : nameEn;
        }
    }

    public static final String NPC = "majordome_zoldyck";
    public static final float ZOLDYCK_BONUS = 1.5f;
    /** Données du joueur : le contrat en cours. */
    static final String TAG = "hxh_contract";
    /** Données de la cible : titulaire et échéance. */
    public static final String OWNER = "hxh_contract_owner", DEADLINE = "hxh_contract_deadline", REWARD = "hxh_contract_reward",
            BOARD = "hxh_contract_board";
    public static final String ZOLDYCK = "zoldyck", BLACKLIST = "blacklist";

    private static volatile Map<ResourceLocation, Contract> CONTRACTS = Map.of();

    private Contracts() {
        super(new Gson(), "contracts");
    }

    public static void register(AddReloadListenerEvent event) {
        event.addListener(new Contracts());
    }

    public static void registerEvents(IEventBus bus) {
        bus.addListener(LivingDeathEvent.class, Contracts::onDeath);
        bus.addListener(ServerTickEvent.Post.class, e -> tick(e.getServer()));
        bus.addListener(EntityJoinLevelEvent.class, e -> {
            // une cible dont le contrat a expiré pendant que son tronçon dormait ne revient pas
            if (!e.getLevel().isClientSide() && e.getEntity().getPersistentData().contains(DEADLINE)
                    && e.getEntity().getPersistentData().getLong(DEADLINE) < e.getLevel().getGameTime()) e.setCanceled(true);
        });
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, Contract> map = new TreeMap<>();
        files.forEach((id, json) -> {
            try {
                JsonObject o = json.getAsJsonObject();
                JsonObject name = GsonHelper.getAsJsonObject(o, "name");
                map.put(id, new Contract(id, GsonHelper.getAsString(name, "fr"), GsonHelper.getAsString(name, "en", GsonHelper.getAsString(name, "fr")),
                        GsonHelper.getAsString(o, "preset"), GsonHelper.getAsInt(o, "reward"), GsonHelper.getAsInt(o, "min_distance", 120),
                        GsonHelper.getAsInt(o, "max_distance", 250), GsonHelper.getAsInt(o, "time_seconds", 1200), GsonHelper.getAsInt(o, "weight", 1),
                        GsonHelper.getAsString(o, "board", ZOLDYCK), GsonHelper.getAsInt(o, "min_stars", 0)));
            } catch (RuntimeException ex) {
                HxHMod.LOGGER.error("[HxH] Contrat invalide {}", id, ex);
            }
        });
        CONTRACTS = Map.copyOf(map);
        HxHMod.LOGGER.info("[HxH] {} contrat(s) Zoldyck chargé(s)", map.size());
    }

    public static Map<ResourceLocation, Contract> all() {
        return CONTRACTS;
    }

    @Nullable
    public static Contract get(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id.contains(":") ? id : HxHMod.MODID + ":" + id);
        return rl == null ? null : CONTRACTS.get(rl);
    }

    // ---------- État du joueur ----------

    public static boolean hasContract(ServerPlayer p) {
        return p.getPersistentData().contains(TAG);
    }

    private static CompoundTag state(ServerPlayer p) {
        return p.getPersistentData().getCompound(TAG);
    }

    /** Difficulté « Paisible » : les cibles (mobs hostiles) disparaîtraient, les contrats sont refusés. */
    public static boolean peaceful(ServerPlayer p) {
        return p.level().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL;
    }

    /** Nom de la cible du contrat en cours, ou null. */
    @Nullable
    public static String activeName(ServerPlayer p) {
        return hasContract(p) ? state(p).getString("name") : null;
    }

    /** Le contrat en cours vient de la Blacklist ? */
    public static boolean activeIsBlacklist(ServerPlayer p) {
        Contract c = hasContract(p) ? get(state(p).getString("id")) : null;
        return c != null && c.blacklist();
    }

    /** Où attend la cible du contrat en cours (pour la boussole de quête), ou null. */
    @Nullable
    public static BlockPos targetPos(ServerPlayer p) {
        if (!hasContract(p)) return null;
        CompoundTag s = state(p);
        // Cible chargée : sa vraie position (elle a pu bouger), sinon le point d'apparition
        if (s.hasUUID("target") && p.serverLevel().getEntity(s.getUUID("target")) instanceof LivingEntity t && t.isAlive()) {
            return t.blockPosition();
        }
        return new BlockPos(s.getInt("x"), p.getBlockY(), s.getInt("z"));
    }

    public static int rewardFor(ServerPlayer p, Contract c) {
        var fam = com.evandouard.hxh.family.Families.of(p);
        return !c.blacklist() && fam != null && fam.id().equals("zoldyck") ? Math.round(c.reward() * ZOLDYCK_BONUS) : c.reward();
    }

    // ---------- Majordome ----------

    public static void talk(ServerPlayer player) {
        if (hasContract(player)) {
            CompoundTag s = state(player);
            long left = Math.max(0, (s.getLong("deadline") - player.level().getGameTime()) / 20);
            say(player, Component.translatable("dialog.hxh.majordome.reminder", s.getString("name"), s.getInt("x"), s.getInt("z"), left / 60, left % 60));
            return;
        }
        if (peaceful(player)) {
            // En paisible, la cible (un mob hostile) disparaîtrait aussitôt : pas de contrat
            say(player, Component.translatable("dialog.hxh.majordome.peaceful"));
            return;
        }
        Contract c = pick(player.getRandom().nextLong());
        if (c == null) {
            say(player, Component.translatable("dialog.hxh.majordome.none"));
            return;
        }
        double angle = player.getRandom().nextDouble() * Math.PI * 2;
        int dist = c.minDistance() + player.getRandom().nextInt(Math.max(1, c.maxDistance() - c.minDistance() + 1));
        ServerLevel level = player.serverLevel();
        int x = player.getBlockX() + (int) Math.round(Math.cos(angle) * dist);
        int z = player.getBlockZ() + (int) Math.round(Math.sin(angle) * dist);
        level.getChunk(x >> 4, z >> 4);
        BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        start(player, c, Vec3.atBottomCenterOf(ground));
    }

    @Nullable
    static Contract pick(long seed) {
        java.util.List<Contract> pool = CONTRACTS.values().stream().filter(c -> !c.blacklist()).toList();
        int total = pool.stream().mapToInt(Contract::weight).sum();
        if (total <= 0) return null;
        int r = new Random(seed).nextInt(total);
        for (Contract c : pool) {
            r -= c.weight();
            if (r < 0) return c;
        }
        return null;
    }

    /** Ouvre le contrat : la cible apparaît à {@code at}. @return la cible, ou null */
    @Nullable
    public static LivingEntity start(ServerPlayer player, Contract c, Vec3 at) {
        MobPreset preset = MobPresets.get(c.preset());
        if (preset == null) return null;
        ServerLevel level = player.serverLevel();
        LivingEntity target = preset.spawn(level, at, level.getRandom().nextFloat() * 360f);
        if (target == null) return null;
        String lang = player.clientInformation().language();
        long deadline = level.getGameTime() + c.timeSeconds() * 20L;
        int reward = rewardFor(player, c);
        target.setCustomName(Component.literal(c.name(lang)).withStyle(ChatFormatting.DARK_RED));
        target.setCustomNameVisible(true);
        if (target instanceof Mob m) m.setPersistenceRequired();
        target.getPersistentData().putUUID(OWNER, player.getUUID());
        target.getPersistentData().putLong(DEADLINE, deadline);
        target.getPersistentData().putInt(REWARD, reward);
        target.getPersistentData().putString(BOARD, c.board());
        target.addTag("hxh_contract");
        // Un squelette ou un zombie ne doit pas brûler au soleil avant qu'on l'atteigne (contrat raté pour rien)
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE,
                net.minecraft.world.effect.MobEffectInstance.INFINITE_DURATION, 0, false, false));
        CompoundTag s = new CompoundTag();
        s.putString("id", c.id().toString());
        s.putString("name", c.name(lang));
        s.putUUID("target", target.getUUID());
        s.putLong("deadline", deadline);
        s.putInt("x", (int) Math.floor(at.x));
        s.putInt("z", (int) Math.floor(at.z));
        s.putInt("reward", reward);
        player.getPersistentData().put(TAG, s);
        QuestManager.sync(player);
        if (c.blacklist()) {
            player.sendSystemMessage(Component.translatable("message.hxh.blacklist_taken", c.name(lang), s.getInt("x"), s.getInt("z"),
                    c.timeSeconds() / 60, reward).withStyle(ChatFormatting.GOLD));
        } else {
            say(player, Component.translatable("dialog.hxh.majordome.offer", c.name(lang), s.getInt("x"), s.getInt("z"),
                    c.timeSeconds() / 60, reward));
        }
        return target;
    }

    private static void say(ServerPlayer player, Component text) {
        player.sendSystemMessage(Component.translatable("dialog.hxh.format",
                Component.translatable("npc.hxh." + NPC).withStyle(ChatFormatting.YELLOW), text));
    }

    // ---------- Fin du contrat ----------

    private static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        var tag = dead.getPersistentData();
        if (dead.level().isClientSide() || !tag.hasUUID(OWNER)) return;
        UUID owner = tag.getUUID(OWNER);
        tag.remove(OWNER);
        MinecraftServer server = dead.getServer();
        ServerPlayer p = server == null ? null : server.getPlayerList().getPlayer(owner);
        ServerPlayer killer = event.getSource().getEntity() instanceof ServerPlayer k ? k
                : dead.getKillCredit() instanceof ServerPlayer k2 ? k2 : null;
        if (killer != null && killer.getUUID().equals(owner)) complete(killer, tag.getInt(REWARD), BLACKLIST.equals(tag.getString(BOARD)));
        else if (p != null) fail(p, "dialog.hxh.majordome.stolen");
    }

    /** Contrat rempli : récompense. */
    public static void complete(ServerPlayer p, int reward, boolean blacklist) {
        p.getPersistentData().remove(TAG);
        if (blacklist) QuestManager.data(p).addBlacklistCaught();
        else QuestManager.data(p).addContractDone();
        HunterStars.update(p);
        QuestManager.onHuntDone(p, blacklist);
        QuestManager.data(p).addJenny(reward);
        QuestManager.sync(p);
        p.sendSystemMessage(Component.translatable("message.hxh.contract_done", reward).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
    }

    // ---------- Blacklist ----------

    /** Refus de prendre cette traque de la Blacklist, ou null. */
    @Nullable
    public static String blacklistRefusal(ServerPlayer p, Contract c) {
        if (!c.blacklist()) return "message.hxh.blacklist_unknown";
        if (!QuestManager.data(p).isHunter()) return "message.hxh.blacklist_license";
        if (QuestManager.data(p).hunterStars() < c.minStars()) return "message.hxh.blacklist_stars";
        if (hasContract(p)) return "message.hxh.blacklist_busy";
        if (peaceful(p)) return "message.hxh.contract_peaceful";
        return null;
    }

    /** Prend une traque de la Blacklist : la cible apparaît loin du joueur. */
    @Nullable
    public static String takeBlacklist(ServerPlayer p, Contract c) {
        String refusal = blacklistRefusal(p, c);
        if (refusal != null) return refusal;
        double angle = p.getRandom().nextDouble() * Math.PI * 2;
        int dist = c.minDistance() + p.getRandom().nextInt(Math.max(1, c.maxDistance() - c.minDistance() + 1));
        ServerLevel level = p.serverLevel();
        int x = p.getBlockX() + (int) Math.round(Math.cos(angle) * dist);
        int z = p.getBlockZ() + (int) Math.round(Math.sin(angle) * dist);
        level.getChunk(x >> 4, z >> 4);
        BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        return start(p, c, Vec3.atBottomCenterOf(ground)) == null ? "message.hxh.blacklist_unknown" : null;
    }

    /** Liste cliquable de la Blacklist dans le chat (ouverte depuis l'ordinateur Hunter ou /hunter blacklist). */
    public static void showBlacklist(ServerPlayer p) {
        var q = QuestManager.data(p);
        String lang = p.clientInformation().language();
        p.sendSystemMessage(Component.translatable("message.hxh.blacklist_header", q.blacklistCaught()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        if (!q.isHunter()) {
            p.sendSystemMessage(Component.translatable("message.hxh.blacklist_license").withStyle(ChatFormatting.GRAY));
            return;
        }
        java.util.List<Contract> list = CONTRACTS.values().stream().filter(Contract::blacklist)
                .sorted(java.util.Comparator.comparingInt(Contract::minStars).thenComparingInt(Contract::reward)).toList();
        for (Contract c : list) {
            boolean ok = q.hunterStars() >= c.minStars();
            var line = Component.literal(" • " + c.name(lang) + " — " + c.reward() + " J ").withStyle(ok ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY);
            if (ok) {
                line.append(Component.translatable("message.hxh.blacklist_hunt").withStyle(st -> st.withColor(ChatFormatting.YELLOW)
                        .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND,
                                "/hunter blacklist take " + c.id().getPath()))));
            } else {
                line.append(Component.translatable("message.hxh.blacklist_needs", "★".repeat(c.minStars())));
            }
            p.sendSystemMessage(line);
        }
    }

    static void fail(ServerPlayer p, String key) {
        p.getPersistentData().remove(TAG);
        QuestManager.sync(p); // la bulle du contrat disparaît
        p.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.RED));
    }

    /** Toutes les 5 s : délai dépassé → contrat raté, la cible s'en va. */
    public static void tick(MinecraftServer server) {
        if (server.overworld().getGameTime() % 100 != 0) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            expireIfLate(p);
            HunterStars.update(p);
            // la cible bouge : la boussole de quête suit
            if (hasContract(p)) QuestManager.sync(p);
        }
    }

    public static void expireIfLate(ServerPlayer p) {
        if (!hasContract(p)) return;
        CompoundTag s = state(p);
        if (p.level().getGameTime() < s.getLong("deadline")) return;
        for (ServerLevel level : p.server.getAllLevels()) {
            var e = level.getEntity(s.getUUID("target"));
            if (e != null) e.discard();
        }
        fail(p, "dialog.hxh.majordome.late");
    }
}
