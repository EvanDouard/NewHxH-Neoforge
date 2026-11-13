package com.evandouard.hxh.org;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Organisation (fichier {@code data/<mod>/organizations/<id>.json}) : nom, couleur, alliés et ennemis. On la rejoint
 * par une quête (récompense {@code reward_join}). Les membres d'organisations ennemies peuvent se frapper avec
 * leurs techniques ; entre alliés et membres d'une même organisation, jamais. {@code member_kits} : personnages
 * qui en font partie (un mob avec ce kit est un membre, par exemple pour le serment de Kurapika).
 */
public record Organization(String id, String nameFr, String nameEn, int color, List<String> allies, List<String> enemies,
                           String descFr, String descEn, List<String> memberKits) {

    public static Organization parse(ResourceLocation file, JsonObject o) {
        JsonObject name = o.getAsJsonObject("name");
        JsonObject desc = o.has("description") ? o.getAsJsonObject("description") : new JsonObject();
        String fr = name.has("fr") ? name.get("fr").getAsString() : file.getPath();
        String en = name.has("en") ? name.get("en").getAsString() : fr;
        int color = o.has("color") ? Integer.parseInt(o.get("color").getAsString().replace("#", ""), 16) : 0xFFFFFF;
        return new Organization(file.getPath(), fr, en, color, strings(o, "allies"), strings(o, "enemies"),
                desc.has("fr") ? desc.get("fr").getAsString() : "", desc.has("en") ? desc.get("en").getAsString() : "",
                strings(o, "member_kits"));
    }

    private static List<String> strings(JsonObject o, String key) {
        List<String> out = new ArrayList<>();
        if (o.has(key)) for (var e : o.getAsJsonArray(key)) out.add(e.getAsString());
        return List.copyOf(out);
    }

    /** Nom dans la langue du joueur (texte serveur : on donne les deux si besoin). */
    public String name(String lang) {
        return lang != null && lang.startsWith("fr") ? nameFr : nameEn;
    }

    public boolean isEnemy(String other) {
        return enemies.contains(other);
    }

    public boolean isAlly(String other) {
        return id.equals(other) || allies.contains(other);
    }
}
