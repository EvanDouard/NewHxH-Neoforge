package com.evandouard.hxh.mob;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.Mastery;
import com.evandouard.hxh.data.NenMode;
import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.hatsu.HatsuKit;
import com.evandouard.hxh.hatsu.HatsuKits;
import com.evandouard.hxh.nen.NenModes;
import com.evandouard.hxh.stats.HunterStats;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mob d'exemple décrit en JSON ({@code data/<namespace>/mob_presets/<nom>.json}) : n'importe quel mob de Minecraft
 * avec des stats Hunter, éventuellement le Nen et un kit de Hatsu, de l'équipement… Sert à tester le combat.
 * <pre>
 * {
 *   "entity": "minecraft:zombie",
 *   "stats": { "all": 10, "strength": 50 },           // niveaux 0 à 90 ; "all" = valeur par défaut
 *   "nen": { "level": 30, "kit": "gon", "mastery": 20, "mode": "ten" },   // optionnel ; "type" si pas de kit
 *   "equipment": { "mainhand": "minecraft:iron_sword", "head": "minecraft:iron_helmet" },   // optionnel
 *   "no_ai": false, "knockback_resistance": 0.0, "persistent": true
 * }
 * </pre>
 * Le nom affiché est la traduction {@code mob_preset.<namespace>.<nom>}.
 */
public record MobPreset(ResourceLocation id, ResourceLocation entity, Map<Stat, Integer> stats,
                        int nenLevel, @Nullable String kit, @Nullable NenType nenType, int mastery,
                        @Nullable NenMode mode, Map<EquipmentSlot, ResourceLocation> equipment,
                        boolean noAi, double knockbackResistance, boolean persistent) {

    public String nameKey() {
        return "mob_preset." + id.getNamespace() + "." + id.getPath();
    }

    public static MobPreset parse(ResourceLocation id, JsonElement element) {
        JsonObject json = element.getAsJsonObject();
        ResourceLocation entity = ResourceLocation.parse(GsonHelper.getAsString(json, "entity"));
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(entity)) throw new IllegalArgumentException("Mob inconnu : " + entity);

        Map<Stat, Integer> stats = new EnumMap<>(Stat.class);
        JsonObject s = GsonHelper.getAsJsonObject(json, "stats", new JsonObject());
        int all = GsonHelper.getAsInt(s, "all", 0);
        for (Stat stat : Stat.values()) stats.put(stat, clamp(GsonHelper.getAsInt(s, stat.id(), all)));

        int nenLevel = 0, mastery = 0;
        String kit = null;
        NenType type = null;
        NenMode mode = null;
        if (json.has("nen")) {
            JsonObject n = GsonHelper.getAsJsonObject(json, "nen");
            nenLevel = clamp(GsonHelper.getAsInt(n, "level", 1));
            kit = GsonHelper.getAsString(n, "kit", null);
            if (kit != null && HatsuKits.get(kit) == null) throw new IllegalArgumentException("Kit inconnu : " + kit);
            String t = GsonHelper.getAsString(n, "type", null);
            type = t == null ? null : NenType.byId(t);
            mastery = clamp(GsonHelper.getAsInt(n, "mastery", nenLevel / 2));
            String m = GsonHelper.getAsString(n, "mode", null);
            mode = m == null ? null : NenMode.byId(m);
        }

        Map<EquipmentSlot, ResourceLocation> equipment = new LinkedHashMap<>();
        JsonObject eq = GsonHelper.getAsJsonObject(json, "equipment", new JsonObject());
        for (Map.Entry<String, JsonElement> e : eq.entrySet()) {
            EquipmentSlot slot = EquipmentSlot.byName(e.getKey());
            ResourceLocation item = ResourceLocation.parse(e.getValue().getAsString());
            if (!BuiltInRegistries.ITEM.containsKey(item)) throw new IllegalArgumentException("Objet inconnu : " + item);
            equipment.put(slot, item);
        }
        return new MobPreset(id, entity, stats, nenLevel, kit, type, mastery, mode, equipment,
                GsonHelper.getAsBoolean(json, "no_ai", false),
                GsonHelper.getAsDouble(json, "knockback_resistance", 0.0),
                GsonHelper.getAsBoolean(json, "persistent", true));
    }

    private static int clamp(int level) {
        return Math.max(0, Math.min(com.evandouard.hxh.nen.NenMobAI.MOB_MAX_LEVEL, level));
    }

    /** Fait apparaître le mob à cette position, regard tourné vers {@code yaw}. */
    @Nullable
    public LivingEntity spawn(ServerLevel level, Vec3 pos, float yaw) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entity);
        Entity created = type.create(level);
        if (!(created instanceof LivingEntity living)) {
            if (created != null) created.discard();
            return null;
        }
        living.moveTo(pos.x, pos.y, pos.z, yaw, 0f);
        living.setYHeadRot(yaw);
        living.setYBodyRot(yaw);

        HunterData data = HunterStats.get(living);
        stats.forEach(data::setLevel);
        if (nenLevel > 0) {
            HatsuKit k = kit == null ? null : HatsuKits.get(kit);
            NenType t = k != null ? k.type() : nenType != null ? nenType : NenType.ENHANCER;
            data.setNenType(t);
            data.setNenRevealed(true);
            NenModes.unlock(living); // les stats sont déjà réglées : pas de tirage au hasard
            data.setNenLevel(nenLevel);
            for (Mastery m : Mastery.values()) data.setMasteryLevel(m, mastery);
            if (k != null) data.setKit(k.id());
            data.setAuraMax(NenModes.computeAuraMax(data));
            data.setAura(data.auraMax());
            if (mode != null) data.setMode(mode);
        }
        HunterStats.applyAttributes(living);
        AttributeInstance kb = living.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kb != null && knockbackResistance > 0) kb.setBaseValue(knockbackResistance);

        if (living instanceof Mob mob) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ResourceLocation item = equipment.get(slot);
                mob.setItemSlot(slot, item == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.get(item)));
                mob.setDropChance(slot, 0f);
            }
            if (noAi) mob.setNoAi(true);
            if (persistent) mob.setPersistenceRequired();
        }
        living.setHealth(living.getMaxHealth());
        living.setCustomName(Component.translatable(nameKey()));
        living.setCustomNameVisible(true);
        level.addFreshEntity(living);
        return living;
    }
}
