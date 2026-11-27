package com.evandouard.hxh.hatsu;

import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.fx.NenFx;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

import static com.evandouard.hxh.hatsu.Techniques.Effect;
import static com.evandouard.hxh.hatsu.Techniques.Look;
import static com.evandouard.hxh.hatsu.Techniques.Style;

/**
 * Personnages secondaires qui existent en mobs (modèle, œuf) sans être proposés au joueur : Gotoh, Canary, Hanzo,
 * Satotz, Tonpa et Alluka. Leurs kits sont rangés avec ceux des mobs ({@link MobKits#named}) ; techniques tirées de ce
 * qu'ils font dans l'œuvre (Gotoh lance des pièces, Canary frappe du bâton, Hanzo est un ninja, Satotz court sans
 * fin, Tonpa empoisonne, Alluka exauce un vœu par Nanika).
 */
public final class SupportingCast {
    private SupportingCast() {}

    // ---------- Gotoh : pièces lancées (Émission) ----------
    /** Pièce lancée : une pièce d'aura file à 16 blocs et perce (petit recul). */
    public static final Technique GOTOH_COIN = Techniques.register(new Techniques.Strike("cast_gotoh_coin", NenType.EMITTER,
            15, 60, 16, 0.97, 5f, 0.4f, List.of(), new Look(Style.ORB, 0xFFE8C860, 0.6f), SoundEvents.CHAIN_HIT));
    /** Tir en rafale : réflexes de majordome Zoldyck ; Vitesse et Hâte 6 s. */
    public static final Technique GOTOH_FOCUS = Techniques.register(new Techniques.SelfBuff("cast_gotoh_focus", NenType.EMITTER,
            20, 300, List.of(new Effect(MobEffects.MOVEMENT_SPEED, 120, 0), new Effect(MobEffects.DIG_SPEED, 120, 1)),
            new Look(Style.FLARE, 0xFFE8C860, 1.0f), SoundEvents.EXPERIENCE_ORB_PICKUP));

    // ---------- Canary : bâton (Renforcement) ----------
    /** Coup de bâton : 4 blocs, gros recul (elle garde l'allée des Zoldyck). */
    public static final Technique CANARY_STAFF = Techniques.register(new Techniques.Strike("cast_canary_staff", NenType.ENHANCER,
            12, 60, 4, 0.85, 6f, 1.4f, List.of(), new Look(Style.IMPACT, NenFx.ENHANCER, 1.1f), SoundEvents.PLAYER_ATTACK_KNOCKBACK));

    // ---------- Hanzo : ninja (Transmutation) ----------
    /** Lame cachée : coupe rapide à 3,5 blocs qui fait saigner (Faiblesse). */
    public static final Technique HANZO_BLADE = Techniques.register(new Techniques.Strike("cast_hanzo_blade", NenType.TRANSMUTER,
            12, 50, 3.5, 0.85, 6f, 0.2f, List.of(new Effect(MobEffects.WEAKNESS, 60, 0)),
            new Look(Style.SLASH, 0xFFB0C8D8, 1.0f), SoundEvents.PLAYER_ATTACK_SWEEP));
    /** Art ninja : il disparaît (Invisibilité, Vitesse) 4 s. */
    public static final Technique HANZO_VANISH = Techniques.register(new Techniques.SelfBuff("cast_hanzo_vanish", NenType.TRANSMUTER,
            25, 360, List.of(new Effect(MobEffects.INVISIBILITY, 80, 0), new Effect(MobEffects.MOVEMENT_SPEED, 80, 2)),
            new Look(Style.FLARE, 0xFFB0C8D8, 1.0f), SoundEvents.ILLUSIONER_MIRROR_MOVE));

    // ---------- Satotz : course sans fin (Renforcement) ----------
    /** Marathon : pas qui ne s'arrête jamais ; Vitesse II et Saut 10 s. */
    public static final Technique SATOTZ_MARATHON = Techniques.register(new Techniques.SelfBuff("cast_satotz_marathon", NenType.ENHANCER,
            20, 400, List.of(new Effect(MobEffects.MOVEMENT_SPEED, 200, 1), new Effect(MobEffects.JUMP, 200, 1)),
            new Look(Style.FLARE, NenFx.ENHANCER, 1.0f), SoundEvents.HORSE_GALLOP));

    // ---------- Tonpa : jus empoisonné (Manipulation) ----------
    /** Jus « amical » : il offre une boisson piégée ; Nausée et Poison 6 s. */
    public static final Technique TONPA_JUICE = Techniques.register(new Techniques.Strike("cast_tonpa_juice", NenType.MANIPULATOR,
            15, 200, 4, 0.85, 1f, 0f, List.of(new Effect(MobEffects.CONFUSION, 120, 0), new Effect(MobEffects.POISON, 120, 0)),
            new Look(Style.ORB, 0xFF9AC85A, 0.8f), SoundEvents.GENERIC_DRINK));

    // ---------- Alluka / Nanika : vœu (Spécialisation) ----------
    /** Vœu de Nanika : guérison complète du vœu exaucé ; Régénération III et Absorption 6 s. */
    public static final Technique ALLUKA_WISH = Techniques.register(new Techniques.SelfBuff("cast_alluka_wish", NenType.SPECIALIST,
            40, 600, List.of(new Effect(MobEffects.REGENERATION, 120, 2), new Effect(MobEffects.ABSORPTION, 120, 1)),
            new Look(Style.FLARE, 0xFF2A2A2A, 1.6f), SoundEvents.AMETHYST_BLOCK_CHIME));
    /** Rayon de Nanika : un trait d'énergie noire à 12 blocs, très gros dégâts. */
    public static final Technique NANIKA_BEAM = Techniques.register(new Techniques.Strike("cast_nanika_beam", NenType.SPECIALIST,
            40, 400, 12, 0.95, 12f, 1.2f, List.of(), new Look(Style.BOLT, 0xFF1A1A1A, 1.4f), SoundEvents.WARDEN_SONIC_BOOM));

    static {
        MobKits.named("cast_gotoh", NenType.EMITTER, GOTOH_COIN, GOTOH_FOCUS);
        MobKits.named("cast_canary", NenType.ENHANCER, CANARY_STAFF);
        MobKits.named("cast_hanzo", NenType.TRANSMUTER, HANZO_BLADE, HANZO_VANISH);
        MobKits.named("cast_satotz", NenType.ENHANCER, SATOTZ_MARATHON);
        MobKits.named("cast_tonpa", NenType.MANIPULATOR, TONPA_JUICE);
        MobKits.named("cast_alluka", NenType.SPECIALIST, ALLUKA_WISH, NANIKA_BEAM);
    }

    /** Charge la classe (techniques et kits s'enregistrent). */
    public static void init() {}
}
