package com.evandouard.hxh.registry;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.npc.NpcEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, HxHMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<NpcEntity>> NPC = ENTITIES.register("npc",
            () -> EntityType.Builder.of(NpcEntity::new, MobCategory.MISC).sized(0.6f, 1.8f).clientTrackingRange(10).build("npc"));

    /** Projectiles de Nen (boule de la Pierre de Gon, lame des Ciseaux…). */
    public static final DeferredHolder<EntityType<?>, EntityType<com.evandouard.hxh.hatsu.NenProjectile>> NEN_PROJECTILE =
            ENTITIES.register("nen_projectile", () -> EntityType.Builder.<com.evandouard.hxh.hatsu.NenProjectile>of(
                            com.evandouard.hxh.hatsu.NenProjectile::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(0.6f, 0.6f).clientTrackingRange(10).updateInterval(1).noSave().build("nen_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.evandouard.hxh.arena.ArenaFighterEntity>> ARENA_FIGHTER =
            ENTITIES.register("arena_fighter", () -> EntityType.Builder.of(com.evandouard.hxh.arena.ArenaFighterEntity::new,
                    MobCategory.MONSTER).sized(0.6f, 1.8f).clientTrackingRange(10).build("arena_fighter"));

    private ModEntities() {}

    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(NPC.get(), NpcEntity.createAttributes().build());
        event.put(ARENA_FIGHTER.get(), com.evandouard.hxh.arena.ArenaFighterEntity.createAttributes().build());
    }
}
