package com.evandouard.hxh.arena;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.NenMode;
import com.evandouard.hxh.effect.ModEffects;
import com.evandouard.hxh.fx.NenFx;
import com.evandouard.hxh.npc.NpcEntity;
import com.evandouard.hxh.quest.QuestData;
import com.evandouard.hxh.quest.QuestManager;
import com.evandouard.hxh.registry.ModEntities;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Comme dans l'anime : au 200e étage, Hisoka barre le couloir de l'hôtesse avec son aura. Un joueur qui n'a pas
 * encore pris son inscription au 200e et qui ne tient pas le Ten (ou une posture plus forte : Ren, Ken) est repoussé
 * et pris de peur à l'approche de l'hôtesse. Avec le Ten, l'aura glisse sur lui et il passe.
 */
public final class AuraWall {
    /** Rayon autour de l'hôtesse du 200e où l'aura d'Hisoka repousse. */
    public static final double RADIUS = 7.0;
    /** Hisoka se tient entre l'entrée et l'hôtesse, à cette distance d'elle. */
    public static final double HISOKA_OFFSET = 3.5;
    public static final String TAG = "hxh_aura_wall";

    private AuraWall() {}

    /** Le joueur passe-t-il l'aura ? Inscrit au 200e, ou en Ten / Ren / Ken. */
    public static boolean canPass(ServerPlayer player) {
        QuestData d = QuestManager.data(player);
        if (d.registered200() || player.isCreative() || player.isSpectator()) return true;
        HunterData h = HunterStats.get(player);
        if (!h.nenUnlocked()) return false;
        NenMode m = h.mode();
        return m == NenMode.TEN || m == NenMode.REN || m == NenMode.KEN;
    }

    /** Chaque seconde, pour chaque joueur hors combat dans la Tour. */
    public static void tick(ServerLevel level, ServerPlayer player) {
        QuestData d = QuestManager.data(player);
        if (d.arenaFloor() < ArenaRules.NEN_FLOOR || d.registered200()) return;
        NpcEntity desk = receptionistNear(level, player);
        if (desk == null) return;
        NpcEntity hisoka = ensureHisoka(level, desk, player);
        if (canPass(player)) return;
        Vec3 from = desk.position();
        Vec3 away = player.position().subtract(from).multiply(1, 0, 1);
        away = away.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : away.normalize();
        player.setDeltaMovement(away.scale(1.1).add(0, 0.25, 0));
        player.hurtMarked = true;
        player.addEffect(new MobEffectInstance(ModEffects.FEAR, 60, 1, false, false, true));
        if (hisoka != null) NenFx.flare(hisoka, 2.2f, NenFx.HISOKA);
        player.displayClientMessage(Component.translatable("message.hxh.arena_aura_wall").withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    /** Hôtesse de la tranche du 200e, proche du joueur. */
    static NpcEntity receptionistNear(ServerLevel level, ServerPlayer player) {
        int nenLoc = ArenaWorld.locationOfBracket(ArenaManager.bracketOf(player).index());
        List<NpcEntity> desks = level.getEntitiesOfClass(NpcEntity.class, new AABB(player.blockPosition()).inflate(RADIUS),
                n -> n.npcId().equals("receptionist") && ArenaWorld.locationAt(n.home()) == nenLoc);
        return desks.isEmpty() ? null : desks.get(0);
    }

    /** Hisoka se tient entre le joueur et l'hôtesse ; on le pose s'il n'est pas déjà là. */
    static NpcEntity ensureHisoka(ServerLevel level, NpcEntity desk, ServerPlayer player) {
        List<NpcEntity> found = level.getEntitiesOfClass(NpcEntity.class, desk.getBoundingBox().inflate(RADIUS + 3),
                n -> n.getTags().contains(TAG));
        if (!found.isEmpty()) return found.get(0);
        NpcEntity h = ModEntities.NPC.get().create(level);
        if (h == null) return null;
        Vec3 dir = player.position().subtract(desk.position()).multiply(1, 0, 1);
        dir = dir.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : dir.normalize();
        Vec3 at = desk.position().add(dir.scale(HISOKA_OFFSET));
        float yaw = (float) (Math.toDegrees(Math.atan2(-dir.x, dir.z)));
        h.moveTo(at.x, desk.getY(), at.z, yaw, 0);
        h.setNpcId("hisoka");
        h.setHome(net.minecraft.core.BlockPos.containing(at.x, desk.getY(), at.z));
        h.addTag(TAG);
        level.addFreshEntity(h);
        return h;
    }
}
