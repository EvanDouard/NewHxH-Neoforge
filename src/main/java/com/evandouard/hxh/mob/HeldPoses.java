package com.evandouard.hxh.mob;

import com.evandouard.hxh.data.ModAttachments;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Poses tenues des personnages GeckoLib (animations {@code meditate} et {@code pray} générées pour chaque modèle
 * par tools/models/poses.py) : la même méditation que le joueur, en tailleur, et la prière de gratitude de Netero
 * en seiza, choisies d'après les données Hunter synchronisées de l'entité.
 */
public final class HeldPoses {
    public static final RawAnimation MEDITATE = RawAnimation.begin().thenLoop("meditate");
    public static final RawAnimation PRAY = RawAnimation.begin().thenLoop("pray");
    /** Fondu d'entrée et de sortie de la pose, en ticks (plus long que la marche : on s'assoit lentement). */
    public static final int TRANSITION = 12;

    private HeldPoses() {}

    /** Animation de pose à jouer, ou {@code null} si l'entité ne médite pas. */
    public static RawAnimation of(LivingEntity e) {
        if (!e.hasData(ModAttachments.HUNTER) || !e.getData(ModAttachments.HUNTER).meditating()) return null;
        return com.evandouard.hxh.hatsu.Netero.isNetero(e) ? PRAY : MEDITATE;
    }
}
