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

    public void reset() {
        active = "";
        step = 0;
        progress = 0;
        completed.clear();
        hunter = false;
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
    }
}
