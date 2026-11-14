package com.evandouard.hxh.hatsu;

import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.nen.Targeting;
import net.minecraft.core.Holder;
import com.evandouard.hxh.fx.NenFx;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registre des techniques. Les effets sont volontairement simples pour l'instant :
 * les visuels détaillés (aura qui grossit dans le poing, souffle au sol…) viendront ensuite.
 */
public final class Techniques {
    private static final Map<String, Technique> ALL = new LinkedHashMap<>();

    private Techniques() {}

    @Nullable
    public static Technique get(String id) {
        return ALL.get(id);
    }

    /** Code numérique stable d'une technique (tient dans un float exact) : pour la synchroniser comme valeur de kit. */
    public static int code(String id) {
        return 1 + Math.floorMod(id.hashCode(), 1 << 22);
    }

    /** Technique dont le code est {@code code}, ou null. */
    public static Technique byCode(int code) {
        if (code <= 0) return null;
        MobTechniques.init();
        for (Technique t : ALL.values()) if (code(t.id()) == code) return t;
        return null;
    }

    public static java.util.Collection<String> allIds() {
        return ALL.keySet();
    }

    public static Map<String, Technique> all() {
        return ALL;
    }

    static <T extends Technique> T register(T technique) {
        ALL.put(technique.id(), technique);
        return technique;
    }

    // ---------- Gon : Jajanken ----------
    /**
     * Jajanken : Pierre. On charge sur place (aura consommée en continu, 2/tick, jusqu'à 5 s) ; une boule d'aura
     * d'une demi-taille de joueur brille dans le poing et change de couleur à chaque palier. Au relâchement elle part
     * tout droit, creuse sa trace, emporte la cible et explose. Dégâts : 4 + 0,12 par point d'aura chargé (× puissance).
     */
    public static final Technique GON_ROCK = register(new ChargedOrb("gon_rock", NenType.ENHANCER, 10, 160,
            2f, 100, 12f, 4f, 0.12f, NenFx.ENHANCER));
    /**
     * Jajanken : Pierre sans retenue. La boule enfle une fois jusqu'à deux fois la taille du lanceur, puis se
     * recondense, minuscule et aveuglante (environ 1 s pour enfler, puis elle brille). Plus chère (40 au départ, au
     * moins 30 à charger), plus de dégâts, explosion et trace 1,6 fois plus grosses.
     */
    public static final Technique GON_ROCK_EPIC = register(new ChargedOrb("gon_rock_epic", NenType.ENHANCER, 40, 400,
            1.5f, 80, 30f, 10f, 0.25f, NenFx.ENHANCER).epic(1.6f)).requiresNenLevel(30);
    /** Jajanken : Ciseaux. Lame d'aura qui file tout droit (14 blocs) jusqu'à toucher un bloc ou une cible. */
    public static final Technique GON_SCISSORS = register(new Blade("gon_scissors", NenType.TRANSMUTER, 25, 100,
            7f, NenFx.TRANSMUTER)).requiresNenLevel(5);
    /**
     * Jajanken : Papier. Gon ouvre la main et libère son aura d'un coup : un cône d'émission (8 blocs, 35°) qui
     * frappe tout ce qui est devant et le repousse loin, sans creuser.
     */
    public static final Technique GON_PAPER = register(new PalmWave("gon_paper", NenType.EMITTER, 30, 120,
            8.0, 35.0, 9f, 2.2f, NenFx.EMITTER)).requiresNenLevel(10);

    // ---------- Killua : électricité ----------
    /** Thunderbolt : maintenu, l'électricité frappe en continu ; plus on tient, plus ça coûte (voir {@link ThunderHold}). */
    public static final Technique KILLUA_THUNDERBOLT = register(new ThunderHold.Technique_("killua_thunderbolt", NenType.TRANSMUTER, 10, 120));
    public static final Technique KILLUA_GODSPEED = register(new SelfBuff("killua_godspeed", NenType.TRANSMUTER, 50, 600,
            List.of(new Effect(MobEffects.MOVEMENT_SPEED, 200, 2), new Effect(MobEffects.DIG_SPEED, 200, 1)),
            new Look(Style.CRACKLE, NenFx.TRANSMUTER, 1f), SoundEvents.LIGHTNING_BOLT_THUNDER)).requiresNenLevel(10);

    // ---------- Kurapika : chaînes ----------
    public static final Technique KURAPIKA_CHAIN_JAIL = register(new Strike("kurapika_chain_jail", NenType.CONJURER, 45, 400,
            8, 0.95, 1f, 0f, List.of(new Effect(MobEffects.MOVEMENT_SLOWDOWN, 100, 6), new Effect(MobEffects.WEAKNESS, 100, 1)),
            new Look(Style.CHAIN, NenFx.CONJURER, 1f), SoundEvents.CHAIN_PLACE)).requiresNenLevel(5);
    public static final Technique KURAPIKA_HOLY_CHAIN = register(new SelfHeal("kurapika_holy_chain", NenType.CONJURER, 35, 300,
            8f, new Look(Style.HOLY_CHAIN, NenFx.CONJURER, 1f), SoundEvents.CHAIN_PLACE));

    // ---------- Leorio : émission ----------
    public static final Technique LEORIO_REMOTE_PUNCH = register(new Strike("leorio_remote_punch", NenType.EMITTER, 30, 120,
            12, 0.97, 7f, 1.0f, List.of(), new Look(Style.WARP_FIST, NenFx.EMITTER, 1.8f), SoundEvents.PLAYER_ATTACK_KNOCKBACK));
    public static final Technique LEORIO_SHOCKWAVE = register(new Blast("leorio_shockwave", NenType.EMITTER, 40, 200,
            4, 4f, 1.2f, new Look(Style.SHOCKWAVE, NenFx.EMITTER, 1f), SoundEvents.GENERIC_EXPLODE.value())).requiresNenLevel(5);

    // ---------- Illumi : aiguilles ----------
    public static final Technique ILLUMI_NEEDLE = register(new Strike("illumi_needle", NenType.MANIPULATOR, 25, 100,
            12, 0.97, 3f, 0f, List.of(new Effect(MobEffects.MOVEMENT_SLOWDOWN, 100, 2), new Effect(MobEffects.CONFUSION, 100, 0)),
            new Look(Style.NEEDLE, NenFx.MANIPULATOR, 1f), SoundEvents.ARROW_HIT));
    public static final Technique ILLUMI_TRANSFORM = register(new SelfBuff("illumi_transform", NenType.MANIPULATOR, 50, 600,
            List.of(new Effect(MobEffects.DAMAGE_RESISTANCE, 300, 0), new Effect(MobEffects.DAMAGE_BOOST, 300, 0)),
            new Look(Style.NEEDLE_CROWN, NenFx.DARK, 1f), SoundEvents.ZOMBIE_VILLAGER_CURE)).requiresNenLevel(10);

    // ---------- Neferpitou : spécialiste ----------
    public static final Technique PITOU_TERPSICHORA = register(new SelfBuff("pitou_terpsichora", NenType.SPECIALIST, 60, 600,
            List.of(new Effect(MobEffects.DAMAGE_BOOST, 200, 1), new Effect(MobEffects.MOVEMENT_SPEED, 200, 1),
                    new Effect(MobEffects.JUMP, 200, 1)),
            new Look(Style.PUPPET, NenFx.SPECIALIST, 1.4f), SoundEvents.WARDEN_ROAR)).requiresNenLevel(10);
    /**
     * Docteur Blythe, maintenu : la poupée-chirurgienne apparaît au-dessus du blessé devant Pitou (ou de Pitou s'il
     * n'y a personne) et le recoud tant que la touche est tenue (2 PV toutes les 0,5 s), en consommant 0,5 d'aura
     * par tick. Pitou reste immobile pendant l'opération ; la recharge part au relâchement.
     */
    public static final Technique PITOU_DOCTOR_BLYTHE = register(new DollHeal("pitou_doctor_blythe", NenType.SPECIALIST, 20, 400,
            2f, 0.5f));

    // =====================================================================
    // Hisoka (Transmutation)
    // =====================================================================
    /**
     * Bungee Gum : une aura élastique et collante attache la cible (12 blocs). Tant que le lien tient (10 s), elle est
     * ramenée vers Hisoka dès qu'elle s'éloigne de plus de 5 blocs. Relancer : le lien se contracte d'un coup.
     */
    public static final Technique HISOKA_BUNGEE_GUM = register(new BungeeGum("hisoka_bungee_gum", NenType.TRANSMUTER, 20, 30,
            12, 200));
    /** Texture Surprise : Hisoka recouvre son corps d'une fausse texture et disparaît 8 s ; les mobs le perdent de vue. */
    public static final Technique HISOKA_TEXTURE_SURPRISE = register(new TextureSurprise("hisoka_texture_surprise",
            NenType.TRANSMUTER, 40, 500, 160)).requiresNenLevel(10);
    /** Cartes renforcées : une carte à jouer lancée comme une lame (18 blocs). */
    public static final Technique HISOKA_CARDS = register(new CardThrow("hisoka_cards", NenType.TRANSMUTER, 10, 30, 6f));

    // =====================================================================
    // Types de techniques réutilisables
    // =====================================================================

    public record Effect(Holder<MobEffect> effect, int ticks, int amplifier) {
        MobEffectInstance create(float power) {
            // Sans les bulles d'effet de Minecraft (direction artistique : pas de particules vanilla)
            return new MobEffectInstance(effect, Math.round(ticks * Math.max(0.5f, power)), amplifier, false, false, true);
        }
    }

    public static DamageSource damageFrom(LivingEntity caster) {
        DamageSource base = caster instanceof Player player
                ? caster.damageSources().playerAttack(player)
                : caster.damageSources().mobAttack(caster);
        return new NenDamageSource(base.typeHolder(), caster, caster);
    }

    /** Allure d'une technique (direction artistique : formes dessinées, pas de particules de Minecraft). */
    public enum Style { IMPACT, SLASH, ORB, BOLT, CHAIN, NEEDLE, SHOCKWAVE, FLARE, CRACKLE, HOLY_CHAIN, WARP_FIST, NEEDLE_CROWN, PUPPET }

    /** Allure : style, couleur d'aura, taille. */
    public record Look(Style style, int color, float size) {}

    static void sound(LivingEntity at, SoundEvent sound) {
        at.level().playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.PLAYERS, 0.8f, 1f);
    }

    /** Effet visuel d'une frappe : sur la cible touchée, ou vers le point visé si elle rate. */
    static void showStrike(Look look, LivingEntity caster, @Nullable LivingEntity target, Vec3 end, int holdTicks) {
        Vec3 at = target != null ? target.getBoundingBox().getCenter() : end;
        switch (look.style()) {
            case IMPACT -> {
                NenFx.impact(caster, at, target != null ? look.size() : look.size() * 0.6f, look.color());
                if (target != null && look.size() >= 2f) NenFx.shockwave(target, look.size() * 1.2f, look.color());
            }
            case SLASH -> NenFx.slash(caster, at, look.size(), look.color());
            case ORB -> NenFx.orb(caster, target, end, look.size(), look.color());
            case WARP_FIST -> NenFx.warpFist(caster, target, end, look.size(), look.color());
            case BOLT -> {
                NenFx.bolt(caster, target, end, look.color());
                // La cible reste parcourue d'arcs le temps de la paralysie
                if (target != null) {
                    NenFx.crackle(target, look.color(), Math.max(20, holdTicks));
                    NenFx.shockwave(target, 1.6f, look.color());
                }
            }
            case CHAIN -> NenFx.chain(caster, target, end, look.color(), target != null ? holdTicks : 14);
            case NEEDLE -> NenFx.needle(caster, target, end, target != null ? holdTicks : 14);
            default -> NenFx.impact(caster, at, look.size(), look.color());
        }
    }

    /** Effet visuel sur soi (renforcements, soins). */
    static void showSelf(Look look, LivingEntity caster, int holdTicks) {
        switch (look.style()) {
            case CRACKLE -> {
                NenFx.flare(caster, look.size(), look.color());
                NenFx.crackle(caster, look.color(), holdTicks);
            }
            case HOLY_CHAIN -> NenFx.holyChain(caster, look.color(), 50);
            case PUPPET -> {
                NenFx.flare(caster, look.size(), look.color());
                NenFx.puppetStrings(caster, look.color(), holdTicks);
            }
            case NEEDLE_CROWN -> {
                NenFx.flare(caster, look.size(), look.color());
                NenFx.needleCrown(caster, look.color(), holdTicks);
            }
            default -> NenFx.flare(caster, look.size(), look.color());
        }
    }

    // ---------- Coups de base (sans Nen, dès le début, à la fin de la liste du menu) ----------

    /** Identifiants des coups de base, dans l'ordre du menu. */
    public static final List<String> BASE = List.of("basic_attack", "basic_kick", "basic_charged");

    public static final Technique BASIC_ATTACK = register(new BaseMove("basic_attack", 12, 4, 0.08f) {
        @Override
        public boolean perform(LivingEntity caster, float power) {
            return com.evandouard.hxh.combat.Combat.basicPunch(caster) != null;
        }
    });
    public static final Technique BASIC_KICK = register(new BaseMove("basic_kick", 30, 10, 0.2f) {
        @Override
        public boolean perform(LivingEntity caster, float power) {
            return com.evandouard.hxh.combat.Combat.kickNow(caster) != null;
        }
    });
    public static final Technique BASIC_CHARGED = register(new BaseMove("basic_charged", 40, 12, 0.28f) {
        @Override
        public boolean isCharged() {
            return true;
        }

        @Override
        public net.minecraft.network.chat.Component conditions() {
            return net.minecraft.network.chat.Component.translatable("technique.hxh.cond.strike");
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            com.evandouard.hxh.combat.Combat.startStrike(caster);
            return true;
        }
    });

    /** Coup de base : gratuit, sans Nen ; recharge {@code base} ticks, moins {@code perEndurance} par niveau d'Endurance. */
    public abstract static class BaseMove extends Technique {
        private final int min;
        private final float perEndurance;

        BaseMove(String id, int base, int min, float perEndurance) {
            super(id, NenType.ENHANCER, 0, base);
            this.min = min;
            this.perEndurance = perEndurance;
        }

        @Override
        public boolean isBase() {
            return true;
        }

        @Override
        public int cooldownFor(com.evandouard.hxh.data.HunterData data) {
            int endurance = data.level(com.evandouard.hxh.data.Stat.ENDURANCE);
            return Math.max(min, Math.round(cooldownTicks() - endurance * perEndurance));
        }

        @Override
        public net.minecraft.network.chat.Component conditions() {
            return net.minecraft.network.chat.Component.translatable("technique.hxh.cond.melee");
        }
    }

    /** Technique qu'on charge en maintenant la touche, puis qu'on lance en boule (voir {@link NenCharge}). */
    public static class ChargedOrb extends Technique {
        final float drainPerTick, minSpent, baseDamage, damagePerAura;
        final int maxTicks, color;
        /** Version sans retenue : la boule enfle une fois puis se recondense, aveuglante. */
        boolean epic;
        /** Multiplie la taille de la trace et de l'explosion. */
        float blastScale = 1f;

        ChargedOrb(String id, NenType category, int startCost, int cooldown, float drainPerTick, int maxTicks,
                   float minSpent, float baseDamage, float damagePerAura, int color) {
            super(id, category, startCost, cooldown);
            this.drainPerTick = drainPerTick;
            this.maxTicks = maxTicks;
            this.minSpent = minSpent;
            this.baseDamage = baseDamage;
            this.damagePerAura = damagePerAura;
            this.color = color;
        }

        ChargedOrb epic(float blastScale) {
            this.epic = true;
            this.blastScale = blastScale;
            return this;
        }

        public boolean isEpic() {
            return epic;
        }

        @Override
        public boolean isUltimate() {
            return isEpic() || super.isUltimate();
        }

        @Override
        public boolean isCharged() {
            return true;
        }

        @Override
        public net.minecraft.network.chat.Component conditions() {
            return net.minecraft.network.chat.Component.translatable("technique.hxh.cond.charged",
                    Math.round(drainPerTick * 20), maxTicks / 20);
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            NenCharge.start(caster, this, power);
            return true;
        }
    }

    /** Lame d'aura lancée tout droit (voir {@link NenProjectile#BLADE}). */
    public static class Blade extends Technique {
        private final float damage;
        private final int color;

        Blade(String id, NenType category, int cost, int cooldown, float damage, int color) {
            super(id, category, cost, cooldown);
            this.damage = damage;
            this.color = color;
        }

        @Override
        public net.minecraft.network.chat.Component conditions() {
            return net.minecraft.network.chat.Component.translatable("technique.hxh.cond.projectile",
                    (int) NenProjectile.BLADE_RANGE);
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            NenProjectile.launch(caster, NenProjectile.BLADE, power, damage * power, color);
            sound(caster, SoundEvents.PLAYER_ATTACK_SWEEP);
            return true;
        }
    }

    /** Technique décrite mais pas encore jouable (personnages en réflexion). */
    public static final class Idea extends Technique {
        public Idea(String id, NenType category) {
            super(id, category, 0, 0);
        }

        @Override
        public boolean implemented() {
            return false;
        }

        @Override
        public net.minecraft.network.chat.Component conditions() {
            return net.minecraft.network.chat.Component.translatable("technique.hxh.cond.idea");
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            return false;
        }
    }

    /** Enregistre une technique-idée (appelé par HatsuKits pour les personnages à venir). */
    static Technique idea(String id, NenType category) {
        return register(new Idea(id, category));
    }

    /** Frappe une cible devant soi (au corps à corps ou à distance). */
    static final class Strike extends Technique {
        private final double range, minDot;
        private final float damage, knockback;
        private final List<Effect> effects;
        private final Look look;
        private final SoundEvent sound;

        Strike(String id, NenType category, int cost, int cooldown, double range, double minDot, float damage,
               float knockback, List<Effect> effects, Look look, SoundEvent sound) {
            super(id, category, cost, cooldown);
            this.range = range;
            this.minDot = minDot;
            this.damage = damage;
            this.knockback = knockback;
            this.effects = effects;
            this.look = look;
            this.sound = sound;
        }

        /** Durée de l'effet le plus long (une chaîne reste enroulée, une aiguille reste plantée, aussi longtemps). */
        private int holdTicks(float power) {
            int t = 20;
            for (Effect e : effects) t = Math.max(t, Math.round(e.ticks() * Math.max(0.5f, power)));
            return t;
        }

        @Override
        public net.minecraft.network.chat.Component conditions() {
            return net.minecraft.network.chat.Component.translatable("technique.hxh.cond.target",
                    String.format(java.util.Locale.ROOT, "%.1f", range));
        }

        @Override
        public double aiRange() {
            return range;
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            LivingEntity target = Targeting.inFront(caster, range, minDot);
            if (target == null) {
                // Raté : la technique part quand même vers le point visé et abîme le décor qu'elle percute
                Vec3 end = caster.getEyePosition().add(caster.getLookAngle().scale(range));
                com.evandouard.hxh.combat.Impacts.missedStrike(caster, range, damage * power);
                showStrike(look, caster, null, end, 0);
                caster.level().playSound(null, end.x, end.y, end.z, sound, SoundSource.PLAYERS, 0.8f, 1f);
                return false;
            }
            showStrike(look, caster, target, target.getBoundingBox().getCenter(), holdTicks(power));
            target.invulnerableTime = 0;
            target.hurt(damageFrom(caster), damage * power);
            if (knockback > 0) target.knockback(knockback * power, caster.getX() - target.getX(), caster.getZ() - target.getZ());
            for (Effect e : effects) target.addEffect(e.create(power), caster);
            sound(target, sound);
            return true;
        }
    }

    /** Onde de choc autour du lanceur. */
    static final class Blast extends Technique {
        private final double radius;
        private final float damage, knockback;
        private final Look look;
        private final SoundEvent sound;

        Blast(String id, NenType category, int cost, int cooldown, double radius, float damage, float knockback,
              Look look, SoundEvent sound) {
            super(id, category, cost, cooldown);
            this.radius = radius;
            this.damage = damage;
            this.knockback = knockback;
            this.look = look;
            this.sound = sound;
        }

        @Override
        public net.minecraft.network.chat.Component conditions() {
            return net.minecraft.network.chat.Component.translatable("technique.hxh.cond.area",
                    String.format(java.util.Locale.ROOT, "%.0f", radius));
        }

        @Override
        public double aiRange() {
            return radius;
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            double radius = this.radius * NenAffinity.sizeFactor(caster, this); // plus petite hors de son type
            List<LivingEntity> targets = Targeting.around(caster, radius);
            for (LivingEntity t : targets) {
                t.invulnerableTime = 0;
                t.hurt(damageFrom(caster), damage * power);
                t.knockback(knockback * power, caster.getX() - t.getX(), caster.getZ() - t.getZ());
            }
            NenFx.shockwave(caster, (float) radius, look.color());
            NenFx.impact(caster, caster.position().add(0, 0.3, 0), 1.6f, look.color());
            for (LivingEntity t : targets) NenFx.impactOn(caster, t, 1f, look.color());
            sound(caster, sound);
            return true; // l'onde part même sans cible
        }
    }

    /**
     * Émission en cône depuis la paume (Papier de Gon) : touche toutes les cibles dans le cône et les projette dans
     * la direction du regard.
     */
    public static final class PalmWave extends Technique {
        private final double length, cosHalfAngle, halfAngle;
        private final float damage, push;
        private final int color;

        PalmWave(String id, NenType category, int cost, int cooldown, double length, double halfAngleDeg,
                 float damage, float push, int color) {
            super(id, category, cost, cooldown);
            this.length = length;
            this.halfAngle = halfAngleDeg;
            this.cosHalfAngle = Math.cos(Math.toRadians(halfAngleDeg));
            this.damage = damage;
            this.push = push;
            this.color = color;
        }

        @Override
        public net.minecraft.network.chat.Component conditions() {
            return net.minecraft.network.chat.Component.translatable("technique.hxh.cond.cone",
                    String.format(java.util.Locale.ROOT, "%.0f", length), String.format(java.util.Locale.ROOT, "%.0f", halfAngle));
        }

        @Override
        public double aiRange() {
            return length * 0.8;
        }

        /** Cibles dans le cône devant le regard. */
        public List<LivingEntity> targets(LivingEntity caster) {
            net.minecraft.world.phys.Vec3 eye = caster.getEyePosition();
            net.minecraft.world.phys.Vec3 look = caster.getLookAngle();
            List<LivingEntity> out = new java.util.ArrayList<>();
            for (LivingEntity e : caster.level().getEntitiesOfClass(LivingEntity.class,
                    caster.getBoundingBox().inflate(length), e -> e != caster && e.isAlive() && !e.isSpectator())) {
                net.minecraft.world.phys.Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
                double d = to.length();
                if (d < 1e-4 || d > length + e.getBbWidth()) continue;
                // tolérance : la largeur de la cible compte, pour qu'un mob tout proche ne passe pas « à côté »
                double slack = Math.atan2(e.getBbWidth() * 0.6, d);
                if (Math.acos(Math.min(1, to.normalize().dot(look))) > Math.acos(cosHalfAngle) + slack) continue;
                out.add(e);
            }
            return out;
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            net.minecraft.world.phys.Vec3 look = caster.getLookAngle();
            List<LivingEntity> hit = targets(caster);
            for (LivingEntity t : hit) {
                t.invulnerableTime = 0;
                t.hurt(damageFrom(caster), damage * power);
                double kb = push * power * (1 - t.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE));
                t.setDeltaMovement(t.getDeltaMovement().add(look.x * kb, 0.35 + Math.max(0, look.y) * kb, look.z * kb));
                t.hurtMarked = true;
                NenFx.impactOn(caster, t, 1.3f, color);
            }
            NenFx.palmWave(caster, length, color);
            caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.FIREWORK_ROCKET_BLAST, net.minecraft.sounds.SoundSource.PLAYERS, 1f, 0.6f);
            caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), net.minecraft.sounds.SoundSource.PLAYERS, 1f, 0.8f);
            return true; // l'aura part même sans cible
        }
    }

    /** Soin d'un patient par une poupée invoquée (Docteur Blythe). */
    public static final class DollHeal extends Technique {
        private final float perPulse;
        private final float drain;

        DollHeal(String id, NenType category, int cost, int cooldown, float perPulse, float drain) {
            super(id, category, cost, cooldown);
            this.perPulse = perPulse;
            this.drain = drain;
        }

        public float drain() {
            return drain;
        }

        @Override
        public boolean isHeal() {
            return true;
        }

        /** Maintenu : la recharge part au relâchement. */
        @Override
        public boolean isCharged() {
            return true;
        }

        @Override
        public net.minecraft.network.chat.Component conditions() {
            return net.minecraft.network.chat.Component.translatable("technique.hxh.cond.hold_doll",
                    String.format(java.util.Locale.ROOT, "%.0f", drain * 20));
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            LivingEntity patient = com.evandouard.hxh.nen.Targeting.inFront(caster, 6, 0.8);
            if (patient == null) patient = caster;
            LingeringNen.startHeldDoll(caster, patient, this, perPulse * power, drain);
            caster.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, LingeringNen.DOLL_MAX_HOLD, 6,
                    false, false, false));
            sound(caster, SoundEvents.AMETHYST_BLOCK_CHIME);
            return true;
        }
    }

    /** Bungee Gum d'Hisoka : attacher, puis contracter. */
    public static final class BungeeGum extends Technique {
        private final double range;
        private final int ticks;

        BungeeGum(String id, NenType category, int cost, int cooldown, double range, int ticks) {
            super(id, category, cost, cooldown);
            this.range = range;
            this.ticks = ticks;
        }

        @Override
        public net.minecraft.network.chat.Component conditions() {
            return net.minecraft.network.chat.Component.translatable("technique.hxh.cond.gum", (int) range);
        }

        @Override
        public double aiRange() {
            return range * 0.8;
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            if (LingeringNen.snapGum(caster, power)) {
                sound(caster, SoundEvents.SLIME_JUMP);
                return true;
            }
            LivingEntity target = com.evandouard.hxh.nen.Targeting.inFront(caster, range, 0.9);
            if (target == null) return false;
            LingeringNen.attachGum(caster, target, ticks);
            sound(caster, SoundEvents.SLIME_SQUISH);
            return true;
        }
    }

    /** Texture Surprise : camouflage. */
    public static final class TextureSurprise extends Technique {
        private final int ticks;

        TextureSurprise(String id, NenType category, int cost, int cooldown, int ticks) {
            super(id, category, cost, cooldown);
            this.ticks = ticks;
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            com.evandouard.hxh.nen.NenSenses.nenHide(caster, ticks);
            for (net.minecraft.world.entity.Mob mob : caster.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                    caster.getBoundingBox().inflate(24), m -> m.getTarget() == caster)) {
                mob.setTarget(null);
            }
            NenFx.cardBurst(caster);
            sound(caster, SoundEvents.BOOK_PAGE_TURN);
            return true;
        }
    }

    /** Carte à jouer renforcée lancée comme une lame. */
    public static final class CardThrow extends Technique {
        private final float damage;

        CardThrow(String id, NenType category, int cost, int cooldown, float damage) {
            super(id, category, cost, cooldown);
            this.damage = damage;
        }

        @Override
        public net.minecraft.network.chat.Component conditions() {
            return net.minecraft.network.chat.Component.translatable("technique.hxh.cond.projectile",
                    (int) NenProjectile.CARD_RANGE);
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            NenProjectile.launch(caster, NenProjectile.CARD, power, damage * power, NenFx.HISOKA);
            sound(caster, SoundEvents.PLAYER_ATTACK_SWEEP);
            return true;
        }
    }

    /** Renforcement sur soi. */
    static final class SelfBuff extends Technique {
        private final List<Effect> effects;
        private final Look look;
        private final SoundEvent sound;

        SelfBuff(String id, NenType category, int cost, int cooldown, List<Effect> effects,
                 Look look, SoundEvent sound) {
            super(id, category, cost, cooldown);
            this.effects = effects;
            this.look = look;
            this.sound = sound;
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            int hold = 20;
            for (Effect e : effects) {
                MobEffectInstance inst = e.create(power);
                caster.addEffect(inst, caster);
                hold = Math.max(hold, inst.getDuration());
            }
            showSelf(look, caster, hold);
            sound(caster, sound);
            return true;
        }
    }

    /** Soin instantané sur soi. */
    static final class SelfHeal extends Technique {
        private final float amount;
        private final Look look;
        private final SoundEvent sound;

        SelfHeal(String id, NenType category, int cost, int cooldown, float amount,
                 Look look, SoundEvent sound) {
            super(id, category, cost, cooldown);
            this.amount = amount;
            this.look = look;
            this.sound = sound;
        }

        @Override
        public boolean isHeal() {
            return true;
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            caster.heal(amount * power);
            showSelf(look, caster, 40);
            sound(caster, sound);
            return true;
        }
    }

    // Coups secrets de famille (enregistrés après tout le reste)
    static {
        FamilyMoves.init();
        ArenaChallengers.init();
    }
}
