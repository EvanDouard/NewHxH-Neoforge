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
        if (data.nenUnlocked() && com.evandouard.hxh.nen.NenLearning.applies(living)) {
            java.util.List<String> names = new java.util.ArrayList<>();
            for (com.evandouard.hxh.nen.NenLearning.Ability ab : com.evandouard.hxh.nen.NenLearning.Ability.values()) {
                if (data.learned(ab.id())) names.add(Component.translatable(ab.translationKey()).getString());
            }
            lines.add(Component.translatable("profile.hxh.learned", names.isEmpty()
                    ? Component.translatable("profile.hxh.learned_none") : Component.literal(String.join(", ", names))));
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

    /** Quête suivie, autres quêtes en cours, puis quêtes terminées ou pas encore commencées. */
    public static List<Component> quests(ServerPlayer player) {
        QuestData d = QuestManager.data(player);
        List<Component> lines = new ArrayList<>();
        List<String> running = d.runningQuests();
        lines.add(Component.translatable("profile.hxh.quests.tracked").withStyle(ChatFormatting.GOLD));
        Quest tracked = d.active().isEmpty() ? null : QuestRegistry.get(ResourceLocation.parse(d.active()));
        if (tracked == null || d.step() >= QuestManager.steps(d, tracked).size()) {
            lines.add(Component.translatable("profile.hxh.quests.none").withStyle(ChatFormatting.GRAY));
        } else {
            lines.add(tracked.title(player).copy().withStyle(ChatFormatting.YELLOW));
            lines.add(objective(player, d, tracked, d.step(), d.progress()));
            Quest.Step cur = QuestManager.steps(d, tracked).get(d.step());
            if (cur.type().equals("custom") && !cur.note().isEmpty()) {
                lines.add(Component.translatable("profile.hxh.quests.custom_note", cur.note()).withStyle(ChatFormatting.DARK_GRAY));
            }
            lines.add(Component.translatable("profile.hxh.quests.step", d.step() + 1, QuestManager.steps(d, tracked).size()).withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.empty());
        lines.add(Component.translatable("profile.hxh.quests.running").withStyle(ChatFormatting.GOLD));
        boolean any = false;
        for (String id : running) {
            if (tracked != null && id.equals(tracked.id().toString())) continue;
            Quest q = QuestRegistry.get(ResourceLocation.parse(id));
            if (q == null) continue;
            any = true;
            int[] st = runningState(d, id);
            lines.add(Component.literal("• ").withStyle(ChatFormatting.AQUA).append(q.title(player).copy().withStyle(ChatFormatting.WHITE)));
            if (st[0] < QuestManager.steps(d, q).size()) lines.add(Component.literal("   ").append(objective(player, d, q, st[0], st[1])).withStyle(ChatFormatting.GRAY));
        }
        if (!any) lines.add(Component.translatable("profile.hxh.quests.no_other").withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.empty());
        lines.add(Component.translatable("profile.hxh.quests.others").withStyle(ChatFormatting.GOLD));
        for (Quest q : QuestRegistry.all()) {
            String id = q.id().toString();
            if (running.contains(id)) continue;
            boolean done = d.isCompleted(id);
            lines.add(Component.literal(done ? "✔ " : "• ").withStyle(done ? ChatFormatting.GREEN : ChatFormatting.GRAY)
                    .append(q.title(player).copy().withStyle(ChatFormatting.WHITE))
                    .append(Component.literal("  "))
                    .append(Component.translatable(done ? "profile.hxh.quests.done" : "profile.hxh.quests.not_started")
                            .withStyle(ChatFormatting.DARK_GRAY)));
        }
        return lines;
    }

    private static int[] runningState(QuestData d, String id) {
        int[] st = d.stateOf(id);
        return st == null ? new int[]{0, 0} : st;
    }

    private static MutableComponent objective(ServerPlayer player, QuestData d, Quest q, int stepIndex, int progressValue) {
        Quest.Step step = QuestManager.steps(d, q).get(stepIndex);
        String[] pending = com.evandouard.hxh.quest.QuestManager.isArenaQuest(q)
                ? com.evandouard.hxh.arena.ArenaManager.pendingObjective(d) : null;
        if (pending != null) {
            return Component.translatable("profile.hxh.quests.objective", Component.translatable(pending[0], pending[1]));
        }
        MutableComponent obj = Component.translatable("profile.hxh.quests.objective", q.objective(step, player));
        if (step.goal() > 0) {
            int progress = step.type().equals("follow") ? progressValue / 20
                    : step.type().equals("arena_floor") ? Math.min(d.arenaFloor(), step.count()) : progressValue;
            obj.append(Component.literal(" (" + progress + "/" + step.goal() + ")"));
        }
        return obj;
    }

    /** Tour Céleste et Jenny. */
    public static List<Component> arena(ServerPlayer player) {
        QuestData d = QuestManager.data(player);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("profile.hxh.jenny", d.jenny()).withStyle(ChatFormatting.YELLOW));
        lines.add(Component.translatable("profile.hxh.hunter", Component.translatable(d.isHunter() ? "command.hxh.yes" : "command.hxh.no")));
        var fam = com.evandouard.hxh.family.Families.get(d.family());
        lines.add(Component.translatable("profile.hxh.family", fam == null ? Component.translatable("family.hxh.none")
                : Component.literal(fam.name(player.clientInformation().language())).withStyle(s -> s.withColor(fam.color()))));
        var org = com.evandouard.hxh.org.Organizations.get(d.organization());
        lines.add(Component.translatable("profile.hxh.organization", org == null ? Component.translatable("org.hxh.none_short")
                : com.evandouard.hxh.org.Organizations.label(org, player)));
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
