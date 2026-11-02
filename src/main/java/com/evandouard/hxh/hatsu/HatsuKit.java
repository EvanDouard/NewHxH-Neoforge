package com.evandouard.hxh.hatsu;

import com.evandouard.hxh.data.NenType;

import java.util.List;

/**
 * Un personnage emblématique et son Hatsu : ce que le joueur choisit (définitivement) au déblocage.
 * Un mob du même personnage utilise exactement les mêmes techniques.
 */
public record HatsuKit(String id, NenType type, List<String> techniques) {
    public String translationKey() {
        return "kit.hxh." + id;
    }

    public String descriptionKey() {
        return "kit.hxh." + id + ".desc";
    }
}
