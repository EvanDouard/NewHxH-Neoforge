package com.evandouard.hxh.exam;

import com.evandouard.hxh.quest.Quest;
import com.evandouard.hxh.quest.QuestData;
import com.evandouard.hxh.quest.QuestManager;
import com.evandouard.hxh.quest.QuestRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tournoi final organisé par Netero (Phase 5) : des duels un contre un, gagnés quand l'adversaire dit « J'abandonne ».
 * Le candidat sur le point de perdre abandonne aussi ; il est repêché une fois (tableau des perdants, comme dans le
 * manga où un seul candidat échoue), au second abandon l'Examen est raté. Étape {@code "type": "duel"},
 * {@code "count"} = victoires à obtenir.
 */
public final class ExamTournament {
    private ExamTournament() {}

    public static final String DUEL_TAG = "hxh_duel";
    /** Part de vie sous laquelle on abandonne. */
    static final float GIVE_UP_AT = 0.25f;
    static final String[] OPPONENTS = {"Hanzo", "Pokkle", "Bodoro", "Gittarackur", "Todo", "Ponzu"};
    /** Abandons du joueur pendant le tournoi en cours. */
    private static final Map<UUID, Integer> GAVE_UP = new HashMap<>();

    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.HIGH, LivingIncomingDamageEvent.class, ExamTournament::onDamage);
    }

    private static String ownerTag(UUID player) {
        return DUEL_TAG + "_" + player;
    }

    /** Chaque seconde pendant un duel : un adversaire doit être en face du joueur. */
    public static void tick(ServerPlayer player, Quest.Step step, QuestData d) {
        if (player.tickCount % 20 != 0) return;
        // Un adversaire nommé par l'étape (prisonnier de la Tour de Trick…), sinon un candidat du tournoi
        if (opponent(player) == null) spawnOpponent(player, d.progress(), step.entity());
    }

    public static Vindicator opponent(ServerPlayer player) {
        String tag = ownerTag(player.getUUID());
        return player.serverLevel().getEntitiesOfClass(Vindicator.class, new AABB(player.blockPosition()).inflate(48),
                v -> v.getTags().contains(tag) && v.isAlive()).stream().findFirst().orElse(null);
    }

    /** Fait entrer l'adversaire n° {@code round} face au joueur. */
    public static Vindicator spawnOpponent(ServerPlayer player, int round) {
        return spawnOpponent(player, round, "");
    }

    public static Vindicator spawnOpponent(ServerPlayer player, int round, String forcedName) {
        ServerLevel level = player.serverLevel();
        Vindicator v = EntityType.VINDICATOR.create(level);
        if (v == null) return null;
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
        if (look.lengthSqr() < 0.01) look = new Vec3(0, 0, -1);
        Vec3 at = player.position().add(look.scale(5));
        v.moveTo(at.x, player.getY(), at.z, player.getYRot() + 180f, 0);
        v.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(round % 2 == 0 ? Items.IRON_AXE : Items.STONE_SWORD));
        v.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0f);
        var hp = v.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(30 + 10 * round);
        v.setHealth(v.getMaxHealth());
        String name = !forcedName.isEmpty() ? forcedName
                : OPPONENTS[(round + Math.abs(player.getUUID().hashCode())) % OPPONENTS.length];
        v.setCustomName(Component.literal(name));
        v.setCustomNameVisible(true);
        v.setPersistenceRequired();
        v.addTag(DUEL_TAG);
        v.addTag(ownerTag(player.getUUID()));
        v.setTarget(player);
        level.addFreshEntity(v);
        player.sendSystemMessage(Component.translatable("exam.hxh.duel_start", name).withStyle(ChatFormatting.GOLD));
        return v;
    }

    private static void onDamage(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide) return;
        // L'adversaire va tomber trop bas : il abandonne
        if (victim instanceof Vindicator v && v.getTags().contains(DUEL_TAG)) {
            if (v.getHealth() - e.getAmount() > v.getMaxHealth() * GIVE_UP_AT) return;
            ServerPlayer owner = ownerOf(v);
            e.setCanceled(true);
            if (owner != null) opponentGivesUp(owner, v);
            else v.discard();
            return;
        }
        // Le candidat va tomber : il dit « J'abandonne » plutôt que de mourir
        if (victim instanceof ServerPlayer p && inDuel(p) && p.getHealth() - e.getAmount() <= p.getMaxHealth() * GIVE_UP_AT) {
            e.setCanceled(true);
            playerGivesUp(p);
        }
    }

    private static ServerPlayer ownerOf(Vindicator v) {
        if (!(v.level() instanceof ServerLevel level)) return null;
        for (ServerPlayer p : level.players()) if (v.getTags().contains(ownerTag(p.getUUID()))) return p;
        return null;
    }

    public static boolean inDuel(ServerPlayer player) {
        QuestData d = QuestManager.data(player);
        int[] st = d.stateOf(QuestRegistry.HUNTER_EXAM.toString());
        Quest q = QuestRegistry.get(QuestRegistry.HUNTER_EXAM);
        if (st == null || q == null) return false;
        var steps = QuestManager.steps(d, q);
        return st[0] < steps.size() && steps.get(st[0]).type().equals("duel");
    }

    /** « J'abandonne ! » : une victoire de plus pour le joueur. */
    public static void opponentGivesUp(ServerPlayer player, Vindicator v) {
        player.sendSystemMessage(Component.translatable("dialog.hxh.format",
                v.getCustomName() == null ? Component.literal("?") : v.getCustomName().copy().withStyle(ChatFormatting.YELLOW),
                Component.translatable("exam.hxh.duel_give_up")));
        v.discard();
        QuestManager.duelWon(player);
    }

    /** Le joueur abandonne : repêché la première fois, éliminé la seconde. */
    public static void playerGivesUp(ServerPlayer player) {
        Vindicator v = opponent(player);
        if (v != null) v.discard();
        player.setHealth(player.getMaxHealth());
        int n = GAVE_UP.merge(player.getUUID(), 1, Integer::sum);
        if (n >= 2) {
            GAVE_UP.remove(player.getUUID());
            player.sendSystemMessage(Component.translatable("exam.hxh.duel_out").withStyle(ChatFormatting.RED));
            QuestManager.failExam(player);
        } else {
            player.sendSystemMessage(Component.translatable("exam.hxh.duel_second_chance").withStyle(ChatFormatting.YELLOW));
        }
    }

    /** Fin du tournoi (réussi ou raté) : on oublie les abandons. */
    public static void reset(ServerPlayer player) {
        GAVE_UP.remove(player.getUUID());
        Vindicator v = opponent(player);
        if (v != null) v.discard();
    }
}
