package com.evandouard.hxh.travel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Aéroports du monde : chaque guichetier posé avec le repère de PNJ en est un. Le monde retient leur nom, leur place
 * et qui les a déjà visités (une destination ne s'achète qu'une fois visitée).
 */
public final class Airports extends SavedData {
    private static final String NAME = "hxh_airports";

    /** Un aéroport : numéro (pour les liens du chat), nom, dimension, place du guichet, visiteurs. */
    public static final class Airport {
        public final int id;
        public String name;
        public final String dim;
        public final long pos;
        final Set<UUID> visitors = new HashSet<>();

        Airport(int id, String name, String dim, long pos) {
            this.id = id;
            this.name = name;
            this.dim = dim;
            this.pos = pos;
        }

        public BlockPos blockPos() {
            return BlockPos.of(pos);
        }

        public boolean visitedBy(UUID player) {
            return visitors.contains(player);
        }
    }

    private final Map<Integer, Airport> airports = new LinkedHashMap<>();
    private int nextId = 1;

    public static Airports get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(Airports::new, Airports::load, null), NAME);
    }

    /** Aéroport du guichet à cette place (à 3 blocs près), créé s'il est nouveau. */
    public Airport at(String dim, BlockPos pos, String defaultName) {
        for (Airport a : airports.values()) {
            if (a.dim.equals(dim) && a.blockPos().distSqr(pos) <= 9) return a;
        }
        Airport a = new Airport(nextId++, defaultName, dim, pos.asLong());
        airports.put(a.id, a);
        setDirty();
        return a;
    }

    @Nullable
    public Airport byId(int id) {
        return airports.get(id);
    }

    @Nullable
    public Airport nearest(String dim, BlockPos pos, double maxDistance) {
        Airport best = null;
        double bestD = maxDistance * maxDistance;
        for (Airport a : airports.values()) {
            if (!a.dim.equals(dim)) continue;
            double d = a.blockPos().distSqr(pos);
            if (d <= bestD) {
                best = a;
                bestD = d;
            }
        }
        return best;
    }

    /** Destinations connues du joueur (visitées), sauf celle où il se trouve. */
    public List<Airport> destinations(UUID player, Airport from) {
        List<Airport> out = new ArrayList<>();
        for (Airport a : airports.values()) if (a != from && a.visitedBy(player)) out.add(a);
        return out;
    }

    /** Première visite : la destination s'ouvre. @return true si elle vient de s'ouvrir */
    public boolean visit(Airport a, UUID player) {
        boolean added = a.visitors.add(player);
        if (added) setDirty();
        return added;
    }

    public void rename(Airport a, String name) {
        a.name = name;
        setDirty();
    }

    public void remove(Airport a) {
        airports.remove(a.id);
        setDirty();
    }

    public List<Airport> all() {
        return List.copyOf(airports.values());
    }

    private static Airports load(CompoundTag tag, HolderLookup.Provider provider) {
        Airports d = new Airports();
        d.nextId = Math.max(1, tag.getInt("next"));
        for (Tag t : tag.getList("airports", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            Airport a = new Airport(c.getInt("id"), c.getString("name"), c.getString("dim"), c.getLong("pos"));
            for (Tag v : c.getList("visitors", Tag.TAG_STRING)) a.visitors.add(UUID.fromString(v.getAsString()));
            d.airports.put(a.id, a);
            d.nextId = Math.max(d.nextId, a.id + 1);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("next", nextId);
        ListTag list = new ListTag();
        for (Airport a : airports.values()) {
            CompoundTag c = new CompoundTag();
            c.putInt("id", a.id);
            c.putString("name", a.name);
            c.putString("dim", a.dim);
            c.putLong("pos", a.pos);
            ListTag v = new ListTag();
            for (UUID u : a.visitors) v.add(StringTag.valueOf(u.toString()));
            c.put("visitors", v);
            list.add(c);
        }
        tag.put("airports", list);
        return tag;
    }
}
