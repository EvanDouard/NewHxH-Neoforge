package com.evandouard.hxh.computer;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.quest.QuestData;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Une fiche de l'ordinateur Hunter, décrite en données ({@code data/hxh/hunter_computer/<id>.json}) :
 * <pre>
 * {
 *   "category": { "fr": "Lieux", "en": "Places" },
 *   "category_order": 10,                          // place de la catégorie dans la liste
 *   "order": 20,                                   // tri dans la catégorie
 *   "title": { "fr": "La Tour Céleste", "en": "Heavens Arena" },
 *   "text":  { "fr": "…", "en": "…" },             // \n pour aller à la ligne
 *   "requires": ["floor:1"],                       // toutes doivent être vraies (vide = toujours visible)
 *   "hint": { "fr": "Inscris-toi à la Tour Céleste." }  // affiché tant que la fiche est verrouillée
 * }
 * </pre>
 * Conditions : {@code hunter} (licence), {@code nen} (Nen éveillé), {@code kit} (Hatsu choisi),
 * {@code floor:<n>} (étage atteint à la Tour), {@code quest:<id>} (quête terminée), {@code learned:<capacité>}
 * (ten, ren, gyo, en, ken…), {@code jenny:<n>} (en avoir au moins autant).
 */
public record ComputerEntry(ResourceLocation id, Map<String, String> category, int categoryOrder, int order, Map<String, String> title,
                            Map<String, String> text, List<String> requires, Map<String, String> hint) {

    public static ComputerEntry parse(ResourceLocation id, JsonObject json) {
        List<String> req = new ArrayList<>();
        if (json.has("requires")) for (JsonElement e : json.getAsJsonArray("requires")) req.add(e.getAsString());
        return new ComputerEntry(id, texts(json, "category"), GsonHelper.getAsInt(json, "category_order", 100),
                GsonHelper.getAsInt(json, "order", 100),
                texts(json, "title"), texts(json, "text"), List.copyOf(req), texts(json, "hint"));
    }

    private static Map<String, String> texts(JsonObject json, String key) {
        Map<String, String> out = new LinkedHashMap<>();
        if (!json.has(key)) return out;
        JsonElement e = json.get(key);
        if (e.isJsonPrimitive()) out.put("*", e.getAsString());
        else for (var en : e.getAsJsonObject().entrySet()) out.put(en.getKey(), en.getValue().getAsString());
        return out;
    }

    /** La fiche est-elle débloquée pour ce joueur ? Une condition inconnue la garde verrouillée. */
    public boolean unlocked(QuestData quests, HunterData hunter, boolean learningOff) {
        for (String r : requires) {
            String key = r.contains(":") ? r.substring(0, r.indexOf(':')) : r;
            String arg = r.contains(":") ? r.substring(r.indexOf(':') + 1) : "";
            boolean ok = switch (key) {
                case "hunter" -> quests.isHunter();
                case "nen" -> hunter.nenUnlocked();
                case "kit" -> !hunter.kit().isEmpty();
                case "floor" -> quests.arenaFloor() >= parse(arg) && quests.arenaRegistered();
                case "quest" -> quests.isCompleted(arg.contains(":") ? arg : "hxh:" + arg);
                case "learned" -> learningOff ? hunter.nenUnlocked() : hunter.learned(arg);
                case "jenny" -> quests.jenny() >= parse(arg);
                default -> false;
            };
            if (!ok) return false;
        }
        return true;
    }

    private static int parse(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }
}
