package com.evandouard.hxh.nen;

import com.evandouard.hxh.HxHConfig;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.Mastery;
import com.evandouard.hxh.nen.NenLearning.Ability;
import com.evandouard.hxh.quest.QuestManager;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Graphe des dépendances entre techniques du Nen. Wing enseigne les bases (Ten, In, Ren, Hatsu) ; les techniques
 * avancées (Zetsu, Shu, Ken, En, Gyo) et les combinaisons (Ko, Ryu) se débloquent <b>toutes seules</b> dès que le
 * joueur atteint les niveaux de maîtrise requis dans les techniques dont elles dépendent, qu'il ait vu Wing ou non.
 * Les quêtes {@code nen_adv_*} de Wing ne font qu'indiquer quoi faire : elles se terminent d'elles-mêmes.
 */
public final class NenTree {
    /** Une condition : atteindre {@code level} dans la maîtrise {@code mastery}. */
    public record Req(Mastery mastery, int level) {}

    /** Nœud du graphe : une capacité, ses conditions, et sa couche. */
    public record Node(Ability ability, int layer, List<Req> reqs, boolean needsHatsu, @Nullable Ability uncertain) {
        public boolean basic() {
            return layer == 0;
        }
    }

    public static final int BASE = 0, ADVANCED = 1, COMBO = 2, MASTERY = 3;

    /** Les nœuds, dans l'ordre d'affichage. Les niveaux de base viennent de la configuration quand elle en a. */
    public static List<Node> nodes() {
        int zetsuIn = HxHConfig.ZETSU_UNLOCK_IN_LEVEL.get();
        int kenTen = HxHConfig.KEN_UNLOCK_TEN_LEVEL.get();
        List<Node> n = new ArrayList<>();
        n.add(new Node(Ability.TEN, BASE, List.of(), false, null));
        n.add(new Node(Ability.IN, BASE, List.of(), false, null));
        n.add(new Node(Ability.REN, BASE, List.of(), false, null));
        n.add(new Node(Ability.HATSU, BASE, List.of(), false, null));
        n.add(new Node(Ability.ZETSU, ADVANCED, List.of(new Req(Mastery.IN, zetsuIn)), false, null));
        n.add(new Node(Ability.SHU, ADVANCED, List.of(new Req(Mastery.TEN, kenTen + 3)), false, null));
        n.add(new Node(Ability.KEN, ADVANCED, List.of(new Req(Mastery.TEN, kenTen), new Req(Mastery.REN, 3)), false, null));
        n.add(new Node(Ability.EN, ADVANCED, List.of(new Req(Mastery.TEN, kenTen + 5), new Req(Mastery.REN, 8)), false, null));
        n.add(new Node(Ability.GYO, ADVANCED, List.of(new Req(Mastery.REN, 5), new Req(Mastery.IN, 3)), false, null));
        n.add(new Node(Ability.KO, COMBO, List.of(new Req(Mastery.TEN, 10), new Req(Mastery.IN, zetsuIn + 5),
                new Req(Mastery.REN, 10), new Req(Mastery.GYO, 5)), true, null));
        n.add(new Node(Ability.RYU, MASTERY, List.of(new Req(Mastery.GYO, 15), new Req(Mastery.KO, 10),
                new Req(Mastery.KEN, 10)), false, null));
        return n;
    }

    /** Dépendances affichées (parent → enfant) ; le dernier champ vrai = lien incertain (pointillé). */
    public record Edge(Ability from, Ability to, boolean uncertain) {}

    public static List<Edge> edges() {
        return List.of(
                new Edge(Ability.IN, Ability.ZETSU, false),
                new Edge(Ability.TEN, Ability.SHU, false),
                new Edge(Ability.TEN, Ability.KEN, false), new Edge(Ability.REN, Ability.KEN, false),
                new Edge(Ability.TEN, Ability.EN, false), new Edge(Ability.REN, Ability.EN, false),
                new Edge(Ability.REN, Ability.GYO, false), new Edge(Ability.IN, Ability.GYO, false),
                new Edge(Ability.TEN, Ability.KO, false), new Edge(Ability.ZETSU, Ability.KO, false),
                new Edge(Ability.REN, Ability.KO, false), new Edge(Ability.HATSU, Ability.KO, false),
                new Edge(Ability.GYO, Ability.KO, false),
                new Edge(Ability.GYO, Ability.RYU, false), new Edge(Ability.KO, Ability.RYU, false),
                new Edge(Ability.KEN, Ability.RYU, true));
    }

    @Nullable
    public static Node node(Ability a) {
        for (Node n : nodes()) if (n.ability() == a) return n;
        return null;
    }

    /** Les conditions de niveau sont remplies (et le Hatsu connu si le nœud l'exige). */
    public static boolean met(HunterData d, Node n) {
        if (n.needsHatsu() && !d.learned(Ability.HATSU.id())) return false;
        for (Req r : n.reqs()) if (d.masteryLevel(r.mastery()) < r.level()) return false;
        return true;
    }

    /** Avancement 0..1 vers le déblocage (moyenne des conditions). */
    public static float progress(HunterData d, Node n) {
        if (d.learned(n.ability().id())) return 1f;
        if (n.reqs().isEmpty()) return 0f;
        float sum = 0;
        for (Req r : n.reqs()) sum += Math.min(1f, d.masteryLevel(r.mastery()) / (float) Math.max(1, r.level()));
        return sum / n.reqs().size();
    }

    /**
     * État du graphe pour l'écran du joueur : une ligne par nœud, {@code id|couche|appris|mastery:requis:actuel,…}
     * (le Hatsu exigé par un nœud est {@code hatsu:1:0/1}).
     */
    public static List<String> encode(ServerPlayer player) {
        HunterData d = HunterStats.get(player);
        List<String> out = new ArrayList<>();
        // En-tête : niveau de Nen, plafond des maîtrises, avancement vers le niveau suivant (%)
        int nenLevel = d.nenLevel();
        int pct = nenLevel >= Mastery.MAX_LEVEL ? 100 : Math.round(100f * d.nenXp() / Mastery.nenXpToNext(nenLevel));
        out.add("meta|" + nenLevel + "|" + d.masteryCap() + "|" + pct);
        for (Node n : nodes()) {
            List<String> reqs = new ArrayList<>();
            for (Req r : n.reqs()) reqs.add(r.mastery().id() + ":" + r.level() + ":" + d.masteryLevel(r.mastery()));
            if (n.needsHatsu()) reqs.add("hatsu:1:" + (NenLearning.knows(player, Ability.HATSU) ? 1 : 0));
            out.add(n.ability().id() + "|" + n.layer() + "|" + (NenLearning.knows(player, n.ability()) ? 1 : 0)
                    + "|" + String.join(",", reqs));
        }
        return out;
    }

    /** Identifiant de la quête de Wing associée à une technique avancée (ou null). */
    @Nullable
    public static ResourceLocation questOf(Ability a) {
        return switch (a) {
            case ZETSU, SHU, KEN, EN, GYO, KO, RYU -> ResourceLocation.fromNamespaceAndPath("hxh", "nen_adv_" + a.id());
            default -> null;
        };
    }

    /**
     * Débloque ce qui peut l'être. Appelé chaque tick (travaille une fois par seconde) ; sans effet si
     * l'apprentissage est désactivé (tout est su d'office).
     */
    public static void tick(ServerPlayer player) {
        if (player.tickCount % 20 != 7 || !NenLearning.applies(player)) return;
        HunterData d = HunterStats.get(player);
        if (!d.learned(Ability.TEN.id())) return; // pas encore commencé : rien à débloquer
        for (Node n : nodes()) {
            if (n.basic() || d.learned(n.ability().id()) || !met(d, n)) continue;
            unlock(player, d, n.ability());
        }
    }

    /** Débloque une technique avancée : apprise d'office, quête de Wing terminée, annonce au joueur. */
    public static void unlock(ServerPlayer player, HunterData d, Ability a) {
        if (!d.learn(a.id())) return;
        ResourceLocation quest = questOf(a);
        if (quest != null) QuestManager.completeSilently(player, quest);
        Component name = Component.translatable(a.translationKey());
        player.sendSystemMessage(Component.translatable("message.hxh.nen_unlocked", name).withStyle(ChatFormatting.AQUA));
        com.evandouard.hxh.network.Hud.toast(player, Component.translatable("hud.hxh.toast.nen_unlocked"),
                name, "◆", com.evandouard.hxh.network.Hud.GOLD);
    }
}
