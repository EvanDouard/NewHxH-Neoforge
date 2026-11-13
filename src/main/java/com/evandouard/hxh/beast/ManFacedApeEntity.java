package com.evandouard.hxh.beast;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Singe à visage humain (marais de Numele) : il imite la voix des gens pour attirer ses proies dans le marais, comme
 * le faux Satotz de l'Examen. De temps en temps, un joueur proche entend « l'examinateur » l'appeler dans le chat.
 * Ne brûle pas au soleil. Modèle provisoire (corps humanoïde, texture de momie) en attendant le modèle Blockbench.
 */
public class ManFacedApeEntity extends Zombie {
    /** Portée et délai moyen (ticks) de ses appels trompeurs. */
    public static final double LURE_RANGE = 24;
    public static final int LURE_EVERY = 600;
    private static final int LINES = 4;

    public ManFacedApeEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24)
                .add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.ARMOR, 2)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0);
    }

    public static boolean canSpawn(EntityType<ManFacedApeEntity> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos,
                                   RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && checkMobSpawnRules(type, level, reason, pos, random);
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    public boolean isBaby() {
        return false;
    }

    /** Une réplique trompeuse (tirée au hasard), comme si un humain appelait à l'aide ou montrait le chemin. */
    public static Component lureLine(RandomSource random) {
        return Component.translatable("dialog.hxh.man_faced_ape.lure_" + random.nextInt(LINES));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel) || getTarget() != null || random.nextInt(LURE_EVERY) != 0) return;
        Player near = level().getNearestPlayer(this, LURE_RANGE);
        if (near == null || near.isCreative() || near.isSpectator()) return;
        near.sendSystemMessage(Component.translatable("dialog.hxh.format",
                Component.translatable("entity.hxh.man_faced_ape.voice").withStyle(ChatFormatting.YELLOW), lureLine(random)));
    }
}
