package com.evandouard.hxh.hatsu;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.fx.NenFx;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Serments et restrictions (« Contrainte et Serment ») : on s'impose une condition, les techniques de Hatsu
 * gagnent en puissance. Un seul serment à la fois. Le rompre (ou y renoncer) scelle le Hatsu un moment : c'est le
 * prix du serment. Marche pour toute entité : un mob peut avoir un serment dans son préréglage.
 */
public final class Vows {
    private Vows() {}

    /** Durée du sceau après un serment rompu ou abandonné : 10 minutes. */
    public static final int BREAK_SEAL_TICKS = 12000;
    /** Serment ultime : 60 s de puissance, puis le Hatsu est scellé une journée de jeu. */
    public static final int ULTIMATE_TICKS = 1200;
    /** Après « Tout sacrifier » : tout le Nen scellé une dizaine de jours de jeu. */
    public static final int ULTIMATE_SEAL_TICKS = 24000 * 10;
    /** Prix du serment de vie, en PV par technique. */
    public static final float LIFE_PRICE = 2f;

    public enum Vow {
        /** Kurapika : les techniques ne visent que les monstres. Toucher un humain, un animal ou un PNJ le rompt. */
        MONSTERS_ONLY(1.5f),
        /** Temps de l'Empereur : chaque technique coûte 2 PV en plus de l'aura (refusée sous 2 PV). */
        LIFE_PRICE(1.4f),
        /** Mains nues : aucune technique avec un objet en main (simple refus, pas de rupture). */
        BARE_HANDS(1.25f),
        /** Combat loyal : jamais dans le dos. Toucher une cible qui ne te fait pas face le rompt. */
        FAIR_FIGHT(1.3f),
        /** Une seule technique : celle choisie au serment, les autres sont refusées. */
        SINGLE_TECHNIQUE(1.6f),
        /** Gon adulte : tout sacrifier. ×3 pendant 60 s, puis Hatsu scellé une journée. */
        ULTIMATE(3.0f),
        /** Heure supplémentaire : tout ×0,7 le jour, ×1,3 la nuit. */
        OVERTIME(1.3f),
        /** Service de jour : tout ×1,3 le jour, ×0,7 la nuit (l'inverse d'Heure supplémentaire). */
        DAY_SHIFT(1.3f),
        /** Serment propre au personnage (voir {@link CharacterVows}) : puissance et condition selon le kit. */
        CHARACTER(1.0f);

        public final float power;

        Vow(float power) {
            this.power = power;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public String translationKey() {
            return "vow.hxh." + id();
        }

        public String descriptionKey() {
            return "vow.hxh." + id() + ".desc";
        }

        @Nullable
        public static Vow byId(String id) {
            for (Vow v : values()) if (v.id().equals(id)) return v;
            return null;
        }
    }

    public static void registerEvents(IEventBus bus) {
        bus.addListener(EventPriority.LOW, LivingIncomingDamageEvent.class, Vows::onDamage);
    }

    @Nullable
    public static Vow of(HunterData data) {
        return Vow.byId(data.vow());
    }

    /** Serment en vigueur (prêté et pas mis en pause). */
    @Nullable
    public static Vow active(HunterData data) {
        Vow v = of(data);
        return v != null && !(toggleable(v) && data.vowOff()) ? v : null;
    }

    /** Serments à condition, qu'on peut mettre en pause et reprendre sans les rompre. */
    public static boolean toggleable(Vow v) {
        return v == Vow.FAIR_FIGHT || v == Vow.MONSTERS_ONLY || v == Vow.BARE_HANDS;
    }

    /**
     * Multiplicateur des stats sous serment : tout est multiplié (Force, Vitesse… et donc l'aura via la Volonté),
     * même au-delà des limites. Pas pour « Une seule technique », qui ne porte que sur sa technique.
     */
    public static float statMultiplier(HunterData data) {
        Vow v = active(data);
        if (v == null || v == Vow.SINGLE_TECHNIQUE || v == Vow.CHARACTER) return 1f;
        return timedPower(data, v);
    }

    /** Multiplicateur réduit des serments jour/nuit. */
    public static final float TIMED_LOW = 0.7f;

    /** Heure supplémentaire / Service de jour : ×1,3 dans sa période, ×0,7 dans l'autre ; les autres : leur puissance. */
    public static float timedPower(HunterData data, Vow v) {
        if (v == Vow.OVERTIME) return data.vowNight() ? v.power : TIMED_LOW;
        if (v == Vow.DAY_SHIFT) return data.vowNight() ? TIMED_LOW : v.power;
        return v.power;
    }

    /** Nuit dans le monde de l'entité (sans cycle jour/nuit, comme à la Tour Céleste : toujours le jour). */
    public static boolean isNight(LivingEntity entity) {
        if (entity.level().dimensionType().hasFixedTime()) return false;
        long t = entity.level().getDayTime() % 24000L;
        return t >= 13000L && t < 23000L;
    }

    /** Serment de personnage en cours, ou null. */
    @Nullable
    public static CharacterVows.Def characterVow(HunterData data) {
        return active(data) == Vow.CHARACTER ? CharacterVows.forKit(data.kit()) : null;
    }

    /** Nom du serment (celui du personnage pour un serment de personnage). */
    public static Component name(HunterData data, Vow v) {
        CharacterVows.Def def = v == Vow.CHARACTER ? CharacterVows.forKit(data.kit()) : null;
        return Component.translatable(def != null ? def.translationKey() : v.translationKey());
    }

    public static boolean nenSealed(HunterData d, long gameTime) {
        return gameTime < d.nenSealedUntil();
    }

    /** Met en pause ou reprend un serment à condition. @return clé du message */
    public static String toggle(LivingEntity entity) {
        HunterData d = HunterStats.get(entity);
        Vow v = of(d);
        if (v == null) return "message.hxh.vow_none";
        if (!toggleable(v)) return "message.hxh.vow_not_toggleable";
        d.setVowOff(!d.vowOff());
        com.evandouard.hxh.stats.HunterStats.applyAttributes(entity);
        return d.vowOff() ? "message.hxh.vow_paused" : "message.hxh.vow_resumed";
    }

    public static boolean sealed(HunterData data, long gameTime) {
        return gameTime < data.sealedUntil();
    }

    // ---------- Prêter, rompre ----------

    /** Prête un serment. @return null si accepté, sinon la clé du refus */
    @Nullable
    public static String take(LivingEntity entity, String vowId) {
        HunterData d = HunterStats.get(entity);
        Vow v = Vow.byId(vowId);
        if (v == null) return "message.hxh.vow_unknown";
        if (!d.hasKit()) return "message.hxh.vow_need_hatsu";
        if (sealed(d, entity.level().getGameTime())) return "message.hxh.vow_sealed";
        if (!d.vow().isEmpty()) return "message.hxh.vow_already";
        String tech = "";
        if (v == Vow.CHARACTER) {
            CharacterVows.Def def = CharacterVows.forKit(d.kit());
            if (def == null) return "message.hxh.vow_no_character";
            tech = def.technique();
        }
        if (v == Vow.SINGLE_TECHNIQUE) {
            Technique t = Hatsu.selectedTechnique(d);
            if (t == null || t.needsNoNen()) return "message.hxh.vow_select_technique";
            tech = t.id();
        }
        d.setVow(v.id(), tech, v == Vow.ULTIMATE ? entity.level().getGameTime() + ULTIMATE_TICKS : 0L);
        d.setVowOff(false);
        d.setVowNight(isNight(entity));
        HunterStats.applyAttributes(entity);
        NenFx.flare(entity, v == Vow.ULTIMATE ? 2.4f : 1.2f, d.auraColor());
        entity.level().playSound(null, entity.blockPosition(), v == Vow.ULTIMATE ? SoundEvents.WITHER_SPAWN : SoundEvents.BEACON_ACTIVATE,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 0.8f);
        if (entity instanceof Player p) {
            p.displayClientMessage(Component.translatable("message.hxh.vow_taken", name(d, v))
                    .withStyle(ChatFormatting.GOLD), false);
        }
        return null;
    }

    /** Le serment est rompu (ou abandonné) : il disparaît et le Hatsu est scellé. */
    public static void breakVow(LivingEntity entity, String reasonKey) {
        HunterData d = HunterStats.get(entity);
        if (d.vow().isEmpty()) return;
        d.setVow("", "", 0L);
        d.setSealedUntil(entity.level().getGameTime() + BREAK_SEAL_TICKS);
        HunterStats.applyAttributes(entity);
        NenFx.flare(entity, 1.6f, 0x111111);
        entity.level().playSound(null, entity.blockPosition(), SoundEvents.CHAIN_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1f, 0.6f);
        if (entity instanceof Player p) {
            p.displayClientMessage(Component.translatable("message.hxh.vow_broken", Component.translatable(reasonKey),
                    BREAK_SEAL_TICKS / 1200).withStyle(ChatFormatting.DARK_RED), false);
        }
    }

    /** Effet de rappel du serment ; fin du serment ultime : le prix tombe. */
    public static void tick(LivingEntity entity) {
        HunterData d = HunterStats.get(entity);
        if (entity.tickCount % 20 == 0) {
            // Serments jour/nuit : la puissance bascule au lever et au coucher du soleil
            Vow cur = of(d);
            if (cur == Vow.OVERTIME || cur == Vow.DAY_SHIFT) {
                boolean night = isNight(entity);
                if (night != d.vowNight()) {
                    d.setVowNight(night);
                    HunterStats.applyAttributes(entity);
                    if (entity instanceof Player p) p.displayClientMessage(Component.translatable("message.hxh.vow_timed_switch",
                            Component.translatable(cur.translationKey()), "×" + timedPower(d, cur)).withStyle(ChatFormatting.GOLD), true);
                }
            }
            boolean on = active(d) != null;
            boolean has = entity.hasEffect(com.evandouard.hxh.effect.ModEffects.VOW);
            if (on && !has) entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.evandouard.hxh.effect.ModEffects.VOW,
                    net.minecraft.world.effect.MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
            else if (!on && has) entity.removeEffect(com.evandouard.hxh.effect.ModEffects.VOW);
        }
        if (d.vowUntil() <= 0 || entity.level().getGameTime() < d.vowUntil()) return;
        d.setVow("", "", 0L);
        d.setSealedUntil(entity.level().getGameTime() + ULTIMATE_SEAL_TICKS);
        d.setNenSealedUntil(entity.level().getGameTime() + ULTIMATE_SEAL_TICKS);
        HunterStats.applyAttributes(entity);
        if (entity instanceof Player p) {
            p.displayClientMessage(Component.translatable("message.hxh.vow_ultimate_over").withStyle(ChatFormatting.DARK_RED), false);
        }
    }

    // ---------- Effets ----------

    /** Refus lisible sans l'entité (HUD côté client compris) : sceau, technique unique. */
    @Nullable
    public static String dataRefusal(HunterData d, Technique t, long gameTime) {
        if (t.needsNoNen()) return null;
        if (nenSealed(d, gameTime)) return "message.hxh.nen_sealed";
        if (sealed(d, gameTime)) return "message.hxh.vow_sealed";
        if (active(d) == Vow.SINGLE_TECHNIQUE && !t.id().equals(d.vowTechnique())) return "message.hxh.vow_single";
        return null;
    }

    /** Refus qui dépend de l'entité (main occupée, vie trop basse). */
    @Nullable
    public static String entityRefusal(LivingEntity entity, HunterData d, Technique t) {
        if (t.needsNoNen()) return null;
        Vow v = active(d);
        if (v == Vow.BARE_HANDS && !entity.getMainHandItem().isEmpty()) return "message.hxh.vow_bare_hands";
        if (v == Vow.LIFE_PRICE && entity.getHealth() <= LIFE_PRICE + 0.01f) return "message.hxh.vow_life_low";
        CharacterVows.Def def = characterVow(d);
        if (def != null && def.condition() == CharacterVows.Condition.CASTER_HURT && t.id().equals(def.technique())
                && entity.getHealth() > entity.getMaxHealth() * def.ratio()) return "message.hxh.vow_character_hurt";
        return null;
    }

    /** Prix payé au lancement (serment de vie). */
    public static void pay(LivingEntity entity, HunterData d, Technique t) {
        if (t.needsNoNen() || active(d) != Vow.LIFE_PRICE) return;
        entity.setHealth(Math.max(1f, entity.getHealth() - LIFE_PRICE));
    }

    /** Multiplicateur de puissance d'une technique sous serment. */
    public static float powerMultiplier(HunterData d, Technique t) {
        if (t.needsNoNen()) return 1f;
        Vow v = active(d);
        if (v == null) return 1f;
        if (v == Vow.SINGLE_TECHNIQUE && !t.id().equals(d.vowTechnique())) return 1f;
        if (v == Vow.CHARACTER) {
            CharacterVows.Def def = CharacterVows.forKit(d.kit());
            return def != null && t.id().equals(def.technique()) ? def.power() : 1f;
        }
        return timedPower(d, v);
    }

    /**
     * Les techniques touchent : un serment à condition non respectée (cible de dos, cible qui n'est pas un monstre)
     * annule le coup au lieu de rompre le serment.
     */
    private static void onDamage(LivingIncomingDamageEvent event) {
        if (!NenDamageSource.isNen(event.getSource())) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity caster) || caster == event.getEntity()) return;
        if (!caster.hasData(ModAttachments.HUNTER)) return;
        Vow v = active(HunterStats.get(caster));
        if (v == null) return;
        LivingEntity target = event.getEntity();
        if (v == Vow.CHARACTER) {
            characterHit(event, caster, target);
            return;
        }
        boolean refused = (v == Vow.MONSTERS_ONLY && !(target instanceof Enemy)) || (v == Vow.FAIR_FIGHT && !facing(target, caster));
        if (refused) {
            event.setCanceled(true);
            if (caster instanceof Player p) p.displayClientMessage(Component.translatable(v == Vow.FAIR_FIGHT
                    ? "message.hxh.vow_blocked_back" : "message.hxh.vow_blocked_monster").withStyle(ChatFormatting.GOLD), true);
        }
    }

    /**
     * Serment de personnage à cible imposée (Kurapika) : la technique jurée touche quelqu'un hors de l'organisation
     * → le coup ne porte pas et la pénalité tombe (la mort pour Kurapika, comme dans le manga).
     */
    private static void characterHit(LivingIncomingDamageEvent event, LivingEntity caster, LivingEntity target) {
        CharacterVows.Def def = characterVow(HunterStats.get(caster));
        if (def == null || def.condition() != CharacterVows.Condition.TARGET_ORG) return;
        Technique t = com.evandouard.hxh.combat.AreaScaling.castingOf(caster);
        if (t == null || !t.id().equals(def.technique())) return;
        if (com.evandouard.hxh.org.Organizations.belongs(target, def.org())) return;
        event.setCanceled(true);
        punish(caster, def);
    }

    /** Pénalité d'un serment de personnage enfreint. */
    public static void punish(LivingEntity caster, CharacterVows.Def def) {
        switch (def.penalty()) {
            case DEATH -> {
                breakVow(caster, "vow.hxh.reason.character");
                if (caster instanceof Player p) p.displayClientMessage(Component.translatable("message.hxh.vow_character_death")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false);
                NenFx.flare(caster, 2f, 0xFF8B0000);
                caster.hurt(caster.damageSources().magic(), Float.MAX_VALUE);
            }
            case SEAL -> breakVow(caster, "vow.hxh.reason.character");
            case REFUSE -> { }
        }
    }

    /** La cible regarde-t-elle vers le lanceur (moins de 90° entre son regard et la direction du lanceur) ? */
    public static boolean facing(LivingEntity target, LivingEntity caster) {
        Vec3 look = Vec3.directionFromRotation(0, target.getYHeadRot());
        Vec3 to = caster.position().subtract(target.position()).multiply(1, 0, 1);
        return to.lengthSqr() < 1e-4 || look.dot(to.normalize()) > 0;
    }
}
