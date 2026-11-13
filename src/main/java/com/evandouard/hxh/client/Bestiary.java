package com.evandouard.hxh.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Bestiaire du carnet (onglet « Bestiaire ») : chaque créature du mod avec ce qu'elle est, où la trouver et son
 * danger. Textes dans les fichiers de langue ({@code bestiary.hxh.<id>.desc} et {@code .where}).
 */
public final class Bestiary {
    private Bestiary() {}

    /** Créature : identifiant d'entité du mod et danger (0 = paisible, 3 = très dangereux). */
    public record Entry(String id, int danger) {}

    public static final List<Entry> ENTRIES = List.of(
            new Entry("kiriko", 0),
            new Entry("spider_eagle", 0),
            new Entry("great_stamp", 2),
            new Entry("foxbear", 2),
            new Entry("man_faced_ape", 1),
            new Entry("chimera_ant", 3));

    public static List<Component> lines() {
        List<Component> out = new ArrayList<>();
        out.add(Component.translatable("bestiary.hxh.intro").withStyle(ChatFormatting.GRAY));
        out.add(Component.empty());
        for (Entry e : ENTRIES) {
            String stars = "★".repeat(e.danger()) + "☆".repeat(3 - e.danger());
            out.add(Component.translatable("entity.hxh." + e.id()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                    .append(Component.literal("  " + stars).withStyle(e.danger() >= 3 ? ChatFormatting.RED : ChatFormatting.YELLOW)));
            out.add(Component.translatable("bestiary.hxh.where", Component.translatable("bestiary.hxh." + e.id() + ".where"))
                    .withStyle(ChatFormatting.AQUA));
            out.add(Component.translatable("bestiary.hxh." + e.id() + ".desc"));
            out.add(Component.empty());
        }
        out.add(Component.translatable("bestiary.hxh.nen_mobs").withStyle(ChatFormatting.GRAY));
        return out;
    }
}
