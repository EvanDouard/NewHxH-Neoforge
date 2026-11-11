package com.evandouard.hxh.hatsu;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * Coup porté par une technique de Nen : même type de dégâts qu'une attaque normale (crédit du tueur, butin…),
 * mais reconnaissable pour afficher les dégâts du Nen en bleu.
 */
public class NenDamageSource extends DamageSource {
    public NenDamageSource(Holder<DamageType> type, @Nullable Entity direct, @Nullable Entity causing) {
        super(type, direct, causing);
    }

    /** Dégâts de Nen : technique, ou projectile de Nen (boule de la Pierre, lame des Ciseaux…). */
    public static boolean isNen(DamageSource source) {
        return source instanceof NenDamageSource || source.getDirectEntity() instanceof NenProjectile;
    }
}
