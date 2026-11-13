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

    /** Candidats humains de l'Examen (Zevil, 288e Examen, tournoi, prisonniers de la Tour). */
    public static final DeferredHolder<EntityType<?>, EntityType<com.evandouard.hxh.exam.ExamCandidate>> EXAM_CANDIDATE =
            ENTITIES.register("exam_candidate", () -> EntityType.Builder.of(com.evandouard.hxh.exam.ExamCandidate::new,
                    MobCategory.MONSTER).sized(0.6f, 1.8f).clientTrackingRange(10).build("exam_candidate"));

    /** Renard-ours : prédateur des forêts (bestiaire). */
    public static final DeferredHolder<EntityType<?>, EntityType<com.evandouard.hxh.beast.FoxbearEntity>> FOXBEAR =
            ENTITIES.register("foxbear", () -> EntityType.Builder.of(com.evandouard.hxh.beast.FoxbearEntity::new,
                    MobCategory.CREATURE).sized(1.8f, 1.9f).clientTrackingRange(10).build("foxbear"));

    /** Great Stamp : cochon géant de la forêt de Biska (bestiaire). */
    public static final DeferredHolder<EntityType<?>, EntityType<com.evandouard.hxh.beast.GreatStampEntity>> GREAT_STAMP =
            ENTITIES.register("great_stamp", () -> EntityType.Builder.of(com.evandouard.hxh.beast.GreatStampEntity::new,
                    MobCategory.CREATURE).sized(1.9f, 1.9f).clientTrackingRange(10).build("great_stamp"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.evandouard.hxh.beast.SpiderEagleEntity>> SPIDER_EAGLE =
            ENTITIES.register("spider_eagle", () -> EntityType.Builder.of(com.evandouard.hxh.beast.SpiderEagleEntity::new,
                    MobCategory.CREATURE).sized(1.2f, 2.0f).clientTrackingRange(10).build("spider_eagle"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.evandouard.hxh.beast.ManFacedApeEntity>> MAN_FACED_APE =
            ENTITIES.register("man_faced_ape", () -> EntityType.Builder.of(com.evandouard.hxh.beast.ManFacedApeEntity::new,
                    MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(8).build("man_faced_ape"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.evandouard.hxh.beast.ChimeraAntEntity>> CHIMERA_ANT =
            ENTITIES.register("chimera_ant", () -> EntityType.Builder.of(com.evandouard.hxh.beast.ChimeraAntEntity::new,
                    MobCategory.MONSTER).sized(2.0f, 1.3f).clientTrackingRange(8).build("chimera_ant"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.evandouard.hxh.beast.KirikoEntity>> KIRIKO =
            ENTITIES.register("kiriko", () -> EntityType.Builder.of(com.evandouard.hxh.beast.KirikoEntity::new,
                    MobCategory.CREATURE).sized(1.4f, 2.4f).clientTrackingRange(10).build("kiriko"));

    private ModEntities() {}

    public static void onSpawnPlacements(net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event) {
        event.register(FOXBEAR.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.evandouard.hxh.beast.FoxbearEntity::canSpawn,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(SPIDER_EAGLE.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                com.evandouard.hxh.beast.SpiderEagleEntity::canSpawn,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(MAN_FACED_APE.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.evandouard.hxh.beast.ManFacedApeEntity::canSpawn,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(CHIMERA_ANT.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.evandouard.hxh.beast.ChimeraAntEntity::canSpawn,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GREAT_STAMP.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> net.minecraft.world.entity.animal.Animal.checkAnimalSpawnRules(
                        net.minecraft.world.entity.EntityType.PIG, level, reason, pos, random),
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(NPC.get(), NpcEntity.createAttributes().build());
        event.put(ARENA_FIGHTER.get(), com.evandouard.hxh.arena.ArenaFighterEntity.createAttributes().build());
        event.put(EXAM_CANDIDATE.get(), com.evandouard.hxh.exam.ExamCandidate.createAttributes().build());
        event.put(FOXBEAR.get(), com.evandouard.hxh.beast.FoxbearEntity.createAttributes().build());
        event.put(GREAT_STAMP.get(), com.evandouard.hxh.beast.GreatStampEntity.createAttributes().build());
        event.put(SPIDER_EAGLE.get(), com.evandouard.hxh.beast.SpiderEagleEntity.createAttributes().build());
        event.put(KIRIKO.get(), com.evandouard.hxh.beast.KirikoEntity.createAttributes().build());
        event.put(MAN_FACED_APE.get(), com.evandouard.hxh.beast.ManFacedApeEntity.createAttributes().build());
        event.put(CHIMERA_ANT.get(), com.evandouard.hxh.beast.ChimeraAntEntity.createAttributes().build());
    }
}
