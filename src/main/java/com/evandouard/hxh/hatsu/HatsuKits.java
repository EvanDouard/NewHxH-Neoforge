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
    public static final HatsuKit NETERO = playable("netero", ENHANCER, List.of(Netero.PRAYER),
            Netero.KANNON_FISTS, Netero.KANNON, Netero.ZERO_HAND);
    public static final HatsuKit HISOKA = playable("hisoka", TRANSMUTER, List.of(Passives.HISOKA_THRILL),
            Techniques.HISOKA_BUNGEE_GUM, Techniques.HISOKA_CARDS, Techniques.HISOKA_TEXTURE_SURPRISE);


    // ---------- Personnages de la fiche docs/kits-personnages.md (première version jouable) ----------
    public static final HatsuKit PARISTON = playable("pariston", SPECIALIST, List.of(Characters.PARISTON_CHARISMA), Characters.PARISTON_LOVE_DEBT);
    public static final HatsuKit GING = playable("ging", EMITTER, List.of(Characters.GING_ARCHAEOLOGIST), Characters.GING_EMISSION_PUNCH, Characters.GING_COPY);
    public static final HatsuKit CHEADLE = playable("cheadle", MANIPULATOR, List.of(Characters.CHEADLE_SECRETARY), Characters.CHEADLE_RULES);
    public static final HatsuKit BOTOBAI = playable("botobai", ENHANCER, List.of(Characters.BOTOBAI_VETERAN), Characters.BOTOBAI_PRESSURE);
    public static final HatsuKit MENCHI = playable("menchi", TRANSMUTER, List.of(Characters.MENCHI_GASTRONOME), Characters.MENCHI_GOURMET_MEAL);
    public static final HatsuKit BUSHIDORA = playable("bushidora", ENHANCER, List.of(Characters.BUSHIDORA_SWORDSMAN), Characters.BUSHIDORA_AURA_BLADE);
    public static final HatsuKit TSEZGUERRA = playable("tsezguerra", ENHANCER, List.of(Characters.TSEZGUERRA_LEADER), Characters.TSEZGUERRA_CHARGED_STRIKE);
    public static final HatsuKit SILVA = playable("silva", EMITTER, List.of(Passives.ZOLDYCK_BODY), Characters.SILVA_EXPLOSIVE_ORBS);
    public static final HatsuKit ZENO = playable("zeno", EMITTER, List.of(Passives.ZOLDYCK_BODY), Characters.ZENO_DRAGON_HEAD, Characters.ZENO_DRAGON_DIVE);
    public static final HatsuKit KALLUTO = playable("kalluto", MANIPULATOR, List.of(Passives.ZOLDYCK_BODY), Characters.KALLUTO_PAPER_DANCE);
    public static final HatsuKit NOBUNAGA = playable("nobunaga", ENHANCER, List.of(Characters.NOBUNAGA_EN), Characters.NOBUNAGA_IAI);
    public static final HatsuKit UVOGIN = playable("uvogin", ENHANCER, List.of(Characters.UVOGIN_IRON_BODY), Characters.UVOGIN_BIG_BANG_IMPACT);
    public static final HatsuKit FRANKLIN = playable("franklin", EMITTER, List.of(Characters.FRANKLIN_STURDY), Characters.FRANKLIN_DOUBLE_MACHINE_GUN);
    public static final HatsuKit MACHI = playable("machi", TRANSMUTER, List.of(Characters.MACHI_SUTURE), Characters.MACHI_NEN_THREADS);
    public static final HatsuKit SHIZUKU = playable("shizuku", CONJURER, List.of(Characters.SHIZUKU_CLEANER), Characters.SHIZUKU_BLINKY);
    public static final HatsuKit PAKUNODA = playable("pakunoda", SPECIALIST, List.of(Characters.PAKUNODA_MIND_READER), Characters.PAKUNODA_MEMORY_BOMB);
    public static final HatsuKit NEON = playable("neon", SPECIALIST, List.of(Characters.NEON_FORTUNE), Characters.NEON_LOVELY_GHOSTWRITER);
    public static final HatsuKit RAZOR = playable("razor", EMITTER, List.of(Characters.RAZOR_GAME_MASTER), Characters.RAZOR_AURA_BALL, Characters.RAZOR_FOURTEEN_DEVILS);
    public static final HatsuKit GENTHRU = playable("genthru", CONJURER, List.of(Characters.GENTHRU_BOMBER), Characters.GENTHRU_LITTLE_FLOWER, Characters.GENTHRU_COUNTDOWN);
    public static final HatsuKit KITE = playable("kite", CONJURER, List.of(Characters.KITE_HUNTER), Characters.KITE_CRAZY_SLOTS);
    public static final HatsuKit MOREL = playable("morel", MANIPULATOR, List.of(Characters.MOREL_SMOKE), Characters.MOREL_DEEP_PURPLE);
    public static final HatsuKit PALM = playable("palm", ENHANCER, List.of(Characters.PALM_WINK_BLUE), Characters.PALM_BLACK_WIDOW);
    public static final HatsuKit MERUEM = playable("meruem", EMITTER, List.of(Characters.MERUEM_KING), Characters.MERUEM_RAGE_BLAST, Characters.MERUEM_PHOTON);
    public static final HatsuKit YOUPI = playable("youpi", TRANSMUTER, List.of(Characters.YOUPI_VITALITY), Characters.YOUPI_METAMORPHOSIS, Characters.YOUPI_RAGE_BLAST);
    public static final HatsuKit ZUSHI = playable("zushi", MANIPULATOR, List.of(Characters.ZUSHI_DILIGENT), Characters.ZUSHI_REN_PUNCH);
    public static final HatsuKit SENRITSU = playable("senritsu", EMITTER, List.of(Characters.SENRITSU_HEART_SOUND), Characters.SENRITSU_MELODY);
    public static final HatsuKit WING = playable("wing", ENHANCER, List.of(Characters.WING_TEACHER), Characters.WING_SHINGEN_RYU);

    /**
     * Personnages à l'aura hostile, incontrôlée : des flammes même au repos. Hypothèses tirées de l'histoire, à revoir :
     * <ul>
     *     <li>hisoka : soif de combat, aura malsaine dès sa première apparition ;</li>
     *     <li>illumi : aura glaçante, meurtrière (Killua en tremble) ;</li>
     *     <li>meruem, neferpitou, youpi : Gardes royaux et Roi, aura monstrueuse (Pitou fait fuir Kaito d'un regard,
     *         Youpi est fait de colère) ;</li>
     *     <li>feitan, uvogin, phinks : membres de la Brigade les plus brutaux ;</li>
     *     <li>genthru : le Bombardier, hostile et sans pitié.</li>
     * </ul>
     * Les autres (Gon, Killua, Kurapika, Leorio, Netero, Chrollo, Shaiapouf…) ont une aura calme et maîtrisée.
     */
    private static final java.util.Set<String> WILD_AURA = java.util.Set.of("hisoka", "illumi", "meruem", "neferpitou",
            "youpi", "feitan", "uvogin", "phinks", "genthru");

    public static boolean wildAuraByDefault(String kit) {
        return kit != null && WILD_AURA.contains(kit);
    }

    static {
        // ---------- Idées : personnages du Trello ----------
        idea("biscuit", TRANSMUTER, true, "biscuit_gem_expert", "biscuit_esthetician", "biscuit_true_form");
        idea("chrollo", SPECIALIST, true, "chrollo_leader", "chrollo_skill_hunter");
        idea("feitan", TRANSMUTER, true, "feitan_pain", "feitan_rising_sun");
        idea("phinks", ENHANCER, true, "phinks_brawler", "phinks_ripper_cyclotron");
        idea("knov", EMITTER, true, "knov_strategist", "knov_hide_and_seek");
        idea("knuckle", CONJURER, true, "knuckle_tough", "knuckle_hakoware");
        idea("shaiapouf", MANIPULATOR, true, "pouf_wings", "pouf_spiritual_message", "pouf_beelzebub");

        // ---------- Idées : ajouts proposés ----------
        idea("shalnark", MANIPULATOR, true, "shalnark_tech", "shalnark_black_voice", "shalnark_autopilot");
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
