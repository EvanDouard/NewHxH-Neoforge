package com.evandouard.hxh.gametest;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.hatsu.Hatsu;
import com.evandouard.hxh.hatsu.Techniques;
import com.evandouard.hxh.mob.GonUltimeEntity;
import com.evandouard.hxh.registry.ModEntities;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Gon ultime : enfant sans Jajanken ultime, puis adulte sous 30 % de vie (stats ×3, ultime accessible). */
@GameTestHolder(HxHMod.MODID)
@PrefixGameTestTemplate(false)
public final class GonUltimeTests {
    private GonUltimeTests() {}

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void gonUltimeTurnsAdultWhenLowOnHealth(GameTestHelper helper) {
        GonUltimeEntity gon = helper.spawn(ModEntities.GON_ULTIME.get(), 1, 1, 1);
        int[] strengthBefore = new int[1];
        helper.runAfterDelay(5, () -> {
            HunterData data = HunterStats.get(gon);
            helper.assertTrue(gon.isPrepared() && "gon".equals(data.kit()), "Gon ultime a le kit de Gon");
            helper.assertTrue(!gon.isAdult() && "gon".equals(gon.modelId()), "Il commence enfant");
            long now = helper.getLevel().getGameTime();
            helper.assertTrue(Hatsu.castRefusal(data, Techniques.GON_ROCK_EPIC, now) != null,
                    "Enfant : pas encore de Jajanken ultime");
            strengthBefore[0] = data.level(Stat.STRENGTH);
            gon.setHealth(gon.getMaxHealth() * 0.2f);
        });
        helper.runAfterDelay(10, () -> helper.succeedWhen(() -> {
            HunterData data = HunterStats.get(gon);
            helper.assertTrue(gon.isAdult(), "Sous 30 % de vie, Gon passe en mode adulte");
            helper.assertTrue("gon_adulte".equals(gon.modelId()), "Modèle adulte");
            helper.assertTrue(gon.getBbHeight() > 2.2f, "Plus grand en adulte (" + gon.getBbHeight() + ")");
            helper.assertTrue(data.level(Stat.STRENGTH) >= Math.min(100, strengthBefore[0] * 3), "Stats ×3");
            helper.assertTrue(gon.getHealth() >= gon.getMaxHealth() * 0.9f, "Vie rendue à la transformation");
            long now = helper.getLevel().getGameTime();
            helper.assertTrue(Hatsu.castRefusal(data, Techniques.GON_ROCK_EPIC, now) == null,
                    "Adulte : Jajanken ultime accessible (" + Hatsu.castRefusal(data, Techniques.GON_ROCK_EPIC, now) + ")");
        }));
    }
}
