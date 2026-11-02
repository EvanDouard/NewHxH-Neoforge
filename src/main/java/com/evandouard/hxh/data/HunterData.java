package com.evandouard.hxh.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
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

    private final Map<Stat, Integer> levels = new EnumMap<>(Stat.class);
    private final Map<Stat, Integer> xp = new EnumMap<>(Stat.class);

    private boolean nenUnlocked = false;
    private boolean nenRevealed = false;
    @Nullable
    private NenType nenType = null;

    private final Set<String> dirty = new HashSet<>();

    public HunterData() {
        for (Stat s : Stat.values()) {
            levels.put(s, 0);
            xp.put(s, 0);
        }
    }

    // ---------- Stats ----------

    public int level(Stat stat) {
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
            out.put(NEN, nen);
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
    }

    /** Remet tout à zéro (stats et Nen). */
    public void reset() {
        read(new HunterData().writeAll());
        nenType = null;
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
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return writeAll();
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        read(nbt);
        dirty.clear();
    }
}
