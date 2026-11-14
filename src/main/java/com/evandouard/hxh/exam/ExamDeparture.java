package com.evandouard.hxh.exam;

import com.evandouard.hxh.npc.NpcEntity;
import com.evandouard.hxh.quest.Quest;
import com.evandouard.hxh.quest.QuestData;
import com.evandouard.hxh.quest.QuestManager;
import com.evandouard.hxh.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

/**
 * Dirigeables de l'Examen dans le monde : le guide (puis l'Association, après chaque épreuve réussie) donne les
 * coordonnées du dirigeable de la prochaine épreuve. Le candidat s'y rend à pied ; l'hôtesse qui l'attend à bord lui
 * propose d'embarquer, et le dirigeable l'emmène au lieu de l'épreuve (dimension de l'Examen). L'épreuve finie,
 * réussie ou ratée, il est ramené à ce dirigeable. Chaque dirigeable est posé pour un joueur, à
 * {@link #MIN_DISTANCE}–{@link #MAX_DISTANCE} blocs du précédent point de passage, et reste dans le monde.
 * Une structure d'Evan {@code hxh:exam/dirigeable} remplacera le dirigeable provisoire.
 */
public final class ExamDeparture {
    private ExamDeparture() {}

    public static final int MIN_DISTANCE = 180, MAX_DISTANCE = 360;
    /** Distance à laquelle le passeur reconnaît le départ du joueur. */
    static final double MATCH = 12;

    /**
     * Pose un départ pour le joueur autour de {@code from} (le guide) et le retient. Renvoie sa position, ou null
     * (tests, pas de monde).
     */
    public static BlockPos create(ServerPlayer player, BlockPos from) {
        QuestData d = QuestManager.data(player);
        if (!ExamTrips.enabled(player)) {
            d.setExamDeparture(from.asLong());
            return from;
        }
        ServerLevel level = player.serverLevel();
        if (ExamWorld.isExamLevel(level)) level = player.server.overworld();
        BlockPos ground = findSpot(level, from, player.getRandom());
        BlockPos npcPos = build(level, ground);
        NpcEntity npc = ModEntities.NPC.get().create(level);
        if (npc != null) {
            npc.moveTo(npcPos.getX() + 0.5, npcPos.getY(), npcPos.getZ() + 0.5, 0, 0);
            npc.setNpcId("passeur");
            npc.setHome(npcPos);
            level.addFreshEntity(npc);
        }
        d.setExamDeparture(npcPos.asLong());
        return npcPos;
    }

    /** Hauteur minimale du pont au-dessus du sol (plus haut s'il y a des arbres ou du relief sous le dirigeable). */
    static final int DECK = 6;
    /** Emprise du dirigeable autour du mât (demi-longueur, demi-largeur), ballon compris. */
    private static final int HALF_X = 12, HALF_Z = 5;

    /**
     * Endroit d'amarrage à {@link #MIN_DISTANCE}–{@link #MAX_DISTANCE} blocs : sol dégagé et plat (pas d'eau, pas
     * d'arbre sur le mât, pas de village ni d'autre structure). Jusqu'à 64 essais ; sinon le meilleur trouvé.
     * Renvoie le sol au pied du mât.
     */
    static BlockPos findSpot(ServerLevel level, BlockPos from, net.minecraft.util.RandomSource rnd) {
        BlockPos best = null;
        int bestScore = Integer.MAX_VALUE;
        for (int attempt = 0; attempt < 64; attempt++) {
            double angle = rnd.nextDouble() * Math.PI * 2;
            int dist = MIN_DISTANCE + rnd.nextInt(MAX_DISTANCE - MIN_DISTANCE + 1);
            int x = from.getX() + (int) Math.round(Math.cos(angle) * dist);
            int z = from.getZ() + (int) Math.round(Math.sin(angle) * dist);
            level.getChunk(x >> 4, z >> 4); // génère le terrain pour connaître la surface
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            int score = spotScore(level, ground);
            if (score < bestScore) {
                bestScore = score;
                best = ground;
            }
            if (score == 0) break;
        }
        return best;
    }

    /** 0 = parfait ; plus c'est grand, moins l'endroit convient (eau, feuillage, pente, structure). */
    private static int spotScore(ServerLevel level, BlockPos ground) {
        int score = 0;
        if (!level.getFluidState(ground).isEmpty() || !level.getFluidState(ground.below()).isEmpty()) score += 100;
        if (level.structureManager().hasAnyStructureAt(ground)) score += 100;
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            int sx = ground.getX() - 7 + dx, sz = ground.getZ() + dz; // autour du mât
            int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz);
            int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, sx, sz);
            if (top > h + 1) score += 10; // feuillage au-dessus du pied du mât
            lo = Math.min(lo, h);
            hi = Math.max(hi, h);
        }
        score += Math.max(0, hi - lo - 1) * 5;
        return score;
    }

    /** Pose un bloc seulement là où il n'y a rien : le dirigeable ne coupe ni arbre, ni maison, ni montagne. */
    private static void put(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        net.minecraft.world.level.block.state.BlockState there = level.getBlockState(pos);
        if (there.isAir() || there.canBeReplaced()) level.setBlock(pos, state, 2);
    }

    private static void put(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.Block block) {
        put(level, pos, block.defaultBlockState());
    }

    /**
     * Dirigeable provisoire amarré à un mât : il flotte au-dessus de tout ce qu'il survole (le pont est au moins à
     * {@link #DECK} blocs du sol, plus haut s'il y a des arbres ou du relief), et ses blocs ne remplacent que l'air :
     * rien n'est détruit. On monte par l'échelle du mât. Renvoie la place de l'hôtesse sur le pont.
     */
    private static BlockPos build(ServerLevel level, BlockPos ground) {
        // hauteur du pont : au-dessus du plus haut bloc (feuillage compris) sous l'emprise du dirigeable
        int top = ground.getY();
        for (int dx = -HALF_X; dx <= HALF_X; dx += 2) for (int dz = -HALF_Z; dz <= HALF_Z; dz += 2) {
            top = Math.max(top, level.getHeight(Heightmap.Types.WORLD_SURFACE, ground.getX() + dx, ground.getZ() + dz));
        }
        int deck = Math.max(DECK, top - ground.getY() + 2);
        BlockPos c = ground;
        // mât d'amarrage et échelle, jusqu'au pont
        for (int y = 0; y <= deck + 1; y++) {
            put(level, c.offset(-7, y, 0), Blocks.SPRUCE_LOG);
            if (y <= deck) put(level, c.offset(-6, y, 0), Blocks.LADDER.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.LadderBlock.FACING, net.minecraft.core.Direction.EAST));
        }
        put(level, c.offset(-7, deck + 2, 0), Blocks.LANTERN);
        // nacelle
        for (int x = -5; x <= 5; x++) for (int z = -2; z <= 2; z++) {
            put(level, c.offset(x, deck - 1, z), Blocks.DARK_OAK_PLANKS);
            boolean edge = Math.abs(x) == 5 || Math.abs(z) == 2;
            if (edge && !(x == -5 && z == 0)) put(level, c.offset(x, deck, z), Blocks.DARK_OAK_FENCE);
        }
        for (int[] k : new int[][]{{-5, -2}, {5, -2}, {-5, 2}, {5, 2}})
            for (int y = 1; y <= 3; y++) put(level, c.offset(k[0], deck + y, k[1]), Blocks.CHAIN);
        // ballon : coque d'ellipsoïde rayée blanc et rouge (l'intérieur reste tel quel)
        int by = deck + 7;
        for (int x = -10; x <= 10; x++) for (int y = -3; y <= 3; y++) for (int z = -4; z <= 4; z++) {
            double e = (x * x) / 100.0 + (y * y) / 9.0 + (z * z) / 16.0;
            if (e > 1.0 || e <= 0.6) continue;
            put(level, c.offset(x, by + y, z), Math.floorMod(x, 4) < 2 ? Blocks.WHITE_WOOL : Blocks.RED_WOOL);
        }
        // dérives et hélice
        for (int y = -2; y <= 2; y++) put(level, c.offset(11, by + y, 0), Blocks.RED_WOOL);
        for (int z = -2; z <= 2; z++) put(level, c.offset(11, by, z), Blocks.RED_WOOL);
        put(level, c.offset(6, deck, 0), Blocks.IRON_BARS);
        put(level, c.offset(7, deck, 0), Blocks.IRON_BARS);
        return c.offset(2, deck, 0);
    }

    /** Le joueur parle à l'hôtesse : si c'est son dirigeable, elle lui propose d'embarquer (lien cliquable). */
    public static boolean talk(ServerPlayer player, NpcEntity hostess) {
        QuestData d = QuestManager.data(player);
        if (!isMine(d, hostess.home())) {
            say(player, Component.translatable("dialog.hxh.passeur.not_yours"));
            return false;
        }
        ExamSite site = pendingSite(d);
        if (site == null) return false;
        say(player, Component.translatable("dialog.hxh.passeur.offer", Component.translatable("exam.hxh.site." + site.id())));
        player.sendSystemMessage(Component.translatable("dialog.hxh.passeur.accept").withStyle(s -> s
                .withColor(ChatFormatting.GREEN).withUnderlined(true)
                .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/hunter exam board " + d.examDeparture()))
                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                        Component.translatable("dialog.hxh.passeur.accept_hover")))));
        return true;
    }

    private static boolean isMine(QuestData d, BlockPos home) {
        long dep = d.examDeparture();
        return dep != Long.MIN_VALUE && BlockPos.of(dep).distSqr(home) <= MATCH * MATCH;
    }

    private static void say(ServerPlayer player, Component line) {
        player.sendSystemMessage(Component.translatable("dialog.hxh.format",
                Component.translatable("npc.hxh.passeur").withStyle(ChatFormatting.YELLOW), line));
    }

    /** « Embarquer » : à bord de son dirigeable, le joueur part pour le lieu de l'épreuve. */
    public static boolean board(ServerPlayer player) {
        return board(player, Long.MIN_VALUE);
    }

    /**
     * {@code ticket} = le dirigeable du lien cliqué ; un lien d'une épreuve passée (déjà réalisée ou tentée)
     * ne correspond plus au dirigeable qui attend le joueur.
     */
    public static boolean board(ServerPlayer player, long ticket) {
        QuestData d = QuestManager.data(player);
        long dep = d.examDeparture();
        if (dep == Long.MIN_VALUE || (ticket != Long.MIN_VALUE && ticket != dep)) {
            player.sendSystemMessage(Component.translatable("dialog.hxh.passeur.gone").withStyle(ChatFormatting.RED));
            return false;
        }
        if ((ExamTrips.enabled(player)
                && (ExamWorld.isExamLevel(player.level()) || BlockPos.of(dep).distSqr(player.blockPosition()) > MATCH * MATCH))) {
            player.sendSystemMessage(Component.translatable("dialog.hxh.passeur.too_far").withStyle(ChatFormatting.RED));
            return false;
        }
        ExamSite site = pendingSite(d);
        if (site == null) return false;
        say(player, Component.translatable("dialog.hxh.passeur.board", Component.translatable("exam.hxh.site." + site.id())));
        int instance = d.examInstance(site.id());
        d.markExamVisited(site.id(), instance);
        d.setExamDeparture(Long.MIN_VALUE);
        ExamTrips.sendTo(player, site, instance);
        QuestManager.announceCurrentTrial(player);
        QuestManager.sync(player);
        return true;
    }

    /** Lieu de l'épreuve qui attend le joueur : celui de l'étape en cours (ou de la suivante qui en a un). */
    public static ExamSite pendingSite(QuestData d) {
        Quest q = com.evandouard.hxh.quest.QuestRegistry.get(com.evandouard.hxh.quest.QuestRegistry.HUNTER_EXAM);
        int[] st = d.stateOf(com.evandouard.hxh.quest.QuestRegistry.HUNTER_EXAM.toString());
        if (q == null || st == null) return null;
        List<Quest.Step> steps = QuestManager.steps(d, q);
        for (int i = st[0]; i < steps.size(); i++) {
            ExamSite s = ExamSite.byId(steps.get(i).site());
            if (s != null) return s;
        }
        return null;
    }
}
