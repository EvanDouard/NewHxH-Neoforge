package com.evandouard.hxh.world;

import com.evandouard.hxh.HxHGameRules;
import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.mob.MobPreset;
import com.evandouard.hxh.mob.MobPresets;
import com.evandouard.hxh.quest.QuestManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Mobs naturels au Nen : une petite part des mobs qui apparaissent naturellement (ou par générateur) maîtrisent le
 * Nen, d'après les mobs d'exemple qui ont un champ {@code "natural"} (même type de mob, chance, prime). La règle
 * {@code hxhNenMobChance} ajuste toutes les chances (en %). Abattre un mob qui porte une prime rapporte des Jenny.
 */
public final class NenWildlife {
    private NenWildlife() {}

    /** Données persistantes du mob : mob d'exemple à appliquer à son arrivée, et prime en Jenny. */
    public static final String PENDING = "hxh_nen_preset", BOUNTY = "hxh_bounty";

    public static void register(IEventBus bus) {
        bus.addListener(FinalizeSpawnEvent.class, NenWildlife::onFinalize);
        bus.addListener(EntityJoinLevelEvent.class, NenWildlife::onJoin);
        bus.addListener(LivingDeathEvent.class, NenWildlife::onDeath);
    }

    private static void onFinalize(FinalizeSpawnEvent e) {
        MobSpawnType why = e.getSpawnType();
        if (why != MobSpawnType.NATURAL && why != MobSpawnType.SPAWNER && why != MobSpawnType.CHUNK_GENERATION) return;
        if (e.getLevel().getLevel().getGameRules().getInt(HxHGameRules.NEN_MOB_CHANCE) <= 0) return;
        MobPreset preset = pick(e.getEntity(), e.getLevel().getRandom(), e.getLevel().getLevel().getGameRules().getInt(HxHGameRules.NEN_MOB_CHANCE));
        if (preset != null) e.getEntity().getPersistentData().putString(PENDING, preset.id().toString());
    }

    /** Tire au sort, pour ce mob, un mob d'exemple naturel de son type (ou rien). */
    public static MobPreset pick(Mob mob, RandomSource random, int percent) {
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        for (MobPreset p : MobPresets.all().values()) {
            if (p.naturalChance() <= 0 || !p.entity().equals(type)) continue;
            if (random.nextFloat() < p.naturalChance() * percent / 100f) return p;
        }
        return null;
    }

    private static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof LivingEntity living)) return;
        var tag = living.getPersistentData();
        if (!tag.contains(PENDING)) return;
        MobPreset preset = MobPresets.get(tag.getString(PENDING));
        tag.remove(PENDING);
        if (preset == null) return;
        convert(living, preset);
    }

    /** Applique un mob d'exemple naturel à un mob existant (prime comprise). */
    public static void convert(LivingEntity living, MobPreset preset) {
        preset.applyTo(living, true);
        if (preset.bounty() > 0) living.getPersistentData().putInt(BOUNTY, preset.bounty());
        living.addTag("hxh_natural_nen");
    }

    private static void onDeath(LivingDeathEvent e) {
        LivingEntity dead = e.getEntity();
        if (dead.level().isClientSide() || !dead.getPersistentData().contains(BOUNTY)) return;
        // le tueur : l'auteur du coup, sinon celui que le jeu crédite (mort au soleil, en tombant, d'un saignement…)
        ServerPlayer killer = e.getSource().getEntity() instanceof ServerPlayer p ? p
                : dead.getKillCredit() instanceof ServerPlayer p2 ? p2
                : dead.getLastHurtByMob() instanceof ServerPlayer p3 ? p3 : null;
        if (killer == null) return;
        int bounty = dead.getPersistentData().getInt(BOUNTY);
        dead.getPersistentData().remove(BOUNTY);
        pay(killer, dead, bounty);
    }

    /** Paie une prime au joueur qui a abattu le mob. */
    public static void pay(ServerPlayer killer, LivingEntity dead, int bounty) {
        QuestManager.data(killer).addJenny(bounty);
        killer.sendSystemMessage(Component.translatable("message.hxh.bounty_paid", dead.getDisplayName(), bounty)
                .withStyle(ChatFormatting.GOLD));
        QuestManager.sync(killer);
        HxHMod.LOGGER.debug("[HxH] Prime de {} J payée à {}", bounty, killer.getName().getString());
    }
}
