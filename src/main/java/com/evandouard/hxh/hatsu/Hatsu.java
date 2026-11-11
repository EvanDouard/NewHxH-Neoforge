package com.evandouard.hxh.hatsu;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.NenMode;
import com.evandouard.hxh.nen.NenModes;
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
        java.util.List<String> ids = techniqueIds(data);
        return Techniques.get(ids.get(Math.floorMod(data.selected(), ids.size())));
    }

    /** Menu des pouvoirs : techniques du personnage, puis les coups de base (toujours là, même sans Nen). */
    public static java.util.List<String> techniqueIds(HunterData data) {
        HatsuKit kit = kitOf(data);
        java.util.List<String> ids = new java.util.ArrayList<>();
        if (kit != null) ids.addAll(kit.techniques());
        // Ging : la technique copiée prend place dans la liste tant qu'il reste des utilisations
        Technique copied = Characters.gingCopied(data);
        if (copied != null && !ids.contains(copied.id())) ids.add(copied.id());
        ids.addAll(Techniques.BASE);
        return ids;
    }

    /**
     * Raison pour laquelle on ne peut pas choisir ce personnage, ou null si c'est possible.
     * Utilisé par le serveur et par l'écran de choix pour griser les boutons.
     */
    @Nullable
    public static String chooseRefusal(HunterData data, HatsuKit kit) {
        return chooseRefusal(data, kit, false);
    }

    /** En créatif ({@code free}) : n'importe quel personnage jouable, à tout moment, même pour en changer. */
    @Nullable
    public static String chooseRefusal(HunterData data, HatsuKit kit, boolean free) {
        if (free) return kit.implemented() ? null : "message.hxh.hatsu_not_available";
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
        if (kit == null || MobKits.get(kitId) != null) return "message.hxh.hatsu_unknown"; // Hatsu de mob : pas au choix
        HunterData data = HunterStats.get(entity);
        boolean free = entity instanceof net.minecraft.world.entity.player.Player p && p.isCreative();
        if (!free && com.evandouard.hxh.nen.NenLearning.refusal(entity, com.evandouard.hxh.nen.NenLearning.Ability.HATSU) != null) {
            return com.evandouard.hxh.nen.NenLearning.refusal(entity, com.evandouard.hxh.nen.NenLearning.Ability.HATSU);
        }
        String refusal = chooseRefusal(data, kit, free);
        if (refusal != null) return refusal;
        if (free) {
            // Créatif : Nen éveillé et type aligné sur le personnage, pour pouvoir l'essayer tout de suite
            if (!data.nenUnlocked()) NenModes.unlock(entity);
            data.setNenType(kit.type());
            data.setNenRevealed(true);
            data.setSelected(0);
        }
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
        Specials.cleanup(entity);
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
        data.setSelected((data.selected() + 1) % techniqueIds(data).size());
    }

    /** Puissance : affinité de l'hexagone × bonus de Volonté (+1 % par niveau). */
    /** Puissance pour ce lanceur : un mob au kit de Gon a un Jajanken affaibli (le vrai reste au joueur). */
    public static float powerFor(LivingEntity entity, HunterData data, Technique technique) {
        float p = power(data, technique);
        if (!(entity instanceof Player) && "gon".equals(data.kit())) p *= MOB_GON_POWER;
        return p;
    }

    /** Facteur du Jajanken d'un mob (zombies de Gon…). */
    public static final float MOB_GON_POWER = 0.6f;

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
        if (technique.isBase()) return gameTime < data.readyAt(technique.id()) ? "message.hxh.hatsu_cooldown" : null;
        if (!data.nenUnlocked()) return "message.hxh.nen_locked";
        // la recharge court toujours, Zetsu compris : on l'affiche en premier
        if (gameTime < data.readyAt(technique.id())) return "message.hxh.hatsu_cooldown";
        if (data.mode() == NenMode.ZETSU) return "message.hxh.hatsu_zetsu";
        if (data.nenLevel() < technique.minNenLevel()) return "message.hxh.hatsu_nen_level";
        if (data.aura() < technique.auraCost()) return "message.hxh.no_aura";
        return technique.extraRefusal(data);
    }

    /** Lance la technique sélectionnée. @return null si lancée, sinon la clé du refus */
    @Nullable
    public static String cast(LivingEntity entity) {
        HunterData data = HunterStats.get(entity);
        Technique technique = selectedTechnique(data);
        if (technique == null) return "message.hxh.hatsu_none";
        long now = entity.level().getGameTime();
        if (NenCharge.isCharging(entity) || ThunderHold.isHolding(entity) || HeldStrike.isHolding(entity)
                || com.evandouard.hxh.combat.Combat.isCharging(entity)) return null; // déjà en charge
        if (!technique.isBase() && com.evandouard.hxh.nen.NenLearning.refusal(entity, com.evandouard.hxh.nen.NenLearning.Ability.HATSU) != null) {
            return com.evandouard.hxh.nen.NenLearning.refusal(entity, com.evandouard.hxh.nen.NenLearning.Ability.HATSU);
        }
        // Tour Céleste : pas de technique hors du ring (halls, guichets, chambres)
        if (entity instanceof net.minecraft.server.level.ServerPlayer sp
                && sp.level().dimension().equals(com.evandouard.hxh.arena.ArenaWorld.DIMENSION)
                && !com.evandouard.hxh.arena.ArenaManager.inFight(sp)) return "message.hxh.arena_no_hatsu";
        String refusal = castRefusal(data, technique, now);
        if (refusal != null) return refusal;
        if (technique instanceof Techniques.ChargedOrb orb && data.aura() < orb.auraCost() + orb.minSpent) {
            return "message.hxh.no_aura"; // pas de quoi charger un vrai coup
        }
        if (technique.isCharged()) {
            // On commence à charger : la recharge partira au relâchement
            data.setAura(data.aura() - technique.auraCost());
            technique.perform(entity, powerFor(entity, data, technique));
            return null;
        }
        // Raté ou pas, l'aura et la recharge partent : lancer dans le vide a un prix
        boolean hit = technique.perform(entity, powerFor(entity, data, technique));
        data.setAura(data.aura() - technique.auraCost());
        if (technique.resource() != null) data.addKitValue(technique.resource(), -technique.resourceCost(), Float.MAX_VALUE);
        int cooldown = technique.cooldownFor(data);
        if ("cheadle".equals(data.kit())) cooldown = Math.round(cooldown * 0.9f); // Secrétaire : recharges −10 %
        data.setReadyAt(technique.id(), now + cooldown);
        Characters.observe(entity, technique); // Ging retient ce qu'il voit
        Characters.gingUsed(entity, technique); // une utilisation de sa copie
        entity.getPersistentData().putString(Specials.LAST_TECHNIQUE, technique.id()); // Chrollo peut la voler
        com.evandouard.hxh.nen.NenProgress.addNenXp(entity, Math.max(1, technique.auraCost() / 5));
        return hit ? null : "message.hxh.hatsu_missed";
    }

    /** Touche de technique relâchée : lance une technique en charge. */
    public static void releaseCast(LivingEntity entity) {
        NenCharge.release(entity);
        ThunderHold.release(entity);
        HeldStrike.release(entity);
        // Frappe chargée (coup de base) : la recharge part au relâchement
        if (com.evandouard.hxh.combat.Combat.releaseChargedTechnique(entity)) {
            HunterData data = HunterStats.get(entity);
            data.setReadyAt(Techniques.BASIC_CHARGED.id(),
                    entity.level().getGameTime() + Techniques.BASIC_CHARGED.cooldownFor(data));
        }
    }

    /** Sélectionne une technique du menu par identifiant. @return false si absente du menu */
    public static boolean select(LivingEntity entity, String id) {
        HunterData data = HunterStats.get(entity);
        if (id.matches("\\d+")) {
            data.setSelected(Integer.parseInt(id) % techniqueIds(data).size());
            return true;
        }
        int i = techniqueIds(data).indexOf(id);
        if (i < 0) return false;
        data.setSelected(i);
        return true;
    }

    public static void requestCast(LivingEntity entity) {
        String refusal = cast(entity);
        // Un raté n'affiche rien : on voit la technique partir dans le vide
        if (refusal != null && !"message.hxh.hatsu_missed".equals(refusal) && entity instanceof Player player) {
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
