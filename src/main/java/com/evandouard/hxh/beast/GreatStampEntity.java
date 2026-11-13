package com.evandouard.hxh.beast;

import com.evandouard.hxh.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Great Stamp (cochon géant de la forêt de Biska, épreuve de Buhara) : il charge et projette comme un hoglin, mais
 * son museau est blindé : les coups portés de face ne font que {@link #FRONT_FACTOR} des dégâts. Il faut le frapper
 * sur le côté, de dos, ou sur le crâne (d'en haut). Ne se transforme jamais en zoglin. Modèle provisoire (hoglin
 * agrandi) en attendant le modèle Blockbench ; stats à revoir à l'équilibrage final.
 */
public class GreatStampEntity extends Hoglin {
    /** Part des dégâts qui passe le museau blindé. */
    public static final float FRONT_FACTOR = 0.3f;

    public GreatStampEntity(EntityType<? extends Hoglin> type, Level level) {
        super(type, level);
        setImmuneToZombification(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50)
                .add(Attributes.ATTACK_DAMAGE, 7)
                .add(Attributes.ATTACK_KNOCKBACK, 1.2)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
                .add(Attributes.FOLLOW_RANGE, 20);
    }

    /** Le coup vient-il de face (moins de 50° du museau) et pas d'en haut ? */
    public boolean hitOnSnout(@Nullable Entity attacker) {
        if (attacker == null) return false;
        Vec3 look = Vec3.directionFromRotation(0, getYRot());
        Vec3 to = attacker.position().subtract(position());
        if (attacker.getY() > getY() + getBbHeight() + 0.4) return false; // frappé sur le crâne
        Vec3 flat = to.multiply(1, 0, 1);
        return flat.lengthSqr() > 1e-4 && look.dot(flat.normalize()) > 0.64;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (hitOnSnout(source.getEntity())) amount *= FRONT_FACTOR;
        return super.hurt(source, amount);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        GreatStampEntity baby = ModEntities.GREAT_STAMP.get().create(level);
        if (baby != null) baby.setPersistenceRequired();
        return baby;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
