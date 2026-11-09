package com.evandouard.hxh.quest;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tirage des épreuves d'une quête à épreuves (Examen Hunter) : entre {@code minTrials} et {@code maxTrials}
 * épreuves, dans l'ordre des phases, une épreuve liée ({@code follows}) juste après celle qu'elle suit.
 * Les étapes sans épreuve sont fixes : celles d'avant la première épreuve ouvrent le parcours (trouver un guide),
 * celles d'après le ferment (la licence).
 */
public final class TrialPlan {
    private TrialPlan() {}

    /** Pour les tests : prochain tirage imposé (identifiants d'épreuves dans l'ordre), null = au hasard. */
    public static List<String> forcedForTests;

    /** Épreuves tirées, dans l'ordre où elles seront passées. */
    public static List<String> draw(Quest q, RandomSource random) {
        if (forcedForTests != null) {
            List<String> forced = forcedForTests;
            forcedForTests = null;
            return forced;
        }
        List<Quest.Trial> pool = new ArrayList<>(q.trials().values());
        // Mélange de Fisher-Yates avec le hasard du jeu
        for (int i = pool.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Quest.Trial t = pool.get(i);
            pool.set(i, pool.get(j));
            pool.set(j, t);
        }
        int max = Math.min(q.maxTrials(), pool.size());
        int min = Math.min(q.minTrials(), max);
        int want = min + (max > min ? random.nextInt(max - min + 1) : 0);
        List<String> picked = new ArrayList<>();
        for (int pass = 0; pass < 2 && picked.size() < want; pass++) {
            for (Quest.Trial t : pool) {
                if (picked.size() >= want) break;
                if (picked.contains(t.id())) continue;
                List<String> chain = chain(q, t);
                List<String> missing = chain.stream().filter(c -> !picked.contains(c)).toList();
                // Au 1er passage, une épreuve liée n'entre qu'avec ce qui la précède ; au 2e, on complète
                if (picked.size() + missing.size() <= want) picked.addAll(missing);
            }
        }
        return order(q, picked, pool);
    }

    /** L'épreuve et celles qu'elle suit, de la plus ancienne à elle. */
    static List<String> chain(Quest q, Quest.Trial t) {
        List<String> out = new ArrayList<>();
        Quest.Trial cur = t;
        while (cur != null && !out.contains(cur.id())) {
            out.add(0, cur.id());
            cur = cur.follows().isEmpty() ? null : q.trials().get(cur.follows());
        }
        return out;
    }

    /** Ordre de passage : par phase de la racine de chaque chaîne, au hasard entre chaînes de même phase. */
    private static List<String> order(Quest q, List<String> picked, List<Quest.Trial> shuffled) {
        Map<String, Integer> rank = new HashMap<>();
        for (int i = 0; i < shuffled.size(); i++) rank.put(shuffled.get(i).id(), i);
        Map<String, String> root = new HashMap<>();
        Map<String, Integer> depth = new HashMap<>();
        for (String id : picked) {
            List<String> c = chain(q, q.trials().get(id));
            root.put(id, c.get(0));
            depth.put(id, c.size());
        }
        List<String> out = new ArrayList<>(picked);
        out.sort(Comparator.<String>comparingInt(id -> q.trials().get(root.get(id)).phase())
                .thenComparingInt(id -> rank.get(root.get(id)))
                .thenComparingInt(depth::get));
        return out;
    }

    /** Indices des étapes de la quête pour ces épreuves : étapes fixes d'ouverture, épreuves, étapes de clôture. */
    public static int[] steps(Quest q, List<String> trials) {
        List<Quest.Step> all = q.steps();
        int first = -1;
        for (int i = 0; i < all.size(); i++) if (!all.get(i).trial().isEmpty()) {
            first = i;
            break;
        }
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) if (all.get(i).trial().isEmpty() && (first < 0 || i < first)) out.add(i);
        for (String t : trials) for (int i = 0; i < all.size(); i++) if (all.get(i).trial().equals(t)) out.add(i);
        for (int i = 0; i < all.size(); i++) if (all.get(i).trial().isEmpty() && first >= 0 && i > first) out.add(i);
        return out.stream().mapToInt(Integer::intValue).toArray();
    }
}
