package com.evandouard.hxh.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Bestiaire de l'ordinateur Hunter (catégorie « Bestiaire ») : chaque créature du mod avec ce qu'elle est, où la trouver et son
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

    /** Fiches de l'ordinateur Hunter : une par créature, catégorie « Bestiaire », id {@code bestiary_<créature>}. */
    public static List<com.evandouard.hxh.network.OpenComputerPayload.Entry> computerEntries() {
        String category = Component.translatable("screen.hxh.computer.bestiary").getString();
        List<com.evandouard.hxh.network.OpenComputerPayload.Entry> out = new ArrayList<>();
        out.add(new com.evandouard.hxh.network.OpenComputerPayload.Entry("bestiary_intro", category,
                Component.translatable("screen.hxh.computer.bestiary_intro").getString(),
                Component.translatable("bestiary.hxh.intro").getString() + "\n\n"
                        + Component.translatable("bestiary.hxh.nen_mobs").getString(), true, ""));
        for (Entry e : ENTRIES) {
            String text = Component.translatable("bestiary.hxh.danger", Component.translatable("bestiary.hxh.danger." + e.danger())).getString()
                    + "\n" + Component.translatable("bestiary.hxh.where", Component.translatable("bestiary.hxh." + e.id() + ".where")).getString()
                    + "\n\n" + Component.translatable("bestiary.hxh." + e.id() + ".desc").getString();
            out.add(new com.evandouard.hxh.network.OpenComputerPayload.Entry("bestiary_" + e.id(), category,
                    Component.translatable("entity.hxh." + e.id()).getString(), text, true, ""));
        }
        return out;
    }

    public static List<Component> lines() {
        List<Component> out = new ArrayList<>();
        out.add(Component.translatable("bestiary.hxh.intro").withStyle(ChatFormatting.GRAY));
        out.add(Component.empty());
        for (Entry e : ENTRIES) {
            ChatFormatting color = switch (e.danger()) {
                case 0 -> ChatFormatting.GREEN;
                case 1 -> ChatFormatting.YELLOW;
                case 2 -> ChatFormatting.GOLD;
                default -> ChatFormatting.RED;
            };
            out.add(Component.translatable("entity.hxh." + e.id()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                    .append(Component.literal("  ").append(Component.translatable("bestiary.hxh.danger",
                            Component.translatable("bestiary.hxh.danger." + e.danger()))).withStyle(color)));
            out.add(Component.translatable("bestiary.hxh.where", Component.translatable("bestiary.hxh." + e.id() + ".where"))
                    .withStyle(ChatFormatting.AQUA));
            out.add(Component.translatable("bestiary.hxh." + e.id() + ".desc"));
            out.add(Component.empty());
        }
        out.add(Component.translatable("bestiary.hxh.nen_mobs").withStyle(ChatFormatting.GRAY));
        return out;
    }
}
