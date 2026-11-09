package com.evandouard.hxh.exam;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/** Lieux de l'Examen déjà posés, et place du Navigateur dans l'Overworld. */
public final class ExamSavedData extends SavedData {
    private static final String NAME = "hxh_hunter_exam";

    final Set<String> built = new HashSet<>();
    long navigator = Long.MIN_VALUE;

    public static ExamSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ExamSavedData::new, ExamSavedData::load, null), NAME);
    }

    private static ExamSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        ExamSavedData d = new ExamSavedData();
        for (Tag t : tag.getList("built", Tag.TAG_STRING)) d.built.add(t.getAsString());
        if (tag.contains("navigator")) d.navigator = tag.getLong("navigator");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (String s : built) list.add(StringTag.valueOf(s));
        tag.put("built", list);
        if (navigator != Long.MIN_VALUE) tag.putLong("navigator", navigator);
        return tag;
    }

    public long navigator() {
        return navigator;
    }
}
