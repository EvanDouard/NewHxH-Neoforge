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

    /** Peur : face à une aura écrasante, on bouge moins vite et on frappe moins fort. */
    public static final DeferredHolder<MobEffect, MobEffect> FEAR = EFFECTS.register("fear", () -> new FearEffect()
            .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, HxHMod.id("fear_speed"),
                    -0.15, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, HxHMod.id("fear_damage"),
                    -0.2, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    private ModEffects() {}

    public static final class FearEffect extends MobEffect {
        FearEffect() {
            super(MobEffectCategory.HARMFUL, 0x3A2A4A);
        }
    }

    public static final class NenEffect extends MobEffect {
        NenEffect(int color) {
            super(MobEffectCategory.BENEFICIAL, color);
        }
    }
}
