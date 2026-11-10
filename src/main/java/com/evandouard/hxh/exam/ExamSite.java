package com.evandouard.hxh.exam;

import com.evandouard.hxh.HxHMod;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/** Les lieux de l'Examen Hunter, dans l'ordre du parcours. */
public enum ExamSite {
    ZABAN("zaban_restaurant"),
    HALL("hall_inscription"),
    BISKA("biska"),
    HOTEL("hotel"),
    /** Bateau vers l'île de Dolle, pris dans la tempête (épreuve du capitaine). */
    BOAT("bateau"),
    /** Île de Dolle : la maison de la Mamie au quiz, sur le chemin de Zaban. */
    DOLLE("dolle"),
    /** Marais de Numele, à la sortie du tunnel : brouillard, il faut suivre Satotz. */
    NUMELE("numele"),
    /** Mont Split : sauter dans la faille pour cueillir un œuf d'aigle-araignée. */
    MOUNT_SPLIT("mont_split"),
    /** Île de Zevil, où l'on débarque en bateau : chasse aux plaques. */
    ZEVIL("zevil"),
    /** Salle du dirigeable de l'Association : le jeu de la balle de Netero. */
    AIRSHIP("dirigeable"),
    /** Tour de Trick (Phase 3) : descendre du toit à la sortie. */
    TOWER("tour_de_trick"),
    /** Forêt des Kiriko (en route pour Zaban) : démasquer la bête qui a pris l'apparence du mari. */
    KIRIKO("foret_kiriko");

    private final String file;

    ExamSite(String file) {
        this.file = file;
    }

    /** Identifiant court utilisé dans les quêtes ({@code "site": "zaban"}). */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Structure d'Evan, si elle existe : {@code hxh:exam/<fichier>}. */
    public ResourceLocation template() {
        return HxHMod.id("exam/" + file);
    }

    @Nullable
    public static ExamSite byId(String id) {
        for (ExamSite s : values()) if (s.id().equals(id) || s.file.equals(id)) return s;
        return null;
    }
}
