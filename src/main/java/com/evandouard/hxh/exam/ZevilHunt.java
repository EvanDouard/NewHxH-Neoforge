package com.evandouard.hxh.exam;

import com.evandouard.hxh.quest.QuestData;
import com.evandouard.hxh.quest.QuestManager;
import com.evandouard.hxh.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chasse aux plaques de l'île de Zevil : le candidat n'est pas seul contre tous. Les autres candidats se battent
 * aussi entre eux : chacun compte ses plaques ({@link #PLATES}, en plus de la sienne en main gauche) ; le vainqueur
 * récupère toutes celles du vaincu. Un candidat qui atteint {@link #NPC_GOAL} plaques est qualifié : il quitte l'île.
 * D'autres débarquent lentement ({@link #SPAWN_EVERY}), jusqu'à {@link #TOTAL_SPAWNS} en tout ; ensuite on continue
 * avec ceux qui restent. Quand {@link #QUALIFY_LIMIT} candidats sont qualifiés, l'épreuve est terminée : qui n'a pas
 * ses points est éliminé. Le porteur de la plaque de la cible ne se qualifie jamais (sa plaque reste à prendre), et il
 * reste toujours assez de plaques en jeu pour finir.
 */
public final class ZevilHunt {
    private ZevilHunt() {}

    /** Plaques ramassées par un candidat (sans compter la sienne). */
    public static final String PLATES = "hxh_plates";
    public static final int NPC_GOAL = 4, QUALIFY_LIMIT = 4, TOTAL_SPAWNS = 18, MAX_ALIVE = 7, SPAWN_EVERY = 500;

    /** État d'un exemplaire de l'île. */
    static final class State {
        int spawned, qualified;
        long nextSpawn, lastTick;
    }

    private static final Map<Integer, State> STATES = new HashMap<>();

    public static void registerEvents(IEventBus bus) {
        bus.addListener(LivingDeathEvent.class, ZevilHunt::onDeath);
    }

    static State state(int instance) {
        return STATES.computeIfAbsent(instance, k -> new State());
    }

    /** Pour les tests et après une élimination : l'île repart de zéro. */
    public static void reset(int instance) {
        STATES.remove(instance);
    }

    /** Plaques d'un candidat (la sienne compte 1, celle de la cible 3, plus celles ramassées). */
    public static int plates(ExamCandidate c) {
        int own = c.getTags().contains(ExamHazards.TARGET_TAG) ? 3 : 1;
        return own + c.getPersistentData().getInt(PLATES);
    }

    static List<ExamCandidate> candidates(ServerLevel level, BlockPos c) {
        return level.getEntitiesOfClass(ExamCandidate.class, new AABB(c).inflate(64),
                v -> v.isAlive() && v.getTags().contains(ExamHazards.CANDIDATE_TAG));
    }

    /** Une fois par tick et par exemplaire (appelé pour chaque joueur présent sur l'île). */
    static void tick(ServerPlayer player, ServerLevel level, BlockPos c, int instance) {
        State s = state(instance);
        long now = level.getGameTime();
        if (s.lastTick == now) return;
        s.lastTick = now;
        if (s.nextSpawn == 0) {
            s.spawned = Math.max(s.spawned, candidates(level, c).size());
            s.nextSpawn = now + SPAWN_EVERY;
        }
        if (now % 20 != 0) return;
        List<ExamCandidate> alive = candidates(level, c);
        // qualifiés : ils quittent l'île avec leurs plaques
        for (ExamCandidate v : alive) {
            if (v.getTags().contains(ExamHazards.TARGET_TAG) || plates(v) < NPC_GOAL) continue;
            s.qualified++;
            com.evandouard.hxh.fx.NenFx.shockwaveAt(level, v.position(), 1.5f, 0xFFFFFFFF);
            level.playSound(null, v.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                    net.minecraft.sounds.SoundSource.NEUTRAL, 0.6f, 1.4f);
            tell(level, c, Component.translatable("exam.hxh.zevil.qualified", v.getDisplayName(), s.qualified, QUALIFY_LIMIT)
                    .withStyle(ChatFormatting.GOLD));
            v.discard();
        }
        if (s.qualified >= QUALIFY_LIMIT) {
            end(level, c, instance);
            return;
        }
        // nouveaux candidats, lentement, loin des joueurs
        alive = candidates(level, c);
        if (now >= s.nextSpawn && alive.size() < MAX_ALIVE && s.spawned < TOTAL_SPAWNS) {
            ExamWorld.Site site = ExamWorld.cached(ExamSite.ZEVIL, instance);
            if (site != null && !site.prey().isEmpty()) {
                BlockPos at = farthestSpawn(site.prey(), player.blockPosition(), level.getRandom());
                boolean target = alive.stream().noneMatch(v -> v.getTags().contains(ExamHazards.TARGET_TAG));
                ExamHazards.candidate(level, at, target, level.getRandom().nextInt(400));
                s.spawned++;
                tell(level, c, Component.translatable("exam.hxh.zevil.landed").withStyle(ChatFormatting.GRAY));
            }
            s.nextSpawn = now + SPAWN_EVERY;
        }
        if (now % 100 == 0) {
            player.displayClientMessage(Component.translatable("exam.hxh.zevil.status", s.qualified, QUALIFY_LIMIT, alive.size())
                    .withStyle(ChatFormatting.YELLOW), true);
        }
    }

    private static BlockPos farthestSpawn(List<BlockPos> spots, BlockPos from, net.minecraft.util.RandomSource rnd) {
        BlockPos best = spots.get(rnd.nextInt(spots.size()));
        for (int i = 0; i < 4; i++) {
            BlockPos p = spots.get(rnd.nextInt(spots.size()));
            if (p.distSqr(from) > best.distSqr(from)) best = p;
        }
        return best;
    }

    /** Assez de candidats qualifiés : l'épreuve s'arrête, ceux qui n'ont pas leurs points sont éliminés. */
    private static void end(ServerLevel level, BlockPos c, int instance) {
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(c).inflate(80))) {
            QuestData d = QuestManager.data(p);
            var step = QuestManager.currentStep(d);
            if (step == null || !step.type().equals("points")) continue;
            p.sendSystemMessage(Component.translatable("exam.hxh.zevil.over", QUALIFY_LIMIT).withStyle(ChatFormatting.RED));
            QuestManager.failExam(p);
        }
        for (ExamCandidate v : candidates(level, c)) v.discard();
        reset(instance);
    }

    private static void tell(ServerLevel level, BlockPos c, Component msg) {
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(c).inflate(80))) p.sendSystemMessage(msg);
    }

    /**
     * Un candidat tombe : vaincu par un autre candidat, celui-ci prend toutes ses plaques (la sienne comprise) ;
     * vaincu par un joueur, ses plaques tombent au sol (la sienne en main gauche tombe d'elle-même).
     */
    private static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ExamCandidate victim) || !victim.getTags().contains(ExamHazards.CANDIDATE_TAG)) return;
        if (!(victim.level() instanceof ServerLevel level)) return;
        int collected = victim.getPersistentData().getInt(PLATES);
        boolean target = victim.getTags().contains(ExamHazards.TARGET_TAG);
        if (event.getSource().getEntity() instanceof ExamCandidate killer && killer != victim) {
            if (target) {
                // la plaque de la cible ne disparaît pas dans la poche d'un PNJ : elle tombe, et la cible passe au vainqueur
                killer.addTag(ExamHazards.TARGET_TAG);
                killer.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.TARGET_PLATE.get()));
                killer.getPersistentData().putInt(PLATES, killer.getPersistentData().getInt(PLATES) + collected
                        + (killer.getTags().contains(ExamHazards.TARGET_TAG) ? 1 : 0));
            } else {
                killer.getPersistentData().putInt(PLATES, killer.getPersistentData().getInt(PLATES) + collected + 1);
            }
            victim.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY); // pas de plaque au sol
            return;
        }
        // vaincu par le joueur (ou autre chose) : les plaques ramassées tombent
        if (collected > 0) {
            ItemEntity drop = new ItemEntity(level, victim.getX(), victim.getY() + 0.5, victim.getZ(),
                    new ItemStack(ModItems.EXAM_PLATE.get(), collected));
            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
        }
    }

    /** Pour l'IA : un candidat de Zevil vise aussi les autres candidats de l'île. */
    public static boolean rivals(LivingEntity self, LivingEntity other) {
        return other != self && self.getTags().contains(ExamHazards.CANDIDATE_TAG)
                && other instanceof ExamCandidate && other.getTags().contains(ExamHazards.CANDIDATE_TAG);
    }
}
