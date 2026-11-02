package com.evandouard.hxh.hatsu;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.NenMode;
import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Logique serveur du Hatsu : choix définitif du personnage, technique sélectionnée, lancement.
 * Identique pour un joueur et un mob.
 */
public final class Hatsu {
    private Hatsu() {}

    @Nullable
    public static HatsuKit kitOf(HunterData data) {
        return data.hasKit() ? HatsuKits.get(data.kit()) : null;
    }

    @Nullable
    public static Technique selectedTechnique(HunterData data) {
        HatsuKit kit = kitOf(data);
        if (kit == null || kit.techniques().isEmpty()) return null;
        return Techniques.get(kit.techniques().get(Math.floorMod(data.selected(), kit.techniques().size())));
    }

    /**
     * Raison pour laquelle on ne peut pas choisir ce personnage, ou null si c'est possible.
     * Utilisé par le serveur et par l'écran de choix pour griser les boutons.
     */
    @Nullable
    public static String chooseRefusal(HunterData data, HatsuKit kit) {
        if (!data.nenUnlocked()) return "message.hxh.nen_locked";
        if (!data.nenRevealed() || data.nenType() == null) return "message.hxh.hatsu_need_divination";
        if (data.hasKit()) return "message.hxh.hatsu_already_chosen";
        if (!kit.implemented()) return "message.hxh.hatsu_not_available";
        if (kit.type() != data.nenType()) return "message.hxh.hatsu_wrong_type";
        return null;
    }

    /** Choix définitif. @return null si accepté, sinon la clé de traduction du refus */
    @Nullable
    public static String choose(LivingEntity entity, String kitId) {
        HatsuKit kit = HatsuKits.get(kitId);
        if (kit == null) return "message.hxh.hatsu_unknown";
        HunterData data = HunterStats.get(entity);
        String refusal = chooseRefusal(data, kit);
        if (refusal != null) return refusal;
        data.setKit(kit.id());
        return null;
    }

    // ---------- Passifs ----------

    /** Passifs actifs : ceux du personnage choisi qui sont jouables (rien en Zetsu : l'aura est coupée). */
    public static java.util.List<Passive> activePassives(LivingEntity entity) {
        if (!entity.hasData(com.evandouard.hxh.data.ModAttachments.HUNTER)) return java.util.List.of();
        HunterData data = HunterStats.get(entity);
        HatsuKit kit = kitOf(data);
        if (kit == null || !data.nenUnlocked() || data.mode() == NenMode.ZETSU) return java.util.List.of();
        return kit.passives().stream().filter(Passive::implemented).toList();
    }

    public static void tickPassives(LivingEntity entity) {
        for (Passive p : activePassives(entity)) p.tick(entity);
    }

    public static float modifyIncoming(LivingEntity victim, net.minecraft.world.damagesource.DamageSource source, float amount) {
        for (Passive p : activePassives(victim)) amount = p.modifyIncoming(victim, source, amount);
        return amount;
    }

    public static float modifyOutgoingMelee(LivingEntity attacker, LivingEntity target, float amount) {
        for (Passive p : activePassives(attacker)) amount = p.modifyOutgoingMelee(attacker, target, amount);
        return amount;
    }

    /** Passe à la technique suivante (boucle), sans délai. */
    public static void cycle(LivingEntity entity) {
        HunterData data = HunterStats.get(entity);
        HatsuKit kit = kitOf(data);
        if (kit == null || kit.techniques().isEmpty()) return;
        data.setSelected((data.selected() + 1) % kit.techniques().size());
    }

    /** Puissance : affinité de l'hexagone × bonus de Volonté (+1 % par niveau). */
    public static float power(HunterData data, Technique technique) {
        NenType type = data.nenType();
        float affinity = type == null ? 0.4f : type.affinity(technique.category());
        return affinity * (1f + data.level(Stat.WILLPOWER) * 0.01f);
    }

    /**
     * Raison pour laquelle la technique ne peut pas partir maintenant, ou null si elle est prête.
     * Partagé avec l'encart du HUD côté client.
     */
    @Nullable
    public static String castRefusal(HunterData data, Technique technique, long gameTime) {
        if (!data.nenUnlocked()) return "message.hxh.nen_locked";
        if (data.mode() == NenMode.ZETSU) return "message.hxh.hatsu_zetsu";
        if (gameTime < data.readyAt(technique.id())) return "message.hxh.hatsu_cooldown";
        if (data.aura() < technique.auraCost()) return "message.hxh.no_aura";
        return null;
    }

    /** Lance la technique sélectionnée. @return null si lancée, sinon la clé du refus */
    @Nullable
    public static String cast(LivingEntity entity) {
        HunterData data = HunterStats.get(entity);
        Technique technique = selectedTechnique(data);
        if (technique == null) return "message.hxh.hatsu_none";
        long now = entity.level().getGameTime();
        String refusal = castRefusal(data, technique, now);
        if (refusal != null) return refusal;
        // Raté ou pas, l'aura et la recharge partent : lancer dans le vide a un prix
        boolean hit = technique.perform(entity, power(data, technique));
        data.setAura(data.aura() - technique.auraCost());
        data.setReadyAt(technique.id(), now + technique.cooldownTicks());
        return hit ? null : "message.hxh.hatsu_missed";
    }

    public static void requestCast(LivingEntity entity) {
        String refusal = cast(entity);
        if (refusal != null && entity instanceof Player player) {
            player.displayClientMessage(Component.translatable(refusal), true);
        }
    }

    public static void requestChoose(LivingEntity entity, String kitId) {
        String refusal = choose(entity, kitId);
        if (entity instanceof Player player) {
            HatsuKit kit = HatsuKits.get(kitId);
            player.displayClientMessage(refusal != null || kit == null
                    ? Component.translatable(refusal == null ? "message.hxh.hatsu_unknown" : refusal)
                    : Component.translatable("message.hxh.hatsu_chosen", Component.translatable(kit.translationKey())), false);
        }
    }
}
