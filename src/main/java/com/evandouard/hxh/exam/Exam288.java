package com.evandouard.hxh.exam;

import com.evandouard.hxh.quest.Quest;
import com.evandouard.hxh.quest.QuestData;
import com.evandouard.hxh.quest.QuestManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Le 288e Examen (OVA « G.I. Final ») : dès l'ouverture, Killua met tous les candidats hors de combat d'un coup et
 * passe seul. Ici : une meute de candidats se rue sur toi, il faut tous les assommer (aucun ne meurt : sous 25 % de vie,
 * il s'effondre) avant la fin du temps. Étape {@code "type": "knockout"}, {@code count} candidats en
 * {@code seconds} secondes ; le temps écoulé élimine.
 */
public final class Exam288 {
    private Exam288() {}

    public static final String TAG = "hxh_288";
    private static final Map<UUID, Integer> TICKS = new HashMap<>();
    private static final Map<UUID, Boolean> SPAWNED = new HashMap<>();

    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.HIGH, LivingIncomingDamageEvent.class, Exam288::onDamage);
    }

    private static String owner(UUID player) {
        return TAG + "_" + player;
    }

    public static int remaining(ServerPlayer player) {
        String tag = owner(player.getUUID());
        return player.serverLevel().getEntitiesOfClass(ExamCandidate.class, new AABB(player.blockPosition()).inflate(48),
                v -> v.getTags().contains(tag) && v.isAlive()).size();
    }

    /** Chaque tick de l'étape : la meute entre au premier tick ; le temps écoulé élimine. */
    public static void tick(ServerPlayer player, Quest.Step step, QuestData d) {
        if (!SPAWNED.containsKey(player.getUUID())) {
            SPAWNED.put(player.getUUID(), true);
            TICKS.put(player.getUUID(), 0);
            spawnPack(player, step.count() - d.progress());
            player.sendSystemMessage(Component.translatable("exam.hxh.ko_start", step.count(), step.seconds()).withStyle(ChatFormatting.GOLD));
        }
        int t = TICKS.merge(player.getUUID(), 1, Integer::sum);
        if (t % 20 == 0) QuestManager.sync(player);
        if (step.seconds() > 0 && t >= step.seconds() * 20) {
            player.sendSystemMessage(Component.translatable("exam.hxh.ko_time").withStyle(ChatFormatting.RED));
            reset(player);
            QuestManager.failExam(player);
        }
    }

    public static int secondsElapsed(ServerPlayer player) {
        return TICKS.getOrDefault(player.getUUID(), 0) / 20;
    }

    /** Fait entrer {@code n} candidats en cercle autour du joueur. */
    public static void spawnPack(ServerPlayer player, int n) {
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < n; i++) {
            ExamCandidate v = com.evandouard.hxh.registry.ModEntities.EXAM_CANDIDATE.get().create(level);
            if (v == null) continue;
            double a = i * Math.PI * 2 / n;
            Vec3 at = ExamWorld.safeSpot(level, player.position(), a, 7); // jamais au-dessus du vide
            v.moveTo(at.x, at.y, at.z, (float) Math.toDegrees(a) + 90f, 0);
            var hp = v.getAttribute(Attributes.MAX_HEALTH);
            if (hp != null) hp.setBaseValue(12);
            v.setHealth(v.getMaxHealth());
            v.setCustomName(Component.translatable("exam.hxh.candidate", 100 + level.getRandom().nextInt(300)));
            v.setPersistenceRequired();
            v.addTag(TAG);
            v.addTag(owner(player.getUUID()));
            v.setTarget(player);
            level.addFreshEntity(v);
        }
    }

    private static void onDamage(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ExamCandidate v) || !v.getTags().contains(TAG) || v.level().isClientSide) return;
        if (v.getHealth() - e.getAmount() > v.getMaxHealth() * 0.25f) return;
        e.setCanceled(true);
        ServerPlayer owner = null;
        if (v.level() instanceof ServerLevel level)
            for (ServerPlayer p : level.players()) if (v.getTags().contains(owner(p.getUUID()))) owner = p;
        knockOut(owner, v);
    }

    /** Un candidat s'effondre : il quitte l'arène, une de plus au compteur. */
    public static void knockOut(ServerPlayer player, ExamCandidate v) {
        if (v.level() instanceof ServerLevel level)
            com.evandouard.hxh.fx.NenFx.impactAt(level, v.position().add(0, 1, 0), 1.2f, 0xFFFFFF);
        v.discard();
        if (player != null) QuestManager.knockedOut(player);
    }

    /** Fin de l'épreuve (réussie, ratée, passée) : la meute repart. */
    public static void reset(ServerPlayer player) {
        TICKS.remove(player.getUUID());
        SPAWNED.remove(player.getUUID());
        String tag = owner(player.getUUID());
        for (ExamCandidate v : player.serverLevel().getEntitiesOfClass(ExamCandidate.class, new AABB(player.blockPosition()).inflate(64),
                v -> v.getTags().contains(tag))) v.discard();
    }
}
