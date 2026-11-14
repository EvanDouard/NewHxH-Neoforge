package com.evandouard.hxh.hatsu;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.effect.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Type de chaque technique et efficacité selon le type de Nen du lanceur.
 * <ul>
 *   <li>Une technique a un ou deux types de Nen ({@link Technique#category()} + un second, rangé ici), ou est
 *   <b>Physique</b> (coups de base, coups secrets de famille : la Main-griffe des Zoldyck aussi) : 100 % pour tous.</li>
 *   <li>Efficacité = moyenne des affinités de l'hexagone pour ses types (100 %, 80 %, 60 %, 40 %).</li>
 *   <li>Hors de son type, les dégâts baissent (la puissance) et la zone aussi ({@link #sizeFactor}).</li>
 *   <li>Yeux écarlates (Kurta) : le Nen devient Spécialisation et toutes les catégories passent à 100 %.</li>
 * </ul>
 * Valable pour toute LivingEntity : un mob Kurapika aux yeux écarlates en profite comme le joueur.
 */
public final class NenAffinity {
    private NenAffinity() {}

    /** Second type des techniques à deux types (estimation de départ, à ajuster). */
    private static final Map<String, NenType> SECONDARY = new HashMap<>();
    /** Techniques physiques en plus des coups de base et de famille. */
    private static final java.util.Set<String> PHYSICAL = new java.util.HashSet<>();

    static {
        NenType EN = NenType.ENHANCER, TR = NenType.TRANSMUTER, CO = NenType.CONJURER, SP = NenType.SPECIALIST,
                MA = NenType.MANIPULATOR, EM = NenType.EMITTER;
        two("gon_scissors", EN); two("gon_paper", EN);
        two("killua_thunderbolt", EM); two("killua_godspeed", MA);
        two("kurapika_chain_jail", MA); two("kurapika_holy_chain", EN);
        two("illumi_transform", TR);
        two("pitou_terpsichora", MA); two("pitou_doctor_blythe", CO);
        two("hisoka_cards", EM);
        two("netero_kannon", EM); two("netero_zero_hand", EM);
        two("zeno_dragon_head", TR); two("zeno_dragon_dive", TR);
        two("silva_great_orb", TR); two("silva_explosive_orbs", TR);
        two("kite_crazy_slots", SP);
        two("razor_fourteen_devils", CO);
        two("meruem_photon", EN); two("meruem_rage_blast", EN);
        two("youpi_rage_blast", EM);
        two("feitan_rising_sun", CO);
        two("knov_mark_door", CO); two("knov_hide_and_seek", CO);
        two("knuckle_pot_clean", EN);
        two("morel_deep_purple", TR);
        two("machi_nen_threads", MA);
        two("chrollo_skill_hunter", CO);
        two("ging_copy", SP);
        two("biscuit_esthetician", CO);
        two("palm_black_widow", TR);
        two("bushidora_aura_blade", TR);
        two("pouf_beelzebub", TR);
        two("senritsu_melody", MA);
        two("genthru_countdown", EM);
        two("arena_riehlvelt_air_cannon", EM);
    }

    /** Techniques déclarées à deux types (pour les tests : toutes doivent exister). */
    public static java.util.Set<String> secondaryIds() {
        return java.util.Collections.unmodifiableSet(SECONDARY.keySet());
    }

    private static void two(String id, NenType second) {
        SECONDARY.put(id, second);
    }

    /** La technique est-elle Physique (sans Nen, 100 % pour tout le monde) ? */
    public static boolean physical(Technique t) {
        return t.needsNoNen() || PHYSICAL.contains(t.id());
    }

    /** Types de Nen de la technique (vide si Physique), un ou deux. */
    public static List<NenType> types(Technique t) {
        if (physical(t)) return List.of();
        NenType second = SECONDARY.get(t.id());
        return second == null || second == t.category() ? List.of(t.category()) : List.of(t.category(), second);
    }

    /** Les yeux écarlates sont-ils actifs ? */
    public static boolean scarletEyes(LivingEntity entity) {
        return entity != null && entity.hasEffect(ModEffects.SCARLET_EYES);
    }

    /** Type de Nen effectif : Spécialisation sous les yeux écarlates, sinon le sien (null s'il n'en a pas). */
    @Nullable
    public static NenType typeOf(@Nullable LivingEntity entity, HunterData data) {
        if (scarletEyes(entity)) return NenType.SPECIALIST;
        return data.nenType();
    }

    /** Efficacité de la technique pour ce lanceur (0,4 à 1). */
    public static float of(@Nullable LivingEntity entity, HunterData data, Technique t) {
        if (physical(t) || scarletEyes(entity)) return 1f;
        NenType type = data.nenType();
        if (type == null) return 0.4f;
        List<NenType> types = types(t);
        float sum = 0;
        for (NenType k : types) sum += type.affinity(k);
        return sum / types.size();
    }

    public static float of(LivingEntity entity, Technique t) {
        if (!entity.hasData(ModAttachments.HUNTER)) return 1f;
        return of(entity, entity.getData(ModAttachments.HUNTER), t);
    }

    /**
     * Taille des zones (rayon, largeur) hors de son type : 100 % dans son type, 70 % à l'opposé de l'hexagone.
     * Les dégâts, eux, suivent la puissance.
     */
    public static float sizeFactor(LivingEntity caster, Technique t) {
        return 0.5f + 0.5f * of(caster, t);
    }

    // ---------- Pastille de couleur (HUD, liste des techniques) ----------

    /** Couleur de la pastille : vert Renforcement, violet Transformation, rouge Matérialisation, bleu Spécialisation,
     * gris Manipulation, jaune Émission ; marron pour Physique ({@code null}). */
    public static int color(@Nullable NenType type) {
        if (type == null) return 0xFF8B5A2B;
        return switch (type) {
            case ENHANCER -> 0xFF3FB34F;
            case TRANSMUTER -> 0xFF9B4FD8;
            case CONJURER -> 0xFFD8353B;
            case SPECIALIST -> 0xFF3A6FE0;
            case MANIPULATOR -> 0xFF8C8C8C;
            case EMITTER -> 0xFFE8C22E;
        };
    }

    /** Kanji de la catégorie (強 化, 変 化, 具 現 化, 特 質, 操 作, 放 出) ; 体 pour Physique. */
    public static String kanji(@Nullable NenType type) {
        if (type == null) return "体";
        return switch (type) {
            case ENHANCER -> "強";
            case TRANSMUTER -> "変";
            case CONJURER -> "具";
            case SPECIALIST -> "特";
            case MANIPULATOR -> "操";
            case EMITTER -> "放";
        };
    }

    /** Pour l'affichage : les types, ou une liste à un élément null pour Physique. */
    public static List<NenType> badges(Technique t) {
        List<NenType> types = types(t);
        return types.isEmpty() ? java.util.Collections.<NenType>singletonList(null) : types;
    }
}
