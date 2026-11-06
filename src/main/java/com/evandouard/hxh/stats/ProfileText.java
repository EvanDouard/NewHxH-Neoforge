package com.evandouard.hxh.stats;

import com.evandouard.hxh.arena.ArenaBrackets;
import com.evandouard.hxh.arena.ArenaRules;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.hatsu.Hatsu;
import com.evandouard.hxh.hatsu.HatsuKit;
import com.evandouard.hxh.hatsu.Technique;
import com.evandouard.hxh.quest.Quest;
import com.evandouard.hxh.quest.QuestData;
import com.evandouard.hxh.quest.QuestManager;
import com.evandouard.hxh.quest.QuestRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Textes du carnet de Hunter (touche P) et de {@code /hunter show}, préparés côté serveur
 * (qui fait autorité) puis envoyés au client.
 */
public final class ProfileText {
    private ProfileText() {}

    /** Statistiques, Nen, Hatsu, soin. */
    public static List<Component> statsAndNen(LivingEntity living) {
        HunterData data = HunterStats.get(living);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("command.hxh.show.header", living.getDisplayName(), data.hunterLevel())
                .withStyle(ChatFormatting.GOLD));
        for (Stat stat : Stat.values()) {
            int level = data.level(stat);
            MutableComponent xpPart = level >= Stat.MAX_LEVEL
                    ? Component.translatable("command.hxh.show.max")
                    : Component.translatable("command.hxh.show.xp", data.xp(stat), Stat.xpToNext(level));
            lines.add(Component.translatable("command.hxh.show.stat",
                    Component.translatable(stat.translationKey()), level, xpPart));
        }
        Component nenType = data.nenType() == null
                ? Component.translatable("nen.hxh.none")
                : Component.translatable(data.nenType().translationKey());
        if (!data.nenUnlocked()) {
            lines.add(Component.translatable("command.hxh.show.nen_locked"));
        } else if (data.nenRevealed()) {
            lines.add(Component.translatable("command.hxh.show.nen", nenType));
        } else {
            lines.add(Component.translatable("command.hxh.show.nen", "???"));
        }
        if (data.nenUnlocked()) {
            lines.add(Component.translatable("command.hxh.show.nen_level", data.nenLevel(),
                    data.nenLevel() >= com.evandouard.hxh.data.Mastery.MAX_LEVEL
                            ? Component.translatable("command.hxh.show.max")
                            : Component.translatable("command.hxh.show.xp", data.nenXp(),
                            com.evandouard.hxh.data.Mastery.nenXpToNext(data.nenLevel()))));
            MutableComponent masteries = Component.translatable("command.hxh.show.masteries");
            boolean first = true;
            for (com.evandouard.hxh.data.Mastery m : com.evandouard.hxh.data.Mastery.values()) {
                masteries.append(Component.literal(first ? " " : " · "))
                        .append(Component.translatable(m.translationKey()))
                        .append(Component.literal(" " + data.masteryLevel(m)));
                first = false;
            }
            lines.add(masteries.withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("command.hxh.show.resistance",
                    String.format(java.util.Locale.ROOT, "%.1f", com.evandouard.hxh.nen.NenDefense.totalResistance(living))));
            lines.add(Component.translatable("command.hxh.show.aura", (int) data.aura(), data.auraMax(),
                    Component.translatable(data.mode().translationKey())));
        }
        HatsuKit kit = Hatsu.kitOf(data);
        if (kit != null) {
            Technique current = Hatsu.selectedTechnique(data);
            lines.add(Component.translatable("command.hxh.show.hatsu", Component.translatable(kit.translationKey()),
                    current == null ? Component.literal("-") : Component.translatable(current.translationKey())));
        }
        if (data.healUnlocked()) {
            lines.add(Component.translatable("command.hxh.show.heal", data.healLevel(),
                    data.healLevel() >= HunterData.HEAL_MAX_LEVEL
                            ? Component.translatable("command.hxh.show.max")
                            : Component.translatable("command.hxh.show.xp", data.healXp(), HunterData.healXpToNext(data.healLevel()))));
        }
        if (!data.nenUnlocked() && data.meditationTicks() > 0) {
            lines.add(Component.translatable("command.hxh.show.meditation", data.meditationTicks() / 20));
        }
        if (living instanceof ServerPlayer) {
            // Le talent se révèle avec le type de Nen (divination par l'eau)
            Component talent = data.talent() == null || !data.nenRevealed()
                    ? Component.literal("???")
                    : Component.translatable(data.talent().translationKey()).append(Component.literal(" (×"
                    + String.valueOf(data.talent().multiplier()).replace(".0", "").replace('.', ',') + ")"));
            lines.add(Component.translatable("profile.hxh.talent", talent));
        }
        return lines;
    }

    /** Quête suivie, puis les autres quêtes (terminées ou pas encore commencées). */
    public static List<Component> quests(ServerPlayer player) {
        QuestData d = QuestManager.data(player);
        List<Component> lines = new ArrayList<>();
        Quest active = d.active().isEmpty() ? null : QuestRegistry.get(ResourceLocation.parse(d.active()));
        lines.add(Component.translatable("profile.hxh.quests.tracked").withStyle(ChatFormatting.GOLD));
        if (active == null || d.step() >= active.steps().size()) {
            lines.add(Component.translatable("profile.hxh.quests.none").withStyle(ChatFormatting.GRAY));
        } else {
            Quest.Step step = active.steps().get(d.step());
            lines.add(Component.translatable(active.titleKey()).withStyle(ChatFormatting.YELLOW));
            MutableComponent obj = Component.translatable("profile.hxh.quests.objective", Component.translatable(active.stepKey(step)));
            if (step.goal() > 0) {
                int progress = step.type().equals("follow") ? d.progress() / 20
                        : step.type().equals("arena_floor") ? Math.min(d.arenaFloor(), step.count()) : d.progress();
                obj.append(Component.literal(" (" + progress + "/" + step.goal() + ")"));
            }
            lines.add(obj);
            lines.add(Component.translatable("profile.hxh.quests.step", d.step() + 1, active.steps().size()).withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.empty());
        lines.add(Component.translatable("profile.hxh.quests.others").withStyle(ChatFormatting.GOLD));
        for (Quest q : QuestRegistry.all()) {
            if (active != null && q.id().equals(active.id())) continue;
            boolean done = d.isCompleted(q.id().toString());
            lines.add(Component.literal(done ? "✔ " : "• ").withStyle(done ? ChatFormatting.GREEN : ChatFormatting.GRAY)
                    .append(Component.translatable(q.titleKey()).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal("  "))
                    .append(Component.translatable(done ? "profile.hxh.quests.done" : "profile.hxh.quests.not_started")
                            .withStyle(ChatFormatting.DARK_GRAY)));
        }
        lines.add(Component.empty());
        lines.add(Component.translatable("profile.hxh.quests.soon").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        return lines;
    }

    /** Tour Céleste et Jenny. */
    public static List<Component> arena(ServerPlayer player) {
        QuestData d = QuestManager.data(player);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("profile.hxh.jenny", d.jenny()).withStyle(ChatFormatting.YELLOW));
        lines.add(Component.translatable("profile.hxh.hunter", Component.translatable(d.isHunter() ? "command.hxh.yes" : "command.hxh.no")));
        lines.add(Component.empty());
        lines.add(Component.translatable("profile.hxh.arena.title").withStyle(ChatFormatting.GOLD));
        if (!d.arenaRegistered()) {
            lines.add(Component.translatable("profile.hxh.arena.not_registered").withStyle(ChatFormatting.GRAY));
            return lines;
        }
        int floor = d.arenaFloor();
        if (ArenaRules.isFloorMaster(floor)) {
            lines.add(Component.translatable("profile.hxh.arena.master").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        } else {
            lines.add(Component.translatable("profile.hxh.arena.floor", floor, ArenaBrackets.forFloor(floor).label()));
        }
        lines.add(Component.translatable("profile.hxh.arena.record", d.arenaWins(), d.arenaLosses()));
        int bracket = ArenaBrackets.indexOf(floor);
        boolean needs = d.registeredBracket() < bracket || (floor >= ArenaRules.NEN_FLOOR && !d.registered200());
        if (needs && !ArenaRules.isFloorMaster(floor)) {
            lines.add(Component.translatable("profile.hxh.arena.register").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return lines;
    }
}
