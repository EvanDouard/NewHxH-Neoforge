package com.evandouard.hxh.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Une quête décrite en JSON ({@code data/<namespace>/quests/<nom>.json}) : une suite d'étapes.
 * Le guide complet est dans {@code docs/creer-une-quete.md}.
 * <p>
 * Types d'étapes : {@code talk} (parler à un PNJ), {@code kill} (tuer {@code count} {@code entity}),
 * {@code follow} (rester à moins de {@code max_distance} blocs du PNJ pendant {@code seconds} s),
 * {@code bring} (apporter {@code count} {@code item} au PNJ), {@code reach} (aller à {@code pos}, à {@code radius}
 * blocs près), {@code custom} (condition décrite dans {@code note}, à coder plus tard ; validée par
 * {@code /hunter quest validate}). Tour Céleste : {@code arena_win}, {@code arena_floor}, {@code arena_register_200}.
 * <p>
 * Textes : soit des clés de traduction ({@code quest.<ns>.<nom>}, {@code quest.<ns>.<nom>.<étape>},
 * {@code dialog.<ns>.<nom>.<étape>} dans les fichiers de langue), soit directement dans le JSON :
 * {@code "title": {"fr": "…", "en": "…"}} (ou une simple chaîne). Les fichiers de langue l'emportent s'ils existent.
 */
public record Quest(ResourceLocation id, Map<String, String> title, String startNpc, List<String> requires,
                    Map<String, String> intro, Map<String, Map<String, String>> npcNames, List<Step> steps,
                    List<String> problems) {

    /** Types d'étapes connus. */
    public static final Set<String> TYPES = Set.of("talk", "kill", "follow", "bring", "reach", "custom",
            "nen_mode", "nen_sense", "nen_flag",
            "arena_win", "arena_floor", "arena_register_200");

    public record Step(String id, String type, String npc, String entity, int count, int seconds,
                       double maxDistance, String rewardItem, boolean rewardHunter, int rewardJenny,
                       String item, double[] pos, double radius, String note,
                       Map<String, String> objective, Map<String, String> dialog,
                       String mode, List<String> rewardLearn) {

        /** Objectif chiffré de l'étape (pour la progression affichée) : nombre de mobs ou secondes, sinon 0. */
        public int goal() {
            return switch (type) {
                case "kill", "arena_win", "arena_floor" -> count;
                case "bring" -> count;
                case "follow", "nen_mode", "nen_sense" -> seconds;
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

    public String dialogKey(Step step) {
        return "dialog." + id.getNamespace() + "." + id.getPath() + "." + step.id();
    }

    // ---------- Textes ----------

    public Component title(ServerPlayer player) {
        return text(titleKey(), title, player);
    }

    public Component objective(Step step, ServerPlayer player) {
        return text(stepKey(step), step.objective(), player);
    }

    public Component dialog(Step step, ServerPlayer player) {
        return text(dialogKey(step), step.dialog(), player);
    }

    /** Réplique du PNJ qui donne la quête, au moment où elle démarre. */
    public Component introDialog(ServerPlayer player) {
        return text("dialog." + id.getNamespace() + "." + id.getPath() + ".intro", intro, player);
    }

    /**
     * Texte d'une clé de traduction, avec le texte écrit dans le JSON comme repli (dans la langue du joueur si
     * elle y est). Sans texte dans le JSON : la clé seule (fichiers de langue).
     */
    public static Component text(String key, Map<String, String> inline, ServerPlayer player, Object... args) {
        if (inline == null || inline.isEmpty()) return Component.translatable(key, args);
        String lang = player == null ? "fr_fr" : player.clientInformation().language().toLowerCase(Locale.ROOT);
        return Component.translatableWithFallback(key, pick(inline, lang), args);
    }

    /** Texte dans la langue demandée : « fr_fr », puis « fr », puis « * », « fr », « en », sinon le premier. */
    public static String pick(Map<String, String> texts, String lang) {
        if (texts.containsKey(lang)) return texts.get(lang);
        String shortLang = lang.length() >= 2 ? lang.substring(0, 2) : lang;
        for (String k : List.of(shortLang, "*", "fr", "en")) if (texts.containsKey(k)) return texts.get(k);
        return texts.values().iterator().next();
    }

    // ---------- Lecture du JSON ----------

    private static String str(JsonObject o, String key, String fallback) {
        return o.has(key) ? o.get(key).getAsString() : fallback;
    }

    /** Texte du JSON : une chaîne (toutes langues) ou {"fr": "…", "en": "…"}. */
    static Map<String, String> texts(JsonObject o, String key) {
        Map<String, String> map = new LinkedHashMap<>();
        if (!o.has(key)) return map;
        JsonElement e = o.get(key);
        if (e.isJsonPrimitive()) {
            map.put("*", e.getAsString());
        } else if (e.isJsonObject()) {
            for (Map.Entry<String, JsonElement> t : e.getAsJsonObject().entrySet()) {
                map.put(t.getKey().toLowerCase(Locale.ROOT), t.getValue().getAsString());
            }
        }
        return map;
    }

    /** Une chaîne ou une liste de chaînes. */
    static List<String> stringList(JsonObject o, String key) {
        List<String> out = new ArrayList<>();
        if (!o.has(key)) return out;
        JsonElement e = o.get(key);
        if (e.isJsonArray()) for (JsonElement x : e.getAsJsonArray()) out.add(x.getAsString());
        else out.add(e.getAsString());
        return List.copyOf(out);
    }

    public static Quest parse(ResourceLocation id, JsonElement json) {
        JsonObject root = json.getAsJsonObject();
        List<String> problems = new ArrayList<>();
        JsonArray array = root.has("steps") ? root.getAsJsonArray("steps") : new JsonArray();
        if (array.isEmpty()) problems.add("aucune étape (\"steps\")");
        List<Step> steps = new ArrayList<>();
        for (JsonElement e : array) {
            JsonObject s = e.getAsJsonObject();
            String stepId = str(s, "id", "step" + steps.size());
            String type = str(s, "type", "talk");
            String where = "étape « " + stepId + " » : ";
            double[] pos = null;
            if (s.has("pos")) {
                JsonArray p = s.getAsJsonArray("pos");
                pos = new double[]{p.get(0).getAsDouble(), p.get(1).getAsDouble(), p.get(2).getAsDouble()};
            }
            Step step = new Step(
                    stepId,
                    type,
                    str(s, "npc", ""),
                    str(s, "entity", ""),
                    s.has("count") ? s.get("count").getAsInt() : 1,
                    s.has("seconds") ? s.get("seconds").getAsInt() : 0,
                    s.has("max_distance") ? s.get("max_distance").getAsDouble() : 16,
                    str(s, "reward_item", ""),
                    s.has("reward_hunter") && s.get("reward_hunter").getAsBoolean(),
                    s.has("reward_jenny") ? s.get("reward_jenny").getAsInt() : 0,
                    str(s, "item", ""),
                    pos,
                    s.has("radius") ? s.get("radius").getAsDouble() : 4,
                    str(s, "note", ""),
                    texts(s, "objective"),
                    texts(s, "dialog"),
                    str(s, "mode", ""),
                    stringList(s, "reward_learn"));
            // Vérifications : les erreurs ne bloquent pas, elles s'affichent avec /hunter quest check
            if (!TYPES.contains(type)) problems.add(where + "type inconnu « " + type + " »");
            if (List.of("talk", "follow", "bring").contains(type) && step.npc().isEmpty()) problems.add(where + "il manque \"npc\"");
            if (type.equals("kill") && !BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.tryParse(step.entity())))
                problems.add(where + "mob inconnu « " + step.entity() + " »");
            if (type.equals("bring") && !BuiltInRegistries.ITEM.containsKey(ResourceLocation.tryParse(step.item())))
                problems.add(where + "objet inconnu « " + step.item() + " »");
            if (type.equals("reach") && pos == null) problems.add(where + "il manque \"pos\": [x, y, z]");
            if (type.equals("custom") && step.note().isEmpty()) problems.add(where + "décrire la condition dans \"note\"");
            if (type.equals("nen_mode") && com.evandouard.hxh.data.NenMode.byId(step.mode()) == null)
                problems.add(where + "posture inconnue « " + step.mode() + " » (ten, zetsu, in, ren, ken)");
            if (type.equals("nen_sense") && !List.of("gyo", "en").contains(step.mode()))
                problems.add(where + "sens inconnu « " + step.mode() + " » (gyo, en)");
            if (type.equals("nen_flag") && !List.of("unlocked", "revealed", "kit").contains(step.mode()))
                problems.add(where + "drapeau inconnu « " + step.mode() + " » (unlocked, revealed, kit)");
            for (String a : step.rewardLearn())
                if (com.evandouard.hxh.nen.NenLearning.Ability.byId(a) == null) problems.add(where + "capacité inconnue « " + a + " »");
            if (!step.rewardItem().isEmpty() && !BuiltInRegistries.ITEM.containsKey(ResourceLocation.tryParse(step.rewardItem())))
                problems.add(where + "récompense inconnue « " + step.rewardItem() + " »");
            steps.add(step);
        }
        List<String> requires = new ArrayList<>();
        if (root.has("requires")) for (JsonElement r : root.getAsJsonArray("requires")) requires.add(r.getAsString());
        Map<String, Map<String, String>> npcNames = new LinkedHashMap<>();
        if (root.has("npcs")) {
            for (Map.Entry<String, JsonElement> n : root.getAsJsonObject("npcs").entrySet()) {
                JsonObject wrap = new JsonObject();
                wrap.add("name", n.getValue());
                npcNames.put(n.getKey(), texts(wrap, "name"));
            }
        }
        return new Quest(id, texts(root, "title"), str(root, "start_npc", ""), List.copyOf(requires),
                texts(root, "intro"), npcNames, List.copyOf(steps), List.copyOf(problems));
    }
}
