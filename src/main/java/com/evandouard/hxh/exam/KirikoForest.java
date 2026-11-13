package com.evandouard.hxh.exam;

import com.evandouard.hxh.npc.NpcEntity;
import com.evandouard.hxh.quest.QuestManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * La forêt des Kiriko (avant Zaban) : une femme blessée, et deux « maris » identiques. L'un est un Kiriko, une bête
 * magique qui a pris l'apparence du mari. On examine les deux (leur parler) : le vrai porte la blessure qu'il a reçue
 * en défendant sa femme, le Kiriko a un détail qui cloche. Puis on désigne le Kiriko à la femme : juste, il se
 * révèle et l'épreuve est réussie ; faux, le candidat est éliminé.
 * Lequel est le faux dépend de l'exemplaire de la forêt et du candidat.
 */
public final class KirikoForest {
    private KirikoForest() {}

    /** Pour les tests : index du faux mari imposé (1 ou 2). */
    public static Integer forcedForTests;

    /** Index (1 = gauche, 2 = droite) du mari qui est en fait un Kiriko. */
    public static int fakeIndex(ServerPlayer player) {
        if (forcedForTests != null) return forcedForTests;
        int instance = QuestManager.data(player).examInstance(ExamSite.KIRIKO.id());
        return 1 + Math.floorMod(instance + player.getUUID().hashCode(), 2);
    }

    /** Parler à un des maris : un indice, différent pour le vrai et le faux (quatre variantes chacun). */
    public static void examine(ServerPlayer player, NpcEntity husband) {
        int index = husband.npcId().endsWith("2") ? 2 : 1;
        boolean fake = index == fakeIndex(player);
        int variant = Math.floorMod(player.getUUID().hashCode() + index, 4);
        String key = "exam.hxh.kiriko." + (fake ? "fake" : "real") + "." + variant;
        player.sendSystemMessage(Component.translatable("dialog.hxh.format",
                Component.translatable("npc.hxh." + husband.npcId()).withStyle(ChatFormatting.YELLOW),
                Component.translatable(key)));
    }

    /** Durée pendant laquelle le faux mari reste caché (le Kiriko s'envole entre-temps), et départ du Kiriko. */
    public static final int HIDDEN_TICKS = 220, KIRIKO_LEAVES = 160;

    /**
     * Le faux mari se transforme : il disparaît dans une gerbe, un Kiriko apparaît à sa place, salue, puis s'envole.
     * Le PNJ revient ensuite (la forêt sert aussi aux autres candidats). @return le Kiriko
     */
    public static com.evandouard.hxh.beast.KirikoEntity reveal(ServerLevel level, NpcEntity husband) {
        husband.setInvisible(true);
        husband.setCustomNameVisible(false);
        var kiriko = com.evandouard.hxh.registry.ModEntities.KIRIKO.get().create(level);
        if (kiriko != null) {
            kiriko.moveTo(husband.getX(), husband.getY(), husband.getZ(), husband.getYRot(), 0);
            kiriko.leaveIn(KIRIKO_LEAVES);
            kiriko.setCustomName(Component.translatable("entity.hxh.kiriko"));
            level.addFreshEntity(kiriko);
            com.evandouard.hxh.fx.NenFx.shockwaveAt(level, kiriko.position(), 2.5f, 0xFFFFFFFF);
            level.playSound(null, husband.blockPosition(), net.minecraft.sounds.SoundEvents.ENDER_DRAGON_FLAP, net.minecraft.sounds.SoundSource.NEUTRAL, 1f, 1.4f);
        }
        com.evandouard.hxh.hatsu.NenTasks.later(HIDDEN_TICKS, () -> {
            if (!husband.isAlive()) return;
            husband.setInvisible(false);
            husband.setCustomNameVisible(true);
        });
        return kiriko;
    }

    /** Désigner le Kiriko (1 = gauche, 2 = droite). */
    public static boolean accuse(ServerPlayer player, int n) {
        boolean right = n == fakeIndex(player);
        if (right) {
            NpcEntity fake = QuestManager.findNpc(player, "husband_" + n, 32);
            if (fake != null && fake.level() instanceof ServerLevel level) {
                com.evandouard.hxh.fx.NenFx.slashAt(level, fake.position().add(0, 1, 0), 1.8f, 0xFFFFFF);
                reveal(level, fake);
            }
            player.sendSystemMessage(Component.translatable("exam.hxh.kiriko.reveal").withStyle(ChatFormatting.GREEN));
        } else {
            player.sendSystemMessage(Component.translatable("exam.hxh.kiriko.wrong").withStyle(ChatFormatting.RED));
        }
        return right;
    }
}
