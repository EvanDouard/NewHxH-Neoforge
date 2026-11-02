package com.evandouard.hxh.hatsu;

import com.evandouard.hxh.data.NenType;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Les personnages emblématiques proposés au choix du Hatsu. Premier lot : un par type.
 */
public final class HatsuKits {
    private static final Map<String, HatsuKit> ALL = new LinkedHashMap<>();

    private HatsuKits() {}

    private static HatsuKit register(String id, NenType type, Technique... techniques) {
        HatsuKit kit = new HatsuKit(id, type, java.util.Arrays.stream(techniques).map(Technique::id).toList());
        ALL.put(id, kit);
        return kit;
    }

    public static final HatsuKit GON = register("gon", NenType.ENHANCER,
            Techniques.GON_ROCK, Techniques.GON_SCISSORS, Techniques.GON_PAPER);
    public static final HatsuKit KILLUA = register("killua", NenType.TRANSMUTER,
            Techniques.KILLUA_THUNDERBOLT, Techniques.KILLUA_GODSPEED);
    public static final HatsuKit KURAPIKA = register("kurapika", NenType.CONJURER,
            Techniques.KURAPIKA_CHAIN_JAIL, Techniques.KURAPIKA_HOLY_CHAIN);
    public static final HatsuKit NEFERPITOU = register("neferpitou", NenType.SPECIALIST,
            Techniques.PITOU_TERPSICHORA, Techniques.PITOU_DOCTOR_BLYTHE);
    public static final HatsuKit ILLUMI = register("illumi", NenType.MANIPULATOR,
            Techniques.ILLUMI_NEEDLE, Techniques.ILLUMI_TRANSFORM);
    public static final HatsuKit LEORIO = register("leorio", NenType.EMITTER,
            Techniques.LEORIO_REMOTE_PUNCH, Techniques.LEORIO_SHOCKWAVE);

    @Nullable
    public static HatsuKit get(String id) {
        return ALL.get(id);
    }

    public static List<HatsuKit> all() {
        return List.copyOf(ALL.values());
    }
}
