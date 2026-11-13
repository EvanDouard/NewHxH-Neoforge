package com.evandouard.hxh.nen;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.effect.ModEffects;
import com.evandouard.hxh.fx.NenFx;
import com.evandouard.hxh.hatsu.Specials;
import com.evandouard.hxh.quest.QuestData;
import com.evandouard.hxh.quest.QuestManager;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Exorcisme de Nen (comme Abengane) : retire les auras étrangères accrochées à quelqu'un. Les malédictions :
 * <ul>
 *     <li>la malédiction de Nen (rancune posthume d'un manieur tué, effet {@code nen_curse}) ;</li>
 *     <li>la dette et la banqueroute du Hakoware de Knuckle ;</li>
 *     <li>la bombe du compte à rebours de Genthru (désamorcée) ;</li>
 *     <li>la manipulation par une aiguille d'Illumi (mobs).</li>
 * </ul>
 * Le sceau d'un serment rompu n'est pas une aura étrangère : l'exorcisme n'y touche pas. On paie l'exorciste (PNJ
 * {@code exorciste}) en Jenny : {@link #PRICE_PER_CURSE} par malédiction, moitié prix avec la licence de Hunter.
 */
public final class Exorcism {
    private Exorcism() {}

    public static final String NPC = "exorciste";
    public static final String BOMB = "hxh_countdown_bomb";
    public static final int PRICE_PER_CURSE = 300;
    /** Rancune posthume : chance qu'un manieur de Nen tué (niveau de Nen ≥ 20) maudisse son tueur, et durée. */
    public static final float CURSE_CHANCE = 0.1f;
    public static final int CURSE_MIN_NEN = 20;
    public static final int CURSE_TICKS = 24000;

    public static void registerEvents(IEventBus bus) {
        bus.addListener(LivingDeathEvent.class, Exorcism::onDeath);
    }

    /** Malédictions présentes (clés de traduction {@code curse.hxh.<clé>}). */
    public static List<String> curses(LivingEntity e) {
        List<String> out = new ArrayList<>();
        if (e.hasEffect(ModEffects.NEN_CURSE)) out.add("nen_curse");
        var tag = e.getPersistentData();
        if (tag.contains(Specials.HAKO_DEBT) || Specials.bankrupt(e)) out.add("hakoware");
        if (tag.contains(BOMB)) out.add("bomb");
        if (tag.hasUUID(Specials.CONTROLLED_BY)) out.add("control");
        return out;
    }

    /** Retire toutes les malédictions. @return combien il y en avait */
    public static int exorcise(LivingEntity e) {
        List<String> found = curses(e);
        if (found.isEmpty()) return 0;
        e.removeEffect(ModEffects.NEN_CURSE);
        var tag = e.getPersistentData();
        tag.remove(Specials.HAKO_DEBT);
        if (Specials.bankrupt(e)) {
            tag.remove(Specials.BANKRUPT_UNTIL);
            if (e.hasData(ModAttachments.HUNTER)) {
                HunterData d = HunterStats.get(e);
                if (d.mode() == com.evandouard.hxh.data.NenMode.ZETSU) d.setMode(com.evandouard.hxh.data.NenMode.NONE);
            }
        }
        tag.remove(BOMB);
        tag.remove(Specials.CONTROLLED_BY);
        tag.remove(Specials.CONTROLLED_UNTIL);
        NenFx.flare(e, 1.8f, 0xFFF2F6FF);
        e.level().playSound(null, e.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.NEUTRAL, 0.9f, 0.7f);
        return found.size();
    }

    public static int price(ServerPlayer player, int curses) {
        int p = curses * PRICE_PER_CURSE;
        return QuestManager.data(player).isHunter() ? p / 2 : p;
    }

    /** On parle à l'exorciste. */
    public static void talk(ServerPlayer player) {
        List<String> found = curses(player);
        if (found.isEmpty()) {
            say(player, Component.translatable("dialog.hxh.exorciste.clean"));
            return;
        }
        int price = price(player, found.size());
        QuestData d = QuestManager.data(player);
        Component list = Component.literal(String.join(", ", found.stream()
                .map(k -> Component.translatable("curse.hxh." + k).getString()).toList()));
        if (d.jenny() < price) {
            say(player, Component.translatable("dialog.hxh.exorciste.too_poor", list, price));
            return;
        }
        d.addJenny(-price);
        exorcise(player);
        say(player, Component.translatable("dialog.hxh.exorciste.done", list, price));
    }

    private static void say(ServerPlayer player, Component text) {
        player.sendSystemMessage(Component.translatable("dialog.hxh.format",
                Component.translatable("npc.hxh." + NPC).withStyle(ChatFormatting.YELLOW), text));
    }

    /** Rancune posthume : un manieur de Nen vaincu peut maudire celui qui l'a tué. */
    private static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide() || !(event.getSource().getEntity() instanceof Player killer)) return;
        if (!dead.hasData(ModAttachments.HUNTER) || dead instanceof Player) return;
        HunterData d = HunterStats.get(dead);
        if (!d.nenUnlocked() || d.nenLevel() < CURSE_MIN_NEN) return;
        if (dead.getRandom().nextFloat() >= CURSE_CHANCE) return;
        curse(killer);
    }

    /** Pose la malédiction de Nen. */
    public static void curse(LivingEntity target) {
        target.addEffect(new MobEffectInstance(ModEffects.NEN_CURSE, CURSE_TICKS, 0, false, false, true));
        NenFx.flare(target, 1.4f, 0xFF8A1A3A);
        if (target instanceof Player p) {
            p.displayClientMessage(Component.translatable("message.hxh.nen_cursed").withStyle(ChatFormatting.DARK_PURPLE), false);
        }
    }
}
