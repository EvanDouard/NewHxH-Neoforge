package com.evandouard.hxh.airship;

import com.evandouard.hxh.network.HxHNetwork;
import com.evandouard.hxh.network.OpenAirshipPayload;
import com.evandouard.hxh.quest.QuestManager;
import com.evandouard.hxh.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Le dirigeable : le joueur choisit une destination sur la carte, paie en Jenny et arrive sur une plateforme (posée sur
 * le sol, avec une borne pour repartir). Le client n'envoie que « je veux aller à X » ; tout est vérifié ici.
 */
public final class Airship {
    /** Distance maximale à une borne pour réserver un billet. */
    public static final int TERMINAL_RANGE = 6;
    private static final int PAD_HALF = 2;
    /** La dimension du monde de Hunter x Hunter. */
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> WORLD =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, com.evandouard.hxh.HxHMod.id("world"));

    private Airship() {}

    /** Ouvre la carte chez le joueur (destinations, prix, Jenny). */
    public static void open(ServerPlayer player) {
        int jenny = QuestManager.data(player).jenny();
        List<OpenAirshipPayload.Dest> dests = new ArrayList<>();
        for (AirshipDestination d : AirshipRegistry.all()) dests.add(new OpenAirshipPayload.Dest(d.id().toString(), d.x(), d.z(), d.price()));
        HxHNetwork.sendTo(player, new OpenAirshipPayload(jenny, dests));
    }

    /** Une borne de dirigeable est-elle à portée ? */
    public static boolean nearTerminal(ServerPlayer player) {
        BlockPos c = player.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-TERMINAL_RANGE, -3, -TERMINAL_RANGE), c.offset(TERMINAL_RANGE, 3, TERMINAL_RANGE)))
            if (player.level().getBlockState(p).is(ModBlocks.AIRSHIP_TERMINAL.get())) return true;
        return false;
    }

    /**
     * Voyage vers une destination.
     * @param needTerminal exige une borne à portée (vrai pour les joueurs ; faux pour les commandes d'opérateur)
     * @return null si le voyage a lieu, sinon la clé du message de refus
     */
    @Nullable
    public static String travel(ServerPlayer player, String id, boolean needTerminal) {
        AirshipDestination dest = AirshipRegistry.get(id);
        if (dest == null) return "message.hxh.airship.unknown";
        if (needTerminal && !nearTerminal(player)) return "message.hxh.airship.no_terminal";
        var data = QuestManager.data(player);
        if (data.jenny() < dest.price()) return "message.hxh.airship.not_enough";
        data.addJenny(-dest.price());
        arrive(player, dest);
        return null;
    }

    /** Dépose le joueur sur la plateforme de la destination (dans le monde HxH, ou le monde courant s'il n'existe pas), sans rien débiter. */
    public static void arrive(ServerPlayer player, AirshipDestination dest) {
        ServerLevel level = player.server.getLevel(WORLD);
        if (level == null) level = player.serverLevel();
        BlockPos landing;
        if (com.evandouard.hxh.world.WorldPlaces.ensureBuilt(level, dest.x(), dest.z())) {
            // Le lieu a sa propre zone d'arrivée (et sa borne) : on n'ajoute aucune plateforme
            landing = safeLanding(level, dest.x(), dest.z());
        } else {
            landing = buildPad(level, dest.x(), dest.z());
        }
        player.teleportTo(level, landing.getX() + 0.5, landing.getY(), landing.getZ() + 0.5, player.getYRot(), 0f);
        player.resetFallDistance();
        level.playSound(null, landing, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.2f);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.hxh.airship.arrived",
                net.minecraft.network.chat.Component.translatable(dest.nameKey())), true);
    }

    private static boolean free(ServerLevel level, BlockPos p) {
        var st = level.getBlockState(p);
        return st.getCollisionShape(level, p).isEmpty() && st.getFluidState().isEmpty();
    }

    /** Point d'arrivée dans un lieu déjà bâti : on lit la vraie colonne (deux blocs libres au-dessus d'un sol plein) plutôt que de se fier à la carte des hauteurs. */
    static BlockPos safeLanding(ServerLevel level, int x, int z) {
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int y = Math.max(top, level.getSeaLevel() + 1);
        int start = y;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int guard = 0;
        while (guard++ < 40 && !(free(level, m.set(x, y, z)) && free(level, m.set(x, y + 1, z)))) y++;
        guard = 0;
        while (guard++ < 40 && y > level.getMinBuildHeight() + 1 && free(level, m.set(x, y - 1, z))) y--;
        com.evandouard.hxh.HxHMod.LOGGER.info("[HxH] Arrivée dans un lieu : carte des hauteurs {}, départ {}, point retenu {} ({} au pied, {} au sol)",
                top, start, y, level.getBlockState(new BlockPos(x, y, z)).getBlock(), level.getBlockState(new BlockPos(x, y - 1, z)).getBlock());
        return new BlockPos(x, y, z);
    }

    /** Étiquette posée sur un joueur déjà accueilli dans le monde HxH (une seule arrivée automatique). */
    public static final String WELCOMED_TAG = "hxh_welcomed";

    /** Première connexion : si la règle est active, le joueur naît sur l'île de la Baleine (et y réapparaîtra). Jamais en CI. */
    public static boolean welcome(ServerPlayer player) {
        if (player.getPersistentData().getBoolean(WELCOMED_TAG)) return false;
        player.getPersistentData().putBoolean(WELCOMED_TAG, true);
        if (System.getProperty("hxh.ci.scenario") != null) return false;
        if (!player.serverLevel().getGameRules().getBoolean(com.evandouard.hxh.HxHGameRules.SPAWN_IN_HXH_WORLD)) return false;
        AirshipDestination home = AirshipRegistry.get("hxh:whale_island");
        if (home == null || player.server.getLevel(WORLD) == null) return false;
        arrive(player, home);
        player.setRespawnPosition(WORLD, player.blockPosition(), player.getYRot(), true, false);
        return true;
    }

    /** Plateforme d'arrivée posée sur le sol en (x, z) : sol de briques, air dégagé, borne pour repartir. @return le point d'arrivée */
    static BlockPos buildPad(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4); // charge (et génère si besoin) le tronçon d'arrivée
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        y = Math.max(y, level.getSeaLevel() + 1);
        for (int dx = -PAD_HALF; dx <= PAD_HALF; dx++) {
            for (int dz = -PAD_HALF; dz <= PAD_HALF; dz++) {
                level.setBlock(new BlockPos(x + dx, y - 1, z + dz), Blocks.STONE_BRICKS.defaultBlockState(), Block.UPDATE_CLIENTS);
                for (int dy = 0; dy < 4; dy++) level.setBlock(new BlockPos(x + dx, y + dy, z + dz), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        level.setBlock(new BlockPos(x + PAD_HALF, y, z), ModBlocks.AIRSHIP_TERMINAL.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST), Block.UPDATE_CLIENTS);
        return new BlockPos(x, y, z);
    }
}
