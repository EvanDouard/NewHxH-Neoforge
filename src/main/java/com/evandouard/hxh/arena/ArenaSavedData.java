package com.evandouard.hxh.arena;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ce que la Tour retient d'un démarrage à l'autre : version construite de chaque lieu, place de chaque PNJ fixe,
 * chambres des joueurs. Les PNJ ne sont jamais « cherchés » pour savoir s'il faut les créer : les entités d'un
 * chunk se chargent après ses blocs (c'est ce qui dupliquait l'hôtesse).
 */
public final class ArenaSavedData extends SavedData {
    private static final String NAME = "hxh_heavens_arena";

    boolean lobbyBuilt = false;
    /** Lieu → signature de ce qui y est construit (reconstruit si elle change). */
    final Map<Integer, String> signatures = new HashMap<>();
    /** « receptionist@lieu » / « referee@lieu » → place du PNJ (BlockPos.asLong). */
    final Map<String, Long> npcHomes = new HashMap<>();
    final Map<UUID, Integer> roomSlots = new HashMap<>();
    final Map<UUID, Long> roomArrivals = new HashMap<>();

    public static ArenaSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ArenaSavedData::new, ArenaSavedData::load, null), NAME);
    }

    private static ArenaSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        ArenaSavedData d = new ArenaSavedData();
        d.lobbyBuilt = tag.getBoolean("lobby_built");
        CompoundTag sig = tag.getCompound("signatures");
        for (String k : sig.getAllKeys()) d.signatures.put(Integer.parseInt(k), sig.getString(k));
        CompoundTag npcs = tag.getCompound("npcs");
        for (String k : npcs.getAllKeys()) d.npcHomes.put(k, npcs.getLong(k));
        CompoundTag rooms = tag.getCompound("rooms");
        for (String k : rooms.getAllKeys()) {
            CompoundTag r = rooms.getCompound(k);
            UUID id = UUID.fromString(k);
            d.roomSlots.put(id, r.getInt("slot"));
            if (r.contains("arrival")) d.roomArrivals.put(id, r.getLong("arrival"));
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putBoolean("lobby_built", lobbyBuilt);
        CompoundTag sig = new CompoundTag();
        signatures.forEach((k, v) -> sig.putString(String.valueOf(k), v));
        tag.put("signatures", sig);
        CompoundTag npcs = new CompoundTag();
        npcHomes.forEach(npcs::putLong);
        tag.put("npcs", npcs);
        CompoundTag rooms = new CompoundTag();
        roomSlots.forEach((id, slot) -> {
            CompoundTag r = new CompoundTag();
            r.putInt("slot", slot);
            Long a = roomArrivals.get(id);
            if (a != null) r.putLong("arrival", a);
            rooms.put(id.toString(), r);
        });
        tag.put("rooms", rooms);
        return tag;
    }
}
