package com.evandouard.hxh.quest;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.HashSet;
import java.util.Set;

/**
 * Progression des quêtes d'un joueur : la quête en cours, son étape, la progression de l'étape,
 * les quêtes terminées, et le statut de Hunter (licence obtenue).
 */
public final class QuestData implements INBTSerializable<CompoundTag> {
    String active = "";
    int step = 0;
    /** Mobs tués, ou ticks passés à suivre le PNJ, selon l'étape. */
    int progress = 0;
    final Set<String> completed = new HashSet<>();
    boolean hunter = false;

    // ---------- Tour Céleste ----------
    boolean arenaRegistered = false;
    int arenaFloor = 1;
    int arenaWins = 0;
    int arenaLosses = 0;
    int jenny = 0;
    /** Où renvoyer le joueur quand il quitte la Tour (dimension + position), vide = point d'apparition. */
    String returnDim = "";
    double returnX, returnY, returnZ;

    public String active() {
        return active;
    }

    public int step() {
        return step;
    }

    public int progress() {
        return progress;
    }

    public boolean isCompleted(String questId) {
        return completed.contains(questId);
    }

    public boolean isHunter() {
        return hunter;
    }

    public void setHunter(boolean value) {
        hunter = value;
    }

    public boolean arenaRegistered() {
        return arenaRegistered;
    }

    public void setArenaRegistered(boolean value) {
        arenaRegistered = value;
    }

    public int arenaFloor() {
        return arenaFloor;
    }

    public void setArenaFloor(int floor) {
        arenaFloor = Math.max(1, Math.min(com.evandouard.hxh.arena.ArenaRules.TOP_FLOOR, floor));
    }

    public int arenaWins() {
        return arenaWins;
    }

    public int arenaLosses() {
        return arenaLosses;
    }

    public int jenny() {
        return jenny;
    }

    public void addJenny(int amount) {
        jenny = Math.max(0, jenny + amount);
    }

    /** Enregistre un combat de la Tour : bilan, nouvel étage et Jenny gagnés. */
    public void recordArenaResult(boolean win, int newFloor, int jennyGained) {
        if (win) arenaWins++;
        else arenaLosses++;
        setArenaFloor(newFloor);
        addJenny(jennyGained);
    }

    public void setReturnPoint(String dimension, double x, double y, double z) {
        returnDim = dimension;
        returnX = x;
        returnY = y;
        returnZ = z;
    }

    public String returnDim() {
        return returnDim;
    }

    public net.minecraft.world.phys.Vec3 returnPos() {
        return new net.minecraft.world.phys.Vec3(returnX, returnY, returnZ);
    }

    public void reset() {
        active = "";
        step = 0;
        progress = 0;
        completed.clear();
        hunter = false;
        arenaRegistered = false;
        arenaFloor = 1;
        arenaWins = 0;
        arenaLosses = 0;
        jenny = 0;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putString("active", active);
        tag.putInt("step", step);
        tag.putInt("progress", progress);
        tag.putBoolean("hunter", hunter);
        ListTag done = new ListTag();
        for (String id : completed) done.add(StringTag.valueOf(id));
        tag.put("completed", done);
        CompoundTag arena = new CompoundTag();
        arena.putBoolean("registered", arenaRegistered);
        arena.putInt("floor", arenaFloor);
        arena.putInt("wins", arenaWins);
        arena.putInt("losses", arenaLosses);
        arena.putInt("jenny", jenny);
        arena.putString("return_dim", returnDim);
        arena.putDouble("return_x", returnX);
        arena.putDouble("return_y", returnY);
        arena.putDouble("return_z", returnZ);
        tag.put("arena", arena);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        active = tag.getString("active");
        step = tag.getInt("step");
        progress = tag.getInt("progress");
        hunter = tag.getBoolean("hunter");
        completed.clear();
        for (Tag t : tag.getList("completed", Tag.TAG_STRING)) completed.add(t.getAsString());
        CompoundTag arena = tag.getCompound("arena");
        arenaRegistered = arena.getBoolean("registered");
        arenaFloor = Math.max(1, arena.getInt("floor"));
        arenaWins = arena.getInt("wins");
        arenaLosses = arena.getInt("losses");
        jenny = arena.getInt("jenny");
        returnDim = arena.getString("return_dim");
        returnX = arena.getDouble("return_x");
        returnY = arena.getDouble("return_y");
        returnZ = arena.getDouble("return_z");
    }
}
