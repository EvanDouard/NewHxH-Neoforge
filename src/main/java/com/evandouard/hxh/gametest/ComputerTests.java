package com.evandouard.hxh.gametest;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.computer.HunterComputer;
import com.evandouard.hxh.network.OpenComputerPayload;
import com.evandouard.hxh.quest.QuestData;
import com.evandouard.hxh.quest.QuestManager;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Ordinateur Hunter : fiches publiques, fiches réservées aux Hunters, texte jamais envoyé quand verrouillé. */
@GameTestHolder(HxHMod.MODID)
@PrefixGameTestTemplate(false)
public final class ComputerTests {

    private static OpenComputerPayload.Entry find(List<OpenComputerPayload.Entry> view, String id) {
        for (OpenComputerPayload.Entry e : view) if (e.id().equals(id)) return e;
        return null;
    }

    @GameTest(template = "empty")
    public static void entriesUnlockWithProgress(GameTestHelper helper) {
        helper.assertTrue(HunterComputer.entries().size() >= 10, "Les fiches sont chargées : " + HunterComputer.entries().size());
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        QuestData d = QuestManager.data(player);
        d.setHunter(false);
        List<OpenComputerPayload.Entry> guest = HunterComputer.view(player);
        OpenComputerPayload.Entry welcome = find(guest, "bienvenue"), troupe = find(guest, "brigade");
        helper.assertTrue(welcome != null && welcome.unlocked() && !welcome.text().isEmpty(), "Fiche publique lisible");
        helper.assertTrue(troupe != null && !troupe.unlocked(), "Brigade Fantôme réservée aux Hunters");
        helper.assertTrue(troupe.text().isEmpty() && troupe.title().equals("???"), "Rien n'est envoyé d'une fiche verrouillée");
        helper.assertFalse(troupe.hint().isEmpty(), "Mais son indice l'est");
        d.setHunter(true);
        helper.assertTrue(find(HunterComputer.view(player), "brigade").unlocked(), "Avec la licence, la fiche s'ouvre");
        helper.succeed();
    }
}
