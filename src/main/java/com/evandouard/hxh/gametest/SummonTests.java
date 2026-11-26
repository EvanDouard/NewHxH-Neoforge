package com.evandouard.hxh.gametest;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.fx.NenFx;
import com.evandouard.hxh.hatsu.Netero;
import com.evandouard.hxh.hatsu.Specials;
import com.evandouard.hxh.summon.NenSummonEntity;
import com.evandouard.hxh.summon.NenSummons;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Invocations en modèle GeckoLib : Kannon, Pot-Clean, Terpsichora, Docteur Blythe. */
@GameTestHolder(HxHMod.MODID)
@PrefixGameTestTemplate(false)
public final class SummonTests {

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void kannonRisesBehindNeteroThenLeaves(GameTestHelper helper) {
        Zombie netero = CharacterKitTests.husk(helper, 2);
        netero.yBodyRot = -90f;
        Netero.kannon(helper.getLevel(), netero, 9f, 0xF0FFFFFF, 20, "strike");
        NenSummonEntity k = NenSummons.find(helper.getLevel(), NenSummons.KANNON, netero);
        helper.assertTrue(k != null, "Kannon apparaît");
        helper.assertTrue(Math.abs(k.scale() - 9f / 4f) < 0.01f, "9 blocs de haut");
        // Netero regarde vers +X : la statue est derrière lui (X plus petit)
        helper.assertTrue(k.getX() < netero.getX() - 1.5, "Derrière Netero : " + (k.getX() - netero.getX()));
        // un second appel remplace la statue au lieu d'en empiler deux
        Netero.kannon(helper.getLevel(), netero, 12f, 0xF0FFFFFF, 20, "zero");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(NenSummonEntity.class, netero.getBoundingBox().inflate(16)).size() == 1,
                "Une seule statue");
        helper.runAfterDelay(25, () -> helper.succeedWhen(() -> helper.assertTrue(
                NenSummons.find(helper.getLevel(), NenSummons.KANNON, netero) == null, "Elle disparaît à la fin")));
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void potCleanFloatsOverTheDebtorAndShowsTheDebt(GameTestHelper helper) {
        Zombie knuckle = CharacterKitTests.husk(helper, 0);
        Zombie debtor = CharacterKitTests.husk(helper, 2);
        HunterData d = CharacterKitTests.hunter(knuckle, NenType.CONJURER, "knuckle");
        helper.assertTrue(Specials.KNUCKLE_POT_CLEAN.perform(knuckle, 1f), "Pot-Clean s'attache");
        NenSummonEntity pc = NenSummons.find(helper.getLevel(), NenSummons.POT_CLEAN, debtor);
        helper.assertTrue(pc != null, "Pot-Clean apparaît au-dessus de la cible");
        helper.assertTrue(pc.getY() > debtor.getY() + debtor.getBbHeight(), "Au-dessus de sa tête");
        com.evandouard.hxh.hatsu.Hatsu.modifyOutgoingMelee(knuckle, debtor, 12f);
        helper.assertTrue("6".equals(pc.label()), "La pancarte affiche la dette (6) : " + pc.label());
        helper.assertTrue(NenSummons.find(helper.getLevel(), NenSummons.POT_CLEAN, debtor) == pc, "Toujours le même Pot-Clean");
        Specials.KNUCKLE_CANCEL.perform(knuckle, 1f);
        helper.assertTrue(NenSummons.find(helper.getLevel(), NenSummons.POT_CLEAN, debtor) == null, "Annuler le retire");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void pitouSummonsTheDollAndThePuppet(GameTestHelper helper) {
        Zombie pitou = CharacterKitTests.husk(helper, 0);
        pitou.yBodyRot = -90f;
        Zombie patient = CharacterKitTests.husk(helper, 2);
        NenFx.doll(pitou, patient, 100);
        NenSummonEntity doll = NenSummons.find(helper.getLevel(), NenSummons.DOCTOR_BLYTHE, patient);
        helper.assertTrue(doll != null && doll.getY() > patient.getY() + patient.getBbHeight(), "Docteur Blythe au-dessus du patient");
        NenFx.puppetStrings(pitou, NenFx.SPECIALIST, 100);
        NenSummonEntity puppet = NenSummons.find(helper.getLevel(), NenSummons.TERPSICHORA, pitou);
        helper.assertTrue(puppet != null && puppet.getX() < pitou.getX(), "Terpsichora derrière Pitou");
        // quand ses effets prennent fin (poupée relâchée), ses invocations s'en vont
        NenFx.clear(pitou);
        helper.assertTrue(NenSummons.find(helper.getLevel(), NenSummons.DOCTOR_BLYTHE, patient) == null
                && NenSummons.find(helper.getLevel(), NenSummons.TERPSICHORA, pitou) == null, "Tout disparaît avec Pitou");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void zenoDragonFliesToTheTarget(GameTestHelper helper) {
        Zombie zeno = CharacterKitTests.husk(helper, 0);
        net.minecraft.world.phys.Vec3 end = zeno.position().add(8, 1, 0);
        NenFx.sprite(helper.getLevel(), NenFx.Sprite.DRAGON, zeno, end, 2.6f, NenFx.GOLD, 16);
        NenSummonEntity d = helper.getLevel().getEntitiesOfClass(NenSummonEntity.class, zeno.getBoundingBox().inflate(12),
                s -> s.model().equals(NenSummons.DRAGON)).stream().findFirst().orElse(null);
        helper.assertTrue(d != null, "Le dragon apparaît");
        helper.runAfterDelay(9, () -> {
            helper.assertTrue(d.position().distanceTo(end) < 1.0, "Il est arrivé sur la cible : " + d.position().distanceTo(end));
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void creaturesReplaceTheirImages(GameTestHelper helper) {
        var level = helper.getLevel();
        Zombie biscuit = CharacterKitTests.husk(helper, 0);
        biscuit.yBodyRot = -90f;
        Zombie victim = CharacterKitTests.husk(helper, 3);
        // Cookie se tient à droite de Biscuit
        NenFx.sprite(level, NenFx.Sprite.COOKIE, biscuit, biscuit.position(), 1.3f, 0xFFFFB6D9, 80);
        NenSummonEntity cookie = NenSummons.find(level, NenSummons.COOKIE, biscuit);
        helper.assertTrue(cookie != null, "Cookie apparaît");
        double side = Math.hypot(cookie.getX() - biscuit.getX(), cookie.getZ() - biscuit.getZ());
        helper.assertTrue(side > 0.8 && side < 1.4, "À côté de Biscuit : " + side);
        // la bombe de Genthru affiche les secondes restantes
        NenFx.sprite(level, NenFx.Sprite.BOMB, victim, victim.position(), 1.5f, 0xFFFF7A3A, 60);
        NenSummonEntity bomb = NenSummons.find(level, NenSummons.BOMB, victim);
        helper.assertTrue(bomb != null && bomb.getY() > victim.getY() + victim.getBbHeight(), "Bombe au-dessus de la cible");
        // un démon de Razor posé au sol sous le point de l'image
        net.minecraft.world.phys.Vec3 at = biscuit.position().add(0, 1.2, 4);
        NenFx.sprite(level, NenFx.Sprite.DEMON, null, at, 2.2f, 0xFFB03A3A, 40);
        NenSummonEntity demon = level.getEntitiesOfClass(NenSummonEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(3),
                s -> s.model().equals(NenSummons.DEMON)).stream().findFirst().orElse(null);
        helper.assertTrue(demon != null && Math.abs(demon.getY() - (at.y - 1.2)) < 0.1, "Démon posé au sol");
        helper.runAfterDelay(3, () -> {
            helper.assertTrue("3".equals(bomb.label()), "Compte à rebours : " + bomb.label());
            helper.succeed();
        });
    }
}
