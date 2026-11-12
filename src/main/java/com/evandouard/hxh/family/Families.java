package com.evandouard.hxh.family;

import com.evandouard.hxh.HxHGameRules;
import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.quest.QuestData;
import com.evandouard.hxh.quest.QuestManager;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/** Familles : chargement, tirage à la première connexion, effets (poison, foudre, yeux écarlates, XP). */
public final class Families extends SimpleJsonResourceReloadListener {
    private static volatile Map<String, Family> FAMILIES = Map.of();

    private Families() {
        super(new Gson(), "families");
    }

    public static void register(AddReloadListenerEvent event) {
        event.addListener(new Families());
    }

    public static void registerEvents(IEventBus bus) {
        bus.addListener(PlayerEvent.PlayerLoggedInEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer sp) assignIfNeeded(sp);
        });
        bus.addListener(MobEffectEvent.Applicable.class, Families::onEffect);
        bus.addListener(LivingIncomingDamageEvent.class, Families::onDamage);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, Family> loaded = new TreeMap<>();
        files.forEach((id, json) -> {
            try {
                loaded.put(id.getPath(), Family.parse(id, json.getAsJsonObject()));
            } catch (Exception e) {
                HxHMod.LOGGER.error("Famille {} invalide : {}", id, e.getMessage());
            }
        });
        FAMILIES = Map.copyOf(loaded);
        HxHMod.LOGGER.info("{} famille(s) chargée(s)", loaded.size());
    }

    @Nullable
    public static Family get(String id) {
        return id == null || id.isEmpty() ? null : FAMILIES.get(id);
    }

    public static Map<String, Family> all() {
        return FAMILIES;
    }

    /** Famille d'une entité (joueurs seulement), ou null. */
    @Nullable
    public static Family of(LivingEntity entity) {
        return entity instanceof ServerPlayer sp ? get(QuestManager.data(sp).family()) : null;
    }

    /** Première connexion : tirage pondéré si la règle hxhRandomFamily est active (sinon, pas de famille). */
    public static void assignIfNeeded(ServerPlayer player) {
        QuestData d = QuestManager.data(player);
        if (d.familyAssigned()) return;
        d.setFamilyAssigned(true);
        if (!player.serverLevel().getGameRules().getBoolean(HxHGameRules.RANDOM_FAMILY)) return;
        Family f = draw(new Random(player.getUUID().getLeastSignificantBits() ^ player.serverLevel().getSeed()));
        if (f == null) return;
        set(player, f.id());
    }

    /** Tirage pondéré sur le champ « weight » ; « none » (ou un reste de poids) = pas de famille. */
    @Nullable
    public static Family draw(Random rnd) {
        int total = 0;
        for (Family f : FAMILIES.values()) total += Math.max(0, f.weight());
        if (total <= 0) return null;
        int roll = rnd.nextInt(total);
        for (Family f : FAMILIES.values()) {
            roll -= Math.max(0, f.weight());
            if (roll < 0) return f.id().equals("none") ? null : f;
        }
        return null;
    }

    public static void set(ServerPlayer player, String id) {
        QuestManager.data(player).setFamily(id);
        Family f = get(id);
        if (f == null) return;
        String lang = player.clientInformation().language();
        player.sendSystemMessage(Component.translatable("family.hxh.born",
                Component.literal(f.name(lang)).withStyle(s -> s.withColor(f.color()).withBold(true)),
                f.description(lang)).withStyle(ChatFormatting.GRAY));
    }

    // ---------- Effets ----------

    private static void onEffect(MobEffectEvent.Applicable event) {
        Family f = of(event.getEntity());
        if (f != null && f.poisonImmune() && event.getEffectInstance().getEffect().value() == MobEffects.POISON.value()) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    private static void onDamage(LivingIncomingDamageEvent event) {
        Family f = of(event.getEntity());
        if (f == null || f.lightningResist() <= 0) return;
        boolean electric = event.getSource().is(DamageTypeTags.IS_LIGHTNING)
                || event.getSource().getMsgId().contains("thunder") || event.getSource().getMsgId().contains("lightning");
        if (electric) event.setAmount(event.getAmount() * (1f - f.lightningResist()));
    }

    /** Puissance des techniques : les Kurta (yeux écarlates) frappent plus fort quand leur vie est basse. */
    public static float powerMultiplier(LivingEntity entity) {
        Family f = of(entity);
        if (f == null || f.lowHealthPower() <= 0) return 1f;
        return entity.getHealth() <= entity.getMaxHealth() * Family.LOW_HEALTH ? 1f + f.lowHealthPower() : 1f;
    }

    /** XP de stat : bonus de famille. */
    public static double xpMultiplier(LivingEntity entity, com.evandouard.hxh.data.Stat stat) {
        Family f = of(entity);
        return f == null ? 1.0 : f.xpMultiplier(stat);
    }
}
