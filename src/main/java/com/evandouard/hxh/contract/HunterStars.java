package com.evandouard.hxh.contract;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.quest.QuestData;
import com.evandouard.hxh.quest.QuestManager;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Rang Hunter en étoiles (1 à 3), comme dans l'œuvre : une étoile par domaine où l'on s'est illustré, licence exigée.
 * Domaines : combat (200e étage de la Tour Céleste), traque (3 criminels de la Blacklist capturés), Nen (niveau de
 * Nen 60). Une étoile ne se perd pas. Chaque nouvelle étoile rapporte une prime de l'Association et ouvre des traques
 * plus dangereuses dans la Blacklist.
 */
public final class HunterStars {
    private HunterStars() {}

    public static final int BLACKLIST_FOR_STAR = 3;
    public static final int NEN_FOR_STAR = 60;
    public static final int JENNY_PER_STAR = 1000;

    /** Domaines remplis (clés {@code stars.hxh.field.<clé>}). */
    public static List<String> fields(QuestData q, HunterData h) {
        List<String> out = new ArrayList<>();
        if (q.reached200()) out.add("combat");
        if (q.blacklistCaught() >= BLACKLIST_FOR_STAR) out.add("hunt");
        if (h.nenLevel() >= NEN_FOR_STAR) out.add("nen");
        return out;
    }

    public static int deserved(QuestData q, HunterData h) {
        return q.isHunter() ? Math.min(3, fields(q, h).size()) : 0;
    }

    /** Monte le rang si le joueur le mérite (jamais de descente), avec annonce et prime. */
    public static void update(ServerPlayer p) {
        QuestData q = QuestManager.data(p);
        int target = deserved(q, HunterStats.get(p));
        while (q.hunterStars() < target) {
            int star = q.hunterStars() + 1;
            q.setHunterStars(star);
            q.addJenny(JENNY_PER_STAR * star);
            p.sendSystemMessage(Component.translatable("message.hxh.hunter_star", stars(star), JENNY_PER_STAR * star)
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
    }

    /** « ★★☆ » */
    public static String stars(int n) {
        return "★".repeat(Math.max(0, n)) + "☆".repeat(Math.max(0, 3 - n));
    }
}
