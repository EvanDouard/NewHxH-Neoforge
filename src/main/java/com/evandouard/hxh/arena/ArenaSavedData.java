package com.evandouard.hxh.arena;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/**
 * Ce qui a déjà été construit dans la Tour : le hall et les étages dont l'arbitre a été posé.
 * Les PNJ ne sont créés qu'une seule fois : chercher l'entité ne suffit pas, car les entités d'un chunk
 * se chargent après les blocs (d'où les hôtesses en double avant).
 */
public final class ArenaSavedData extends SavedData {
    private static final String NAME = "hxh_heavens_arena";

    boolean lobbyBuilt = false;
    final Set<Integer> floorsWithReferee = new HashSet<>();

    public static ArenaSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ArenaSavedData::new, ArenaSavedData::load, null), NAME);
    }

    private static ArenaSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        ArenaSavedData d = new ArenaSavedData();
        d.lobbyBuilt = tag.getBoolean("lobby_built");
        for (int f : tag.getIntArray("floors")) d.floorsWithReferee.add(f);
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putBoolean("lobby_built", lobbyBuilt);
        tag.put("floors", new IntArrayTag(floorsWithReferee.stream().mapToInt(Integer::intValue).toArray()));
        return tag;
    }
}
