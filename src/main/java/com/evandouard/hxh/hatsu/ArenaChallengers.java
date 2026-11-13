package com.evandouard.hxh.hatsu;

import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.fx.NenFx;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.evandouard.hxh.hatsu.Techniques.Effect;
import static com.evandouard.hxh.hatsu.Techniques.Look;
import static com.evandouard.hxh.hatsu.Techniques.Style;

/**
 * Combattants nommés de la Tour Céleste, au-delà du 200e étage (ceux qui « accueillent » les nouveaux manieurs de
 * Nen, comme dans l'anime) : Gido, Sadaso, Riehlvelt et Kastro, puis Hisoka (235–240) avec son vrai kit. Leurs kits ne sont pas proposés au joueur (rangés
 * avec les kits de mobs) ; un combattant de la Tour les reçoit selon l'étage du combat.
 */
public final class ArenaChallengers {
    private ArenaChallengers() {}

    /** Un adversaire nommé et les étages où on l'affronte (bornes comprises). */
    public record Challenger(String id, String kit, int fromFloor, int toFloor, int skinColor) {}

    // ---------- Gido : toupies (Manipulation) ----------
    /** Toupies tourbillon : une nuée de toupies d'aura balaie tout autour (3,5 blocs) et repousse. */
    public static final Technique GIDO_TOPS = Techniques.register(new Techniques.Blast("arena_gido_tops", NenType.MANIPULATOR,
            25, 120, 3.5, 4f, 1.1f, new Look(Style.SHOCKWAVE, NenFx.MANIPULATOR, 1.2f), SoundEvents.ELYTRA_FLYING));
    /** Danse de la tornade : Gido tourne sur sa jambe de bois ; Résistance et Vitesse 6 s. */
    public static final Technique GIDO_TORNADO = Techniques.register(new Techniques.SelfBuff("arena_gido_tornado", NenType.MANIPULATOR,
            30, 400, List.of(new Effect(MobEffects.DAMAGE_RESISTANCE, 120, 1), new Effect(MobEffects.MOVEMENT_SPEED, 120, 1)),
            new Look(Style.FLARE, NenFx.MANIPULATOR, 1.4f), SoundEvents.PLAYER_ATTACK_SWEEP));

    // ---------- Sadaso : main fantôme (Émission) ----------
    /** Main fantôme : son bras gauche invisible agrippe la cible à 9 blocs et l'immobilise 3 s (visible au Gyo). */
    public static final Technique SADASO_GHOST_HAND = Techniques.register(new Techniques.Strike("arena_sadaso_ghost_hand", NenType.EMITTER,
            25, 140, 9, 0.93, 4f, 0f, List.of(new Effect(MobEffects.MOVEMENT_SLOWDOWN, 60, 5)),
            new Look(Style.WARP_FIST, 0xFFDDE6F0, 1.2f), SoundEvents.PHANTOM_BITE));

    // ---------- Riehlvelt : fouet électrique et canon à air (Transmutation) ----------
    /** Fouet électrique : 7 blocs, la cible est parcourue d'éclairs (lenteur). */
    public static final Technique RIEHLVELT_WHIP = Techniques.register(new Techniques.Strike("arena_riehlvelt_whip", NenType.TRANSMUTER,
            20, 100, 7, 0.93, 6f, 0.3f, List.of(new Effect(MobEffects.MOVEMENT_SLOWDOWN, 40, 2)),
            new Look(Style.BOLT, NenFx.TRANSMUTER, 1f), SoundEvents.LIGHTNING_BOLT_IMPACT));
    /** Canon à air comprimé (depuis le fauteuil) : 14 blocs, gros recul. */
    public static final Technique RIEHLVELT_AIR_CANNON = Techniques.register(new Techniques.Strike("arena_riehlvelt_air_cannon", NenType.TRANSMUTER,
            30, 160, 14, 0.96, 5f, 1.8f, List.of(), new Look(Style.ORB, 0xFFE8F4FF, 1.4f), SoundEvents.GENERIC_EXPLODE.value()));

    // ---------- Kastro : morsure du tigre et Double (Renforcement) ----------
    /** Morsure du tigre : mains en gueule de tigre, corps à corps (3,5 blocs), très gros dégâts. */
    public static final Technique KASTRO_TIGER_BITE = Techniques.register(new Techniques.Strike("arena_kastro_tiger_bite", NenType.ENHANCER,
            20, 90, 3.5, 0.85, 9f, 0.6f, List.of(), new Look(Style.SLASH, NenFx.ENHANCER, 1.6f), SoundEvents.RAVAGER_ATTACK));
    /** Double : son double attire les coups pendant que le vrai Kastro disparaît (Invisibilité, Force) 4 s. */
    public static final Technique KASTRO_DOUBLE = Techniques.register(new Techniques.SelfBuff("arena_kastro_double", NenType.ENHANCER,
            40, 500, List.of(new Effect(MobEffects.INVISIBILITY, 80, 0), new Effect(MobEffects.DAMAGE_BOOST, 80, 1)),
            new Look(Style.FLARE, NenFx.ENHANCER, 1.6f), SoundEvents.ILLUSIONER_MIRROR_MOVE));

    public static final List<Challenger> ALL = List.of(
            new Challenger("gido", "arena_gido", 200, 209, 0x8A6A3C),
            new Challenger("sadaso", "arena_sadaso", 210, 219, 0x5A6A7A),
            new Challenger("riehlvelt", "arena_riehlvelt", 220, 224, 0x3C5A8A),
            new Challenger("kastro", "arena_kastro", 225, 234, 0xB03030),
            // Hisoka attend les plus forts : son vrai kit (Bungee Gum, Texture Surprise, cartes)
            new Challenger("hisoka", "hisoka", 235, 240, 0xD04080));

    static {
        MobKits.named("arena_gido", NenType.MANIPULATOR, GIDO_TOPS, GIDO_TORNADO);
        MobKits.named("arena_sadaso", NenType.EMITTER, SADASO_GHOST_HAND);
        MobKits.named("arena_riehlvelt", NenType.TRANSMUTER, RIEHLVELT_WHIP, RIEHLVELT_AIR_CANNON);
        MobKits.named("arena_kastro", NenType.ENHANCER, KASTRO_TIGER_BITE, KASTRO_DOUBLE);
    }

    /** Charge la classe (techniques et kits s'enregistrent). */
    public static void init() {}

    /** Combattant nommé qu'on affronte à cet étage, ou null (adversaire ordinaire). */
    @Nullable
    public static Challenger forFloor(int floor) {
        for (Challenger c : ALL) if (floor >= c.fromFloor() && floor <= c.toFloor()) return c;
        return null;
    }
}
