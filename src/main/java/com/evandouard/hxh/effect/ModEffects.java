package com.evandouard.hxh.effect;

import com.evandouard.hxh.HxHMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Effets « témoins » : ils ne font rien par eux-mêmes, ils rappellent au joueur que le Gyo ou l'En
 * est actif, avec l'affichage d'effet de potion qu'il connaît déjà.
 */
public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, HxHMod.MODID);

    public static final DeferredHolder<MobEffect, NenEffect> GYO = EFFECTS.register("gyo", () -> new NenEffect(0xFFD27F));
    public static final DeferredHolder<MobEffect, NenEffect> EN = EFFECTS.register("en", () -> new NenEffect(0x7FC8FF));

    private ModEffects() {}

    public static final class NenEffect extends MobEffect {
        NenEffect(int color) {
            super(MobEffectCategory.BENEFICIAL, color);
        }
    }
}
