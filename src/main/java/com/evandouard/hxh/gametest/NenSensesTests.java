package com.evandouard.hxh.gametest;

import com.evandouard.hxh.HxHConfig;
import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.Mastery;
import com.evandouard.hxh.data.NenMode;
import com.evandouard.hxh.effect.ModEffects;
import com.evandouard.hxh.nen.NenDefense;
import com.evandouard.hxh.nen.NenModes;
import com.evandouard.hxh.nen.NenSenses;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * In puis Zetsu, Gyo, En, Ryu et Shu.
 */
@GameTestHolder(HxHMod.MODID)
@PrefixGameTestTemplate(false)
public final class NenSensesTests {

    private static Zombie nenHusk(GameTestHelper helper) {
        Zombie z = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(1, 1, 1));
        z.setNoGravity(true);
        NenModes.unlock(z);
        return z;
    }

    @GameTest(template = "empty")
    public static void inComesBeforeZetsu(GameTestHelper helper) {
        Zombie z = nenHusk(helper);
        HunterData d = HunterStats.get(z);
        NenModes.toggle(z, NenMode.ZETSU);
        helper.assertTrue(d.mode() == NenMode.IN, "1er appui : In");
        NenModes.toggle(z, NenMode.ZETSU);
        helper.assertTrue(d.mode() == NenMode.NONE, "Zetsu pas encore débloqué : retour au neutre");
        d.setMasteryLevel(Mastery.IN, HxHConfig.ZETSU_UNLOCK_IN_LEVEL.get());
        NenModes.toggle(z, NenMode.ZETSU);
        helper.assertTrue(d.mode() == NenMode.ZETSU, "Zetsu débloqué : la touche donne directement le Zetsu (plus d'In)");
        NenModes.toggle(z, NenMode.ZETSU);
        helper.assertTrue(d.mode() == NenMode.NONE, "2e appui : neutre");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void gyoAndEnCostAuraAndShowAsEffects(GameTestHelper helper) {
        Zombie z = nenHusk(helper);
        HunterData d = HunterStats.get(z);
        helper.assertTrue(NenSenses.setGyo(z, true) == null && NenSenses.setEn(z, true) == null, "Gyo et En auraient dû s'activer");
        float aura = d.aura();
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(z.hasEffect(ModEffects.GYO) && z.hasEffect(ModEffects.EN), "Effets témoins Gyo et En attendus");
            helper.assertTrue(d.aura() < aura, "Gyo + En doivent consommer de l'aura");
            NenModes.toggle(z, NenMode.ZETSU); // In : Gyo et En se coupent
            helper.assertTrue(!d.gyo() && !d.en(), "L'In coupe le Gyo et l'En");
            helper.assertTrue(NenSenses.setGyo(z, true) != null, "Pas de Gyo pendant l'In");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void enRadiusGrowsWithMastery(GameTestHelper helper) {
        HunterData d = new HunterData();
        double r0 = NenSenses.enRadius(d);
        d.setMasteryLevel(Mastery.EN, 40);
        double r40 = NenSenses.enRadius(d);
        helper.assertTrue(r0 == 2 && r40 == 12, "Rayon de l'En attendu 2 puis 12 : " + r0 + " / " + r40);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ryuTradesDefenseForAttack(GameTestHelper helper) {
        Zombie z = nenHusk(helper);
        HunterData d = HunterStats.get(z);
        NenModes.trySetMode(z, NenMode.TEN);
        float balancedDef = NenDefense.nenResistance(z);
        float balancedAtk = NenDefense.outgoingFactor(z, false);
        d.setRyuAttack(90);
        helper.assertTrue(NenDefense.outgoingFactor(z, false) > balancedAtk, "Ryu offensif : plus de dégâts");
        helper.assertTrue(NenDefense.nenResistance(z) < balancedDef, "Ryu offensif : moins de protection");
        d.setRyuAttack(10);
        helper.assertTrue(NenDefense.nenResistance(z) > balancedDef, "Ryu défensif : plus de protection");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shuStrengthensHeldTool(GameTestHelper helper) {
        Zombie z = nenHusk(helper);
        helper.assertTrue(NenSenses.shuDamageFactor(z) == 1f, "Mains nues : pas de Shu");
        z.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        helper.assertTrue(NenSenses.shuDamageFactor(z) == 1f, "Aura neutre : pas de Shu");
        NenModes.trySetMode(z, NenMode.TEN);
        helper.assertTrue(NenSenses.shuDamageFactor(z) > 1f, "En Ten avec une épée : Shu actif");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void gyoTiersFollowEvansTable(GameTestHelper helper) {
        // 5 pièges · 10 invisibilité Minecraft · 20 Ryu · 25 In · 35 type · 50 invisibilité par le Nen · max Zetsu
        helper.assertTrue(NenSenses.GYO_SEE_TRAPS == 5 && NenSenses.GYO_SEE_INVISIBLE == 10 && NenSenses.GYO_SEE_RYU == 20
                && NenSenses.GYO_SEE_IN == 25 && NenSenses.GYO_SEE_TYPE == 35 && NenSenses.GYO_SEE_NEN_INVISIBLE == 50
                && NenSenses.GYO_SEE_ZETSU == Mastery.MAX_LEVEL, "Paliers du Gyo");
        Zombie potion = nenHusk(helper);
        potion.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.INVISIBILITY, 200));
        Zombie hidden = nenHusk(helper);
        NenSenses.nenHide(hidden, 200);
        Zombie zetsu = nenHusk(helper);
        helper.assertTrue(!NenSenses.gyoReveals(9, potion, false) && NenSenses.gyoReveals(10, potion, false), "Potion : palier 10");
        helper.assertTrue(NenSenses.nenHidden(hidden), "Texture Surprise = invisibilité par le Nen");
        helper.assertTrue(!NenSenses.gyoReveals(49, hidden, false) && NenSenses.gyoReveals(50, hidden, false), "Nen : palier 50");
        helper.assertTrue(!NenSenses.gyoReveals(99, zetsu, true) && NenSenses.gyoReveals(100, zetsu, true), "Zetsu : maîtrise maximale");
        helper.assertTrue(!NenSenses.gyoReveals(-1, potion, false), "Sans Gyo, rien");
        helper.succeed();
    }
}
