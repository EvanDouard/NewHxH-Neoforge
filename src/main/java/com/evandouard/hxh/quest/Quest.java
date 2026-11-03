package com.evandouard.hxh.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Une quête décrite en JSON (data/&lt;namespace&gt;/quests/&lt;nom&gt;.json) : une suite d'étapes.
 * <p>
 * Types d'étapes : {@code talk} (parler à un PNJ), {@code kill} (tuer {@code count} {@code entity}),
 * {@code follow} (rester à moins de {@code max_distance} blocs du PNJ pendant {@code seconds} s).
 * Récompenses d'étape : {@code reward_item} (identifiant d'objet), {@code reward_hunter} (devient Hunter).
 */
public record Quest(ResourceLocation id, List<Step> steps) {

    public record Step(String id, String type, String npc, String entity, int count, int seconds,
                       double maxDistance, String rewardItem, boolean rewardHunter) {

        /** Objectif chiffré de l'étape (pour la progression affichée) : nombre de mobs ou secondes, sinon 0. */
        public int goal() {
            return switch (type) {
                case "kill" -> count;
                case "follow" -> seconds;
                default -> 0;
            };
        }
    }

    public String titleKey() {
        return "quest." + id.getNamespace() + "." + id.getPath();
    }

    public String stepKey(Step step) {
        return titleKey() + "." + step.id();
    }

    private static String str(JsonObject o, String key, String fallback) {
        return o.has(key) ? o.get(key).getAsString() : fallback;
    }

    public static Quest parse(ResourceLocation id, JsonElement json) {
        JsonObject root = json.getAsJsonObject();
        JsonArray array = root.getAsJsonArray("steps");
        List<Step> steps = new ArrayList<>();
        for (JsonElement e : array) {
            JsonObject s = e.getAsJsonObject();
            steps.add(new Step(
                    str(s, "id", "step" + steps.size()),
                    str(s, "type", "talk"),
                    str(s, "npc", ""),
                    str(s, "entity", ""),
                    s.has("count") ? s.get("count").getAsInt() : 1,
                    s.has("seconds") ? s.get("seconds").getAsInt() : 0,
                    s.has("max_distance") ? s.get("max_distance").getAsDouble() : 16,
                    str(s, "reward_item", ""),
                    s.has("reward_hunter") && s.get("reward_hunter").getAsBoolean()));
        }
        return new Quest(id, List.copyOf(steps));
    }
}
