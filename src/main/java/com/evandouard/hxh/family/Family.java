package com.evandouard.hxh.family;

import com.evandouard.hxh.data.Stat;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Famille (fichier {@code data/<mod>/families/<id>.json}) : héritage du sang, tiré au hasard à la première connexion
 * (règle hxhRandomFamily). Tout est en données : poids du tirage, bonus d'XP par stat, immunité au poison, résistance
 * à la foudre, puissance accrue quand la vie est basse (yeux écarlates des Kurta), coup secret ({@code secret_move},
 * une technique de {@link com.evandouard.hxh.hatsu.FamilyMoves}).
 */
public record Family(String id, String nameFr, String nameEn, int color, int weight, Map<Stat, Double> xpBonus,
                     boolean poisonImmune, float lightningResist, float lowHealthPower, String descFr, String descEn,
                     String secretMove, Map<Stat, Integer> clanStats) {

    /** Vie (fraction du maximum) sous laquelle la puissance augmente. */
    public static final float LOW_HEALTH = 0.4f;

    public static Family parse(ResourceLocation file, JsonObject o) {
        JsonObject name = o.getAsJsonObject("name");
        JsonObject desc = o.has("description") ? o.getAsJsonObject("description") : new JsonObject();
        Map<Stat, Double> xp = new EnumMap<>(Stat.class);
        if (o.has("xp_bonus")) {
            for (var e : o.getAsJsonObject("xp_bonus").entrySet()) {
                xp.put(Stat.valueOf(e.getKey().toUpperCase(Locale.ROOT)), e.getValue().getAsDouble());
            }
        }
        return new Family(file.getPath(), name.get("fr").getAsString(), name.has("en") ? name.get("en").getAsString() : name.get("fr").getAsString(),
                o.has("color") ? Integer.parseInt(o.get("color").getAsString().replace("#", ""), 16) : 0xFFFFFF,
                o.has("weight") ? o.get("weight").getAsInt() : 0,
                Map.copyOf(xp),
                o.has("poison_immune") && o.get("poison_immune").getAsBoolean(),
                o.has("lightning_resist") ? o.get("lightning_resist").getAsFloat() : 0f,
                o.has("low_health_power") ? o.get("low_health_power").getAsFloat() : 0f,
                desc.has("fr") ? desc.get("fr").getAsString() : "", desc.has("en") ? desc.get("en").getAsString() : "",
                o.has("secret_move") ? o.get("secret_move").getAsString() : "",
                clanStats(o, xp));
    }

    /**
     * Niveaux de stat gagnés au niveau de clan 100 ({@code clan_stats}) ; par défaut, les stats favorites de la
     * famille ({@code xp_bonus} × 50 : +20 % d'XP → +10 niveaux au clan 100).
     */
    private static Map<Stat, Integer> clanStats(JsonObject o, Map<Stat, Double> xp) {
        Map<Stat, Integer> out = new EnumMap<>(Stat.class);
        if (o.has("clan_stats")) {
            for (var e : o.getAsJsonObject("clan_stats").entrySet()) {
                out.put(Stat.valueOf(e.getKey().toUpperCase(Locale.ROOT)), e.getValue().getAsInt());
            }
        } else {
            for (var e : xp.entrySet()) out.put(e.getKey(), (int) Math.round(e.getValue() * 50));
        }
        return Map.copyOf(out);
    }

    /** Bonus de stats au niveau de clan {@code level} (proportionnel, arrondi vers le bas). */
    public Map<Stat, Integer> clanBonus(int level) {
        Map<Stat, Integer> out = new EnumMap<>(Stat.class);
        for (var e : clanStats.entrySet()) {
            int b = e.getValue() * Math.max(0, Math.min(100, level)) / 100;
            if (b > 0) out.put(e.getKey(), b);
        }
        return out;
    }

    public String name(String lang) {
        return lang != null && lang.startsWith("fr") ? nameFr : nameEn;
    }

    public String description(String lang) {
        return lang != null && lang.startsWith("fr") ? descFr : descEn;
    }

    /** Multiplicateur d'XP pour cette stat (1 = normal). */
    public double xpMultiplier(Stat stat) {
        return 1.0 + xpBonus.getOrDefault(stat, 0.0);
    }
}
