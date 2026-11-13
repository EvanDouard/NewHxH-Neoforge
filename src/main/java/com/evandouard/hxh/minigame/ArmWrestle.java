package com.evandouard.hxh.minigame;

import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.network.ArmWrestleStatePayload;
import com.evandouard.hxh.network.HxHNetwork;
import com.evandouard.hxh.quest.QuestManager;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Bras de fer, façon pêche de Stardew Valley. Une jauge verticale : la zone de l'adversaire monte et descend ; le
 * joueur maintient la touche pour faire monter son repère (il retombe sinon). Repère dans la zone : la manche avance ;
 * dehors : l'adversaire reprend le dessus. Barre pleine = victoire, vide = défaite. Tout est calculé par le serveur,
 * le client n'envoie que « je tiens / je lâche ». La zone est plus large quand on est plus fort que l'adversaire.
 */
public final class ArmWrestle {
    private ArmWrestle() {}

    public static final String NPC = "lutteur";
    public static final int STAKE = 100;
    public static final int STRENGTH_XP = 40;

    /** État d'une manche (public pour les tests). */
    public static final class Match {
        public float marker = 0.2f, velocity = 0f, zoneCenter = 0.5f, zoneTarget = 0.5f, progress = 0.3f;
        public final float zoneSize, zoneSpeed;
        public final int stake;
        public boolean holding;
        public int result; // 0 en cours, 1 gagné, 2 perdu
        final RandomSource random;

        public Match(int playerStrength, int opponentStrength, int stake, long seed) {
            this.zoneSize = clamp(0.2f + (playerStrength - opponentStrength) * 0.003f, 0.08f, 0.38f);
            this.zoneSpeed = clamp(0.006f + opponentStrength * 0.0002f, 0.006f, 0.03f);
            this.stake = stake;
            this.random = RandomSource.create(seed);
        }

        /** Un tick de la manche. */
        public void step() {
            if (result != 0) return;
            velocity += holding ? 0.004f : -0.0035f;
            velocity = clamp(velocity, -0.03f, 0.03f);
            marker += velocity;
            if (marker <= 0f || marker >= 1f) {
                marker = clamp(marker, 0f, 1f);
                velocity = 0f;
            }
            if (Math.abs(zoneTarget - zoneCenter) < zoneSpeed) zoneTarget = zoneSize / 2 + random.nextFloat() * (1f - zoneSize);
            zoneCenter += Math.signum(zoneTarget - zoneCenter) * zoneSpeed;
            boolean inside = Math.abs(marker - zoneCenter) <= zoneSize / 2;
            progress = clamp(progress + (inside ? 0.0045f : -0.0025f), 0f, 1f);
            if (progress >= 1f) result = 1;
            else if (progress <= 0f) result = 2;
        }
    }

    static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static final Map<UUID, Match> MATCHES = new HashMap<>();

    @Nullable
    public static Match match(ServerPlayer p) {
        return MATCHES.get(p.getUUID());
    }

    /** Défi lancé : la mise est prise tout de suite (rendue doublée en cas de victoire). */
    public static boolean start(ServerPlayer p, int opponentStrength, int stake) {
        if (MATCHES.containsKey(p.getUUID())) return false;
        var q = QuestManager.data(p);
        if (q.jenny() < stake) {
            p.displayClientMessage(Component.translatable("armwrestle.hxh.no_jenny", stake), true);
            return false;
        }
        q.addJenny(-stake);
        Match m = new Match(HunterStats.get(p).level(Stat.STRENGTH), opponentStrength, stake, p.getRandom().nextLong());
        MATCHES.put(p.getUUID(), m);
        p.sendSystemMessage(Component.translatable("armwrestle.hxh.start", stake).withStyle(ChatFormatting.GOLD));
        sync(p, m);
        return true;
    }

    /** Le joueur tient ou lâche (seule chose que le client envoie). */
    public static void input(ServerPlayer p, boolean holding) {
        Match m = MATCHES.get(p.getUUID());
        if (m != null) m.holding = holding;
    }

    public static void tick(MinecraftServer server) {
        if (MATCHES.isEmpty()) return;
        for (var it = MATCHES.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            Match m = e.getValue();
            if (p == null) {
                it.remove();
                continue;
            }
            m.step();
            sync(p, m);
            if (m.result != 0) {
                it.remove();
                finish(p, m);
            }
        }
    }

    static void finish(ServerPlayer p, Match m) {
        if (m.result == 1) {
            QuestManager.data(p).addJenny(m.stake * 2);
            HunterStats.addXp(p, Stat.STRENGTH, STRENGTH_XP);
            p.sendSystemMessage(Component.translatable("armwrestle.hxh.won", m.stake * 2, STRENGTH_XP).withStyle(ChatFormatting.GREEN));
            p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.2f);
        } else {
            p.sendSystemMessage(Component.translatable("armwrestle.hxh.lost", m.stake).withStyle(ChatFormatting.RED));
            p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.7f, 0.8f);
        }
    }

    private static void sync(ServerPlayer p, Match m) {
        HxHNetwork.sendTo(p, new ArmWrestleStatePayload(m.marker, m.zoneCenter, m.zoneSize, m.progress, m.result));
    }

    /** Force du champion d'après la Force du joueur : un peu au-dessus, pour que ce soit un vrai défi. */
    public static int championStrength(ServerPlayer p) {
        return Math.min(100, HunterStats.get(p).level(Stat.STRENGTH) + 10);
    }
}
