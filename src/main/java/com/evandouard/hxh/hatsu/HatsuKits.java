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
    public static final HatsuKit SILVA = playable("silva", EMITTER, List.of(Passives.ZOLDYCK_BODY), Characters.SILVA_GREAT_ORB, Characters.SILVA_EXPLOSIVE_ORBS);
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
    public static final HatsuKit BISCUIT = playable("biscuit", TRANSMUTER, List.of(Specials.BISCUIT_TWO_FORMS), Specials.BISCUIT_SWITCH_FORM, Specials.BISCUIT_ESTHETICIAN);
    public static final HatsuKit CHROLLO = playable("chrollo", SPECIALIST, List.of(Specials.CHROLLO_HEAD), Specials.CHROLLO_SKILL_HUNTER);
    public static final HatsuKit FEITAN = playable("feitan", TRANSMUTER, List.of(Specials.FEITAN_PAIN), Specials.FEITAN_RISING_SUN);
    public static final HatsuKit PHINKS = playable("phinks", ENHANCER, List.of(Specials.PHINKS_BRAWLER), Specials.PHINKS_RIPPER_CYCLOTRON);
    public static final HatsuKit SHALNARK = playable("shalnark", MANIPULATOR, List.of(Specials.SHALNARK_TECH), Specials.SHALNARK_BLACK_VOICE, Specials.SHALNARK_AUTOPILOT);
    public static final HatsuKit KNOV = playable("knov", EMITTER, List.of(Specials.KNOV_STRATEGIST), Specials.KNOV_MARK_DOOR, Specials.KNOV_HIDE_AND_SEEK);
    public static final HatsuKit KNUCKLE = playable("knuckle", CONJURER, List.of(Specials.KNUCKLE_HAKOWARE), Specials.KNUCKLE_POT_CLEAN, Specials.KNUCKLE_CANCEL);
    public static final HatsuKit SHAIAPOUF = playable("shaiapouf", MANIPULATOR, List.of(Specials.POUF_WINGS), Specials.POUF_SPIRITUAL_MESSAGE, Specials.POUF_BEELZEBUB);
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

    /** Couleur d'aura de chaque personnage, d'après l'anime (prise au choix du Hatsu ; /hunter nen color la change). */
    private static final java.util.Map<String, Integer> AURA_COLORS = java.util.Map.ofEntries(
            java.util.Map.entry("gon", 0xF5D547), java.util.Map.entry("killua", 0x7FD3FF),
            java.util.Map.entry("kurapika", 0x9AD7FF), java.util.Map.entry("neferpitou", 0x7A2E8C),
            java.util.Map.entry("illumi", 0x4A3570), java.util.Map.entry("leorio", 0xF2A33A),
            java.util.Map.entry("netero", 0xFFD966), java.util.Map.entry("hisoka", 0xFF4FA3),
            java.util.Map.entry("pariston", 0xFF9FC8), java.util.Map.entry("ging", 0xE4E4E4),
            java.util.Map.entry("cheadle", 0x9BC4FF), java.util.Map.entry("botobai", 0xB8B8B8),
            java.util.Map.entry("menchi", 0xFF8AA8), java.util.Map.entry("bushidora", 0x8FBF6F),
            java.util.Map.entry("tsezguerra", 0xE0A040), java.util.Map.entry("silva", 0x8E6CFF),
            java.util.Map.entry("zeno", 0xFFC34D), java.util.Map.entry("kalluto", 0xD9C9FF),
            java.util.Map.entry("nobunaga", 0xE8E8FF), java.util.Map.entry("uvogin", 0xFF6A3D),
            java.util.Map.entry("franklin", 0xD0703A), java.util.Map.entry("machi", 0xFF7FD0),
            java.util.Map.entry("shizuku", 0xB6A8FF), java.util.Map.entry("pakunoda", 0x9F8FFF),
            java.util.Map.entry("neon", 0xFFB0E0), java.util.Map.entry("razor", 0xFF7A3C),
            java.util.Map.entry("genthru", 0xFF5A2A), java.util.Map.entry("kite", 0xE6E6FA),
            java.util.Map.entry("morel", 0x8A5AB0), java.util.Map.entry("palm", 0x6A3A7A),
            java.util.Map.entry("meruem", 0x6A2C91), java.util.Map.entry("youpi", 0xFF3B3B),
            java.util.Map.entry("zushi", 0x9FD4FF), java.util.Map.entry("senritsu", 0xC9A6FF),
            java.util.Map.entry("biscuit", 0xFFB6E1), java.util.Map.entry("chrollo", 0x5B4B8A),
            java.util.Map.entry("feitan", 0xFF4500), java.util.Map.entry("phinks", 0xD9A441),
            java.util.Map.entry("shalnark", 0x7FE0C0), java.util.Map.entry("knov", 0x7AA0D0),
            java.util.Map.entry("knuckle", 0xF0E060), java.util.Map.entry("shaiapouf", 0xC0A0FF),
            java.util.Map.entry("wing", 0x9FD0FF));

    /** Couleur d'aura du personnage, ou -1 s'il n'en a pas de connue. */
    public static int auraColor(String kit) {
        return kit == null ? -1 : AURA_COLORS.getOrDefault(kit, -1);
    }

    static {
        // ---------- Idées : personnages du Trello ----------

        // ---------- Idées : ajouts proposés ----------
    }

    @Nullable
    public static HatsuKit get(String id) {
        HatsuKit k = ALL.get(id);
        return k != null || id == null ? k : MobKits.get(id); // Hatsu propres aux mobs (jamais proposés au joueur)
    }

    public static List<HatsuKit> all() {
        return List.copyOf(ALL.values());
    }

    public static List<HatsuKit> playable() {
        return ALL.values().stream().filter(HatsuKit::implemented).toList();
    }
}
