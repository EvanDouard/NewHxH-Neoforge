package com.evandouard.hxh.npc;

import com.evandouard.hxh.quest.QuestManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * PNJ du mod (Navigateur, Beans, Satotz, Netero…). Invulnérable et persistant ; un clic droit lui parle.
 * Son identifiant ({@code npc}) choisit son nom, sa texture et ses dialogues.
 */
public class NpcEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> NPC_ID =
            SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);

    private BlockPos home = BlockPos.ZERO;

    public NpcEntity(EntityType<? extends NpcEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(NPC_ID, "navigator");
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    public String npcId() {
        return entityData.get(NPC_ID);
    }

    public void setNpcId(String id) {
        entityData.set(NPC_ID, id);
        setCustomName(Component.translatable("npc.hxh." + id));
        setCustomNameVisible(true);
    }

    public BlockPos home() {
        return home;
    }

    public void setHome(BlockPos pos) {
        home = pos;
    }

    /** Déplace le PNJ vers un point (utilisé par Satotz pendant le marathon). */
    public void walkTo(Vec3 target, double speed) {
        getNavigation().moveTo(target.x, target.y, target.z, speed);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer serverPlayer) {
            QuestManager.onTalk(serverPlayer, this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("npc", npcId());
        tag.put("home", NbtUtils.writeBlockPos(home));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("npc")) setNpcId(tag.getString("npc"));
        NbtUtils.readBlockPos(tag, "home").ifPresent(p -> home = p);
    }
}
