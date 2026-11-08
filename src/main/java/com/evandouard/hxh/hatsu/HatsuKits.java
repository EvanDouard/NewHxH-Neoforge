package com.evandouard.hxh.hatsu;

import com.evandouard.hxh.data.NenType;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.evandouard.hxh.data.NenType.*;

/**
 * Les personnages emblématiques proposés au choix du Hatsu.
 * <ul>
 *     <li>Premier lot jouable : un par type.</li>
 *     <li>Personnages « idées » : techniques et passifs décrits (IDÉE : …), pas encore jouables.</li>
 * </ul>
 * Types de Nen d'après le mémo de Togashi (2022) ; « type supposé » quand l'œuvre ne le confirme pas.
 */
public final class HatsuKits {
    private static final Map<String, HatsuKit> ALL = new LinkedHashMap<>();

    private HatsuKits() {}

    private static HatsuKit playable(String id, NenType type, List<Passive> passives, Technique... techniques) {
        HatsuKit kit = new HatsuKit(id, type, true, Arrays.stream(techniques).map(Technique::id).toList(), passives, true);
        ALL.put(id, kit);
        return kit;
    }

    /** Personnage à l'état d'idée : chaque technique et le passif sont seulement décrits. */
    private static void idea(String id, NenType type, boolean confirmed, String passive, String... techniques) {
        for (String t : techniques) Techniques.idea(t, type);
        ALL.put(id, new HatsuKit(id, type, confirmed, List.of(techniques), List.of(Passive.idea(passive)), false));
    }

    // ---------- Premier lot jouable ----------
    public static final HatsuKit GON = playable("gon", ENHANCER, List.of(Passives.GON_WILD_INSTINCT),
            Techniques.GON_ROCK, Techniques.GON_SCISSORS, Techniques.GON_PAPER, Techniques.GON_ROCK_EPIC);
    public static final HatsuKit KILLUA = playable("killua", TRANSMUTER, List.of(Passives.ZOLDYCK_BODY),
            Techniques.KILLUA_THUNDERBOLT, Techniques.KILLUA_GODSPEED);
    public static final HatsuKit KURAPIKA = playable("kurapika", CONJURER, List.of(Passives.DOWSING_CHAIN),
            Techniques.KURAPIKA_CHAIN_JAIL, Techniques.KURAPIKA_HOLY_CHAIN);
    public static final HatsuKit NEFERPITOU = playable("neferpitou", SPECIALIST, List.of(Passives.FELINE_AGILITY),
            Techniques.PITOU_TERPSICHORA, Techniques.PITOU_DOCTOR_BLYTHE);
    public static final HatsuKit ILLUMI = playable("illumi", MANIPULATOR, List.of(Passives.POISON_TRAINING),
            Techniques.ILLUMI_NEEDLE, Techniques.ILLUMI_TRANSFORM);
    public static final HatsuKit LEORIO = playable("leorio", EMITTER, List.of(Passives.DOCTOR),
            Techniques.LEORIO_REMOTE_PUNCH, Techniques.LEORIO_SHOCKWAVE);

    static {
        // ---------- Idées : personnages du Trello ----------
        idea("netero", ENHANCER, true, "netero_prayer", "netero_kannon", "netero_zero_hand");
        idea("pariston", SPECIALIST, false, "pariston_charisma", "pariston_love_debt");
        idea("ging", EMITTER, false, "ging_archaeologist", "ging_copy", "ging_emission_punch");
        idea("cheadle", MANIPULATOR, false, "cheadle_secretary", "cheadle_rules");
        idea("botobai", ENHANCER, false, "botobai_veteran", "botobai_pressure");
        idea("biscuit", TRANSMUTER, true, "biscuit_gem_expert", "biscuit_esthetician", "biscuit_true_form");
        idea("menchi", TRANSMUTER, false, "menchi_gastronome", "menchi_gourmet_meal");
        idea("bushidora", ENHANCER, false, "bushidora_swordsman", "bushidora_aura_blade");
        idea("tsezguerra", ENHANCER, false, "tsezguerra_leader", "tsezguerra_charged_strike");
        idea("silva", EMITTER, true, "silva_zoldyck", "silva_explosive_orbs");
        idea("zeno", EMITTER, true, "zeno_zoldyck", "zeno_dragon_head", "zeno_dragon_dive");
        idea("hisoka", TRANSMUTER, true, "hisoka_thrill", "hisoka_bungee_gum", "hisoka_texture_surprise");
        idea("zushi", MANIPULATOR, false, "zushi_diligent", "zushi_ren_punch");
        idea("chrollo", SPECIALIST, true, "chrollo_leader", "chrollo_skill_hunter");
        idea("feitan", TRANSMUTER, true, "feitan_pain", "feitan_rising_sun");
        idea("nobunaga", ENHANCER, true, "nobunaga_en", "nobunaga_iai");
        idea("phinks", ENHANCER, true, "phinks_brawler", "phinks_ripper_cyclotron");
        idea("neon", SPECIALIST, true, "neon_fortune", "neon_lovely_ghostwriter");
        idea("razor", EMITTER, true, "razor_game_master", "razor_aura_ball", "razor_fourteen_devils");
        idea("kite", CONJURER, true, "kite_hunter", "kite_crazy_slots");
        idea("knov", EMITTER, true, "knov_strategist", "knov_hide_and_seek");
        idea("knuckle", CONJURER, true, "knuckle_tough", "knuckle_hakoware");
        idea("morel", MANIPULATOR, true, "morel_smoke", "morel_deep_purple");
        idea("meruem", EMITTER, true, "meruem_king", "meruem_rage_blast", "meruem_photon");
        idea("shaiapouf", MANIPULATOR, true, "pouf_wings", "pouf_spiritual_message", "pouf_beelzebub");
        idea("youpi", TRANSMUTER, true, "youpi_vitality", "youpi_metamorphosis", "youpi_rage_blast");

        // ---------- Idées : ajouts proposés ----------
        idea("uvogin", ENHANCER, true, "uvogin_iron_body", "uvogin_big_bang_impact");
        idea("franklin", EMITTER, true, "franklin_sturdy", "franklin_double_machine_gun");
        idea("shalnark", MANIPULATOR, true, "shalnark_tech", "shalnark_black_voice", "shalnark_autopilot");
        idea("machi", TRANSMUTER, true, "machi_suture", "machi_nen_threads");
        idea("shizuku", CONJURER, true, "shizuku_cleaner", "shizuku_blinky");
        idea("pakunoda", SPECIALIST, true, "pakunoda_mind_reader", "pakunoda_memory_bomb");
        idea("kalluto", MANIPULATOR, true, "kalluto_zoldyck", "kalluto_paper_dance");
        idea("genthru", CONJURER, true, "genthru_bomber", "genthru_little_flower", "genthru_countdown");
        idea("senritsu", EMITTER, true, "senritsu_heart_sound", "senritsu_melody");
        idea("palm", ENHANCER, true, "palm_wink_blue", "palm_black_widow");
        idea("wing", ENHANCER, true, "wing_teacher", "wing_shingen_ryu");
    }

    @Nullable
    public static HatsuKit get(String id) {
        return ALL.get(id);
    }

    public static List<HatsuKit> all() {
        return List.copyOf(ALL.values());
    }

    public static List<HatsuKit> playable() {
        return ALL.values().stream().filter(HatsuKit::implemented).toList();
    }
}
