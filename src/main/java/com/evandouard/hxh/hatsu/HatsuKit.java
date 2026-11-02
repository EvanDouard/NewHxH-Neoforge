package com.evandouard.hxh.hatsu;

import com.evandouard.hxh.data.NenType;

import java.util.List;

/**
 * Un personnage emblématique et son Hatsu : ce que le joueur choisit (définitivement) au déblocage.
 * Un mob du même personnage utilise exactement les mêmes techniques et passifs.
 *
 * @param typeConfirmed false si le type de Nen n'est pas confirmé dans l'œuvre (type supposé)
 * @param implemented   false tant que le personnage n'est qu'une liste d'idées (non choisissable)
 */
public record HatsuKit(String id, NenType type, boolean typeConfirmed, List<String> techniques,
                       List<Passive> passives, boolean implemented) {
    public String translationKey() {
        return "kit.hxh." + id;
    }

    public String descriptionKey() {
        return "kit.hxh." + id + ".desc";
    }
}
