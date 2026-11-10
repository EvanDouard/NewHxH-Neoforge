package com.evandouard.hxh.gametest;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.hatsu.Hatsu;
import com.evandouard.hxh.hatsu.KitBars;
import com.evandouard.hxh.hatsu.Netero;
import com.evandouard.hxh.nen.NenModes;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Personnages ajoutés depuis la fiche docs/kits-personnages.md. */
@GameTestHolder(HxHMod.MODID)
@PrefixGameTestTemplate(false)
public final class CharacterKitTests {

    /** Husk immobile, regardant vers +X. */
    static Zombie husk(GameTestHelper helper, int x) {
        Zombie z = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(x, 1, 1));
        z.setNoGravity(true);
        z.setYRot(-90f);
        z.setYHeadRot(-90f);
        z.setXRot(0f);
        return z;
    }

    static HunterData hunter(Zombie z, NenType type, String kit) {
        HunterData d = HunterStats.get(z);
        d.setNenType(type);
        NenModes.unlock(z);
        d.setNenRevealed(true);
        d.setNenLevel(100);
        d.setAura(d.auraMax());
        if (kit != null) d.setKit(kit);
        return d;
    }

    @GameTest(template = "empty")
    public static void neteroSpendsPrayerOnKannon(GameTestHelper helper) {
        Zombie netero = husk(helper, 0);
        Zombie target = husk(helper, 2);
        HunterData d = hunter(netero, NenType.ENHANCER, "netero");
        long now = helper.getLevel().getGameTime();
        helper.assertTrue(Hatsu.select(netero, "netero_kannon"), "Kannon sélectionnable");
        helper.assertTrue("message.hxh.no_prayer".equals(Hatsu.castRefusal(d, Netero.KANNON, now)), "Sans Prière : refus");
        // Prier remplit la jauge
        for (int t = 10; t <= 200; t += 10) Netero.prayTick(netero, t);
        helper.assertTrue(Math.abs(d.kitValue(KitBars.PRAYER) - 50f) < 0.01f, "10 s de prière : 50 → " + d.kitValue(KitBars.PRAYER));
        float hp = target.getHealth();
        helper.assertTrue(Hatsu.cast(netero) == null, "La paume de Kannon part");
        helper.assertTrue(Math.abs(d.kitValue(KitBars.PRAYER) - 20f) < 0.01f, "30 de Prière dépensés");
        helper.assertTrue(target.getHealth() < hp, "La cible devant est frappée");
        // Poings de Kannon : coups renforcés
        helper.assertTrue(Hatsu.select(netero, "netero_kannon_fists"), "Poings sélectionnables");
        helper.assertTrue(Hatsu.cast(netero) == null, "Les poings de Kannon s'allument");
        float out = Hatsu.modifyOutgoingMelee(netero, target, 10f);
        helper.assertTrue(out >= 15.9f, "Coups ×1,6 avec les poings de Kannon : " + out);
        // Main zéro : seulement prière pleine
        helper.assertTrue("message.hxh.no_prayer".equals(Hatsu.castRefusal(d, Netero.ZERO_HAND, now)), "Main zéro : prière pleine requise");
        d.setKitValue(KitBars.PRAYER, KitBars.PRAYER_MAX);
        helper.assertTrue(Hatsu.castRefusal(d, Netero.ZERO_HAND, now) == null, "Prière pleine : Main zéro prête");
        helper.succeed();
    }
}
