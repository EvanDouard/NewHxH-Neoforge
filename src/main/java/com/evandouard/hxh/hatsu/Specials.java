package com.evandouard.hxh.hatsu;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.NenMode;
import com.evandouard.hxh.fx.NenFx;
import com.evandouard.hxh.hatsu.Techniques.Effect;
import com.evandouard.hxh.hatsu.Techniques.Look;
import com.evandouard.hxh.hatsu.Techniques.Style;
import com.evandouard.hxh.nen.Targeting;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.evandouard.hxh.data.NenType.*;
import static com.evandouard.hxh.hatsu.Techniques.register;

/**
 * Personnages à mécanique propre (fiche docs/kits-personnages.md, retours d'Evan) : Biscuit (deux formes), Chrollo
 * (livre volé), Feitan (jauge de Douleur), Phinks (tours de bras limités par le Nen), Shalnark (contrôle limité par
 * l'écart de Nen), Knov (portes marquées, refuge), Knuckle (Hakoware), Shaiapouf (vol).
 */
public final class Specials {
    private Specials() {}

    static HunterData data(LivingEntity e) {
        return HunterStats.get(e);
    }

    // =====================================================================
    // Modificateurs d'attributs liés à un personnage (retirés quand on en change)
    // =====================================================================

    /** Modificateur posé tant que le porteur a ce personnage (taille de Biscuit, PV de Youpi, recul de Botobai…). */
    record KitModifier(String kit, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                       ResourceLocation id) {}

    static final List<KitModifier> KIT_MODIFIERS = List.of(
            new KitModifier("biscuit", Attributes.SCALE, HxHMod.id("biscuit_form")),
            new KitModifier("youpi", Attributes.MAX_HEALTH, Characters.YOUPI_HP),
            new KitModifier("botobai", Attributes.KNOCKBACK_RESISTANCE, Characters.VETERAN_KB));

    /** Retire les modificateurs des personnages que l'entité n'a plus ; coupe le vol de Shaiapouf. */
    public static void cleanup(LivingEntity e) {
        if (e.tickCount % 20 != 0) return;
        String kit = data(e).kit();
        for (KitModifier m : KIT_MODIFIERS) {
            if (m.kit().equals(kit)) continue;
            var inst = e.getAttribute(m.attribute());
            if (inst != null && inst.getModifier(m.id()) != null) inst.removeModifier(m.id());
        }
        if (!"shaiapouf".equals(kit) && e instanceof ServerPlayer p && p.getPersistentData().getBoolean(POUF_FLIGHT)) stopFlight(p);
    }

    static void setModifier(LivingEntity e, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr,
                            ResourceLocation id, double value, AttributeModifier.Operation op) {
        var inst = e.getAttribute(attr);
        if (inst == null) return;
        AttributeModifier cur = inst.getModifier(id);
        if (cur != null && cur.amount() == value) return;
        if (cur != null) inst.removeModifier(id);
        inst.addTransientModifier(new AttributeModifier(id, value, op));
    }

    // =====================================================================
    // Biscuit (Transformation) — deux formes
    // =====================================================================
    /** Ressource : 1 = grande forme (2,5 blocs, Force +, aura −), 0 = petite forme (1,5 bloc, aura +, Force −). */
    public static final String BIG = "big_form";
    static final double BIG_SCALE = 2.5 / 1.8 - 1, SMALL_SCALE = 1.5 / 1.8 - 1;

    public static boolean bigForm(LivingEntity e) {
        return data(e).kitValue(BIG) > 0;
    }

    public static final Passive BISCUIT_TWO_FORMS = new Passive("biscuit_two_forms", true) {
        @Override
        public void tick(LivingEntity owner) {
            boolean big = bigForm(owner);
            setModifier(owner, Attributes.SCALE, HxHMod.id("biscuit_form"), big ? BIG_SCALE : SMALL_SCALE,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            if (owner.tickCount % 20 == 0) {
                HunterData d = data(owner);
                // grande forme : l'aura s'use un peu ; petite forme : elle remonte plus vite
                d.setAura(Math.max(0, Math.min(d.auraMax(), d.aura() + (big ? -0.5f : 1f))));
            }
        }

        @Override
        public float modifyOutgoingMelee(LivingEntity owner, LivingEntity target, float amount) {
            return amount * (bigForm(owner) ? 1.5f : 0.8f);
        }
    };

    public static final Technique BISCUIT_SWITCH_FORM = register(new Technique("biscuit_switch_form", TRANSMUTER, 0, 40) {
        @Override
        public boolean perform(LivingEntity caster, float power) {
            boolean big = !bigForm(caster);
            data(caster).setKitValue(BIG, big ? 1 : 0);
            NenFx.flare(caster, big ? 1.8f : 0.9f, big ? 0xFFFF7AA8 : 0xFFFFC0D8);
            NenFx.shockwave(caster, big ? 3f : 1.5f, 0xFFFF7AA8);
            if (caster instanceof Player p) p.displayClientMessage(Component.translatable(big
                    ? "message.hxh.biscuit_big" : "message.hxh.biscuit_small"), true);
            Kinds.sound(caster, big ? SoundEvents.RAVAGER_ROAR : SoundEvents.AMETHYST_BLOCK_CHIME);
            return true;
        }
    });

    /** Cookie apparaît à côté de Biscuit et soigne (elle et ses alliés à 3 blocs). */
    public static final Technique BISCUIT_ESTHETICIAN = register(new Kinds.AllyBuff("biscuit_esthetician", TRANSMUTER, 30, 400,
            3, 8f, List.of(new Effect(MobEffects.REGENERATION, 160, 1), new Effect(MobEffects.SATURATION, 10, 0)),
            new Look(Style.FLARE, 0xFFFFB6D9, 1f), SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT) {
        @Override
        public boolean perform(LivingEntity caster, float power) {
            if (caster.level() instanceof ServerLevel level)
                NenFx.sprite(level, NenFx.Sprite.COOKIE, caster, caster.position(), 1.3f, 0xFFFFB6D9, 80);
            return super.perform(caster, power);
        }
    });

    // =====================================================================
    // Chrollo (Spécialisation) — Tête, livre volé
    // =====================================================================
    static final java.util.Set<String> TROUPE = java.util.Set.of("chrollo", "feitan", "nobunaga", "phinks", "uvogin",
            "franklin", "shalnark", "machi", "shizuku", "pakunoda", "kalluto", "hisoka");
    /** Données persistantes : dernière technique lancée par une entité, et page volée dans le livre de Chrollo. */
    public static final String LAST_TECHNIQUE = "hxh_last_technique", BOOK = "hxh_chrollo_book";

    /** Tête : +5 % de dégâts par membre de la Brigade à 16 blocs (40 % au plus). */
    public static final Passive CHROLLO_HEAD = new Passive("chrollo_head", true) {
        @Override
        public float modifyOutgoingMelee(LivingEntity owner, LivingEntity target, float amount) {
            int n = 0;
            for (LivingEntity e : Targeting.around(owner, 16)) if (TROUPE.contains(data(e).kit()) && e != owner) n++;
            return amount * (1f + Math.min(0.4f, n * 0.05f));
        }
    };

    /** Skill Hunter : livre vide = vole la dernière technique de la cible ; page pleine = la lance une fois. */
    public static final Technique CHROLLO_SKILL_HUNTER = register(new Technique("chrollo_skill_hunter", SPECIALIST, 30, 200) {
        @Override
        public Component conditions() {
            return Component.translatable("technique.hxh.cond.steal", 10);
        }

        @Override
        public double aiRange() {
            return 10;
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            if (caster.level() instanceof ServerLevel lv) NenFx.sprite(lv, NenFx.Sprite.BOOK, caster, caster.position(), 1.1f, NenFx.DARK, 30);
            String page = caster.getPersistentData().getString(BOOK);
            Technique stolen = page.isEmpty() ? null : Techniques.get(page);
            if (stolen != null) {
                caster.getPersistentData().remove(BOOK);
                NenFx.flare(caster, 1f, NenFx.DARK);
                if (caster instanceof Player p) p.displayClientMessage(Component.translatable("message.hxh.chrollo_use",
                        Component.translatable(stolen.translationKey())), true);
                return stolen.perform(caster, power);
            }
            LivingEntity t = Targeting.inFront(caster, 10, 0.92);
            String id = t == null ? "" : t.getPersistentData().getString(LAST_TECHNIQUE);
            Technique take = Techniques.get(id);
            if (take == null || !take.implemented() || take.isCharged() || take.needsNoNen() || id.startsWith("chrollo_")) {
                if (caster instanceof Player p) p.displayClientMessage(Component.translatable("message.hxh.chrollo_nothing"), true);
                return false;
            }
            caster.getPersistentData().putString(BOOK, id);
            NenFx.impactOn(caster, t, 1f, NenFx.DARK);
            NenFx.flare(caster, 1.2f, NenFx.DARK);
            if (caster instanceof Player p) p.displayClientMessage(Component.translatable("message.hxh.chrollo_stole",
                    Component.translatable(take.translationKey())).withStyle(ChatFormatting.DARK_PURPLE), true);
            Kinds.sound(caster, SoundEvents.BOOK_PAGE_TURN);
            return true;
        }
    }).requiresNenLevel(20);

    // =====================================================================
    // Feitan (Transformation) — Douleur
    // =====================================================================
    /** Douleur : chaque PV perdu remplit la jauge (×5) ; elle redescend de 2/s hors combat ; jusqu'à +50 % de dégâts. */
    public static final Passive FEITAN_PAIN = new Passive("feitan_pain", true) {
        @Override
        public void tick(LivingEntity owner) {
            if (owner.tickCount % 20 == 0 && owner.tickCount - owner.getLastHurtByMobTimestamp() > 100)
                data(owner).addKitValue(KitBars.PAIN, -2, KitBars.PAIN_MAX);
        }

        @Override
        public float modifyIncoming(LivingEntity owner, DamageSource source, float amount) {
            data(owner).addKitValue(KitBars.PAIN, amount * 5, KitBars.PAIN_MAX);
            return amount;
        }

        @Override
        public float modifyOutgoingMelee(LivingEntity owner, LivingEntity target, float amount) {
            return amount * (1f + data(owner).kitValue(KitBars.PAIN) / KitBars.PAIN_MAX * 0.5f);
        }
    };

    /** Soleil levant : Douleur pleine : un soleil de feu brûle tout à 8 blocs (16 dégâts, en feu 6 s). */
    public static final Technique FEITAN_RISING_SUN = register(new Technique("feitan_rising_sun", TRANSMUTER, 40, 600) {
        @Override
        public Component conditions() {
            return Component.translatable("technique.hxh.cond.area", "8");
        }

        @Override
        public double aiRange() {
            return 8;
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            if (!(caster.level() instanceof ServerLevel level)) return false;
            NenFx.sprite(level, NenFx.Sprite.SUN, caster, caster.position(), 3f, 0xFFFF6A1E, 60);
            NenFx.flare(caster, 2f, 0xFFFF6A1E);
            for (int i = 0; i < 3; i++) {
                NenTasks.later(i * 15, () -> {
                    NenFx.shockwave(caster, 8f, 0xFFFF6A1E);
                    for (LivingEntity t : Kinds.enemiesAround(caster, caster.position().add(0, 1, 0), 8)) {
                        Kinds.hurt(caster, t, 16f / 3 * power);
                        t.igniteForSeconds(6);
                    }
                });
            }
            caster.addEffect(Kinds.quiet(MobEffects.FIRE_RESISTANCE, 100, 0), caster);
            Kinds.sound(caster, SoundEvents.BLAZE_SHOOT);
            return true;
        }
    }).costs(KitBars.PAIN, KitBars.PAIN_MAX).requiresNenLevel(20);

    // =====================================================================
    // Phinks (Renforcement) — Ripper Cyclotron
    // =====================================================================
    public static final String TURNS = "turns";

    /** Tours de bras au plus : 1 par tranche de 10 niveaux de Nen, de 1 à 10. */
    public static int maxTurns(LivingEntity e) {
        return Math.max(1, Math.min(10, data(e).nenLevel() / 10));
    }

    /** Bagarreur : le prochain coup de poing libère les tours accumulés (+2,5 dégâts par tour). */
    public static final Passive PHINKS_BRAWLER = new Passive("phinks_brawler", true) {
        @Override
        public void tick(LivingEntity owner) {
            // les tours se perdent au bout de 10 s sans frapper
            if (owner.tickCount % 200 == 0 && data(owner).kitValue(TURNS) > 0
                    && owner.tickCount - owner.getLastHurtMobTimestamp() > 200) data(owner).setKitValue(TURNS, 0);
        }

        @Override
        public float modifyOutgoingMelee(LivingEntity owner, LivingEntity target, float amount) {
            int turns = (int) data(owner).kitValue(TURNS);
            if (turns <= 0) return amount;
            data(owner).setKitValue(TURNS, 0);
            NenFx.impactOn(owner, target, 1f + turns * 0.25f, NenFx.ENHANCER);
            if (turns >= 5) NenFx.impactFrame(owner, target, 10);
            target.knockback(0.3 * turns, owner.getX() - target.getX(), owner.getZ() - target.getZ());
            return amount + turns * 2.5f;
        }
    };

    /** Ripper Cyclotron : un tour de bras de plus (jusqu'au maximum permis par le Nen). */
    public static final Technique PHINKS_RIPPER_CYCLOTRON = register(new Technique("phinks_ripper_cyclotron", ENHANCER, 4, 6) {
        @Override
        public Component conditions() {
            return Component.translatable("technique.hxh.cond.turns");
        }

        @Override
        public double aiRange() {
            return 3;
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            HunterData d = data(caster);
            int max = maxTurns(caster);
            int turns = (int) d.kitValue(TURNS);
            if (turns >= max) {
                if (caster instanceof Player p) p.displayClientMessage(Component.translatable("message.hxh.phinks_max", max), true);
                return false;
            }
            d.setKitValue(TURNS, turns + 1);
            NenFx.slash(caster, caster.getEyePosition().add(caster.getLookAngle().scale(0.6)).add(0, -0.4, 0), 0.6f + 0.1f * turns, NenFx.ENHANCER);
            if (caster instanceof Player p) p.displayClientMessage(Component.translatable("message.hxh.phinks_turn", turns + 1, max), true);
            Kinds.sound(caster, SoundEvents.PLAYER_ATTACK_SWEEP);
            return true;
        }
    });

    // =====================================================================
    // Shalnark (Manipulation) — Voix noire, pilote automatique
    // =====================================================================
    public static final String CONTROLLED_BY = "hxh_controlled_by", CONTROLLED_UNTIL = "hxh_controlled_until";
    /** Écart de niveau de Nen au-delà duquel l'antenne ne prend pas. */
    public static final int CONTROL_GAP = 10;

    /** Technicien : un mob vaincu lâche parfois un objet de plus (une chance sur quatre). */
    public static final Passive SHALNARK_TECH = passive("shalnark_tech");

    private static Passive passive(String id) {
        return new Passive(id, true);
    }

    public static final Technique SHALNARK_BLACK_VOICE = register(new Technique("shalnark_black_voice", MANIPULATOR, 35, 400) {
        @Override
        public Component conditions() {
            return Component.translatable("technique.hxh.cond.control", 8, CONTROL_GAP);
        }

        @Override
        public double aiRange() {
            return 8;
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            LivingEntity t = Targeting.inFront(caster, 8, 0.92);
            if (!(t instanceof Mob mob) || t instanceof Player) {
                if (caster instanceof Player p) p.displayClientMessage(Component.translatable("message.hxh.shalnark_no_target"), true);
                return false;
            }
            if (!canControl(caster, t)) {
                NenFx.impactOn(caster, t, 0.6f, 0xFF333333);
                if (caster instanceof Player p) p.displayClientMessage(Component.translatable("message.hxh.shalnark_too_strong",
                        data(t).nenLevel(), data(caster).nenLevel()).withStyle(ChatFormatting.RED), true);
                return false;
            }
            mob.getPersistentData().putUUID(CONTROLLED_BY, caster.getUUID());
            mob.getPersistentData().putLong(CONTROLLED_UNTIL, caster.level().getGameTime() + Math.round(600 * power));
            mob.setTarget(null);
            NenFx.needle(caster, t, t.getBoundingBox().getCenter(), 600);
            NenFx.flare(t, 0.8f, 0xFF222222);
            Kinds.sound(t, SoundEvents.ARROW_HIT);
            return true;
        }
    }).requiresNenLevel(10);

    /** L'antenne prend-elle ? Pas si la cible a plus de {@link #CONTROL_GAP} niveaux de Nen de plus que Shalnark. */
    public static boolean canControl(LivingEntity shalnark, LivingEntity target) {
        return data(target).nenLevel() <= data(shalnark).nenLevel() + CONTROL_GAP;
    }

    /** Chaque seconde, un mob contrôlé attaque le plus proche ennemi de son maître. */
    public static void tickControlled(Mob mob) {
        var tag = mob.getPersistentData();
        if (!tag.hasUUID(CONTROLLED_BY) || mob.tickCount % 20 != 0 || !(mob.level() instanceof ServerLevel level)) return;
        if (tag.getLong(CONTROLLED_UNTIL) < level.getGameTime()) {
            tag.remove(CONTROLLED_BY);
            tag.remove(CONTROLLED_UNTIL);
            mob.setTarget(null);
            return;
        }
        UUID master = tag.getUUID(CONTROLLED_BY);
        var m = level.getEntity(master);
        LivingEntity best = null;
        double bd = 16 * 16;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(16), e -> e.isAlive() && e != mob)) {
            if (e.getUUID().equals(master) || (m instanceof LivingEntity ml && Kinds.isAlly(ml, e))) continue;
            if (!(e instanceof net.minecraft.world.entity.monster.Enemy)) continue;
            double d = e.distanceToSqr(mob);
            if (d < bd) {
                bd = d;
                best = e;
            }
        }
        mob.setTarget(best);
    }

    /** Un mob contrôlé ne prend jamais pour cible son maître ni ses alliés. */
    public static boolean forbiddenTarget(Mob mob, LivingEntity target) {
        var tag = mob.getPersistentData();
        if (!tag.hasUUID(CONTROLLED_BY) || target == null) return false;
        if (target.getUUID().equals(tag.getUUID(CONTROLLED_BY))) return true;
        var m = mob.level() instanceof ServerLevel l ? l.getEntity(tag.getUUID(CONTROLLED_BY)) : null;
        return m instanceof LivingEntity ml && Kinds.isAlly(ml, target);
    }

    /** Pilote automatique : 15 s surpuissant (Force II, vitesse II, Résistance), puis 10 s épuisé. */
    public static final Technique SHALNARK_AUTOPILOT = register(new Technique("shalnark_autopilot", MANIPULATOR, 45, 900) {
        @Override
        public boolean perform(LivingEntity caster, float power) {
            caster.addEffect(Kinds.quiet(MobEffects.DAMAGE_BOOST, 300, 1), caster);
            caster.addEffect(Kinds.quiet(MobEffects.MOVEMENT_SPEED, 300, 1), caster);
            caster.addEffect(Kinds.quiet(MobEffects.DAMAGE_RESISTANCE, 300, 0), caster);
            NenFx.flare(caster, 1.5f, 0xFF222222);
            NenFx.needleCrown(caster, 0xFF222222, 300);
            NenTasks.later(300, () -> {
                if (!caster.isAlive()) return;
                caster.addEffect(Kinds.quiet(MobEffects.WEAKNESS, 200, 1), caster);
                caster.addEffect(Kinds.quiet(MobEffects.MOVEMENT_SLOWDOWN, 200, 1), caster);
                if (caster instanceof Player p) p.displayClientMessage(Component.translatable("message.hxh.shalnark_exhausted"), true);
            });
            Kinds.sound(caster, SoundEvents.ZOMBIE_VILLAGER_CONVERTED);
            return true;
        }
    }).requiresNenLevel(30);

    // =====================================================================
    // Knov (Émission) — portes marquées, refuge
    // =====================================================================
    public static final ResourceKey<Level> REFUGE = ResourceKey.create(Registries.DIMENSION, HxHMod.id("refuge"));
    /** Données persistantes du joueur : portes marquées (liste), retour (dimension + position). */
    static final String DOORS = "hxh_knov_doors", RETURN_DIM = "hxh_knov_return_dim", RETURN_POS = "hxh_knov_return_pos";

    /** Portes marquables : 1, plus 1 par tranche de 20 niveaux de Nen (6 au plus). */
    public static int maxDoors(LivingEntity e) {
        return Math.min(6, 1 + data(e).nenLevel() / 20);
    }

    public static List<CompoundTag> doors(LivingEntity e) {
        List<CompoundTag> out = new ArrayList<>();
        for (Tag t : e.getPersistentData().getList(DOORS, Tag.TAG_COMPOUND)) out.add((CompoundTag) t);
        return out;
    }

    /** Stratège : prévenu quand un monstre le prend pour cible ; voit ses marques au Gyo. */
    public static final Passive KNOV_STRATEGIST = new Passive("knov_strategist", true) {
        @Override
        public void tick(LivingEntity owner) {
            if (!(owner instanceof ServerPlayer p) || owner.tickCount % 20 != 0) return;
            if (com.evandouard.hxh.nen.NenSenses.gyoLevel(owner) >= com.evandouard.hxh.nen.NenSenses.GYO_SEE_TRAPS) {
                for (CompoundTag d : doors(owner)) {
                    if (!d.getString("dim").equals(owner.level().dimension().location().toString())) continue;
                    BlockPos pos = BlockPos.of(d.getLong("pos"));
                    if (pos.distSqr(owner.blockPosition()) > 48 * 48) continue;
                    NenFx.Payload fx = new NenFx.Payload(NenFx.Kind.SPRITE.ordinal(), -1, NenFx.Sprite.DOOR_MARK.ordinal(),
                            pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0.9f, NenFx.EMITTER, 24);
                    com.evandouard.hxh.network.HxHNetwork.sendTo(p, fx);
                }
            }
            for (Mob m : owner.level().getEntitiesOfClass(Mob.class, owner.getBoundingBox().inflate(16), m -> m.getTarget() == owner)) {
                p.displayClientMessage(Component.translatable("message.hxh.knov_danger", m.getDisplayName()).withStyle(ChatFormatting.YELLOW), true);
                break;
            }
        }
    };

    /** Marquer une porte : la porte visée (5 blocs) reçoit une marque, visible au Gyo ; la plus ancienne saute au-delà du maximum. */
    public static final Technique KNOV_MARK_DOOR = register(new Technique("knov_mark_door", EMITTER, 10, 40) {
        @Override
        public Component conditions() {
            return Component.translatable("technique.hxh.cond.door");
        }

        @Override
        public double aiRange() {
            return 0; // pas pour l'IA
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            HitResult hit = caster.pick(5, 0, false);
            if (!(hit instanceof BlockHitResult bh) || hit.getType() != HitResult.Type.BLOCK
                    || !caster.level().getBlockState(bh.getBlockPos()).is(BlockTags.DOORS)) {
                if (caster instanceof Player p) p.displayClientMessage(Component.translatable("message.hxh.knov_no_door"), true);
                return false;
            }
            BlockPos pos = bh.getBlockPos();
            if (caster.level().getBlockState(pos.below()).is(BlockTags.DOORS)) pos = pos.below();
            ListTag list = caster.getPersistentData().getList(DOORS, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) if (list.getCompound(i).getLong("pos") == pos.asLong()) list.remove(i--);
            CompoundTag d = new CompoundTag();
            d.putString("dim", caster.level().dimension().location().toString());
            d.putLong("pos", pos.asLong());
            list.add(d);
            while (list.size() > maxDoors(caster)) list.remove(0);
            caster.getPersistentData().put(DOORS, list);
            NenFx.impactAt((ServerLevel) caster.level(), Vec3.atCenterOf(pos), 0.8f, NenFx.EMITTER);
            if (caster instanceof Player p) p.displayClientMessage(Component.translatable("message.hxh.knov_marked",
                    list.size(), maxDoors(caster)), true);
            return true;
        }
    });

    /**
     * Cache-cache : hors du refuge, un portail y mène (abri, régénération) ; dans le refuge, on choisit par où
     * ressortir : une porte marquée (liens cliquables) ou l'endroit d'où l'on vient.
     */
    public static final Technique KNOV_HIDE_AND_SEEK = register(new Technique("knov_hide_and_seek", EMITTER, 30, 200) {
        @Override
        public double aiRange() {
            return 0;
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            if (!(caster instanceof ServerPlayer p)) return false;
            if (p.level().dimension().equals(REFUGE)) {
                offerExits(p);
                return true;
            }
            ServerLevel refuge = p.server.getLevel(REFUGE);
            if (refuge == null) return false;
            p.getPersistentData().putString(RETURN_DIM, p.level().dimension().location().toString());
            p.getPersistentData().putLong(RETURN_POS, p.blockPosition().asLong());
            NenFx.flare(p, 1.2f, NenFx.EMITTER);
            BlockPos room = refugeRoom(refuge, p.getUUID());
            p.teleportTo(refuge, room.getX() + 0.5, room.getY(), room.getZ() + 0.5, p.getYRot(), 0);
            p.addEffect(Kinds.quiet(MobEffects.REGENERATION, 400, 1));
            p.displayClientMessage(Component.translatable("message.hxh.knov_refuge"), false);
            offerExits(p);
            return true;
        }
    }).requiresNenLevel(10);

    /** Pièce-refuge du joueur (posée à la première visite), à un emplacement propre à son identifiant. */
    static BlockPos refugeRoom(ServerLevel refuge, UUID id) {
        int slot = Math.floorMod(id.hashCode(), 1000);
        BlockPos c = new BlockPos(slot * 64, 80, 0);
        if (!refuge.getBlockState(c.below()).is(Blocks.SPRUCE_PLANKS)) {
            for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) for (int y = -1; y <= 4; y++) {
                boolean shell = Math.abs(x) == 4 || Math.abs(z) == 4 || y == -1 || y == 4;
                refuge.setBlock(c.offset(x, y, z), shell ? (y == -1 ? Blocks.SPRUCE_PLANKS.defaultBlockState()
                        : Blocks.WHITE_CONCRETE.defaultBlockState()) : Blocks.AIR.defaultBlockState(), 2);
            }
            refuge.setBlock(c.offset(-3, 0, -3), Blocks.LANTERN.defaultBlockState(), 2);
            refuge.setBlock(c.offset(3, 0, -3), Blocks.LANTERN.defaultBlockState(), 2);
            refuge.setBlock(c.offset(0, 0, -3), Blocks.BOOKSHELF.defaultBlockState(), 2);
        }
        return c;
    }

    static void offerExits(ServerPlayer p) {
        List<CompoundTag> doors = doors(p);
        var line = Component.translatable("message.hxh.knov_exits").withStyle(ChatFormatting.GOLD);
        for (int i = 0; i < doors.size(); i++) {
            BlockPos pos = BlockPos.of(doors.get(i).getLong("pos"));
            final int k = i;
            line.append(Component.literal("  "));
            line.append(Component.literal("[" + (i + 1) + " : " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + "]")
                    .withStyle(st -> st.withColor(ChatFormatting.GREEN).withUnderlined(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hunter knov exit " + (k + 1)))));
        }
        line.append(Component.literal("  "));
        line.append(Component.translatable("message.hxh.knov_exit_back").withStyle(st -> st.withColor(ChatFormatting.AQUA).withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hunter knov exit 0"))));
        p.sendSystemMessage(line);
    }

    /** Sortie du refuge : porte n° {@code n} (1…), ou 0 = d'où l'on vient. */
    public static boolean exitRefuge(ServerPlayer p, int n) {
        if (!p.level().dimension().equals(REFUGE)) return false;
        String dim;
        BlockPos pos;
        List<CompoundTag> doors = doors(p);
        if (n >= 1 && n <= doors.size()) {
            dim = doors.get(n - 1).getString("dim");
            pos = BlockPos.of(doors.get(n - 1).getLong("pos"));
        } else {
            dim = p.getPersistentData().getString(RETURN_DIM);
            pos = BlockPos.of(p.getPersistentData().getLong(RETURN_POS));
        }
        ResourceLocation dl = ResourceLocation.tryParse(dim);
        ServerLevel level = dl == null ? null : p.server.getLevel(ResourceKey.create(Registries.DIMENSION, dl));
        if (level == null) level = p.server.overworld();
        p.teleportTo(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, p.getYRot(), 0);
        NenFx.flare(p, 1.2f, NenFx.EMITTER);
        return true;
    }

    // =====================================================================
    // Knuckle (Matérialisation) — Hakoware
    // =====================================================================
    /** Données persistantes de la cible : son créancier et sa dette ; jusqu'à quand elle est en banqueroute. */
    public static final String HAKO_OWNER = "hxh_hakoware_owner", HAKO_DEBT = "hxh_hakoware_debt", BANKRUPT_UNTIL = "hxh_bankrupt_until";
    /** Ressource de Knuckle : dette de sa cible en % de la limite (jauge). */
    public static final String DEBT = "debt";
    static final float INTEREST = 0.05f, BANKRUPT_SECONDS = 20;

    /** Limite de la dette : l'aura max de la cible (50 pour une cible sans Nen). */
    public static float debtLimit(LivingEntity target) {
        HunterData d = data(target);
        return d.nenUnlocked() ? d.auraMax() : 50f;
    }

    static LivingEntity hakoTarget(LivingEntity knuckle) {
        if (!(knuckle.level() instanceof ServerLevel level)) return null;
        var tag = knuckle.getPersistentData();
        if (!tag.hasUUID(HAKO_OWNER)) return null;
        return level.getEntity(tag.getUUID(HAKO_OWNER)) instanceof LivingEntity l && l.isAlive() ? l : null;
    }

    /** Hakoware : chaque coup de Knuckle sur sa cible lui prête la moitié des dégâts ; intérêts doux ; banqueroute. */
    public static final Passive KNUCKLE_HAKOWARE = new Passive("knuckle_hakoware", true) {
        @Override
        public void tick(LivingEntity owner) {
            if (owner.tickCount % 20 != 0) return;
            LivingEntity t = hakoTarget(owner);
            if (t == null) {
                if (data(owner).kitValue(DEBT) != 0) data(owner).setKitValue(DEBT, 0);
                return;
            }
            float debt = t.getPersistentData().getFloat(HAKO_DEBT);
            // intérêts : +5 % toutes les 10 s, sans jamais dépasser la limite d'eux-mêmes
            if (owner.tickCount % 200 == 0 && debt > 0) debt = Math.min(debtLimit(t) * 0.99f, debt * (1 + INTEREST));
            t.getPersistentData().putFloat(HAKO_DEBT, debt);
            data(owner).setKitValue(DEBT, Math.min(100f, debt / debtLimit(t) * 100f));
            if (owner.level() instanceof ServerLevel level)
                NenFx.sprite(level, NenFx.Sprite.POT_CLEAN, t, t.position(), 0.9f, 0xFFB0E0FF, 22);
            if (owner instanceof Player p) p.displayClientMessage(Component.translatable("message.hxh.hako_status",
                    t.getDisplayName(), Math.round(debt), Math.round(debtLimit(t))), true);
        }

        @Override
        public float modifyOutgoingMelee(LivingEntity owner, LivingEntity target, float amount) {
            LivingEntity t = hakoTarget(owner);
            if (t != target) return amount;
            float debt = target.getPersistentData().getFloat(HAKO_DEBT) + amount * 0.5f;
            target.getPersistentData().putFloat(HAKO_DEBT, debt);
            if (debt >= debtLimit(target)) bankrupt(owner, target);
            return amount;
        }
    };

    /** Banqueroute : Zetsu forcé 20 s, plus aucune protection contre le Nen ; Pot-Clean se retire. */
    public static void bankrupt(LivingEntity knuckle, LivingEntity target) {
        long until = target.level().getGameTime() + Math.round(BANKRUPT_SECONDS * 20);
        target.getPersistentData().putLong(BANKRUPT_UNTIL, until);
        target.getPersistentData().remove(HAKO_DEBT);
        knuckle.getPersistentData().remove(HAKO_OWNER);
        data(knuckle).setKitValue(DEBT, 0);
        HunterData d = data(target);
        if (d.nenUnlocked()) d.setMode(NenMode.ZETSU);
        else {
            target.addEffect(Kinds.quiet(MobEffects.WEAKNESS, Math.round(BANKRUPT_SECONDS * 20), 1));
            target.addEffect(Kinds.quiet(MobEffects.MOVEMENT_SLOWDOWN, Math.round(BANKRUPT_SECONDS * 20), 0));
        }
        if (knuckle.level() instanceof ServerLevel level) {
            NenFx.impactAt(level, target.getBoundingBox().getCenter(), 2f, 0xFFB0E0FF);
            NenFx.sprite(level, NenFx.Sprite.POT_CLEAN, target, target.position(), 1.4f, 0xFFFFFFFF, 40);
        }
        if (knuckle instanceof Player p) p.displayClientMessage(Component.translatable("message.hxh.hako_bankrupt",
                target.getDisplayName()).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
        if (target instanceof Player tp) tp.displayClientMessage(Component.translatable("message.hxh.hako_bankrupt_you")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
    }

    /** En banqueroute : on ne peut pas quitter le Zetsu (vérifié par NenModes). */
    public static boolean bankrupt(LivingEntity e) {
        return e.getPersistentData().getLong(BANKRUPT_UNTIL) > e.level().getGameTime();
    }

    /** Activer Pot-Clean sur la cible devant soi (10 blocs) ; une seule cible à la fois. */
    public static final Technique KNUCKLE_POT_CLEAN = register(new Technique("knuckle_pot_clean", CONJURER, 15, 60) {
        @Override
        public Component conditions() {
            return Component.translatable("technique.hxh.cond.target", "10.0");
        }

        @Override
        public double aiRange() {
            return 10;
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            LivingEntity t = Targeting.inFront(caster, 10, 0.92);
            if (t == null) return false;
            LivingEntity old = hakoTarget(caster);
            if (old != null && old != t) old.getPersistentData().remove(HAKO_DEBT);
            caster.getPersistentData().putUUID(HAKO_OWNER, t.getUUID());
            if (!t.getPersistentData().contains(HAKO_DEBT)) t.getPersistentData().putFloat(HAKO_DEBT, 0f);
            NenFx.impactOn(caster, t, 0.8f, 0xFFB0E0FF);
            if (caster.level() instanceof ServerLevel level)
                NenFx.sprite(level, NenFx.Sprite.POT_CLEAN, t, t.position(), 1.1f, 0xFFB0E0FF, 30);
            Kinds.sound(t, SoundEvents.CHICKEN_EGG);
            return true;
        }
    });

    /** Annuler Pot-Clean : la dette est effacée. */
    public static final Technique KNUCKLE_CANCEL = register(new Technique("knuckle_cancel", CONJURER, 0, 20) {
        @Override
        public double aiRange() {
            return 0;
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            LivingEntity t = hakoTarget(caster);
            if (t != null) t.getPersistentData().remove(HAKO_DEBT);
            caster.getPersistentData().remove(HAKO_OWNER);
            data(caster).setKitValue(DEBT, 0);
            return t != null;
        }
    });

    // =====================================================================
    // Shaiapouf (Manipulation) — vol
    // =====================================================================
    static final String POUF_FLIGHT = "hxh_pouf_flight";

    /** Ailes : il vole librement (hors Zetsu) ; les ailes de papillon se dessinent dans son dos en vol. */
    public static final Passive POUF_WINGS = new Passive("pouf_wings", true) {
        @Override
        public void tick(LivingEntity owner) {
            if (owner instanceof ServerPlayer p) {
                boolean allowed = data(p).mode() != NenMode.ZETSU && !p.isCreative() && !p.isSpectator();
                if (allowed && !p.getAbilities().mayfly) {
                    p.getAbilities().mayfly = true;
                    p.getPersistentData().putBoolean(POUF_FLIGHT, true);
                    p.onUpdateAbilities();
                } else if (!allowed && p.getPersistentData().getBoolean(POUF_FLIGHT)) stopFlight(p);
                if (p.getAbilities().flying && owner.tickCount % 20 == 0)
                    NenFx.sprite(p.serverLevel(), NenFx.Sprite.WINGS, p, p.position(), 2.4f, 0xFFE8F4FF, 22);
            } else {
                owner.resetFallDistance(); // un mob Shaiapouf plane : pas de chute
            }
        }

        @Override
        public float modifyIncoming(LivingEntity owner, DamageSource source, float amount) {
            return source.is(net.minecraft.tags.DamageTypeTags.IS_FALL) ? 0f : amount;
        }
    };

    static void stopFlight(ServerPlayer p) {
        p.getPersistentData().remove(POUF_FLIGHT);
        if (p.isCreative() || p.isSpectator()) return;
        p.getAbilities().mayfly = false;
        p.getAbilities().flying = false;
        p.onUpdateAbilities();
    }

    public static final Technique POUF_SPIRITUAL_MESSAGE = register(new Kinds.Pulse("pouf_spiritual_message", MANIPULATOR, 30, 300,
            8, 1f, 0f, List.of(new Effect(MobEffects.GLOWING, 200, 0), new Effect(MobEffects.MOVEMENT_SLOWDOWN, 120, 1),
            new Effect(MobEffects.CONFUSION, 80, 0)), new Look(Style.IMPACT, NenFx.GOLD, 0.8f), SoundEvents.AMETHYST_BLOCK_RESONATE));

    /** Belzébuth : il se dissout en nuée (invisible 6 s, vitesse) et les mini-Pouf harcèlent les ennemis proches. */
    public static final Technique POUF_BEELZEBUB = register(new Technique("pouf_beelzebub", MANIPULATOR, 45, 600) {
        @Override
        public boolean perform(LivingEntity caster, float power) {
            com.evandouard.hxh.nen.NenSenses.nenHide(caster, 120);
            caster.addEffect(Kinds.quiet(MobEffects.MOVEMENT_SPEED, 120, 1), caster);
            for (int i = 0; i < 6; i++) {
                NenTasks.later(i * 20, () -> {
                    if (!caster.isAlive() || !(caster.level() instanceof ServerLevel level)) return;
                    for (LivingEntity t : Kinds.enemiesAround(caster, caster.position().add(0, 1, 0), 6)) {
                        Kinds.hurt(caster, t, 2f * power);
                        NenFx.slashAt(level, t.getBoundingBox().getCenter(), 0.6f, 0xFFE8F4FF);
                    }
                    NenFx.shockwave(caster, 3f, 0xFFE8F4FF);
                });
            }
            Kinds.sound(caster, SoundEvents.BEE_LOOP);
            return true;
        }
    }).requiresNenLevel(20);
}
