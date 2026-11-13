package com.evandouard.hxh.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Données Hunter attachées à n'importe quelle LivingEntity (joueur ou mob).
 * <p>
 * Le format NBT est découpé en clés indépendantes ({@code stat.strength}, {@code nen}…).
 * La même écriture sert à la sauvegarde (toutes les clés) et à la synchro réseau
 * (seulement les clés modifiées depuis le dernier envoi).
 */
public final class HunterData implements INBTSerializable<CompoundTag> {
    private static final String NEN = "nen";
    private static final String AURA = "aura";
    private static final String HEAL = "heal";
    private static final String MEDITATION = "meditation";
    private static final String HATSU = "hatsu";
    private static final String PROGRESS = "progress";
    /** Ressources propres à un personnage (Prière de Netero, Douleur de Feitan, forme de Biscuit…). */
    private static final String KIT_RES = "kit_res";

    /** Couleur d'aura par défaut : bleu clair. */
    public static final int DEFAULT_AURA_COLOR = 0x7FC8FF;
    public static final int HEAL_MAX_LEVEL = 100;

    private final Map<Stat, Integer> levels = new EnumMap<>(Stat.class);
    private final Map<Stat, Integer> xp = new EnumMap<>(Stat.class);

    private boolean nenUnlocked = false;
    private boolean nenRevealed = false;
    @Nullable
    private NenType nenType = null;
    @Nullable
    private NenTalent talent;
    /** Capacités du Nen apprises auprès d'un maître (identifiants de NenLearning.Ability). */
    private final java.util.Set<String> learned = new java.util.LinkedHashSet<>();
    /** Part du bonus de Vitesse utilisée (réglable dans le carnet) : 1 = tout. */
    private float speedCap = 1f;
    /** Aura sauvage (hostile, incontrôlée) : des flammes même au repos. Par défaut, aura calme (vapeur). */
    private boolean wildAura;

    private float aura = 0;
    private int auraMax = 100;
    private NenMode mode = NenMode.NONE;
    /** Dernière valeur entière d'aura envoyée : on ne resynchronise que si elle change. */
    private int lastWrittenAura = -1;
    private int auraColor = DEFAULT_AURA_COLOR;

    // Soin par le Nen (maîtrise à part, XP élevée)
    private boolean healUnlocked = false;
    private int healLevel = 0;
    private int healXp = 0;
    private boolean healing = false;

    // Méditation (une des voies pour éveiller le Nen)
    private boolean meditating = false;
    private int meditationTicks = 0;

    // Niveau de Nen et maîtrises des techniques de base
    private int nenLevel = 0;
    private int nenXp = 0;
    private final Map<Mastery, Integer> masteryLevels = new EnumMap<>(Mastery.class);
    private final Map<Mastery, Integer> masteryXp = new EnumMap<>(Mastery.class);
    /** Ko : la prochaine attaque est chargée (et le corps est découvert) jusqu'à ce tick de jeu. */
    private long koUntil = 0;
    /** Invisibilité par le Nen (Texture Surprise…) : seul un Gyo de haut niveau la perce. Fin en temps de jeu. */
    private long nenHiddenUntil = 0;
    /** Gyo et En : interrupteurs indépendants de la posture (coupés par le Zetsu et l'In). */
    private boolean gyo = false;
    private boolean en = false;
    /** Ryu : part de l'aura mise dans l'attaque, en % (le reste protège le corps). */
    private int ryuAttack = 50;

    // Hatsu : personnage choisi (définitif), technique sélectionnée, recharges (tick de jeu où elle redevient prête)
    private String kit = "";
    private int selected = 0;
    /** Coup secret de la famille (technique sans Nen ajoutée au menu), vide sans famille. */
    private String familyMove = "";
    /** Serment en cours (voir Vows), technique visée par un serment de technique unique, fin du serment ultime. */
    private String vow = "";
    private String vowTechnique = "";
    private long vowUntil = 0;
    /** Hatsu scellé jusqu'à ce tick (serment rompu, fin du serment ultime). */
    private long sealedUntil = 0;
    /** Tout le Nen scellé jusqu'à ce tick (après « Tout sacrifier ») : ni aura, ni posture, ni technique. */
    private long nenSealedUntil = 0;
    /** Serment conditionnel mis en pause par le joueur (combat loyal, monstres seulement, mains nues). */
    private boolean vowOff = false;
    private final Map<String, Long> readyAt = new HashMap<>();

    private final Set<String> dirty = new HashSet<>();
    private final Map<String, Float> kitValues = new HashMap<>();

    public HunterData() {
        for (Stat s : Stat.values()) {
            levels.put(s, 0);
            xp.put(s, 0);
        }
        for (Mastery m : Mastery.values()) {
            masteryLevels.put(m, 0);
            masteryXp.put(m, 0);
        }
    }

    // ---------- Stats ----------

    /** Niveau effectif : le niveau gagné, multiplié par un serment actif (peut dépasser 100, voulu). */
    public int level(Stat stat) {
        int base = levels.get(stat);
        float m = com.evandouard.hxh.hatsu.Vows.statMultiplier(this);
        return m == 1f ? base : Math.round(base * m);
    }

    /** Niveau gagné par l'entraînement, sans serment. */
    public int baseLevel(Stat stat) {
        return levels.get(stat);
    }

    public int xp(Stat stat) {
        return xp.get(stat);
    }

    public void setLevel(Stat stat, int level) {
        levels.put(stat, Math.max(0, Math.min(Stat.MAX_LEVEL, level)));
        xp.put(stat, 0);
        dirty.add(statKey(stat));
    }

    /**
     * Ajoute de l'XP à une stat et applique les montées de niveau.
     * @return le nombre de niveaux gagnés
     */
    public int addXp(Stat stat, int amount) {
        if (amount <= 0) return 0;
        int level = levels.get(stat);
        if (level >= Stat.MAX_LEVEL) return 0;
        int current = xp.get(stat) + amount;
        int gained = 0;
        while (level < Stat.MAX_LEVEL && current >= Stat.xpToNext(level)) {
            current -= Stat.xpToNext(level);
            level++;
            gained++;
        }
        if (level >= Stat.MAX_LEVEL) current = 0;
        levels.put(stat, level);
        xp.put(stat, current);
        dirty.add(statKey(stat));
        return gained;
    }

    /** Niveau Hunter global : moyenne des stats. */
    public int hunterLevel() {
        int sum = 0;
        for (int l : levels.values()) sum += l;
        return sum / Stat.values().length;
    }

    // ---------- Nen ----------

    public boolean nenUnlocked() {
        return nenUnlocked;
    }

    public boolean nenRevealed() {
        return nenRevealed;
    }

    @Nullable
    public NenType nenType() {
        return nenType;
    }

    public void setNenUnlocked(boolean unlocked) {
        this.nenUnlocked = unlocked;
        dirty.add(NEN);
    }

    public void setNenRevealed(boolean revealed) {
        this.nenRevealed = revealed;
        dirty.add(NEN);
    }

    public void setNenType(@Nullable NenType type) {
        this.nenType = type;
        dirty.add(NEN);
    }

    /** Talent pour le Nen (null tant qu'il n'a pas été tiré ; les mobs n'en ont pas : ×1). */
    @Nullable
    public boolean learned(String ability) {
        return learned.contains(ability);
    }

    public java.util.Set<String> learnedAbilities() {
        return java.util.Collections.unmodifiableSet(learned);
    }

    /** @return true si la capacité est nouvelle */
    public boolean learn(String ability) {
        boolean added = learned.add(ability);
        if (added) dirty.add(NEN);
        return added;
    }

    public float speedCap() {
        return speedCap;
    }

    public boolean wildAura() {
        return wildAura;
    }

    public void setWildAura(boolean wild) {
        wildAura = wild;
        dirty.add(NEN);
    }

    public void setSpeedCap(float cap) {
        speedCap = Math.max(0f, Math.min(1f, cap));
        dirty.add(NEN);
    }

    public NenTalent talent() {
        return talent;
    }

    public void setTalent(@Nullable NenTalent talent) {
        this.talent = talent;
        dirty.add(NEN);
    }

    public double talentMultiplier() {
        return talent == null ? 1.0 : talent.multiplier();
    }

    // ---------- Aura ----------

    public float aura() {
        return aura;
    }

    public int auraMax() {
        return auraMax;
    }

    public NenMode mode() {
        return mode;
    }

    public void setAura(float value) {
        if (com.evandouard.hxh.HxHGameRules.noAuraCostActive && value < aura) return; // règle de test : rien ne coûte
        aura = Math.max(0, Math.min(auraMax, value));
        if ((int) aura != lastWrittenAura) dirty.add(AURA);
    }

    public void setAuraMax(int max) {
        if (max == auraMax) return;
        auraMax = Math.max(1, max);
        aura = Math.min(aura, auraMax);
        dirty.add(AURA);
    }

    public void setMode(NenMode newMode) {
        if (newMode == mode) return;
        mode = newMode;
        dirty.add(AURA);
    }

    public int auraColor() {
        return auraColor;
    }

    public void setAuraColor(int rgb) {
        auraColor = rgb & 0xFFFFFF;
        dirty.add(AURA);
    }

    // ---------- Soin par le Nen ----------

    public boolean healUnlocked() {
        return healUnlocked;
    }

    public int healLevel() {
        return healLevel;
    }

    public int healXp() {
        return healXp;
    }

    public boolean healing() {
        return healing;
    }

    public void setHealUnlocked(boolean unlocked) {
        healUnlocked = unlocked;
        dirty.add(HEAL);
    }

    public void setHealLevel(int level) {
        healLevel = Math.max(0, Math.min(HEAL_MAX_LEVEL, level));
        healXp = 0;
        dirty.add(HEAL);
    }

    public void setHealing(boolean value) {
        if (value == healing) return;
        healing = value;
        dirty.add(HEAL);
    }

    /** XP de soin : volontairement bien plus exigeante que les stats. */
    public static int healXpToNext(int level) {
        return 3 * Stat.xpToNext(level) + 20;
    }

    /** @return le nombre de niveaux de soin gagnés */
    public int addHealXp(int amount) {
        if (amount <= 0 || healLevel >= HEAL_MAX_LEVEL) return 0;
        int current = healXp + amount;
        int gained = 0;
        while (healLevel < HEAL_MAX_LEVEL && current >= healXpToNext(healLevel)) {
            current -= healXpToNext(healLevel);
            healLevel++;
            gained++;
        }
        healXp = healLevel >= HEAL_MAX_LEVEL ? 0 : current;
        dirty.add(HEAL);
        return gained;
    }

    // ---------- Méditation ----------

    /** Ressource propre au personnage ({@code prayer}, {@code pain}…), 0 si absente. */
    public float kitValue(String key) {
        return kitValues.getOrDefault(key, 0f);
    }

    public void setKitValue(String key, float value) {
        float old = kitValue(key);
        if (old == value) return;
        if (value == 0f) kitValues.remove(key);
        else kitValues.put(key, value);
        // n'envoie qu'aux changements visibles (au dixième près)
        if (Math.round(old * 10) != Math.round(value * 10) || value == 0f) dirty.add(KIT_RES);
    }

    public void addKitValue(String key, float delta, float max) {
        setKitValue(key, Math.max(0f, Math.min(max, kitValue(key) + delta)));
    }

    public void clearKitValues() {
        if (kitValues.isEmpty()) return;
        kitValues.clear();
        dirty.add(KIT_RES);
    }

    public boolean meditating() {
        return meditating;
    }

    public int meditationTicks() {
        return meditationTicks;
    }

    public void setMeditating(boolean value) {
        if (value == meditating) return;
        meditating = value;
        dirty.add(MEDITATION);
    }

    public void setMeditationTicks(int ticks) {
        int old = meditationTicks;
        meditationTicks = Math.max(0, ticks);
        // On ne synchronise qu'une fois par seconde de progression
        if (old / 20 != meditationTicks / 20) dirty.add(MEDITATION);
    }

    // ---------- Niveau de Nen et maîtrises ----------

    public int nenLevel() {
        return nenLevel;
    }

    public int nenXp() {
        return nenXp;
    }

    public void setNenLevel(int level) {
        nenLevel = Math.max(0, Math.min(Mastery.MAX_LEVEL, level));
        nenXp = 0;
        dirty.add(PROGRESS);
    }

    /** @return le nombre de niveaux de Nen gagnés */
    public int addNenXp(int amount) {
        if (amount <= 0 || nenLevel >= Mastery.MAX_LEVEL) return 0;
        int current = nenXp + amount;
        int gained = 0;
        while (nenLevel < Mastery.MAX_LEVEL && current >= Mastery.nenXpToNext(nenLevel)) {
            current -= Mastery.nenXpToNext(nenLevel);
            nenLevel++;
            gained++;
        }
        nenXp = nenLevel >= Mastery.MAX_LEVEL ? 0 : current;
        dirty.add(PROGRESS);
        return gained;
    }

    public int masteryLevel(Mastery m) {
        return masteryLevels.get(m);
    }

    public int masteryXp(Mastery m) {
        return masteryXp.get(m);
    }

    /** Plafond de verre : une maîtrise ne dépasse pas le niveau de Nen (minimum {@link Mastery#MIN_CAP}). */
    public int masteryCap() {
        return Math.max(Mastery.MIN_CAP, nenLevel);
    }

    public void setMasteryLevel(Mastery m, int level) {
        masteryLevels.put(m, Math.max(0, Math.min(Mastery.MAX_LEVEL, level)));
        masteryXp.put(m, 0);
        dirty.add(PROGRESS);
    }

    /**
     * Ajoute de l'XP à une maîtrise, ralentie en approchant du plafond et nulle une fois dessus.
     * @return le nombre de niveaux gagnés
     */
    public int addMasteryXp(Mastery m, int amount) {
        int level = masteryLevels.get(m);
        int cap = masteryCap();
        if (amount <= 0 || level >= cap) return 0;
        // Plus on est proche du plafond, moins l'entraînement rapporte (de 100 % à 20 %)
        double factor = Math.max(0.2, Math.min(1.0, (cap - level) / 10.0));
        int current = masteryXp.get(m) + Math.max(1, (int) Math.round(amount * factor));
        int gained = 0;
        while (level < cap && current >= Mastery.xpToNext(level)) {
            current -= Mastery.xpToNext(level);
            level++;
            gained++;
        }
        if (level >= cap) current = 0;
        masteryLevels.put(m, level);
        masteryXp.put(m, current);
        dirty.add(PROGRESS);
        return gained;
    }

    public boolean gyo() {
        return gyo;
    }

    public boolean en() {
        return en;
    }

    public void setGyo(boolean value) {
        if (value == gyo) return;
        gyo = value;
        dirty.add(AURA);
    }

    public void setEn(boolean value) {
        if (value == en) return;
        en = value;
        dirty.add(AURA);
    }

    public int ryuAttack() {
        return ryuAttack;
    }

    public void setRyuAttack(int percent) {
        ryuAttack = Math.max(0, Math.min(100, percent));
        dirty.add(PROGRESS);
    }

    /** Plafond d'aura gagné en Ren (temporaire, non sauvegardé). */
    private float renSurge = 0f;

    public float renSurge() {
        return renSurge;
    }

    public void setRenSurge(float value) {
        renSurge = Math.max(0f, value);
    }

    public long koUntil() {
        return koUntil;
    }

    public void setKoUntil(long gameTime) {
        koUntil = gameTime;
        dirty.add(AURA);
    }

    public long nenHiddenUntil() {
        return nenHiddenUntil;
    }

    public void setNenHiddenUntil(long gameTime) {
        nenHiddenUntil = gameTime;
        dirty.add(AURA);
    }

    // ---------- Hatsu ----------

    public String kit() {
        return kit;
    }

    public String familyMove() {
        return familyMove;
    }

    public void setFamilyMove(String id) {
        String v = id == null ? "" : id;
        if (v.equals(familyMove)) return;
        familyMove = v;
        dirty.add(HATSU);
    }

    public String vow() {
        return vow;
    }

    public String vowTechnique() {
        return vowTechnique;
    }

    public long vowUntil() {
        return vowUntil;
    }

    public long sealedUntil() {
        return sealedUntil;
    }

    public void setVow(String id, String technique, long until) {
        vow = id == null ? "" : id;
        vowTechnique = technique == null ? "" : technique;
        vowUntil = until;
        dirty.add(HATSU);
    }

    public void setSealedUntil(long gameTime) {
        sealedUntil = gameTime;
        dirty.add(HATSU);
    }

    public long nenSealedUntil() {
        return nenSealedUntil;
    }

    public void setNenSealedUntil(long gameTime) {
        nenSealedUntil = gameTime;
        dirty.add(HATSU);
    }

    public boolean vowOff() {
        return vowOff;
    }

    public void setVowOff(boolean off) {
        vowOff = off;
        dirty.add(HATSU);
        for (Stat s : Stat.values()) dirty.add(statKey(s));
    }

    public boolean hasKit() {
        return !kit.isEmpty();
    }

    public void setKit(String kitId) {
        if (!java.util.Objects.equals(kit, kitId == null ? "" : kitId)) {
            clearKitValues();
            // l'aura prend la couleur du personnage (comme dans l'anime)
            int color = com.evandouard.hxh.hatsu.HatsuKits.auraColor(kitId);
            if (color != -1) setAuraColor(color);
        }
        kit = kitId == null ? "" : kitId;
        selected = 0;
        readyAt.clear();
        dirty.add(HATSU);
    }

    public int selected() {
        return selected;
    }

    public void setSelected(int index) {
        selected = Math.max(0, index);
        dirty.add(HATSU);
    }

    /** Tick de jeu à partir duquel la technique est de nouveau prête (0 = prête). */
    public long readyAt(String techniqueId) {
        if (com.evandouard.hxh.HxHGameRules.noCooldownsActive) return 0L; // règle de test : aucune recharge
        return readyAt.getOrDefault(techniqueId, 0L);
    }

    public void setReadyAt(String techniqueId, long gameTime) {
        readyAt.put(techniqueId, gameTime);
        dirty.add(HATSU);
    }

    // ---------- Sérialisation ----------

    private static String statKey(Stat stat) {
        return "stat." + stat.id();
    }

    private void writeKey(String key, CompoundTag out) {
        if (key.equals(NEN)) {
            CompoundTag nen = new CompoundTag();
            nen.putBoolean("unlocked", nenUnlocked);
            nen.putBoolean("revealed", nenRevealed);
            if (nenType != null) nen.putString("type", nenType.id());
            if (talent != null) nen.putString("talent", talent.id());
            nen.putFloat("speed_cap", speedCap);
            nen.putBoolean("wild_aura", wildAura);
            net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
            for (String a : learned) list.add(net.minecraft.nbt.StringTag.valueOf(a));
            nen.put("learned", list);
            out.put(NEN, nen);
            return;
        }
        if (key.equals(AURA)) {
            CompoundTag a = new CompoundTag();
            a.putFloat("value", aura);
            a.putInt("max", auraMax);
            a.putString("mode", mode.id());
            a.putInt("color", auraColor);
            a.putLong("koUntil", koUntil);
            a.putLong("nenHidden", nenHiddenUntil);
            a.putBoolean("gyo", gyo);
            a.putBoolean("en", en);
            out.put(AURA, a);
            lastWrittenAura = (int) aura;
            return;
        }
        if (key.equals(HEAL)) {
            CompoundTag h = new CompoundTag();
            h.putBoolean("unlocked", healUnlocked);
            h.putInt("level", healLevel);
            h.putInt("xp", healXp);
            h.putBoolean("active", healing);
            out.put(HEAL, h);
            return;
        }
        if (key.equals(PROGRESS)) {
            CompoundTag p = new CompoundTag();
            p.putInt("nenLevel", nenLevel);
            p.putInt("nenXp", nenXp);
            CompoundTag ms = new CompoundTag();
            for (Mastery m : Mastery.values()) {
                CompoundTag t = new CompoundTag();
                t.putInt("level", masteryLevels.get(m));
                t.putInt("xp", masteryXp.get(m));
                ms.put(m.id(), t);
            }
            p.put("masteries", ms);
            p.putInt("ryuAttack", ryuAttack);
            out.put(PROGRESS, p);
            return;
        }
        if (key.equals(HATSU)) {
            CompoundTag h = new CompoundTag();
            h.putString("kit", kit);
            h.putInt("selected", selected);
            h.putString("familyMove", familyMove);
            h.putString("vow", vow);
            h.putString("vowTech", vowTechnique);
            h.putLong("vowUntil", vowUntil);
            h.putLong("sealedUntil", sealedUntil);
            h.putLong("nenSealedUntil", nenSealedUntil);
            h.putBoolean("vowOff", vowOff);
            CompoundTag cd = new CompoundTag();
            readyAt.forEach(cd::putLong);
            h.put("readyAt", cd);
            out.put(HATSU, h);
            return;
        }
        if (key.equals(KIT_RES)) {
            CompoundTag k = new CompoundTag();
            kitValues.forEach(k::putFloat);
            out.put(KIT_RES, k);
            return;
        }
        if (key.equals(MEDITATION)) {
            CompoundTag m = new CompoundTag();
            m.putBoolean("active", meditating);
            m.putInt("ticks", meditationTicks);
            out.put(MEDITATION, m);
            return;
        }
        for (Stat s : Stat.values()) {
            if (key.equals(statKey(s))) {
                CompoundTag t = new CompoundTag();
                t.putInt("level", levels.get(s));
                t.putInt("xp", xp.get(s));
                out.put(key, t);
                return;
            }
        }
    }

    /** Écrit toutes les clés. */
    public CompoundTag writeAll() {
        CompoundTag out = new CompoundTag();
        for (Stat s : Stat.values()) writeKey(statKey(s), out);
        writeKey(NEN, out);
        writeKey(AURA, out);
        writeKey(HEAL, out);
        writeKey(MEDITATION, out);
        writeKey(HATSU, out);
        writeKey(PROGRESS, out);
        writeKey(KIT_RES, out);
        return out;
    }

    /** Écrit uniquement les clés modifiées puis les marque comme envoyées. */
    public CompoundTag writeDirtyAndClear() {
        CompoundTag out = new CompoundTag();
        for (String key : dirty) writeKey(key, out);
        dirty.clear();
        return out;
    }

    public boolean isDirty() {
        return !dirty.isEmpty();
    }

    /** Force le prochain envoi réseau à contenir toutes les clés. */
    public void markAllDirty() {
        for (Stat s : Stat.values()) dirty.add(statKey(s));
        dirty.add(NEN);
        dirty.add(AURA);
        dirty.add(HEAL);
        dirty.add(MEDITATION);
        dirty.add(HATSU);
        dirty.add(PROGRESS);
        dirty.add(KIT_RES);
    }

    /** Remet tout à zéro (stats et Nen). */
    public void reset() {
        read(new HunterData().writeAll());
        markAllDirty();
    }

    /** Applique un tag complet ou partiel : seules les clés présentes sont lues. */
    public void read(CompoundTag in) {
        for (Stat s : Stat.values()) {
            String key = statKey(s);
            if (in.contains(key)) {
                CompoundTag t = in.getCompound(key);
                levels.put(s, Math.max(0, Math.min(Stat.MAX_LEVEL, t.getInt("level"))));
                xp.put(s, Math.max(0, t.getInt("xp")));
            }
        }
        if (in.contains(NEN)) {
            CompoundTag nen = in.getCompound(NEN);
            nenUnlocked = nen.getBoolean("unlocked");
            nenRevealed = nen.getBoolean("revealed");
            nenType = nen.contains("type") ? NenType.byId(nen.getString("type")) : null;
            talent = nen.contains("talent") ? NenTalent.byId(nen.getString("talent")) : null;
            learned.clear();
            for (net.minecraft.nbt.Tag t : nen.getList("learned", net.minecraft.nbt.Tag.TAG_STRING)) learned.add(t.getAsString());
            speedCap = nen.contains("speed_cap") ? Math.max(0f, Math.min(1f, nen.getFloat("speed_cap"))) : 1f;
            wildAura = nen.getBoolean("wild_aura");
        }
        if (in.contains(AURA)) {
            CompoundTag a = in.getCompound(AURA);
            auraMax = Math.max(1, a.getInt("max"));
            aura = Math.max(0, Math.min(auraMax, a.getFloat("value")));
            NenMode m = NenMode.byId(a.getString("mode"));
            mode = m == null ? NenMode.NONE : m;
            auraColor = a.contains("color") ? a.getInt("color") : DEFAULT_AURA_COLOR;
            koUntil = a.getLong("koUntil");
            nenHiddenUntil = a.getLong("nenHidden");
            gyo = a.getBoolean("gyo");
            en = a.getBoolean("en");
        }
        if (in.contains(HEAL)) {
            CompoundTag h = in.getCompound(HEAL);
            healUnlocked = h.getBoolean("unlocked");
            healLevel = Math.max(0, Math.min(HEAL_MAX_LEVEL, h.getInt("level")));
            healXp = Math.max(0, h.getInt("xp"));
            healing = h.getBoolean("active");
        }
        if (in.contains(PROGRESS)) {
            CompoundTag p = in.getCompound(PROGRESS);
            nenLevel = Math.max(0, Math.min(Mastery.MAX_LEVEL, p.getInt("nenLevel")));
            nenXp = Math.max(0, p.getInt("nenXp"));
            ryuAttack = p.contains("ryuAttack") ? Math.max(0, Math.min(100, p.getInt("ryuAttack"))) : 50;
            CompoundTag ms = p.getCompound("masteries");
            for (Mastery m : Mastery.values()) {
                CompoundTag t = ms.getCompound(m.id());
                masteryLevels.put(m, Math.max(0, Math.min(Mastery.MAX_LEVEL, t.getInt("level"))));
                masteryXp.put(m, Math.max(0, t.getInt("xp")));
            }
        }
        if (in.contains(HATSU)) {
            CompoundTag h = in.getCompound(HATSU);
            kit = h.getString("kit");
            selected = Math.max(0, h.getInt("selected"));
            familyMove = h.getString("familyMove");
            vow = h.getString("vow");
            vowTechnique = h.getString("vowTech");
            vowUntil = h.getLong("vowUntil");
            sealedUntil = h.getLong("sealedUntil");
            nenSealedUntil = h.getLong("nenSealedUntil");
            vowOff = h.getBoolean("vowOff");
            readyAt.clear();
            CompoundTag cd = h.getCompound("readyAt");
            for (String id : cd.getAllKeys()) readyAt.put(id, cd.getLong(id));
        }
        if (in.contains(KIT_RES)) {
            kitValues.clear();
            CompoundTag k = in.getCompound(KIT_RES);
            for (String id : k.getAllKeys()) kitValues.put(id, k.getFloat(id));
        }
        if (in.contains(MEDITATION)) {
            CompoundTag m = in.getCompound(MEDITATION);
            meditating = m.getBoolean("active");
            meditationTicks = Math.max(0, m.getInt("ticks"));
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return writeAll();
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        read(nbt);
        healing = false;
        meditating = false;
        dirty.clear();
    }
}
