package com.evandouard.hxh.hatsu;

import java.util.List;
import java.util.Map;

/**
 * Jauges propres à certains personnages, affichées sous la jauge d'aura : Prière de Netero, Douleur de Feitan…
 * La valeur est une ressource du personnage ({@code HunterData.kitValue(key)}), de 0 à {@code max}.
 */
public final class KitBars {
    private KitBars() {}

    /** Une jauge : ressource, maximum, couleur ARGB, clé du libellé. */
    public record Bar(String key, float max, int color, String label) {}

    public static final String PRAYER = "prayer", PAIN = "pain";
    public static final float PRAYER_MAX = 100f, PAIN_MAX = 100f;

    private static final Map<String, List<Bar>> BY_KIT = Map.of(
            "netero", List.of(new Bar(PRAYER, PRAYER_MAX, 0xFFFFD23F, "hud.hxh.bar.prayer")),
            "feitan", List.of(new Bar(PAIN, PAIN_MAX, 0xFFE0402A, "hud.hxh.bar.pain")));

    public static List<Bar> of(String kit) {
        return kit == null ? List.of() : BY_KIT.getOrDefault(kit, List.of());
    }
}
