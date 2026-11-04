package com.evandouard.hxh.arena;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * Ce qui a déjà été construit dans la Tour : le hall, et la place de chaque PNJ fixe (hôtesse, arbitres).
 * Les PNJ ne sont créés qu'une seule fois : chercher l'entité ne suffit pas, car les entités d'un chunk
 * se chargent après les blocs (d'où les hôtesses en double avant).
 */
public final class ArenaSavedData extends SavedData {
    private static final String NAME = "hxh_heavens_arena";

    boolean lobbyBuilt = false;
    /** Le hall en place vient de la structure hxh:heavens_arena/lobby (sinon : hall du code). */
    boolean lobbyFromStructure = false;
    /** Étage → place (BlockPos.asLong) de son PNJ fixe : hôtesse pour 0, arbitre pour les autres. */
    final Map<Integer, Long> npcHomes = new HashMap<>();

    public static ArenaSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ArenaSavedData::new, ArenaSavedData::load, null), NAME);
    }

    private static ArenaSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        ArenaSavedData d = new ArenaSavedData();
        d.lobbyBuilt = tag.getBoolean("lobby_built");
        d.lobbyFromStructure = tag.getBoolean("lobby_structure");
        CompoundTag npcs = tag.getCompound("npc_homes");
        for (String k : npcs.getAllKeys()) d.npcHomes.put(Integer.parseInt(k), npcs.getLong(k));
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putBoolean("lobby_built", lobbyBuilt);
        tag.putBoolean("lobby_structure", lobbyFromStructure);
        CompoundTag npcs = new CompoundTag();
        npcHomes.forEach((f, pos) -> npcs.putLong(String.valueOf(f), pos));
        tag.put("npc_homes", npcs);
        return tag;
    }
}
