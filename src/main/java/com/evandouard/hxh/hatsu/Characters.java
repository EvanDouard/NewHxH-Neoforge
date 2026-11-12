package com.evandouard.hxh.hatsu;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.fx.NenFx;
import com.evandouard.hxh.hatsu.Techniques.Effect;
import com.evandouard.hxh.hatsu.Techniques.Look;
import com.evandouard.hxh.hatsu.Techniques.Style;
import com.evandouard.hxh.nen.Targeting;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.evandouard.hxh.data.NenType.*;
import static com.evandouard.hxh.hatsu.Techniques.register;

/**
 * Techniques et passifs des personnages de la fiche {@code docs/kits-personnages.md} qui n'ont pas besoin d'un
 * système à part (Netero, Biscuit, Feitan, Phinks, Shalnark, Knov, Knuckle, Shaiapouf, Chrollo ont leur classe).
 * Première version jouable : formes dessinées existantes ; les rendus sur mesure viendront ensuite.
 */
public final class Characters {
    private Characters() {}

    private static Passive passive(String id) {
        return new Passive(id, true);
    }

    private static HunterData data(LivingEntity e) {
        return HunterStats.get(e);
    }

    // =====================================================================
    // Pariston (Spécialisation*)
    // =====================================================================
    /** Charisme : un monstre qui le prend pour cible a une chance sur trois de l'oublier (toutes les 3 s). */
    public static final Passive PARISTON_CHARISMA = new Passive("pariston_charisma", true) {
        @Override
        public void tick(LivingEntity owner) {
            if (owner.tickCount % 60 != 0) return;
            for (Mob m : owner.level().getEntitiesOfClass(Mob.class, owner.getBoundingBox().inflate(16), m -> m.getTarget() == owner)) {
                if (owner.getRandom().nextInt(3) == 0 && m.getLastHurtByMob() != owner) m.setTarget(null);
            }
        }

        @Override
        public float modifyIncoming(LivingEntity owner, DamageSource source, float amount) {
            // Dette d'amour : la moitié des coups d'un débiteur lui revient
            if (source.getEntity() instanceof LivingEntity attacker && LoveDebt.owes(attacker, owner)) {
                Kinds.hurt(owner, attacker, amount * 0.5f);
                NenFx.impactOn(owner, attacker, 0.8f, NenFx.HISOKA);
            }
            return amount;
        }
    };

    /** Dette d'amour : la cible marquée (10 s) rend la moitié des coups qu'elle lui porte. */
    static final class LoveDebt {
        static final String KEY = "hxh_love_debt";

        static boolean owes(LivingEntity debtor, LivingEntity creditor) {
            var t = debtor.getPersistentData();
            return t.hasUUID(KEY) && t.getUUID(KEY).equals(creditor.getUUID()) && t.getLong(KEY + "_until") > debtor.level().getGameTime();
        }
    }

    public static final Technique PARISTON_LOVE_DEBT = register(new Technique("pariston_love_debt", SPECIALIST, 25, 300) {
        @Override
        public double aiRange() {
            return 10;
        }

        @Override
        public Component conditions() {
            return Component.translatable("technique.hxh.cond.target", "10.0");
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            LivingEntity t = Targeting.inFront(caster, 10, 0.9);
            if (t == null) return false;
            t.getPersistentData().putUUID(LoveDebt.KEY, caster.getUUID());
            t.getPersistentData().putLong(LoveDebt.KEY + "_until", caster.level().getGameTime() + Math.round(200 * power));
            NenFx.mark(caster, t, NenFx.HISOKA, Math.round(200 * power));
            NenFx.impactOn(caster, t, 1f, NenFx.HISOKA);
            if (caster.level() instanceof ServerLevel lv) NenFx.sprite(lv, NenFx.Sprite.HEART, t, t.position(), 0.9f, NenFx.HISOKA, Math.round(200 * power));
            Kinds.sound(t, SoundEvents.AMETHYST_BLOCK_CHIME);
            return true;
        }
    });

    // =====================================================================
    // Ging (Émission*)
    // =====================================================================
    /** Archéologue : indique le coffre le plus proche (24 blocs) toutes les 10 s. */
    public static final Passive GING_ARCHAEOLOGIST = new Passive("ging_archaeologist", true) {
        @Override
        public void tick(LivingEntity owner) {
            if (!(owner instanceof Player p) || owner.tickCount % 200 != 0) return;
            BlockPos at = owner.blockPosition(), best = null;
            double bestD = Double.MAX_VALUE;
            for (BlockPos q : BlockPos.betweenClosed(at.offset(-24, -8, -24), at.offset(24, 8, 24))) {
                var st = owner.level().getBlockState(q);
                if (!st.is(Blocks.CHEST) && !st.is(Blocks.BARREL) && !st.is(Blocks.TRAPPED_CHEST)) continue;
                double d = q.distSqr(at);
                if (d < bestD) {
                    bestD = d;
                    best = q.immutable();
                }
            }
            if (best != null) p.displayClientMessage(Component.translatable("message.hxh.ging_chest",
                    (int) Math.sqrt(bestD), best.getX(), best.getY(), best.getZ()).withStyle(ChatFormatting.GOLD), true);
        }
    };

    /** Clé des données persistantes : dernière technique vue par Ging (copiable). */
    public static final String GING_SEEN = "hxh_ging_seen";

    /** Un lanceur vient d'utiliser une technique : les Ging à 16 blocs la retiennent. */
    public static void observe(LivingEntity caster, Technique technique) {
        if (technique.isBase() || technique.isCharged() || technique.id().startsWith("ging_")) return;
        for (LivingEntity e : Targeting.around(caster, 16))
            if ("ging".equals(data(e).kit())) e.getPersistentData().putString(GING_SEEN, technique.id());
    }

    /** Valeurs de kit de Ging : technique copiée (code, voir {@link Techniques#code}) et utilisations restantes. */
    public static final String GING_COPY_TECH = "ging_copy_tech", GING_COPY_USES = "ging_copy_uses";

    /** Utilisations d'une copie selon le niveau de Nen : 1 au début, jusqu'à 5 à haut niveau. */
    public static int gingMaxUses(int nenLevel) {
        return nenLevel >= 80 ? 5 : nenLevel >= 60 ? 3 : nenLevel >= 40 ? 2 : 1;
    }

    /** Technique copiée par Ging encore utilisable, ou null. */
    public static Technique gingCopied(com.evandouard.hxh.data.HunterData d) {
        if (!"ging".equals(d.kit()) || d.kitValue(GING_COPY_USES) < 1) return null;
        return Techniques.byCode(Math.round(d.kitValue(GING_COPY_TECH)));
    }

    /** Une utilisation de la copie est partie ; plus rien : la copie s'efface de la liste. */
    public static void gingUsed(LivingEntity caster, Technique t) {
        var d = data(caster);
        Technique copied = gingCopied(d);
        if (copied == null || copied != t) return;
        d.addKitValue(GING_COPY_USES, -1, 99);
        if (d.kitValue(GING_COPY_USES) < 1) {
            d.setKitValue(GING_COPY_TECH, 0);
            if (caster instanceof Player p) p.displayClientMessage(Component.translatable("message.hxh.ging_copy_spent"), true);
        }
    }

    /**
     * Copie : la dernière technique vue (à 16 blocs) entre dans la liste des techniques de Ging, avec un nombre
     * d'utilisations (1 au début, jusqu'à 5). Recopier la même technique remet le compteur au maximum.
     */
    public static final Technique GING_COPY = register(new Technique("ging_copy", EMITTER, 20, 200) {
        @Override
        public Component conditions() {
            return Component.translatable("technique.hxh.cond.copy");
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            Technique seen = Techniques.get(caster.getPersistentData().getString(GING_SEEN));
            if (seen == null || !seen.implemented() || seen.isCharged()) {
                if (caster instanceof Player p) p.displayClientMessage(Component.translatable("message.hxh.ging_nothing"), true);
                return false;
            }
            var d = data(caster);
            int uses = gingMaxUses(d.nenLevel());
            d.setKitValue(GING_COPY_TECH, Techniques.code(seen.id()));
            d.setKitValue(GING_COPY_USES, uses);
            NenFx.flare(caster, 1f, 0xFF7FB8FF);
            if (caster instanceof Player p) p.displayClientMessage(Component.translatable("message.hxh.ging_copied",
                    Component.translatable(seen.translationKey()), uses), true);
            return true;
        }
    }).requiresNenLevel(20);

    public static final Technique GING_EMISSION_PUNCH = register(new Techniques.Strike("ging_emission_punch", EMITTER, 20, 80,
            10, 0.95, 7f, 1.2f, List.of(), new Look(Style.WARP_FIST, 0xFFFFE060, 1.4f), SoundEvents.PLAYER_ATTACK_KNOCKBACK));

    // =====================================================================
    // Cheadle (Manipulation*)
    // =====================================================================
    /** Secrétaire : recharges −10 % (appliqué par {@link Hatsu#cast}). */
    public static final Passive CHEADLE_SECRETARY = passive("cheadle_secretary");

    /** Règlement : zone de 8 blocs, 8 s : les ennemis ralentis et incapables de se téléporter. */
    public static final Technique CHEADLE_RULES = register(new Technique("cheadle_rules", MANIPULATOR, 35, 500) {
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
            Vec3 c = caster.position();
            int pulses = 4;
            for (int i = 0; i < pulses; i++) {
                NenTasks.later(i * 40, () -> {
                    if (!(caster.level() instanceof ServerLevel level)) return;
                    NenFx.shockwaveAt(level, c.add(0, 0.1, 0), 8f, 0xFFE8E0C8);
                    for (LivingEntity t : Kinds.enemiesAround(caster, c.add(0, 1, 0), 8)) {
                        t.addEffect(Kinds.quiet(MobEffects.MOVEMENT_SLOWDOWN, 45, 1), caster);
                        t.getPersistentData().putLong(RULES_KEY, level.getGameTime() + 45);
                    }
                });
            }
            Kinds.sound(caster, SoundEvents.BOOK_PAGE_TURN);
            return true;
        }
    }).requiresNenLevel(10);

    /** Données persistantes : tick jusqu'auquel l'entité ne peut pas se téléporter (Règlement). */
    public static final String RULES_KEY = "hxh_no_teleport_until";

    // =====================================================================
    // Botobai (Renforcement*)
    // =====================================================================
    /** Vétéran : résiste au recul et à la peur (les effets de faiblesse et lenteur durent moitié moins). */
    public static final Passive BOTOBAI_VETERAN = new Passive("botobai_veteran", true) {
        @Override
        public void tick(LivingEntity owner) {
            if (owner.tickCount % 20 != 0) return;
            var kb = owner.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
            if (kb != null && kb.getModifier(VETERAN_KB) == null)
                kb.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(VETERAN_KB, 0.5,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
            for (var eff : List.of(MobEffects.WEAKNESS, MobEffects.MOVEMENT_SLOWDOWN)) {
                var inst = owner.getEffect(eff);
                if (inst != null && inst.getDuration() > 40) {
                    owner.removeEffect(eff);
                    owner.addEffect(Kinds.quiet(eff, inst.getDuration() / 2, inst.getAmplifier()));
                }
            }
        }
    };
    static final net.minecraft.resources.ResourceLocation VETERAN_KB = com.evandouard.hxh.HxHMod.id("botobai_veteran");

    public static final Technique BOTOBAI_PRESSURE = register(new Kinds.Pulse("botobai_pressure", ENHANCER, 30, 240,
            6, 3f, 0.3f, List.of(new Effect(MobEffects.MOVEMENT_SLOWDOWN, 80, 2), new Effect(MobEffects.WEAKNESS, 80, 0)),
            new Look(Style.IMPACT, 0xFF9A9A9A, 1f), SoundEvents.RAVAGER_ROAR));

    // =====================================================================
    // Menchi (Transformation*)
    // =====================================================================
    /** Gastronome : la nourriture rassasie deux fois plus (appliqué à la fin d'un repas). */
    public static final Passive MENCHI_GASTRONOME = passive("menchi_gastronome");

    public static final Technique MENCHI_GOURMET_MEAL = register(new Kinds.AllyBuff("menchi_gourmet_meal", TRANSMUTER, 30, 600,
            6, 4f, List.of(new Effect(MobEffects.REGENERATION, 200, 1), new Effect(MobEffects.SATURATION, 20, 1),
            new Effect(MobEffects.DAMAGE_BOOST, 400, 0)),
            new Look(Style.FLARE, 0xFFFFB347, 1f), SoundEvents.GENERIC_EAT));

    // =====================================================================
    // Bushidora (Renforcement*)
    // =====================================================================
    public static final Passive BUSHIDORA_SWORDSMAN = new Passive("bushidora_swordsman", true) {
        @Override
        public float modifyOutgoingMelee(LivingEntity owner, LivingEntity target, float amount) {
            return owner.getMainHandItem().getItem() instanceof SwordItem ? amount * 1.2f : amount;
        }
    };

    public static final Technique BUSHIDORA_AURA_BLADE = register(new Kinds.Sweep("bushidora_aura_blade", ENHANCER, 25, 120,
            6, 70, 9f, 0.6f, 0xFF6FA8FF, SoundEvents.PLAYER_ATTACK_SWEEP));

    // =====================================================================
    // Tsezguerra (Renforcement*)
    // =====================================================================
    /** Meneur : les alliés à 8 blocs gagnent Force I. */
    public static final Passive TSEZGUERRA_LEADER = new Passive("tsezguerra_leader", true) {
        @Override
        public void tick(LivingEntity owner) {
            if (owner.tickCount % 40 != 0) return;
            for (LivingEntity e : Targeting.around(owner, 8))
                if (Kinds.isAlly(owner, e)) e.addEffect(Kinds.quiet(MobEffects.DAMAGE_BOOST, 60, 0), owner);
        }
    };

    public static final Technique TSEZGUERRA_CHARGED_STRIKE = register(new Techniques.Strike("tsezguerra_charged_strike", ENHANCER,
            25, 160, 3.5, 0.85, 13f, 1.6f, List.of(), new Look(Style.IMPACT, NenFx.ENHANCER, 2f), SoundEvents.PLAYER_ATTACK_STRONG));

    // =====================================================================
    // Zoldyck : Silva, Zeno, Kalluto
    // =====================================================================
    /** Grande boule : Silva forme vite (1 s) une grosse boule d'aura et la propulse ; comme la Pierre, sans creuser. */
    public static final Technique SILVA_GREAT_ORB = register(new Techniques.ChargedOrb("silva_great_orb", EMITTER, 10, 160,
            4f, 20, 8f, 10f, 0.3f, 0xFF9A5AE0));

    public static final Technique SILVA_EXPLOSIVE_ORBS = register(new Kinds.Burst("silva_explosive_orbs", EMITTER, 35, 200,
            16, 3, 6, 6f, List.of(), new Look(Style.ORB, 0xFF9A5AE0, 1.2f), SoundEvents.GENERIC_EXPLODE.value()));

    /** Tête de dragon : le dragon file de la main de Zeno jusqu'à la cible (14 blocs), puis la frappe. */
    public static final Technique ZENO_DRAGON_HEAD = register(new Technique("zeno_dragon_head", EMITTER, 30, 160) {
        @Override
        public double aiRange() {
            return 14;
        }

        @Override
        public Component conditions() {
            return Component.translatable("technique.hxh.cond.target", "14.0");
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            if (!(caster.level() instanceof ServerLevel level)) return false;
            LivingEntity t = Targeting.inFront(caster, 14, 0.93);
            Vec3 end = t != null ? t.getBoundingBox().getCenter() : caster.getEyePosition().add(caster.getLookAngle().scale(14));
            NenFx.sprite(level, NenFx.Sprite.DRAGON, caster, end, 2.6f, NenFx.GOLD, 16);
            Kinds.sound(caster, SoundEvents.ENDER_DRAGON_GROWL);
            if (t == null) return false;
            NenTasks.later(8, () -> {
                if (!t.isAlive()) return;
                Kinds.hurt(caster, t, 10f * power);
                t.knockback(1.4 * power, caster.getX() - t.getX(), caster.getZ() - t.getZ());
                NenFx.impactAt(level, t.getBoundingBox().getCenter(), 1.6f, NenFx.GOLD);
            });
            return true;
        }
    });

    public static final Technique ZENO_DRAGON_DIVE = register(new Kinds.Rain("zeno_dragon_dive", EMITTER, 60, 600,
            14, 5, 16, 40, 5f, NenFx.GOLD, SoundEvents.ENDER_DRAGON_FLAP).withSprites(NenFx.Sprite.DRAGON_DIVE, null)).requiresNenLevel(30);

    public static final Technique KALLUTO_PAPER_DANCE = register(new Kinds.Storm("kalluto_paper_dance", MANIPULATOR, 35, 300,
            7, 45, 6, 6, 2.5f, List.of(), 0xFFF5F0E6, SoundEvents.BOOK_PAGE_TURN).withSprite(NenFx.Sprite.PAPER));

    // =====================================================================
    // Brigade Fantôme : Nobunaga, Uvogin, Franklin, Machi, Shizuku, Pakunoda, Neon
    // =====================================================================
    /** En permanent (4 blocs) : les ennemis tout proches brillent. */
    public static final Passive NOBUNAGA_EN = new Passive("nobunaga_en", true) {
        @Override
        public void tick(LivingEntity owner) {
            if (owner.tickCount % 20 != 0) return;
            for (LivingEntity e : Targeting.around(owner, 4))
                if (!Kinds.isAlly(owner, e)) NenFx.mark(owner, e, NenFx.WHITE, 30);
        }
    };

    public static final Technique NOBUNAGA_IAI = register(new Kinds.Pulse("nobunaga_iai", ENHANCER, 25, 140,
            4, 12f, 0.2f, List.of(), new Look(Style.SLASH, NenFx.WHITE, 1.4f), SoundEvents.PLAYER_ATTACK_SWEEP));

    /** Corps d'acier : dégâts physiques −25 %. */
    public static final Passive UVOGIN_IRON_BODY = new Passive("uvogin_iron_body", true) {
        @Override
        public float modifyIncoming(LivingEntity owner, DamageSource source, float amount) {
            return source.is(DamageTypeTags.BYPASSES_ARMOR) || source.is(DamageTypeTags.IS_FIRE) ? amount : amount * 0.75f;
        }
    };

    /**
     * Big Bang Impact : on charge le poing (jusqu'à 2 s) ; relâché, il frappe le sol devant Uvogin. Selon la charge, le
     * coup détruit plus ou moins autour de l'impact (cratère de 1 à 6 blocs, si la règle de destruction le permet),
     * fait plus de dégâts et projette plus loin.
     */
    public static final Technique UVOGIN_BIG_BANG_IMPACT = register(new HeldStrike("uvogin_big_bang_impact", ENHANCER, 30, 400, 40, NenFx.ENHANCER) {
        @Override
        public double aiRange() {
            return 4;
        }

        @Override
        public Component conditions() {
            return Component.translatable("technique.hxh.cond.charge_strike", "4");
        }

        @Override
        protected void strike(LivingEntity caster, float power, float charge) {
            if (!(caster.level() instanceof ServerLevel level)) return;
            LivingEntity t = Targeting.inFront(caster, 4, 0.8);
            Vec3 at = t != null ? t.position() : Kinds.aimPoint(caster, 3.5);
            float radius = 1f + 5f * charge;
            NenFx.quake(level, at, 3f + 5f * charge);
            NenFx.impactAt(level, at.add(0, 0.8, 0), 2f + 3f * charge, NenFx.ENHANCER);
            NenFx.shockwaveAt(level, at.add(0, 0.2, 0), 2f + 6f * charge, NenFx.ENHANCER);
            Kinds.sound(caster, SoundEvents.GENERIC_EXPLODE.value());
            // cratère selon la charge
            if (com.evandouard.hxh.HxHGameRules.canDestroy(level) && charge > 0.15f) {
                net.minecraft.core.BlockPos c = net.minecraft.core.BlockPos.containing(at.add(0, -0.5, 0));
                int ir = (int) Math.ceil(radius);
                for (net.minecraft.core.BlockPos p : net.minecraft.core.BlockPos.betweenClosed(c.offset(-ir, -ir, -ir), c.offset(ir, ir, ir))) {
                    double d = Math.sqrt(p.distSqr(c));
                    if (d > radius || p.getY() > c.getY() + 1 + radius * 0.4) continue;
                    if (d > radius - 1 && level.random.nextFloat() < 0.4f) continue;
                    var st = level.getBlockState(p);
                    if (st.isAir() || level.getBlockEntity(p) != null) continue;
                    float hard = st.getDestroySpeed(level, p);
                    if (hard >= 0 && hard <= 50) level.destroyBlock(p, false, caster);
                }
            }
            for (LivingEntity e : Kinds.enemiesAround(caster, at, 1.5 + 3 * charge)) {
                Kinds.hurt(caster, e, (10f + 18f * charge) * power);
                e.knockback((1 + 2 * charge) * power, caster.getX() - e.getX(), caster.getZ() - e.getZ());
            }
            if (t != null) com.evandouard.hxh.combat.Impacts.land(caster, t,
                    charge > 0.8f ? com.evandouard.hxh.combat.Impacts.Tier.DEVASTATING
                            : charge > 0.4f ? com.evandouard.hxh.combat.Impacts.Tier.HEAVY : com.evandouard.hxh.combat.Impacts.Tier.MEDIUM, () -> {});
        }
    }).requiresNenLevel(20);

    public static final Passive FRANKLIN_STURDY = new Passive("franklin_sturdy", true) {
        @Override
        public float modifyIncoming(LivingEntity owner, DamageSource source, float amount) {
            return amount * 0.85f;
        }
    };

    public static final Technique FRANKLIN_DOUBLE_MACHINE_GUN = register(new Kinds.Burst("franklin_double_machine_gun", EMITTER,
            40, 300, 18, 10, 2, 2.2f, List.of(), new Look(Style.ORB, NenFx.EMITTER, 0.6f), SoundEvents.FIREWORK_ROCKET_BLAST));

    /** Suture : se soigne d'1 PV toutes les 4 s, et ses alliés à 3 blocs aussi. */
    public static final Passive MACHI_SUTURE = new Passive("machi_suture", true) {
        @Override
        public void tick(LivingEntity owner) {
            if (owner.tickCount % 80 != 0) return;
            owner.heal(1f);
            for (LivingEntity e : Targeting.around(owner, 3)) if (Kinds.isAlly(owner, e)) e.heal(1f);
        }
    };

    public static final Technique MACHI_NEN_THREADS = register(new Techniques.Strike("machi_nen_threads", TRANSMUTER, 25, 200,
            10, 0.95, 2f, 0f, List.of(new Effect(MobEffects.MOVEMENT_SLOWDOWN, 100, 3), new Effect(MobEffects.JUMP, 100, 128)),
            new Look(Style.CHAIN, 0xFFE6E6F0, 1f), SoundEvents.SPIDER_AMBIENT));

    /** Ménage : attire les objets au sol à 5 blocs. */
    public static final Passive SHIZUKU_CLEANER = new Passive("shizuku_cleaner", true) {
        @Override
        public void tick(LivingEntity owner) {
            if (!(owner instanceof Player) || owner.tickCount % 5 != 0) return;
            for (ItemEntity it : owner.level().getEntitiesOfClass(ItemEntity.class, owner.getBoundingBox().inflate(5))) {
                Vec3 pull = owner.position().subtract(it.position()).normalize().scale(0.25);
                it.setDeltaMovement(it.getDeltaMovement().add(pull));
            }
        }
    };

    /** Blinky : aspire devant soi les projectiles (détruits), les objets et un peu les ennemis. */
    public static final Technique SHIZUKU_BLINKY = register(new Technique("shizuku_blinky", CONJURER, 30, 240) {
        @Override
        public Component conditions() {
            return Component.translatable("technique.hxh.cond.cone", "8", "45");
        }

        @Override
        public double aiRange() {
            return 8;
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            if (caster.level() instanceof ServerLevel lv) NenFx.sprite(lv, NenFx.Sprite.BLINKY, caster, caster.position(), 1.6f, 0xFFB0B0FF, 34);
            for (int w = 0; w < 5; w++) {
                NenTasks.later(w * 6, () -> {
                    if (!caster.isAlive() || !(caster.level() instanceof ServerLevel level)) return;
                    Vec3 mouth = caster.getEyePosition().add(caster.getLookAngle().scale(1.2));
                    AABB box = caster.getBoundingBox().inflate(8);
                    for (Projectile pr : level.getEntitiesOfClass(Projectile.class, box,
                            pr -> pr.getOwner() != caster && pr.position().subtract(caster.getEyePosition()).normalize().dot(caster.getLookAngle()) > 0.7)) {
                        NenFx.impactAt(level, pr.position(), 0.5f, 0xFFB0B0FF);
                        pr.discard();
                    }
                    for (ItemEntity it : level.getEntitiesOfClass(ItemEntity.class, box))
                        it.setDeltaMovement(mouth.subtract(it.position()).normalize().scale(0.6));
                    for (LivingEntity t : Kinds.Sweep.inArc(caster, 8, 45)) {
                        if (Kinds.isAlly(caster, t)) continue;
                        t.setDeltaMovement(t.getDeltaMovement().add(mouth.subtract(t.position()).normalize().scale(0.25)));
                        t.hurtMarked = true;
                        Kinds.hurt(caster, t, 1.5f * power);
                    }
                    NenFx.shockwaveAt(level, mouth, 1.2f, 0xFFB0B0FF);
                });
            }
            Kinds.sound(caster, SoundEvents.BREEZE_INHALE);
            return true;
        }
    });

    /** Liseuse de pensées : un coup au corps à corps révèle les stats de la cible. */
    public static final Passive PAKUNODA_MIND_READER = new Passive("pakunoda_mind_reader", true) {
        @Override
        public float modifyOutgoingMelee(LivingEntity owner, LivingEntity target, float amount) {
            if (owner instanceof Player p) {
                HunterData d = data(target);
                p.displayClientMessage(Component.translatable("message.hxh.pakunoda_read", target.getDisplayName(),
                        (int) target.getHealth(), (int) target.getMaxHealth(), d.nenLevel(), (int) d.aura()).withStyle(ChatFormatting.AQUA), true);
            }
            return amount;
        }
    };

    public static final Technique PAKUNODA_MEMORY_BOMB = register(new Techniques.Strike("pakunoda_memory_bomb", SPECIALIST, 30, 240,
            16, 0.97, 6f, 0.4f, List.of(new Effect(MobEffects.CONFUSION, 100, 0), new Effect(MobEffects.MOVEMENT_SLOWDOWN, 80, 2),
            new Effect(MobEffects.BLINDNESS, 30, 0)), new Look(Style.ORB, 0xFFD8D0FF, 0.7f), SoundEvents.CROSSBOW_SHOOT));

    /** Divination : chaque jour de jeu, une petite chance (Chance I pour la journée). */
    public static final Passive NEON_FORTUNE = new Passive("neon_fortune", true) {
        @Override
        public void tick(LivingEntity owner) {
            if (owner.tickCount % 200 == 0) owner.addEffect(Kinds.quiet(MobEffects.LUCK, 260, 0));
        }
    };

    /** Lovely Ghostwriter : le poème annonce les dangers à 32 blocs (nombre, direction) et les fait briller. */
    public static final Technique NEON_LOVELY_GHOSTWRITER = register(new Technique("neon_lovely_ghostwriter", SPECIALIST, 20, 600) {
        @Override
        public boolean perform(LivingEntity caster, float power) {
            List<LivingEntity> danger = caster.level().getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(32),
                    e -> e instanceof Enemy && e.isAlive());
            for (LivingEntity e : danger) NenFx.mark(caster, e, 0xFFE0C8FF, 200);
            NenFx.flare(caster, 1f, 0xFFE0C8FF);
            if (caster instanceof Player p) {
                if (danger.isEmpty()) p.displayClientMessage(Component.translatable("message.hxh.neon_calm"), false);
                else {
                    LivingEntity near = danger.stream().min(java.util.Comparator.comparingDouble(caster::distanceToSqr)).get();
                    Vec3 d = near.position().subtract(caster.position());
                    String dir = Math.abs(d.x) > Math.abs(d.z) ? (d.x > 0 ? "east" : "west") : (d.z > 0 ? "south" : "north");
                    p.displayClientMessage(Component.translatable("message.hxh.neon_poem", danger.size(),
                            Component.translatable("message.hxh.dir." + dir)).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC), false);
                }
            }
            Kinds.sound(caster, SoundEvents.BOOK_PAGE_TURN);
            return true;
        }
    });

    // =====================================================================
    // Greed Island : Razor, Genthru
    // =====================================================================
    /** Maître du jeu : +10 % de dégâts au corps à corps. */
    public static final Passive RAZOR_GAME_MASTER = new Passive("razor_game_master", true) {
        @Override
        public float modifyOutgoingMelee(LivingEntity owner, LivingEntity target, float amount) {
            return amount * 1.1f;
        }
    };

    /** Bombardier : dégâts d'explosion reçus −50 %. */
    public static final Passive GENTHRU_BOMBER = new Passive("genthru_bomber", true) {
        @Override
        public float modifyIncoming(LivingEntity owner, DamageSource source, float amount) {
            return source.is(DamageTypeTags.IS_EXPLOSION) ? amount * 0.5f : amount;
        }
    };

    public static final Technique RAZOR_AURA_BALL = register(new Techniques.Strike("razor_aura_ball", EMITTER, 25, 140,
            18, 0.96, 11f, 2.2f, List.of(), new Look(Style.ORB, 0xFFFF7A3C, 1.5f), SoundEvents.SLIME_JUMP));

    public static final Technique RAZOR_FOURTEEN_DEVILS = register(new Kinds.Rain("razor_fourteen_devils", EMITTER, 60, 600,
            12, 4, 14, 42, 5f, 0xFFB03A3A, SoundEvents.SLIME_JUMP).withSprites(null, NenFx.Sprite.DEMON)).requiresNenLevel(30);

    public static final Technique GENTHRU_LITTLE_FLOWER = register(new Technique("genthru_little_flower", CONJURER, 30, 200) {
        @Override
        public double aiRange() {
            return 2.5;
        }

        @Override
        public Component conditions() {
            return Component.translatable("technique.hxh.cond.target", "2.5");
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            LivingEntity t = Targeting.inFront(caster, 2.8, 0.7);
            if (t == null) return false;
            Kinds.hurt(caster, t, 15f * power);
            t.knockback(1.2 * power, caster.getX() - t.getX(), caster.getZ() - t.getZ());
            NenFx.impactOn(caster, t, 2.2f, 0xFFFF8A2A);
            NenFx.flare(t, 1.2f, 0xFFFF8A2A);
            // la main de Genthru encaisse un peu le contrecoup
            caster.hurt(caster.damageSources().magic(), 1f);
            Kinds.sound(t, SoundEvents.GENERIC_EXPLODE.value());
            return true;
        }
    });

    public static final Technique GENTHRU_COUNTDOWN = register(new Kinds.TimeBomb("genthru_countdown", CONJURER, 40, 600,
            8, 60, 14f, 3, 0xFFFF5A2A)).requiresNenLevel(20);

    // =====================================================================
    // Hunters de l'arc des Fourmis : Kite, Morel, Palm
    // =====================================================================
    /** Chasseur : +15 % de dégâts contre les bêtes et les monstres. */
    public static final Passive KITE_HUNTER = new Passive("kite_hunter", true) {
        @Override
        public float modifyOutgoingMelee(LivingEntity owner, LivingEntity target, float amount) {
            return target instanceof Player ? amount : amount * 1.15f;
        }
    };

    /** Fumée : touché, une chance sur cinq de disparaître 2 s dans un nuage. */
    public static final Passive MOREL_SMOKE = new Passive("morel_smoke", true) {
        @Override
        public float modifyIncoming(LivingEntity owner, DamageSource source, float amount) {
            if (owner.getRandom().nextInt(5) == 0) {
                com.evandouard.hxh.nen.NenSenses.nenHide(owner, 40);
                NenFx.shockwave(owner, 1.5f, 0xFF8A5AB0);
            }
            return amount;
        }
    };

    /** Crazy Slot : la roulette tire une arme au hasard parmi quatre. */
    public static final Technique KITE_CRAZY_SLOTS = register(new Technique("kite_crazy_slots", CONJURER, 35, 240) {
        private final Technique[] slots = {
                new Kinds.Sweep("kite_slot_scythe", CONJURER, 0, 0, 6, 80, 12f, 0.8f, 0xFFE0E0E0, SoundEvents.PLAYER_ATTACK_SWEEP),
                new Kinds.Burst("kite_slot_rifle", CONJURER, 0, 0, 20, 4, 4, 4f, List.of(), new Look(Style.ORB, 0xFFE0E0E0, 0.5f), SoundEvents.CROSSBOW_SHOOT),
                new Techniques.Strike("kite_slot_staff", CONJURER, 0, 0, 4, 0.8, 8f, 1.5f,
                        List.of(new Effect(MobEffects.MOVEMENT_SLOWDOWN, 60, 2)), new Look(Style.IMPACT, 0xFFE0E0E0, 1.4f), SoundEvents.PLAYER_ATTACK_STRONG),
                new Kinds.Pulse("kite_slot_clown", CONJURER, 0, 0, 5, 6f, 1f, List.of(new Effect(MobEffects.CONFUSION, 80, 0)),
                        new Look(Style.IMPACT, 0xFFFF6FB5, 1f), SoundEvents.NOTE_BLOCK_BELL.value())};

        @Override
        public double aiRange() {
            return 6;
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            int n = caster.getRandom().nextInt(slots.length);
            if (caster instanceof Player p) p.displayClientMessage(Component.translatable("message.hxh.kite_slot", n + 1,
                    Component.translatable("message.hxh.kite_slot." + n)).withStyle(ChatFormatting.LIGHT_PURPLE), true);
            NenFx.flare(caster, 0.8f, 0xFFFF6FB5);
            if (caster.level() instanceof ServerLevel lv) {
                NenFx.sprite(lv, NenFx.Sprite.CLOWN, caster, caster.position(), 1.3f, 0xFFFF6FB5, 30);
                // l'arme tirée apparaît dans la main de Kite (le clown, lui, n'a pas d'arme)
                NenFx.Sprite weapon = n == 0 ? NenFx.Sprite.SCYTHE : n == 1 ? NenFx.Sprite.RIFLE : n == 2 ? NenFx.Sprite.STAFF : null;
                if (weapon != null) NenFx.sprite(lv, weapon, caster, caster.position(), n == 1 ? 1.5f : 2.2f, 0xFFE0E0E0, 24);
            }
            return slots[n].perform(caster, power);
        }
    });

    public static final Technique MOREL_DEEP_PURPLE = register(new Kinds.Storm("morel_deep_purple", MANIPULATOR, 45, 400,
            8, 50, 8, 10, 3f, List.of(new Effect(MobEffects.BLINDNESS, 40, 0)), 0xFF8A5AB0, SoundEvents.FIRE_EXTINGUISH)
            .withSprite(NenFx.Sprite.SMOKE_SOLDIER))
            .requiresNenLevel(20);

    /** Données persistantes de Palm : la cible espionnée (Wink Blue). */
    public static final String PALM_MARK = "hxh_palm_mark";

    /** Wink Blue : le dernier ennemi frappé reste visible (brille) pendant 30 s. */
    public static final Passive PALM_WINK_BLUE = new Passive("palm_wink_blue", true) {
        @Override
        public float modifyOutgoingMelee(LivingEntity owner, LivingEntity target, float amount) {
            NenFx.mark(owner, target, 0xFF6FA8FF, 600);
            return amount;
        }
    };

    public static final Technique PALM_BLACK_WIDOW = register(new Techniques.SelfBuff("palm_black_widow", ENHANCER, 50, 600,
            List.of(new Effect(MobEffects.DAMAGE_BOOST, 400, 1), new Effect(MobEffects.DAMAGE_RESISTANCE, 400, 0),
                    new Effect(MobEffects.MOVEMENT_SPEED, 400, 0)),
            new Look(Style.FLARE, NenFx.DARK, 1.4f), SoundEvents.WITHER_SPAWN)).requiresNenLevel(20);

    // =====================================================================
    // Fourmis-Chimères : Meruem, Youpi
    // =====================================================================
    /** Roi : chaque ennemi vaincu le renforce (+0,5 % de dégâts, 25 % au plus). Compte = ressource « king ». */
    public static final Passive MERUEM_KING = new Passive("meruem_king", true) {
        @Override
        public float modifyOutgoingMelee(LivingEntity owner, LivingEntity target, float amount) {
            return amount * (1f + Math.min(0.25f, data(owner).kitValue("king") * 0.005f));
        }
    };

    public static final Technique MERUEM_RAGE_BLAST = register(new Techniques.Blast("meruem_rage_blast", EMITTER, 40, 300,
            7, 12f, 2f, new Look(Style.SHOCKWAVE, NenFx.DARK, 1.4f), SoundEvents.WARDEN_SONIC_BOOM));

    public static final Technique MERUEM_PHOTON = register(new Kinds.Beam("meruem_photon", EMITTER, 50, 400,
            20, 1.2, 18f, NenFx.WHITE, SoundEvents.BEACON_ACTIVATE)).requiresNenLevel(30);

    /** Vitalité : +20 PV max. */
    public static final Passive YOUPI_VITALITY = new Passive("youpi_vitality", true) {
        @Override
        public void tick(LivingEntity owner) {
            if (owner.tickCount % 20 != 0) return;
            var hp = owner.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
            if (hp != null && hp.getModifier(YOUPI_HP) == null)
                hp.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(YOUPI_HP, 20,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
        }
    };
    static final net.minecraft.resources.ResourceLocation YOUPI_HP = com.evandouard.hxh.HxHMod.id("youpi_vitality");

    public static final Technique YOUPI_METAMORPHOSIS = register(new Techniques.SelfBuff("youpi_metamorphosis", TRANSMUTER, 40, 500,
            List.of(new Effect(MobEffects.DAMAGE_BOOST, 300, 1), new Effect(MobEffects.DAMAGE_RESISTANCE, 300, 0)),
            new Look(Style.FLARE, 0xFFE0402A, 1.6f), SoundEvents.RAVAGER_ROAR));

    public static final Technique YOUPI_RAGE_BLAST = register(new Techniques.Blast("youpi_rage_blast", TRANSMUTER, 45, 400,
            6, 11f, 2.2f, new Look(Style.SHOCKWAVE, 0xFFE0402A, 1.4f), SoundEvents.WARDEN_ROAR)).requiresNenLevel(20);

    // =====================================================================
    // Autres : Zushi, Senritsu, Wing
    // =====================================================================
    /** Assidu : +25 % d'expérience à l'entraînement (appliqué par HunterStats.addXp). */
    public static final Passive ZUSHI_DILIGENT = passive("zushi_diligent");

    public static final Technique ZUSHI_REN_PUNCH = register(new Techniques.Strike("zushi_ren_punch", MANIPULATOR, 15, 80,
            3, 0.85, 8f, 1f, List.of(), new Look(Style.IMPACT, NenFx.MANIPULATOR, 1.3f), SoundEvents.PLAYER_ATTACK_STRONG));

    /** Battement de cœur : les ennemis cachés (Zetsu, invisibles) à 20 blocs brillent. */
    public static final Passive SENRITSU_HEART_SOUND = new Passive("senritsu_heart_sound", true) {
        @Override
        public void tick(LivingEntity owner) {
            if (owner.tickCount % 40 != 0) return;
            for (LivingEntity e : Targeting.around(owner, 20)) {
                if (Kinds.isAlly(owner, e)) continue;
                boolean hidden = e.isInvisible() || data(e).mode() == com.evandouard.hxh.data.NenMode.ZETSU;
                if (hidden) e.addEffect(Kinds.quiet(MobEffects.GLOWING, 50, 0), owner);
            }
        }
    };

    /** Mélodie : soigne et apaise les alliés, endort à moitié les ennemis (8 blocs). */
    public static final Technique SENRITSU_MELODY = register(new Technique("senritsu_melody", EMITTER, 35, 400) {
        @Override
        public boolean isHeal() {
            return true;
        }

        @Override
        public Component conditions() {
            return Component.translatable("technique.hxh.cond.area", "8");
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            caster.heal(6f * power);
            NenFx.flare(caster, 1f, 0xFFFFB6E1);
            if (caster.level() instanceof ServerLevel lv) for (int k = 0; k < 5; k++) {
                double a = k * Math.PI * 2 / 5;
                NenFx.sprite(lv, NenFx.Sprite.NOTE, null, caster.position().add(Math.cos(a) * 1.5, 1.8, Math.sin(a) * 1.5), 0.8f, 0xFFFFB6E1, 30);
            }
            for (LivingEntity e : Targeting.around(caster, 8)) {
                if (Kinds.isAlly(caster, e)) {
                    e.heal(6f * power);
                    e.addEffect(Kinds.quiet(MobEffects.REGENERATION, 100, 0), caster);
                    NenFx.flare(e, 0.8f, 0xFFFFB6E1);
                } else {
                    e.addEffect(Kinds.quiet(MobEffects.MOVEMENT_SLOWDOWN, 120, 3), caster);
                    e.addEffect(Kinds.quiet(MobEffects.WEAKNESS, 120, 1), caster);
                    NenFx.impactOn(caster, e, 0.6f, 0xFFFFB6E1);
                }
            }
            NenFx.shockwave(caster, 8f, 0xFFFFB6E1);
            Kinds.sound(caster, SoundEvents.NOTE_BLOCK_FLUTE.value());
            return true;
        }
    });

    /** Professeur : les alliés à 8 blocs gagnent un peu de Nen (1 point toutes les 30 s). */
    public static final Passive WING_TEACHER = new Passive("wing_teacher", true) {
        @Override
        public void tick(LivingEntity owner) {
            float guard = data(owner).kitValue("guard");
            if (guard > 0) data(owner).setKitValue("guard", Math.max(0, guard - 1));
            if (owner.tickCount % 600 != 0) return;
            for (LivingEntity e : Targeting.around(owner, 8))
                if (Kinds.isAlly(owner, e) && e != owner) com.evandouard.hxh.nen.NenProgress.addNenXp(e, 1);
        }

        @Override
        public float modifyIncoming(LivingEntity owner, DamageSource source, float amount) {
            // Shingen-ryû : en garde, les coups au corps à corps sont renvoyés en partie
            if (data(owner).kitValue("guard") > 0
                    && source.getDirectEntity() instanceof LivingEntity attacker && attacker == source.getEntity()) {
                Kinds.hurt(owner, attacker, 4f);
                NenFx.impactOn(owner, attacker, 0.9f, NenFx.WHITE);
                return amount * 0.5f;
            }
            return amount;
        }
    };

    /** Shingen-ryû : 10 s de garde : coups reçus réduits de moitié, contre-attaque (4 dégâts) à chaque coup. */
    public static final Technique WING_SHINGEN_RYU = register(new Technique("wing_shingen_ryu", ENHANCER, 25, 300) {
        @Override
        public boolean perform(LivingEntity caster, float power) {
            data(caster).setKitValue("guard", Math.round(200 * power));
            caster.addEffect(Kinds.quiet(MobEffects.DAMAGE_RESISTANCE, Math.round(200 * power), 0), caster);
            NenFx.flare(caster, 1.1f, NenFx.WHITE);
            NenFx.shockwave(caster, 2f, NenFx.WHITE);
            Kinds.sound(caster, SoundEvents.ARMOR_EQUIP_IRON.value());
            return true;
        }
    });

    /** Un ennemi vient de mourir : Meruem (le tueur) se renforce. */
    public static void onKill(LivingEntity killer) {
        if ("meruem".equals(data(killer).kit())) data(killer).addKitValue("king", 1, 50);
    }

    /** Fin d'un repas : Menchi est rassasiée deux fois. */
    public static void onEat(LivingEntity eater, net.minecraft.world.item.ItemStack food) {
        if (!(eater instanceof Player p) || !"menchi".equals(data(eater).kit())) return;
        var props = food.getFoodProperties(p);
        if (props != null) p.getFoodData().eat(props.nutrition(), props.saturation());
    }
}
