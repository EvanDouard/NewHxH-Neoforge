package com.evandouard.hxh.hatsu;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Serments propres à un personnage (en plus des serments communs) : une technique précise devient bien plus forte,
 * en échange d'une condition tirée du manga. Exemples : Kurapika ne peut viser que la Brigade Fantôme avec sa
 * Chaîne-prison, sinon il meurt ; Feitan ne déclenche le Soleil levant que blessé.
 * <p>
 * Gardés ici (et non dans les données) : l'écran des serments, côté client, doit les connaître pour les afficher.
 * Les noms et descriptions sont dans les fichiers de langue ({@code vow.hxh.character.<id>}).
 */
public final class CharacterVows {
    private CharacterVows() {}

    /** Condition : cible membre d'une organisation, ou lanceur blessé (vie sous {@code ratio}). */
    public enum Condition { TARGET_ORG, CASTER_HURT }

    /** Ce qui arrive si la condition n'est pas remplie : la mort, le sceau du Hatsu, ou simplement le refus. */
    public enum Penalty { DEATH, SEAL, REFUSE }

    public record Def(String id, String kit, String technique, float power, Condition condition, String org, float ratio,
                      Penalty penalty) {
        public String translationKey() {
            return "vow.hxh.character." + id;
        }

        public String descriptionKey() {
            return "vow.hxh.character." + id + ".desc";
        }
    }

    public static final List<Def> ALL = List.of(
            new Def("kurapika_judgment", "kurapika", "kurapika_chain_jail", 3f, Condition.TARGET_ORG, "ryodan", 0f, Penalty.DEATH),
            new Def("feitan_rising_sun", "feitan", "feitan_rising_sun", 1.8f, Condition.CASTER_HURT, "", 0.5f, Penalty.REFUSE));

    @Nullable
    public static Def forKit(String kit) {
        for (Def d : ALL) if (d.kit().equals(kit)) return d;
        return null;
    }
}
