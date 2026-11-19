package com.evandouard.hxh.stats;

import com.evandouard.hxh.HxHConfig;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.Mastery;
import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.nen.EnShape;
import com.evandouard.hxh.nen.NenCost;
import com.evandouard.hxh.nen.NenDefense;
import com.evandouard.hxh.nen.NenHealing;
import com.evandouard.hxh.nen.NenLearning;
import com.evandouard.hxh.nen.NenSenses;
import com.evandouard.hxh.nen.NenTree;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/**
 * Détail par niveau de chaque stat et technique, pour les écrans du carnet (clic sur une technique : ce qu'elle fait
 * au niveau 5, 10, 20…). Les valeurs viennent des mêmes formules que le jeu (donc de la configuration du serveur) ;
 * le client ne fait que les afficher.
 *
 * <p>Format des lignes envoyées :
 * <pre>
 * D|id|état|niveau|xp|xpSuivant|plafond      en-tête : niveau, XP actuelle, XP du niveau suivant (-1 = max), plafond
 * M|id|niveau|effet;effet;…                   palier : les effets à ce niveau
 * </pre>
 * Un effet est {@code [!|+]clé=valeur} : {@code !} = efficacité (coût en aura en baisse), {@code +} = déblocage ;
 * la clé est celle d'un texte {@code detail.hxh.eff.<clé>} avec la valeur en argument ({@code @clé} = texte à traduire).
 */
public final class ProfileDetails {
    private ProfileDetails() {}

    /** Les sujets à détailler, dans l'ordre. */
    public static final List<String> STATS = List.of("strength", "speed", "endurance", "vitality", "willpower");
    public static final List<String> AURA = List.of("aura", "heal", "regen");

    private static final int[] STAT_LEVELS = {0, 10, 25, 50, 75, 100};
    private static final int[] NEN_LEVELS = {0, 5, 10, 20, 35, 50, 75, 100};

    public static List<String> encode(ServerPlayer player) {
        HunterData d = HunterStats.get(player);
        List<String> out = new ArrayList<>();
        for (String id : STATS) {
            Stat stat = Stat.byId(id);
            int base = d.baseLevel(stat);
            int next = base >= Stat.MAX_LEVEL ? -1 : Stat.xpToNext(base);
            out.add("D|" + id + "|1|" + d.level(stat) + "|" + d.xp(stat) + "|" + next + "|" + Stat.MAX_LEVEL);
            milestones(out, id, STAT_LEVELS, extraStatLevels(id));
        }
        boolean learning = NenLearning.applies(player);
        for (NenLearning.Ability ab : NenLearning.Ability.values()) {
            String id = ab.id();
            boolean known = !learning || d.learned(id);
            if (ab == NenLearning.Ability.HATSU) {
                int lvl = d.nenLevel();
                int next = lvl >= Mastery.MAX_LEVEL ? -1 : Mastery.nenXpToNext(lvl);
                out.add("D|hatsu|" + (known ? 1 : 0) + "|" + lvl + "|" + d.nenXp() + "|" + next + "|" + Mastery.MAX_LEVEL);
            } else {
                Mastery m = Mastery.byId(id);
                int lvl = d.masteryLevel(m);
                int next = lvl >= Mastery.MAX_LEVEL ? -1 : Mastery.xpToNext(lvl);
                out.add("D|" + id + "|" + (known ? 1 : 0) + "|" + lvl + "|" + d.masteryXp(m) + "|" + next + "|" + d.masteryCap());
            }
            milestones(out, id, NEN_LEVELS, extraNenLevels(ab));
        }
        // Nen : niveau global (plafond des maîtrises)
        int nl = d.nenLevel();
        out.add("D|nen|1|" + nl + "|" + d.nenXp() + "|" + (nl >= Mastery.MAX_LEVEL ? -1 : Mastery.nenXpToNext(nl)) + "|" + Mastery.MAX_LEVEL);
        milestones(out, "nen", NEN_LEVELS, new int[0]);
        // Aura, soin par le Nen, régénération d'aura
        out.add("D|aura|1|" + d.level(Stat.WILLPOWER) + "|" + d.xp(Stat.WILLPOWER) + "|"
                + (d.baseLevel(Stat.WILLPOWER) >= Stat.MAX_LEVEL ? -1 : Stat.xpToNext(d.baseLevel(Stat.WILLPOWER))) + "|" + Stat.MAX_LEVEL);
        milestones(out, "aura", STAT_LEVELS, new int[0]);
        int hl = d.healLevel();
        out.add("D|heal|" + (d.healUnlocked() ? 1 : 0) + "|" + hl + "|" + d.healXp() + "|"
                + (hl >= HunterData.HEAL_MAX_LEVEL ? -1 : HunterData.healXpToNext(hl)) + "|" + HunterData.HEAL_MAX_LEVEL);
        milestones(out, "heal", NEN_LEVELS, new int[0]);
        int zl = d.masteryLevel(Mastery.ZETSU);
        out.add("D|regen|1|" + zl + "|" + d.masteryXp(Mastery.ZETSU) + "|" + (zl >= Mastery.MAX_LEVEL ? -1 : Mastery.xpToNext(zl)) + "|" + d.masteryCap());
        milestones(out, "regen", NEN_LEVELS, new int[0]);
        return out;
    }

    private static void milestones(List<String> out, String id, int[] base, int[] extra) {
        TreeSet<Integer> levels = new TreeSet<>();
        for (int l : base) levels.add(l);
        for (int l : extra) if (l >= 0 && l <= 100) levels.add(l);
        for (int level : levels) out.add("M|" + id + "|" + level + "|" + String.join(";", effects(id, level)));
    }

    // ---------- Paliers de déblocage ----------

    private static int[] extraStatLevels(String id) {
        List<Integer> l = new ArrayList<>();
        if (id.equals("strength")) l.add(HxHConfig.COMBAT_STRONG_FROM.get());
        if (id.equals("speed")) {
            for (int i = 0; i <= 100; i++) if (com.evandouard.hxh.stats.HunterStats.speedBonus(i) >= HxHConfig.STEP_UP_FROM_SPEED_BONUS.get()) {
                l.add(i);
                break;
            }
        }
        return l.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int[] extraNenLevels(NenLearning.Ability ab) {
        List<Integer> l = new ArrayList<>();
        Mastery m = Mastery.byId(ab.id());
        if (m != null) {
            for (NenTree.Node n : NenTree.nodes()) for (NenTree.Req r : n.reqs()) if (r.mastery() == m) l.add(r.level());
        }
        switch (ab) {
            case GYO -> {
                l.add(NenSenses.GYO_SEE_INVISIBLE);
                l.add(NenSenses.GYO_SEE_TYPE);
                l.add(NenSenses.GYO_SEE_NEN_INVISIBLE);
                l.add(NenSenses.GYO_SEE_ZETSU);
            }
            case EN -> l.add(EnShape.UNLOCK_LEVEL);
            case IN -> l.add(HxHConfig.ZETSU_UNLOCK_IN_LEVEL.get());
            default -> { }
        }
        return l.stream().mapToInt(Integer::intValue).toArray();
    }

    // ---------- Effets par niveau ----------

    private static String n(double v) {
        String s = Math.abs(v) >= 100 ? String.format(Locale.ROOT, "%.0f", v) : String.format(Locale.ROOT, "%.2f", v);
        if (s.contains(".")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }

    private static String pct(double ratio) {
        return n(ratio * 100);
    }

    private static String line(String flag, String key, String value) {
        return flag + key + "=" + value;
    }

    /** Conditions de graphe qui se remplissent à ce niveau de maîtrise : « la technique X devient possible ». */
    private static void unlocks(List<String> lines, Mastery m, int level) {
        for (NenTree.Node node : NenTree.nodes()) {
            for (NenTree.Req r : node.reqs()) {
                if (r.mastery() == m && r.level() == level) lines.add(line("+", "cond", "@" + node.ability().translationKey()));
            }
        }
    }

    static List<String> effects(String id, int level) {
        List<String> e = new ArrayList<>();
        switch (id) {
            case "strength" -> {
                e.add(line("", "str_damage", n(level * HxHConfig.STRENGTH_DAMAGE_PER_LEVEL.get())));
                if (level >= HxHConfig.COMBAT_STRONG_FROM.get()) {
                    e.add(line("", "str_kick_push", n(com.evandouard.hxh.combat.Combat.KICK_FAR_BASE + level * com.evandouard.hxh.combat.Combat.KICK_FAR_PER_STRENGTH)));
                    e.add(line("", "str_kick_reach", n(Math.min(24.0, 8.0 + level * 0.15))));
                }
                if (level == HxHConfig.COMBAT_STRONG_FROM.get()) e.add(line("+", "str_strong", ""));
            }
            case "speed" -> {
                double b = com.evandouard.hxh.stats.HunterStats.speedBonus(level);
                e.add(line("", "speed_bonus", pct(b)));
                if (b >= HxHConfig.STEP_UP_FROM_SPEED_BONUS.get()) e.add(line("", "speed_step", ""));
            }
            case "endurance" -> e.add(line("", "end_resist", n(level * HxHConfig.ENDURANCE_FLAT_PER_LEVEL.get())));
            case "vitality" -> e.add(line("", "vit_health", n(level * HxHConfig.VITALITY_HEALTH_PER_LEVEL.get())));
            case "willpower" -> {
                e.add(line("", "wp_aura", String.valueOf(HxHConfig.AURA_BASE.get() + HxHConfig.AURA_PER_WILLPOWER.get() * level)));
                e.add(line("", "wp_fall", pct(com.evandouard.hxh.combat.FallImpact.damageFactor(level))));
            }
            case "aura" -> {
                e.add(line("", "wp_aura", String.valueOf(HxHConfig.AURA_BASE.get() + HxHConfig.AURA_PER_WILLPOWER.get() * level)));
                e.add(line("", "aura_regen", n(HxHConfig.AURA_REGEN_PER_SECOND.get())));
            }
            case "heal" -> {
                e.add(line("", "heal_rate", n(NenHealing.healPerSecond(level))));
                e.add(line("!", "heal_cost", n(NenHealing.auraPerHealth(level))));
            }
            case "regen" -> {
                double base = HxHConfig.AURA_REGEN_PER_SECOND.get();
                e.add(line("", "regen_base", n(base)));
                e.add(line("", "regen_zetsu", n(base * (HxHConfig.ZETSU_REGEN_MULTIPLIER.get() + level * 0.02))));
                e.add(line("", "regen_meditation", n(base * 2)));
            }
            case "nen" -> {
                e.add(line("", "nen_cap", String.valueOf(Math.max(Mastery.MIN_CAP, level))));
                e.add(line("!", "cost_mult", n(NenCost.factor(level))));
            }
            case "ten" -> {
                e.add(line("", "resist", n(NenDefense.tenResistanceAt(level))));
                if (level < NenCost.BRUTAL_UNTIL) e.add(line("!", "posture_drain", n(NenCost.novicePostureDrain(level))));
                else e.add(line("!", "posture_free", ""));
                unlocks(e, Mastery.TEN, level);
            }
            case "in" -> {
                if (level < NenCost.BRUTAL_UNTIL) e.add(line("!", "posture_drain", n(NenCost.novicePostureDrain(level))));
                else e.add(line("!", "posture_free", ""));
                if (level == HxHConfig.ZETSU_UNLOCK_IN_LEVEL.get()) e.add(line("+", "in_zetsu", ""));
                unlocks(e, Mastery.IN, level);
            }
            case "zetsu" -> {
                e.add(line("", "regen_zetsu", n(HxHConfig.AURA_REGEN_PER_SECOND.get() * (HxHConfig.ZETSU_REGEN_MULTIPLIER.get() + level * 0.02))));
                unlocks(e, Mastery.ZETSU, level);
            }
            case "ren" -> {
                e.add(line("", "ren_surge", pct(0.25 + level * 0.005)));
                e.add(line("", "ren_damage", pct(HxHConfig.REN_DAMAGE_BONUS.get() + level * 0.004)));
                e.add(line("!", "drain", n(HxHConfig.REN_DRAIN_PER_SECOND.get() * NenCost.factor(level))));
                e.add(line("!", "cost_mult", n(NenCost.factor(level))));
                unlocks(e, Mastery.REN, level);
            }
            case "ken" -> {
                e.add(line("", "resist", n(NenDefense.kenResistanceAt(level))));
                e.add(line("", "ken_damage", pct((HxHConfig.REN_DAMAGE_BONUS.get() + level * 0.004) * 0.5)));
                e.add(line("!", "drain", n(Math.max(1.5, HxHConfig.KEN_DRAIN_PER_SECOND.get() - level * 0.02) * NenCost.factor(level))));
                e.add(line("!", "cost_mult", n(NenCost.factor(level))));
                unlocks(e, Mastery.KEN, level);
            }
            case "gyo" -> {
                e.add(line("!", "drain", n(Math.max(0.2, HxHConfig.GYO_DRAIN_PER_SECOND.get() - level * 0.008) * NenCost.factor(level))));
                e.add(line("!", "cost_mult", n(NenCost.factor(level))));
                if (level >= NenSenses.GYO_SEE_INVISIBLE) e.add(line("", "gyo_invisible", ""));
                if (level >= NenSenses.GYO_SEE_TYPE) e.add(line("", "gyo_type", ""));
                if (level >= NenSenses.GYO_SEE_NEN_INVISIBLE) e.add(line("", "gyo_nen_invisible", ""));
                if (level >= NenSenses.GYO_SEE_ZETSU) e.add(line("", "gyo_zetsu", ""));
                unlocks(e, Mastery.GYO, level);
            }
            case "en" -> {
                e.add(line("", "en_radius", n(2 + level * 0.25)));
                e.add(line("!", "drain", n(Math.max(0.5, HxHConfig.EN_DRAIN_PER_SECOND.get() - level * 0.01) * NenCost.factor(level))));
                e.add(line("!", "cost_mult", n(NenCost.factor(level))));
                if (level >= EnShape.UNLOCK_LEVEL) e.add(line("", "en_shape", ""));
                unlocks(e, Mastery.EN, level);
            }
            case "ko" -> {
                e.add(line("", "ko_damage", n(HxHConfig.KO_DAMAGE_MULTIPLIER.get() + level * 0.02)));
                e.add(line("", "ko_duration", n((NenDefense.KO_MIN_TICKS + (NenDefense.KO_MAX_TICKS - NenDefense.KO_MIN_TICKS) * level / (double) Mastery.MAX_LEVEL) / 20.0)));
                e.add(line("!", "ko_cost", String.valueOf(Math.round(Math.max(10, HxHConfig.KO_AURA_COST.get() - level * 0.15) * NenCost.factor(level)))));
                e.add(line("!", "cost_mult", n(NenCost.factor(level))));
                unlocks(e, Mastery.KO, level);
            }
            case "ryu" -> {
                e.add(line("", "ryu_swing", pct(0.5 * (0.6 + level * 0.008))));
                unlocks(e, Mastery.RYU, level);
            }
            case "shu" -> {
                e.add(line("", "shu_damage", pct(0.1 + level * 0.004)));
                e.add(line("", "shu_save", pct(0.2 + level * 0.006)));
                e.add(line("!", "shu_cost", n(0.5 * NenCost.factor(level))));
                e.add(line("!", "cost_mult", n(NenCost.factor(level))));
                unlocks(e, Mastery.SHU, level);
            }
            case "hatsu" -> {
                e.add(line("", "hatsu_cap", String.valueOf(Math.max(Mastery.MIN_CAP, level))));
                e.add(line("!", "cost_mult", n(NenCost.factor(level))));
            }
            default -> { }
        }
        return e;
    }
}
