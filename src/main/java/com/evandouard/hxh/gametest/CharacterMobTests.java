package com.evandouard.hxh.gametest;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.mob.CharacterEntity;
import com.evandouard.hxh.registry.ModEntities;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Personnages du manga en mob (Gon, Killua) : Hatsu de leur kit, neutres, s'entraident. */
@GameTestHolder(HxHMod.MODID)
@PrefixGameTestTemplate(false)
public final class CharacterMobTests {
    private CharacterMobTests() {}

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void gonAndKilluaGetTheirHatsuAndStayNeutral(GameTestHelper helper) {
        CharacterEntity gon = helper.spawn(ModEntities.GON.get(), 1, 1, 1);
        CharacterEntity killua = helper.spawn(ModEntities.KILLUA.get(), 3, 1, 1);
        Zombie[] attacker = new Zombie[1];
        helper.runAfterDelay(5, () -> {
            for (CharacterEntity c : new CharacterEntity[]{gon, killua}) {
                HunterData data = HunterStats.get(c);
                helper.assertTrue(c.isPrepared(), c.characterId() + " : mob d'exemple appliqué");
                helper.assertTrue(data.nenUnlocked(), c.characterId() + " : maîtrise le Nen");
                helper.assertTrue(data.kit().equals(c.characterId()), c.characterId() + " : kit " + data.kit());
                helper.assertTrue(!(c instanceof Enemy), c.characterId() + " : neutre, pas hostile");
                helper.assertTrue(c.getTarget() == null, c.characterId() + " : n'attaque personne de lui-même");
                helper.assertTrue(c.getCustomName() != null, c.characterId() + " : nom affiché");
            }
            // on frappe Gon : il riposte, et Killua vient l'aider
            Zombie zombie = helper.spawn(EntityType.ZOMBIE, 2, 1, 4);
            attacker[0] = zombie;
            zombie.setNoAi(true);
            // beaucoup de vie (pas invulnérable : un mob invulnérable ne peut pas être pris pour cible)
            zombie.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
            zombie.setHealth(1000);
            // le coup peut être entièrement absorbé (Ten, passifs) : on enregistre l'agresseur comme le ferait un
            // coup qui porte, c'est ce que lit l'IA de riposte
            gon.hurt(gon.damageSources().mobAttack(zombie), 2f);
            gon.setLastHurtByMob(zombie);
        });
        helper.runAfterDelay(30, () -> helper.succeedWhen(() -> {
            Zombie zombie = attacker[0]; // (les tests voisins peuvent avoir leurs propres zombies)
            helper.assertTrue(zombie != null && zombie.isAlive(), "Le zombie est là");
            helper.assertTrue(gon.getTarget() == zombie, "Gon riposte (cible : " + gon.getTarget() + ", dernier agresseur : " + gon.getLastHurtByMob() + ")");
            helper.assertTrue(killua.getTarget() == zombie, "Killua vient aider Gon (cible : " + killua.getTarget() + ")");
        }));
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void kurapikaGetsHisChainsKit(GameTestHelper helper) {
        CharacterEntity kurapika = helper.spawn(ModEntities.KURAPIKA.get(), 1, 1, 1);
        helper.runAfterDelay(5, () -> helper.succeedWhen(() -> {
            HunterData data = HunterStats.get(kurapika);
            helper.assertTrue(kurapika.isPrepared(), "Mob d'exemple appliqué");
            helper.assertTrue("kurapika".equals(data.kit()), "Kit de Kurapika (" + data.kit() + ")");
            helper.assertTrue(!(kurapika instanceof Enemy), "Neutre");
            helper.assertTrue(kurapika.isAlliedTo(helper.spawn(ModEntities.GON.get(), 3, 1, 1)), "Allié de Gon");
        }));
    }
}
