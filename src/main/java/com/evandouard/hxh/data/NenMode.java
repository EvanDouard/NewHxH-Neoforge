package com.evandouard.hxh.data;

import java.util.Locale;

/**
 * État de base de l'aura. Un seul actif à la fois.
 */
public enum NenMode {
    /** Aura qui fuit naturellement : régénération normale. */
    NONE,
    /** Ten : l'aura enveloppe le corps, réduit les dégâts subis. */
    TEN,
    /** Zetsu : aura coupée, régénération rapide, mais très vulnérable. */
    ZETSU,
    /** Ren : aura amplifiée, dégâts augmentés, consomme l'aura. */
    REN;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "nen_mode.hxh." + id();
    }

    public static NenMode byId(String id) {
        for (NenMode m : values()) if (m.id().equals(id)) return m;
        return null;
    }
}
