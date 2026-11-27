package com.evandouard.hxh.org;

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
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.TreeMap;

/** Charge les organisations ({@code organizations}) ; rechargées par /reload. Adhésion, départ, alliés et ennemis. */
public final class Organizations extends SimpleJsonResourceReloadListener {
    private static volatile Map<String, Organization> ORGS = Map.of();

    private Organizations() {
        super(new Gson(), "organizations");
    }

    public static void register(AddReloadListenerEvent event) {
        event.addListener(new Organizations());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, Organization> loaded = new TreeMap<>();
        files.forEach((id, json) -> {
            try {
                loaded.put(id.getPath(), Organization.parse(id, json.getAsJsonObject()));
            } catch (Exception e) {
                HxHMod.LOGGER.error("Organisation {} invalide : {}", id, e.getMessage());
            }
        });
        ORGS = Map.copyOf(loaded);
        HxHMod.LOGGER.info("{} organisation(s) chargée(s)", loaded.size());
    }

    @Nullable
    public static Organization get(String id) {
        return id == null || id.isEmpty() ? null : ORGS.get(id);
    }

    public static Map<String, Organization> all() {
        return ORGS;
    }

    /** Organisation du joueur, ou null. */
    @Nullable
    public static Organization of(Player player) {
        return player instanceof ServerPlayer sp ? get(QuestManager.data(sp).organization()) : null;
    }

    /** Toutes les organisations d'une entité : celles rejointes (joueur), celles de son kit ou de son espèce (mob), et les automatiques (Nen). */
    public static java.util.Set<String> memberships(net.minecraft.world.entity.LivingEntity e) {
        java.util.Set<String> out = new java.util.LinkedHashSet<>();
        if (e instanceof ServerPlayer sp) out.addAll(QuestManager.data(sp).organizations());
        boolean hasHunter = e.hasData(com.evandouard.hxh.data.ModAttachments.HUNTER);
        com.evandouard.hxh.data.HunterData hd = hasHunter ? e.getData(com.evandouard.hxh.data.ModAttachments.HUNTER) : null;
        String typeId = net.minecraft.world.entity.EntityType.getKey(e.getType()).toString();
        for (Organization o : ORGS.values()) {
            if (!(e instanceof Player) && hd != null && o.memberKits().contains(hd.kit())) out.add(o.id());
            if (o.memberEntities().contains(typeId)) out.add(o.id());
            if (o.auto().equals("nen") && hd != null && hd.nenUnlocked()) out.add(o.id());
        }
        return out;
    }

    /** L'entité fait-elle partie de l'organisation ? */
    public static boolean belongs(net.minecraft.world.entity.LivingEntity e, String orgId) {
        return get(orgId) != null && memberships(e).contains(orgId);
    }

    /** Une organisation de {@code a} a pour ennemie une organisation de {@code b} (ou l'inverse). */
    public static boolean hostile(net.minecraft.world.entity.LivingEntity a, net.minecraft.world.entity.LivingEntity b) {
        if (a == b) return false;
        return hostile(memberships(a), memberships(b));
    }

    public static boolean hostile(java.util.Set<String> as, java.util.Set<String> bs) {
        for (String x : as) {
            Organization ox = get(x);
            if (ox == null) continue;
            for (String y : bs) {
                if (x.equals(y)) continue;
                Organization oy = get(y);
                if (ox.isEnemy(y) || (oy != null && oy.isEnemy(x))) return true;
            }
        }
        return false;
    }

    /** Un mob d'une organisation « hostile à vue » attaque-t-il cette cible ? */
    public static boolean attacksOnSight(net.minecraft.world.entity.LivingEntity mob, net.minecraft.world.entity.LivingEntity target) {
        if (mob == target || target.isSpectator() || (target instanceof Player p && p.isCreative())) return false;
        java.util.Set<String> mine = memberships(mob), theirs = memberships(target);
        for (String x : mine) {
            Organization ox = get(x);
            if (ox == null || !ox.hostileOnSight()) continue;
            for (String y : theirs) {
                Organization oy = get(y);
                if (!x.equals(y) && (ox.isEnemy(y) || (oy != null && oy.isEnemy(x)))) return true;
            }
        }
        return false;
    }

    /** Deux joueurs dans des organisations ennemies : leurs techniques se touchent. */
    public static boolean enemies(Player a, Player b) {
        if (!(a instanceof ServerPlayer) || !(b instanceof ServerPlayer)) return false;
        return hostile(a, b);
    }

    /** Récompense de quête {@code reward_join} : on rejoint l'organisation (on garde les autres). */
    public static void join(ServerPlayer player, String id) {
        Organization org = get(id);
        if (org == null) {
            HxHMod.LOGGER.warn("Organisation inconnue dans une récompense : {}", id);
            return;
        }
        QuestData d = QuestManager.data(player);
        if (d.hasOrganization(id)) return;
        d.addOrganization(id);
        com.evandouard.hxh.network.Hud.toast(player, Component.translatable("org.hxh.toast.joined"), label(org, player), "◆",
                com.evandouard.hxh.network.Hud.GOLD);
        if (!org.enemies().isEmpty()) {
            player.sendSystemMessage(Component.translatable("org.hxh.enemies", String.join(", ",
                    org.enemies().stream().map(e -> get(e) != null ? get(e).name(player.clientInformation().language()) : e).toList()))
                    .withStyle(ChatFormatting.RED));
        }
    }

    /** Quitte toutes les organisations rejointes. */
    public static boolean leave(ServerPlayer player) {
        QuestData d = QuestManager.data(player);
        if (d.organizations().isEmpty()) {
            player.sendSystemMessage(Component.translatable("org.hxh.none").withStyle(ChatFormatting.GRAY));
            return false;
        }
        for (String id : java.util.List.copyOf(d.organizations())) leave(player, id);
        return true;
    }

    /** Quitte une organisation précise. */
    public static boolean leave(ServerPlayer player, String id) {
        QuestData d = QuestManager.data(player);
        Organization old = get(id);
        if (!d.removeOrganization(id)) return false;
        player.sendSystemMessage(Component.translatable("org.hxh.left", old != null ? label(old, player) : Component.literal(id)).withStyle(ChatFormatting.YELLOW));
        return true;
    }

    /** Rayon dans lequel les membres d'une organisation volent au secours de l'un des leurs. */
    public static final double SOLIDARITY_RADIUS = 24;

    public static void registerEvents(net.neoforged.bus.api.IEventBus bus) {
        bus.addListener(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent.class, Organizations::onAttacked);
    }

    /**
     * Solidarité : si un membre d'une organisation est attaqué, les autres membres (même neutres) qui l'entourent
     * se retournent contre l'agresseur, sauf s'il est lui-même de l'organisation.
     */
    private static void onAttacked(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        var victim = event.getEntity();
        if (victim.level().isClientSide() || !(event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker) || attacker == victim) return;
        if (attacker instanceof Player p && (p.isCreative() || p.isSpectator())) return;
        java.util.Set<String> orgs = memberships(victim);
        if (orgs.isEmpty()) return;
        java.util.Set<String> attackerOrgs = memberships(attacker);
        java.util.Set<String> concerned = new java.util.LinkedHashSet<>(orgs);
        concerned.removeAll(attackerOrgs); // l'agresseur est de la maison : pas de guerre interne
        concerned.removeIf(id -> get(id) == null || !get(id).auto().isEmpty()); // appartenance automatique (Nen) : pas de clan
        if (concerned.isEmpty()) return;
        for (net.minecraft.world.entity.Mob m : victim.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                victim.getBoundingBox().inflate(SOLIDARITY_RADIUS), m -> m != victim && m != attacker && m.isAlive())) {
            if (m.getTarget() != null || java.util.Collections.disjoint(memberships(m), concerned)) continue;
            m.setTarget(attacker);
            m.setLastHurtByMob(attacker);
        }
    }

    public static Component label(Organization org, ServerPlayer player) {
        return Component.literal(org.name(player.clientInformation().language())).withStyle(s -> s.withColor(org.color()));
    }
}
