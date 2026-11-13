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
    public static final int ULTIMATE_SEAL_TICKS = 24000;
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
        ULTIMATE(3.0f);

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
        if (v == Vow.SINGLE_TECHNIQUE) {
            Technique t = Hatsu.selectedTechnique(d);
            if (t == null || t.needsNoNen()) return "message.hxh.vow_select_technique";
            tech = t.id();
        }
        d.setVow(v.id(), tech, v == Vow.ULTIMATE ? entity.level().getGameTime() + ULTIMATE_TICKS : 0L);
        NenFx.flare(entity, v == Vow.ULTIMATE ? 2.4f : 1.2f, d.auraColor());
        entity.level().playSound(null, entity.blockPosition(), v == Vow.ULTIMATE ? SoundEvents.WITHER_SPAWN : SoundEvents.BEACON_ACTIVATE,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 0.8f);
        if (entity instanceof Player p) {
            p.displayClientMessage(Component.translatable("message.hxh.vow_taken", Component.translatable(v.translationKey()))
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
        NenFx.flare(entity, 1.6f, 0x111111);
        entity.level().playSound(null, entity.blockPosition(), SoundEvents.CHAIN_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1f, 0.6f);
        if (entity instanceof Player p) {
            p.displayClientMessage(Component.translatable("message.hxh.vow_broken", Component.translatable(reasonKey),
                    BREAK_SEAL_TICKS / 1200).withStyle(ChatFormatting.DARK_RED), false);
        }
    }

    /** Fin du serment ultime : le prix tombe. */
    public static void tick(LivingEntity entity) {
        HunterData d = HunterStats.get(entity);
        if (d.vowUntil() <= 0 || entity.level().getGameTime() < d.vowUntil()) return;
        d.setVow("", "", 0L);
        d.setSealedUntil(entity.level().getGameTime() + ULTIMATE_SEAL_TICKS);
        if (entity instanceof Player p) {
            p.displayClientMessage(Component.translatable("message.hxh.vow_ultimate_over").withStyle(ChatFormatting.DARK_RED), false);
        }
    }

    // ---------- Effets ----------

    /** Refus lisible sans l'entité (HUD côté client compris) : sceau, technique unique. */
    @Nullable
    public static String dataRefusal(HunterData d, Technique t, long gameTime) {
        if (t.needsNoNen()) return null;
        if (sealed(d, gameTime)) return "message.hxh.vow_sealed";
        if (of(d) == Vow.SINGLE_TECHNIQUE && !t.id().equals(d.vowTechnique())) return "message.hxh.vow_single";
        return null;
    }

    /** Refus qui dépend de l'entité (main occupée, vie trop basse). */
    @Nullable
    public static String entityRefusal(LivingEntity entity, HunterData d, Technique t) {
        if (t.needsNoNen()) return null;
        Vow v = of(d);
        if (v == Vow.BARE_HANDS && !entity.getMainHandItem().isEmpty()) return "message.hxh.vow_bare_hands";
        if (v == Vow.LIFE_PRICE && entity.getHealth() <= LIFE_PRICE + 0.01f) return "message.hxh.vow_life_low";
        return null;
    }

    /** Prix payé au lancement (serment de vie). */
    public static void pay(LivingEntity entity, HunterData d, Technique t) {
        if (t.needsNoNen() || of(d) != Vow.LIFE_PRICE) return;
        entity.setHealth(Math.max(1f, entity.getHealth() - LIFE_PRICE));
    }

    /** Multiplicateur de puissance d'une technique sous serment. */
    public static float powerMultiplier(HunterData d, Technique t) {
        if (t.needsNoNen()) return 1f;
        Vow v = of(d);
        if (v == null) return 1f;
        if (v == Vow.SINGLE_TECHNIQUE && !t.id().equals(d.vowTechnique())) return 1f;
        return v.power;
    }

    /** Les techniques touchent : vérifie les serments qui portent sur la cible. */
    private static void onDamage(LivingIncomingDamageEvent event) {
        if (!NenDamageSource.isNen(event.getSource())) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity caster) || caster == event.getEntity()) return;
        if (!caster.hasData(ModAttachments.HUNTER)) return;
        HunterData d = HunterStats.get(caster);
        Vow v = of(d);
        if (v == null) return;
        LivingEntity target = event.getEntity();
        if (v == Vow.MONSTERS_ONLY && !(target instanceof Enemy)) {
            breakVow(caster, "vow.hxh.reason.monsters_only");
        } else if (v == Vow.FAIR_FIGHT && !facing(target, caster)) {
            breakVow(caster, "vow.hxh.reason.fair_fight");
        }
    }

    /** La cible regarde-t-elle vers le lanceur (moins de 90° entre son regard et la direction du lanceur) ? */
    public static boolean facing(LivingEntity target, LivingEntity caster) {
        Vec3 look = Vec3.directionFromRotation(0, target.getYHeadRot());
        Vec3 to = caster.position().subtract(target.position()).multiply(1, 0, 1);
        return to.lengthSqr() < 1e-4 || look.dot(to.normalize()) > 0;
    }
}
