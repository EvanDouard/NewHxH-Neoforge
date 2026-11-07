package com.evandouard.hxh.combat;

/**
 * Animations de combat jouées sur n'importe quelle entité humanoïde (joueur, PNJ, adversaire de la Tour).
 * Le serveur décide quelle animation part ; les clients la jouent sur le modèle (et en vue à la première personne).
 */
public enum CombatAnim {
    /** Coup de poing de la main principale (bras droit pour un droitier). */
    PUNCH_MAIN(7),
    /** Coup de poing de l'autre main : les coups de base alternent droite / gauche. */
    PUNCH_OFF(7),
    /** Armement de la frappe chargée : le bras recule et reste armé tant que la touche est maintenue. */
    CHARGE(200),
    /** Frappe chargée relâchée : gros coup de poing de la main principale. */
    CHARGED_PUNCH(12),
    /** Coup de pied (appui court sur la touche Frappe). */
    KICK(9),
    /** Arrête l'animation en cours (frappe ratée). */
    STOP(0);

    /** Durée en ticks, après quoi le modèle revient à sa pose normale. */
    public final int duration;

    CombatAnim(int duration) {
        this.duration = duration;
    }

    public boolean isPunch() {
        return this == PUNCH_MAIN || this == PUNCH_OFF;
    }

    public static CombatAnim byId(int id) {
        CombatAnim[] all = values();
        return id >= 0 && id < all.length ? all[id] : STOP;
    }
}
