package com.evandouard.hxh.arena;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.Mastery;
import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.hatsu.HatsuKit;
import com.evandouard.hxh.hatsu.HatsuKits;
import com.evandouard.hxh.nen.NenModes;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Adversaire de la Tour Céleste. Sa force dépend de l'étage ; à partir du 200e il maîtrise le Nen
 * et reçoit un Hatsu jouable tiré au hasard (il s'en sert comme n'importe quel mob au Nen).
 */
public class ArenaFighterEntity extends PathfinderMob implements Enemy {
    private static final EntityDataAccessor<Integer> SKIN =
            SynchedEntityData.defineId(ArenaFighterEntity.class, EntityDataSerializers.INT);
    public static final int SKINS = 4;

    private int floor = 1;
    @Nullable
    private UUID opponent;
    /** Immobile pendant le compte à rebours. */
    private boolean waiting = false;

    public ArenaFighterEntity(EntityType<? extends ArenaFighterEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 2)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SKIN, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    public int skin() {
        return entityData.get(SKIN);
    }

    public int floor() {
        return floor;
    }

    @Nullable
    public UUID opponent() {
        return opponent;
    }

    public void setWaiting(boolean value) {
        waiting = value;
        setNoAi(value);
    }

    /**
     * Règle stats, Nen et Hatsu d'après l'étage. Sans Nen sous le 200e étage.
     */
    public void configure(int floor, @Nullable UUID opponent) {
        this.floor = floor;
        this.opponent = opponent;
        entityData.set(SKIN, getRandom().nextInt(SKINS));
        HunterData data = HunterStats.get(this);
        int stat = ArenaRules.opponentStatLevel(floor);
        for (Stat s : Stat.values()) {
            // Un peu de variété d'un adversaire à l'autre
            int v = (int) Math.round(stat * (0.85 + 0.3 * getRandom().nextDouble()));
            data.setLevel(s, Math.max(0, Math.min(com.evandouard.hxh.nen.NenMobAI.MOB_MAX_LEVEL, v)));
        }
        int nen = ArenaRules.opponentNenLevel(floor);
        if (nen > 0) {
            List<HatsuKit> kits = HatsuKits.playable();
            HatsuKit kit = kits.get(getRandom().nextInt(kits.size()));
            data.setNenType(kit.type());
            data.setNenRevealed(true);
            data.setNenLevel(nen);
            for (Mastery m : Mastery.values()) {
                data.setMasteryLevel(m, (int) Math.round(nen * (0.6 + 0.3 * getRandom().nextDouble())));
            }
            NenModes.unlock(this);
            data.setKit(kit.id());
        }
        HunterStats.applyAttributes(this);
        setHealth(getMaxHealth());
        setCustomName(Component.translatable(nen > 0 ? "entity.hxh.arena_fighter.nen" : "entity.hxh.arena_fighter.floor", floor));
        setCustomNameVisible(true);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel server && !waiting) {
            // Il ne vise que son adversaire désigné
            if (opponent != null && (getTarget() == null || !getTarget().isAlive())) {
                Player p = server.getPlayerByUUID(opponent);
                if (p != null && p.level() == level() && !p.isCreative() && !p.isSpectator()) setTarget(p);
            }
            // Adversaire orphelin (redémarrage du serveur, combat annulé) : il quitte l'arène
            if (tickCount > 40 && !ArenaManager.isActiveFighter(this)) discard();
        }
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return super.canAttack(target) && (opponent == null || target.getUUID().equals(opponent) || target == getLastHurtByMob());
    }

    @Override
    public boolean shouldDropLoot() {
        return false;
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("floor", floor);
        tag.putInt("skin", skin());
        if (opponent != null) tag.putUUID("opponent", opponent);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        floor = Math.max(1, tag.getInt("floor"));
        entityData.set(SKIN, tag.getInt("skin"));
        opponent = tag.hasUUID("opponent") ? tag.getUUID("opponent") : null;
    }
}
