package com.evandouard.hxh.hatsu;

import com.evandouard.hxh.data.NenType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.evandouard.hxh.data.NenType.*;

/**
 * Hatsu de chaque espèce de mob (ceux qui n'incarnent pas un personnage). Pas proposés au joueur : un mob qui
 * s'éveille au Nen sans kit reçoit celui de son espèce.
 */
public final class MobKits {
    private MobKits() {}

    private static final Map<String, HatsuKit> BY_ID = new LinkedHashMap<>();
    private static final Map<EntityType<?>, String> BY_TYPE = new LinkedHashMap<>();

    private static void kit(String id, NenType type, Technique technique, EntityType<?>... types) {
        BY_ID.put(id, new HatsuKit(id, type, true, List.of(technique.id()), List.of(), true));
        for (EntityType<?> t : types) BY_TYPE.put(t, id);
    }

    /** Kit nommé (combattants de la Tour…) : plusieurs techniques, pas d'espèce associée, jamais proposé au joueur. */
    static void named(String id, NenType type, Technique... techniques) {
        BY_ID.put(id, new HatsuKit(id, type, true, java.util.Arrays.stream(techniques).map(Technique::id).toList(), List.of(), true));
    }

    static {
        MobTechniques.init();
        ArenaChallengers.init();
        kit("mob_skeleton", EMITTER, MobTechniques.NEN_ARROWS, EntityType.SKELETON, EntityType.STRAY, EntityType.BOGGED);
        kit("mob_creeper", EMITTER, MobTechniques.AURA_MINES, EntityType.CREEPER);
        kit("mob_zombie", CONJURER, MobTechniques.NEN_HORDE, EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER);
        kit("mob_spider", MANIPULATOR, MobTechniques.AURA_WEB, EntityType.SPIDER, EntityType.CAVE_SPIDER);
        kit("mob_enderman", SPECIALIST, MobTechniques.BLINK_STRIKE, EntityType.ENDERMAN);
        kit("mob_pig", ENHANCER, MobTechniques.BOAR_CHARGE, EntityType.PIG, EntityType.HOGLIN);
        kit("mob_cow", ENHANCER, MobTechniques.HEALING_MILK, EntityType.COW, EntityType.MOOSHROOM, EntityType.GOAT);
        kit("mob_sheep", CONJURER, MobTechniques.WOOL_SHIELD, EntityType.SHEEP, EntityType.LLAMA);
        kit("mob_chicken", EMITTER, MobTechniques.EGG_BARRAGE, EntityType.CHICKEN);
        kit("mob_wolf", ENHANCER, MobTechniques.FANG_LUNGE, EntityType.WOLF, EntityType.FOX, EntityType.CAT, EntityType.OCELOT);
        kit("mob_slime", ENHANCER, MobTechniques.BOUNCE_SLAM, EntityType.SLIME, EntityType.MAGMA_CUBE, EntityType.FROG);
        kit("mob_witch", TRANSMUTER, MobTechniques.NEN_BREW, EntityType.WITCH, EntityType.EVOKER);
        kit("mob_golem", ENHANCER, MobTechniques.QUAKE_STOMP, EntityType.IRON_GOLEM, EntityType.RAVAGER);
        kit("mob_blaze", TRANSMUTER, MobTechniques.FIRE_RAIN, EntityType.BLAZE, EntityType.GHAST);
        kit("mob_guardian", EMITTER, MobTechniques.GUARDIAN_BEAM, EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN);
        kit("mob_shulker", SPECIALIST, MobTechniques.SHULKER_BOLT, EntityType.SHULKER);
        kit("mob_phantom", SPECIALIST, MobTechniques.PHANTOM_DIVE, EntityType.PHANTOM);
        kit("mob_bee", ENHANCER, MobTechniques.BEE_STING, EntityType.BEE);
        kit("mob_pillager", EMITTER, MobTechniques.PILLAGER_VOLLEY, EntityType.PILLAGER);
        kit("mob_piglin", ENHANCER, MobTechniques.PIGLIN_GREED, EntityType.PIGLIN, EntityType.PIGLIN_BRUTE, EntityType.ZOMBIFIED_PIGLIN);
        kit("mob_vex", SPECIALIST, MobTechniques.VEX_BLADE, EntityType.VEX);
        kit("mob_warden", EMITTER, MobTechniques.WARDEN_SONIC, EntityType.WARDEN);
        // Le Warden a deux Hatsu : Rugissement dévastateur (Émission) et Cœur du séisme (Renforcement)
        BY_ID.put("mob_warden", new HatsuKit("mob_warden", EMITTER, true, List.of(MobTechniques.WARDEN_SONIC.id(), MobTechniques.WARDEN_QUAKE.id()), List.of(), true));
        kit("mob_wither", SPECIALIST, MobTechniques.WITHER_DECAY, EntityType.WITHER, EntityType.WITHER_SKELETON);
        kit("mob_dragon", SPECIALIST, MobTechniques.DRAGON_BREATH, EntityType.ENDER_DRAGON);
        kit("mob_turtle", ENHANCER, MobTechniques.TURTLE_SHELL, EntityType.TURTLE);
        kit("mob_dolphin", TRANSMUTER, MobTechniques.DOLPHIN_GRACE, EntityType.DOLPHIN);
        kit("mob_panda", ENHANCER, MobTechniques.PANDA_ROLL, EntityType.PANDA);
        kit("mob_squid", TRANSMUTER, MobTechniques.INK_CLOUD, EntityType.SQUID, EntityType.GLOW_SQUID);
        kit("mob_silverfish", ENHANCER, MobTechniques.SILVERFISH_FRENZY, EntityType.SILVERFISH, EntityType.ENDERMITE);
        kit("mob_bat", EMITTER, MobTechniques.BAT_SCREECH, EntityType.BAT);
    }

    @Nullable
    public static HatsuKit get(String id) {
        return BY_ID.get(id);
    }

    /** Kit de l'espèce de ce mob, ou null s'il n'en a pas. */
    @Nullable
    public static String idFor(LivingEntity e) {
        return BY_TYPE.get(e.getType());
    }

    public static List<HatsuKit> all() {
        return List.copyOf(BY_ID.values());
    }

    /** Pour les tests et le journal : identifiant de l'espèce. */
    static String typeName(EntityType<?> t) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(t).toString();
    }
}
