package com.evandouard.hxh.gametest;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.combat.Combat;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.NenMode;
import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.effect.ModEffects;
import com.evandouard.hxh.nen.NenModes;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Combat : combos, coup de pied, frappe chargée, peur.
 */
@GameTestHolder(HxHMod.MODID)
@PrefixGameTestTemplate(false)
public final class CombatTests {

    private static Zombie husk(GameTestHelper helper, int x) {
        Zombie z = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(x, 1, 1));
        z.setNoGravity(true);
        z.setYRot(-90f);
        z.setYHeadRot(-90f);
        z.setXRot(0f);
        return z;
    }

    @GameTest(template = "empty")
    public static void comboBuildsUpAndFinishes(GameTestHelper helper) {
        Zombie a = husk(helper, 0);
        Zombie v = husk(helper, 2);
        float[] f = new float[Combat.COMBO_MAX];
        for (int i = 0; i < Combat.COMBO_MAX; i++) f[i] = Combat.onMeleeHit(a, v);
        helper.assertTrue(f[0] == 1f && f[1] > f[0] && f[4] > f[3], "Le combo doit monter : " + java.util.Arrays.toString(f));
        helper.assertTrue(Combat.comboCount(a) == 0, "Après le coup final, le combo repart de zéro");
        Combat.onMeleeHit(a, v);
        Zombie other = husk(helper, 1);
        helper.assertTrue(Combat.onMeleeHit(a, other) == 1f, "Changer de cible remet le combo à 1");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void combatAnimationsAlternateAndCharge(GameTestHelper helper) {
        Zombie a = husk(helper, 0);
        a.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, net.minecraft.world.item.ItemStack.EMPTY);
        helper.assertTrue(com.evandouard.hxh.combat.CombatAnimations.punch(a) == com.evandouard.hxh.combat.CombatAnim.PUNCH_MAIN,
                "Premier coup : main principale");
        helper.assertTrue(com.evandouard.hxh.combat.CombatAnimations.punch(a) == null, "Même tick : pas de double animation");
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(com.evandouard.hxh.combat.CombatAnimations.punch(a) == com.evandouard.hxh.combat.CombatAnim.PUNCH_OFF,
                    "Deuxième coup : l'autre main");
            Combat.startStrike(a);
            helper.assertTrue(com.evandouard.hxh.combat.CombatAnimations.last(a) == com.evandouard.hxh.combat.CombatAnim.CHARGE,
                    "Touche Frappe maintenue : armement");
            Combat.chargedStrike(a, 1.0f);
            helper.assertTrue(com.evandouard.hxh.combat.CombatAnimations.last(a) == com.evandouard.hxh.combat.CombatAnim.CHARGED_PUNCH,
                    "Frappe chargée : gros coup de poing");
            Combat.kick(a);
            helper.assertTrue(com.evandouard.hxh.combat.CombatAnimations.last(a) == com.evandouard.hxh.combat.CombatAnim.KICK,
                    "Appui court : coup de pied");
            a.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
            helper.runAfterDelay(4, () -> {
                helper.assertTrue(com.evandouard.hxh.combat.CombatAnimations.punch(a) == null, "Avec une arme : animation de Minecraft");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty")
    public static void kickPushesAndHasCooldown(GameTestHelper helper) {
        Zombie a = husk(helper, 0);
        Zombie v = husk(helper, 2);
        float before = v.getHealth();
        helper.assertTrue(Combat.kick(a) == null, "Le coup de pied aurait dû partir");
        helper.assertTrue(v.getHealth() < before, "Le coup de pied blesse");
        helper.assertTrue("message.hxh.kick_cooldown".equals(Combat.kick(a)), "Petite recharge entre deux coups de pied");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void projectionDependsOnStrengthAndCharge(GameTestHelper helper) {
        // Le tableau voulu
        helper.assertTrue(Combat.projection(false, Combat.Charge.NONE) == Combat.Projection.NONE, "Pas chargé + pas fort : rien");
        helper.assertTrue(Combat.projection(false, Combat.Charge.GOOD) == Combat.Projection.NONE, "Chargé (bon) + pas fort : rien");
        helper.assertTrue(Combat.projection(true, Combat.Charge.NONE) == Combat.Projection.LIGHT, "Pas chargé + fort : légère");
        helper.assertTrue(Combat.projection(true, Combat.Charge.GOOD) == Combat.Projection.LIGHT, "Chargé (bon) + fort : légère");
        helper.assertTrue(Combat.projection(false, Combat.Charge.PERFECT) == Combat.Projection.LIGHT, "Parfait + pas fort : légère");
        helper.assertTrue(Combat.projection(true, Combat.Charge.PERFECT) == Combat.Projection.HEAVY, "Parfait + fort : grosse");

        // En vrai : un débutant ne projette pas d'un coup de pied, un combattant fort si
        Zombie weak = husk(helper, 0);
        Zombie v1 = husk(helper, 2);
        Combat.kick(weak);
        double weakPush = Math.abs(v1.getDeltaMovement().x);
        Zombie strong = husk(helper, 6);
        com.evandouard.hxh.stats.HunterStats.setLevel(strong, com.evandouard.hxh.data.Stat.STRENGTH, 60);
        Zombie v2 = husk(helper, 8);
        Combat.kick(strong);
        double strongPush = Math.abs(v2.getDeltaMovement().x);
        helper.assertTrue(strongPush > weakPush + 0.2, "Le coup de pied d'un fort projette plus : " + weakPush + " / " + strongPush);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chargedStrikeRewardsTiming(GameTestHelper helper) {
        helper.assertTrue(Combat.chargeValue(0) == 0f && Combat.chargeValue(Combat.CHARGE_PERIOD_TICKS / 2) == 1f
                && Combat.chargeValue(Combat.CHARGE_PERIOD_TICKS) == 0f, "La barre monte puis redescend");
        Zombie a = husk(helper, 0);
        husk(helper, 2);
        helper.assertTrue(Combat.chargedStrike(a, 0.1f) == 0f, "Trop bas : frappe ratée");
        helper.assertTrue(a.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Frappe ratée : on trébuche");
        float good = Combat.chargedStrike(a, 0.5f);
        float perfect = Combat.chargedStrike(a, 1.0f);
        helper.assertTrue(good > 0 && perfect > good * 1.4f, "Plus la barre est haute, plus ça frappe : " + good + " / " + perfect);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void damageNumbersFormat(GameTestHelper helper) {
        helper.assertTrue(com.evandouard.hxh.combat.DamageNumbers.format(10.4f, false).equals("-10"), "-10 attendu");
        helper.assertTrue(com.evandouard.hxh.combat.DamageNumbers.format(2.5f, false).equals("-2.5"), "-2.5 attendu");
        helper.assertTrue(com.evandouard.hxh.combat.DamageNumbers.format(1.0f, true).equals("+1"), "+1 attendu");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void overwhelmingAuraCausesFear(GameTestHelper helper) {
        Zombie weak = husk(helper, 0);
        Zombie strong = husk(helper, 2);
        NenModes.unlock(strong);
        HunterData d = HunterStats.get(strong);
        for (Stat s : Stat.values()) d.setLevel(s, 60);
        d.setNenLevel(60);
        helper.assertTrue(!Combat.applyFearIfThreatened(weak), "Aura au repos : pas de peur");
        NenModes.trySetMode(strong, NenMode.REN);
        helper.assertTrue(Combat.applyFearIfThreatened(weak), "Aura déployée bien plus puissante : peur");
        helper.assertTrue(weak.hasEffect(ModEffects.FEAR), "Effet Peur attendu");
        helper.succeed();
    }
}
