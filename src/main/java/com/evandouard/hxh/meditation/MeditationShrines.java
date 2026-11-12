package com.evandouard.hxh.meditation;

import com.evandouard.hxh.npc.NpcEntity;
import com.evandouard.hxh.quest.QuestData;
import com.evandouard.hxh.quest.QuestManager;
import com.evandouard.hxh.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Sanctuaires de méditation (Volonté), niveaux 1 à 5 : petits temples posés dans le monde au fil de l'exploration,
 * avec une pierre de méditation au centre. Méditer (touche M) à côté de la pierre multiplie l'XP de Volonté.
 * Les archéologues vendent les coordonnées des sanctuaires 1 à 4 déjà repérés ; ceux de niveau 5, très rares, ne
 * se vendent pas : il faut les trouver soi-même.
 */
public final class MeditationShrines {
    public static final String ARCHAEOLOGIST = "archeologue";
    /** Chance qu'un nouveau tronçon de l'Overworld reçoive un sanctuaire, par niveau (1 à 5). */
    static final double[] CHANCE = {1 / 120.0, 1 / 220.0, 1 / 420.0, 1 / 900.0, 1 / 4000.0};
    /** Multiplicateur d'XP de Volonté en méditant près de la pierre, par niveau. */
    public static final double[] WILL_MULT = {3, 5, 8, 12, 20};
    /** Prix des coordonnées chez l'archéologue (niveaux 1 à 4). */
    public static final int[] PRICE = {100, 250, 600, 1500};
    /** Distance max à la pierre pour que la méditation compte. */
    public static final double STONE_RANGE = 3.5;

    private MeditationShrines() {}

    // ---------- Registre des sanctuaires (monde) ----------

    public static final class Shrines extends SavedData {
        private static final String NAME = "hxh_meditation_shrines";
        final List<long[]> list = new ArrayList<>(); // {pos, tier}
        final java.util.Map<UUID, Set<Long>> bought = new java.util.HashMap<>();

        public static Shrines get(ServerLevel level) {
            return level.getServer().overworld().getDataStorage().computeIfAbsent(
                    new SavedData.Factory<>(Shrines::new, Shrines::load, null), NAME);
        }

        public synchronized void add(BlockPos pos, int tier) {
            for (long[] s : list) if (s[0] == pos.asLong()) return;
            list.add(new long[]{pos.asLong(), tier});
            setDirty();
        }

        public synchronized List<long[]> all() {
            return List.copyOf(list);
        }

        private static Shrines load(CompoundTag tag, HolderLookup.Provider provider) {
            Shrines s = new Shrines();
            for (Tag t : tag.getList("shrines", Tag.TAG_COMPOUND)) {
                CompoundTag c = (CompoundTag) t;
                s.list.add(new long[]{c.getLong("pos"), c.getInt("tier")});
            }
            CompoundTag b = tag.getCompound("bought");
            for (String k : b.getAllKeys()) {
                Set<Long> set = new HashSet<>();
                for (long v : b.getLongArray(k)) set.add(v);
                s.bought.put(UUID.fromString(k), set);
            }
            return s;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
            ListTag l = new ListTag();
            for (long[] s : list) {
                CompoundTag c = new CompoundTag();
                c.putLong("pos", s[0]);
                c.putInt("tier", (int) s[1]);
                l.add(c);
            }
            tag.put("shrines", l);
            CompoundTag b = new CompoundTag();
            bought.forEach((k, v) -> b.putLongArray(k.toString(), v.stream().mapToLong(Long::longValue).toArray()));
            tag.put("bought", b);
            return tag;
        }
    }

    // ---------- Pose au fil de l'exploration ----------

    /** Nouveau tronçon de l'Overworld : peut-être un sanctuaire (tirage fixé par la graine du monde). */
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!event.isNewChunk() || !(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        // pas sur le serveur des GameTests : un sanctuaire y écraserait la zone d'un test
        if (level.getServer() instanceof net.minecraft.gametest.framework.GameTestServer) return;
        ChunkPos cp = event.getChunk().getPos();
        Random rnd = new Random(level.getSeed() ^ (cp.toLong() * 0x9E3779B97F4A7C15L) ^ 0x5EED_5A1L);
        double roll = rnd.nextDouble();
        int tier = 0;
        double acc = 0;
        for (int t = CHANCE.length; t >= 1; t--) {
            acc += CHANCE[t - 1];
            if (roll < acc) {
                tier = t;
                break;
            }
        }
        if (tier == 0) return;
        final int chosen = tier;
        // le tronçon vient d'être généré : on bâtit au tick suivant, sur le fil du serveur
        level.getServer().execute(() -> {
            try {
                BlockPos centre = cp.getWorldPosition().offset(8, 0, 8);
                if (level.isLoaded(centre)) build(level, centre, chosen, true);
            } catch (Exception e) {
                com.evandouard.hxh.HxHMod.LOGGER.warn("[HxH] Sanctuaire non posé en {} : {}", cp, e.toString());
            }
        });
    }

    /**
     * Bâtit un sanctuaire de ce niveau autour de la colonne {@code centre} (posé sur le sol). Il reste dans un rayon
     * de 5 blocs, donc dans le tronçon. @return la place de la pierre, ou null si le sol ne convient pas
     */
    public static BlockPos build(ServerLevel level, BlockPos centre, int tier, boolean needsGround) {
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, centre.getX(), centre.getZ());
        BlockPos ground = new BlockPos(centre.getX(), y - 1, centre.getZ());
        BlockState below = level.getBlockState(ground);
        if (needsGround && (!below.getFluidState().isEmpty() || below.is(net.minecraft.tags.BlockTags.LEAVES) || !below.isSolid())) return null;
        Style st = Style.of(tier);
        int r = 2 + tier / 2 + 1; // 3 à 5
        // dalle, fondations et air au-dessus
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                boolean edge = Math.abs(dx) == r || Math.abs(dz) == r;
                BlockPos p = ground.offset(dx, 0, dz);
                level.setBlock(p, (edge ? st.edge : st.floor).defaultBlockState(), Block.UPDATE_CLIENTS);
                for (int d = 1; d <= 3; d++) {
                    BlockPos q = p.below(d);
                    if (level.getBlockState(q).canBeReplaced() || !level.getBlockState(q).getFluidState().isEmpty())
                        level.setBlock(q, st.edge.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                for (int h = 1; h <= 4 + tier; h++) level.setBlock(p.above(h), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        // piliers aux coins, lanternes
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                BlockPos base = ground.offset(sx * r, 1, sz * r);
                int ph = 2 + tier / 2;
                for (int h = 0; h < ph; h++) level.setBlock(base.above(h), st.pillar.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(base.above(ph), Blocks.LANTERN.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        // niveau 3 et plus : petit bassin autour de la pierre ; niveau 5 : toit de cerisier
        if (tier >= 3) {
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                level.setBlock(ground.offset(dx * 2, 0, dz * 2), Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        if (tier == 5) {
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                if ((dx + dz) % 2 == 0) level.setBlock(ground.offset(dx, 5 + tier / 2, dz), Blocks.CHERRY_LEAVES.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true), Block.UPDATE_CLIENTS);
            }
        }
        BlockPos stone = ground.above();
        level.setBlock(stone, ModBlocks.MEDITATION_STONES.get(tier - 1).get().defaultBlockState(), Block.UPDATE_ALL);
        Shrines.get(level).add(stone, tier);
        return stone;
    }

    private record Style(Block floor, Block edge, Block pillar) {
        static Style of(int tier) {
            return switch (tier) {
                case 1 -> new Style(Blocks.MOSSY_COBBLESTONE, Blocks.COBBLESTONE, Blocks.OAK_LOG);
                case 2 -> new Style(Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS, Blocks.SPRUCE_LOG);
                case 3 -> new Style(Blocks.POLISHED_ANDESITE, Blocks.STONE_BRICKS, Blocks.DARK_OAK_LOG);
                case 4 -> new Style(Blocks.SMOOTH_QUARTZ, Blocks.CHISELED_STONE_BRICKS, Blocks.QUARTZ_PILLAR);
                default -> new Style(Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_TILES, Blocks.CHERRY_LOG);
            };
        }
    }

    // ---------- Méditation ----------

    /** Multiplicateur d'XP de Volonté pour qui médite à côté d'une pierre (1 sinon). */
    public static double willMultiplier(LivingEntity entity) {
        BlockPos at = entity.blockPosition();
        int best = 0;
        int reach = (int) Math.ceil(STONE_RANGE);
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-reach, -1, -reach), at.offset(reach, 1, reach))) {
            if (p.distToCenterSqr(entity.position()) > STONE_RANGE * STONE_RANGE) continue;
            if (entity.level().getBlockState(p).getBlock() instanceof MeditationStoneBlock m) best = Math.max(best, m.tier());
        }
        return best == 0 ? 1 : WILL_MULT[best - 1];
    }

    // ---------- Archéologue ----------

    public static void talk(ServerPlayer player, NpcEntity npc) {
        say(player, Component.translatable("dialog.hxh.archeologue.offer"));
        for (int tier = 1; tier <= 4; tier++) {
            int t = tier;
            player.sendSystemMessage(Component.translatable("shrine.hxh.buy_line", tier, PRICE[tier - 1])
                    .withStyle(s -> s.withColor(ChatFormatting.AQUA).withUnderlined(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hunter shrine buy " + t))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("shrine.hxh.buy_hover", t)))));
        }
        say(player, Component.translatable("dialog.hxh.archeologue.legend"));
    }

    /** Achat des coordonnées du plus proche sanctuaire de ce niveau pas encore acheté. Niveau 5 : jamais vendu. */
    public static boolean buy(ServerPlayer player, int tier) {
        if (tier < 1 || tier > 4) {
            player.sendSystemMessage(Component.translatable("shrine.hxh.never_sold").withStyle(ChatFormatting.RED));
            return false;
        }
        List<NpcEntity> near = player.serverLevel().getEntitiesOfClass(NpcEntity.class, player.getBoundingBox().inflate(8),
                n -> n.npcId().equals(ARCHAEOLOGIST));
        if (near.isEmpty()) {
            player.sendSystemMessage(Component.translatable("shrine.hxh.not_here").withStyle(ChatFormatting.RED));
            return false;
        }
        Shrines shrines = Shrines.get(player.serverLevel());
        Set<Long> mine = shrines.bought.computeIfAbsent(player.getUUID(), k -> new HashSet<>());
        long[] best = null;
        double bestD = Double.MAX_VALUE;
        for (long[] s : shrines.all()) {
            if (s[1] != tier || mine.contains(s[0])) continue;
            double d = BlockPos.of(s[0]).distSqr(player.blockPosition());
            if (d < bestD) {
                bestD = d;
                best = s;
            }
        }
        if (best == null) {
            say(player, Component.translatable("shrine.hxh.none_known", tier));
            return false;
        }
        QuestData d = QuestManager.data(player);
        int price = PRICE[tier - 1];
        if (d.jenny() < price) {
            player.sendSystemMessage(Component.translatable("shrine.hxh.too_poor", price, d.jenny()).withStyle(ChatFormatting.RED));
            return false;
        }
        d.addJenny(-price);
        mine.add(best[0]);
        shrines.setDirty();
        BlockPos p = BlockPos.of(best[0]);
        say(player, Component.translatable("shrine.hxh.sold", tier, p.getX(), p.getY(), p.getZ()));
        return true;
    }

    private static void say(ServerPlayer player, Component line) {
        player.sendSystemMessage(Component.translatable("dialog.hxh.format",
                Component.translatable("npc.hxh." + ARCHAEOLOGIST).withStyle(ChatFormatting.YELLOW), line));
    }
}
