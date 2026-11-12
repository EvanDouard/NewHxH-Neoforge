package com.evandouard.hxh.quest;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.NenMode;
import com.evandouard.hxh.fx.NenFx;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Épreuves de la quête de Netero qui débloque le soin par le Nen (milieu de partie) :
 * <ul>
 *     <li>{@code prayer} : « prière de gratitude », {@code count} coups de base donnés en Ten ou en Ken ;</li>
 *     <li>{@code kannon} : tenir {@code seconds} secondes sous les paumes de Kannon. Un cercle doré marque l'endroit
 *     visé, la paume tombe une seconde plus tard. Netero ne tue pas : un coup qui serait fatal laisse à un cœur et
 *     l'épreuve recommence ;</li>
 *     <li>{@code wounded_meditation} : blessé (moitié de vie ou moins), accroupi et immobile en Ten pendant
 *     {@code seconds} secondes, pour sentir l'aura réparer le corps.</li>
 * </ul>
 */
public final class NeteroTraining {
    /** Une paume toutes les {@code PALM_EVERY} ticks, qui tombe {@code PALM_DELAY} ticks après son cercle. */
    public static final int PALM_EVERY = 24;
    public static final int PALM_DELAY = 20;
    public static final double PALM_RADIUS = 1.7;
    /** Part de la vie maximale qu'enlève une paume (avant le Ken). */
    public static final float PALM_DAMAGE = 0.22f;

    /** Paume en attente : point visé et tick où elle tombe. */
    private record Palm(Vec3 at, long hitTick) {}

    private static final Map<UUID, Palm> PENDING = new ConcurrentHashMap<>();
    private static final Map<ServerPlayer, Vec3> STILL = new WeakHashMap<>();

    private NeteroTraining() {}

    /** Coup de base donné : compte pour la prière s'il est donné en Ten ou en Ken. */
    static void prayer(ServerPlayer player, Quest.Step step, QuestData d) {
        NenMode mode = HunterStats.get(player).mode();
        if (mode != NenMode.TEN && mode != NenMode.KEN) {
            player.displayClientMessage(Component.translatable("quest.hxh.netero.prayer_ten").withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        d.progress++;
        if (d.progress >= step.count()) QuestManager.advance(player);
        else if (d.progress % 10 == 0) QuestManager.sync(player);
    }

    /** Chaque tick : les paumes de Kannon. La progression compte en ticks (affichée en secondes). */
    static void kannon(ServerPlayer player, Quest.Step step, QuestData d) {
        if (!(player.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        Palm palm = PENDING.get(player.getUUID());
        if (palm != null && now >= palm.hitTick()) {
            PENDING.remove(player.getUUID());
            strike(player, level, palm.at(), d);
        }
        if (d.progress % PALM_EVERY == 0 && !PENDING.containsKey(player.getUUID())) {
            // vise là où le joueur sera s'il ne bouge pas, légèrement en avance sur sa course
            Vec3 at = player.position().add(player.getDeltaMovement().multiply(6, 0, 6));
            PENDING.put(player.getUUID(), new Palm(at, now + PALM_DELAY));
            NenFx.shockwaveAt(level, at, (float) PALM_RADIUS, NenFx.GOLD);
            NenFx.sprite(level, NenFx.Sprite.KANNON_HANDS, null, at.add(0, 3.2, 0), 2.6f, NenFx.GOLD, PALM_DELAY + 6);
        }
        d.progress++;
        if (d.progress >= step.seconds() * 20) {
            PENDING.remove(player.getUUID());
            QuestManager.advance(player);
        } else if (d.progress % 20 == 0) QuestManager.sync(player);
    }

    private static void strike(ServerPlayer player, ServerLevel level, Vec3 at, QuestData d) {
        NenFx.impactAt(level, at.add(0, 0.2, 0), 2.4f, NenFx.GOLD);
        NenFx.quake(level, at, 2f);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8f, 1.3f);
        if (player.position().distanceToSqr(at) > PALM_RADIUS * PALM_RADIUS) return;
        float damage = player.getMaxHealth() * PALM_DAMAGE;
        if (HunterStats.get(player).mode() == NenMode.KEN) damage *= 0.5f;
        if (player.getHealth() - damage <= 1f) {
            // Netero retient son coup : on reste à un cœur et l'épreuve recommence
            player.setHealth(1f);
            d.progress = 0;
            PENDING.remove(player.getUUID());
            player.sendSystemMessage(Component.translatable("quest.hxh.netero.kannon_reset").withStyle(ChatFormatting.RED));
            QuestManager.sync(player);
            return;
        }
        player.hurt(player.damageSources().generic(), damage);
    }

    /** Chaque seconde : méditation blessé, accroupi, immobile, en Ten. */
    static void woundedMeditation(ServerPlayer player, Quest.Step step, QuestData d) {
        HunterData h = HunterStats.get(player);
        Vec3 last = STILL.put(player, player.position());
        boolean still = last != null && last.distanceToSqr(player.position()) < 0.01;
        if (player.getHealth() > player.getMaxHealth() * 0.5f) {
            player.displayClientMessage(Component.translatable("quest.hxh.netero.need_wound").withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        if (h.mode() != NenMode.TEN || !player.isShiftKeyDown() || !still) {
            player.displayClientMessage(Component.translatable("quest.hxh.netero.meditate_hint").withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        d.progress++;
        if (d.progress >= step.seconds()) QuestManager.advance(player);
        else QuestManager.sync(player);
    }

    public static void reset(ServerPlayer player) {
        PENDING.remove(player.getUUID());
    }
}
