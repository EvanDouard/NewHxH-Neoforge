package com.evandouard.hxh.hatsu;

import com.evandouard.hxh.HxHMod;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Noms des techniques côté serveur (le serveur n'a pas les traductions) : lus une fois dans les fichiers de langue
 * du mod, pour que {@code /hunter hatsu give} accepte « Jajanken: Rock », « Jajanken : Pierre » ou {@code gon_rock},
 * sans tenir compte des majuscules, accents ni ponctuation.
 */
public final class TechniqueNames {
    private static Map<String, Map<String, String>> names; // langue → id → nom

    private TechniqueNames() {}

    private static synchronized Map<String, Map<String, String>> names() {
        if (names != null) return names;
        Map<String, Map<String, String>> out = new HashMap<>();
        for (String lang : new String[]{"en_us", "fr_fr"}) {
            Map<String, String> m = new HashMap<>();
            try (InputStream in = TechniqueNames.class.getResourceAsStream("/assets/hxh/lang/" + lang + ".json")) {
                if (in != null) {
                    JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                    for (var e : o.entrySet()) {
                        String k = e.getKey();
                        if (k.startsWith("technique.hxh.") && !k.endsWith(".desc") && k.indexOf('.', "technique.hxh.".length()) < 0) {
                            m.put(k.substring("technique.hxh.".length()), e.getValue().getAsString());
                        }
                    }
                }
            } catch (Exception e) {
                HxHMod.LOGGER.warn("Noms des techniques ({}) illisibles : {}", lang, e.getMessage());
            }
            out.put(lang, m);
        }
        names = out;
        return out;
    }

    /** Toutes les techniques jouables (les idées pas encore codées exclues). */
    public static List<Technique> playable() {
        MobTechniques.init();
        // charge les classes qui enregistrent des techniques (sans ça, une classe pas encore utilisée manquerait)
        Object[] loaded = {Characters.GING_COPY, Specials.CHROLLO_SKILL_HUNTER, Netero.KANNON, ArenaChallengers.GIDO_TOPS,
                FamilyMoves.KURTA_SCARLET, Techniques.GON_ROCK};
        if (loaded.length == 0) return List.of();
        List<Technique> out = new ArrayList<>();
        for (Technique t : Techniques.all().values()) if (t.implemented()) out.add(t);
        return out;
    }

    /** Nom dans la langue du joueur (fr si sa langue commence par fr, sinon en), ou l'id. */
    public static String name(String id, String lang) {
        String l = lang != null && lang.startsWith("fr") ? "fr_fr" : "en_us";
        return names().getOrDefault(l, Map.of()).getOrDefault(id, id);
    }

    /** Clé de comparaison : minuscules, sans accents, seulement lettres et chiffres. */
    public static String key(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /** Technique désignée par un id ou un nom (anglais ou français), ou null. */
    @Nullable
    public static Technique find(String text) {
        String t = text.trim();
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) t = t.substring(1, t.length() - 1);
        Technique byId = Techniques.get(t);
        if (byId != null && byId.implemented()) return byId;
        String k = key(t);
        if (k.isEmpty()) return null;
        for (Technique tech : playable()) {
            if (key(tech.id()).equals(k)) return tech;
            for (Map<String, String> m : names().values()) {
                String n = m.get(tech.id());
                if (n != null && key(n).equals(k)) return tech;
            }
        }
        return null;
    }
}
