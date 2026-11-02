package com.evandouard.hxh.gametest;

import com.evandouard.hxh.HxHConfig;
import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.block.WaterDivinationBlock;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.NenMode;
import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.nen.NenModes;
import com.evandouard.hxh.registry.ModBlocks;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Tests de l'aura, des modes Ten / Zetsu / Ren, de l'hexagone et de la divination.
 * Les modes sont testés sur des zombies : un mob doit pouvoir utiliser le Nen comme un joueur.
 */
@GameTestHolder(HxHMod.MODID)
@PrefixGameTestTemplate(false)
public final class NenTests {
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    private static Zombie nenZombie(GameTestHelper helper) {
        Zombie z = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, POS);
        z.setNoGravity(true);
        NenModes.unlock(z);
        return z;
    }

    @GameTest(template = "empty")
    public static void hexagonAffinity(GameTestHelper helper) {
        NenType e = NenType.ENHANCER;
        helper.assertTrue(e.affinity(NenType.ENHANCER) == 1.0f, "Même type : 100 %");
        helper.assertTrue(Math.abs(e.affinity(NenType.TRANSMUTER) - 0.8f) < 1e-4, "Voisin : 80 %");
        helper.assertTrue(Math.abs(e.affinity(NenType.EMITTER) - 0.8f) < 1e-4, "Voisin : 80 %");
        helper.assertTrue(Math.abs(e.affinity(NenType.CONJURER) - 0.6f) < 1e-4, "À deux cases : 60 %");
        helper.assertTrue(Math.abs(e.affinity(NenType.MANIPULATOR) - 0.6f) < 1e-4, "À deux cases : 60 %");
        helper.assertTrue(Math.abs(e.affinity(NenType.SPECIALIST) - 0.4f) < 1e-4, "Opposé : 40 %");
        helper.assertTrue(Math.abs(NenType.CONJURER.affinity(NenType.MANIPULATOR) - 0.6f) < 1e-4,
                "Matérialisation → Manipulation : 60 %");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lockedNenRefusesModes(GameTestHelper helper) {
        Zombie z = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, POS);
        helper.assertTrue(NenModes.trySetMode(z, NenMode.TEN) != null, "Sans Nen, le Ten doit être refusé");
        helper.assertTrue(HunterStats.get(z).mode() == NenMode.NONE, "Le mode ne doit pas changer");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void auraRegenerates(GameTestHelper helper) {
        Zombie z = nenZombie(helper);
        HunterData data = HunterStats.get(z);
        helper.assertTrue(data.aura() == data.auraMax(), "Aura pleine au déblocage");
        data.setAura(0);
        helper.runAfterDelay(41, () -> {
            double expected = 2 * HxHConfig.AURA_REGEN_PER_SECOND.get();
            helper.assertTrue(data.aura() >= expected * 0.9,
                    "Après 2 s, aura attendue ≈ " + expected + ", obtenue " + data.aura());
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void zetsuRegeneratesFaster(GameTestHelper helper) {
        Zombie normal = nenZombie(helper);
        Zombie zetsu = nenZombie(helper);
        HunterStats.get(normal).setAura(0);
        HunterStats.get(zetsu).setAura(0);
        helper.assertTrue(NenModes.trySetMode(zetsu, NenMode.ZETSU) == null, "Zetsu refusé");
        helper.runAfterDelay(41, () -> {
            float a = HunterStats.get(normal).aura();
            float b = HunterStats.get(zetsu).aura();
            helper.assertTrue(b > a * 2, "Zetsu doit régénérer bien plus vite : normal " + a + ", zetsu " + b);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void renRunsOutAndExhausts(GameTestHelper helper) {
        Zombie z = nenZombie(helper);
        HunterData data = HunterStats.get(z);
        data.setAura(2);
        helper.assertTrue(NenModes.trySetMode(z, NenMode.REN) == null, "Ren refusé");
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(data.mode() == NenMode.NONE, "À court d'aura, le Ren doit s'arrêter");
            helper.assertTrue(z.hasEffect(MobEffects.WEAKNESS) && z.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
                    "L'épuisement doit donner faiblesse et lenteur");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void modesChangeDamage(GameTestHelper helper) {
        Zombie z = nenZombie(helper);
        helper.assertTrue(NenModes.incomingFactor(z) == 1f && NenModes.outgoingFactor(z) == 1f, "Neutre : aucun effet");
        NenModes.trySetMode(z, NenMode.TEN);
        helper.assertTrue(NenModes.incomingFactor(z) < 1f, "Ten : moins de dégâts subis");
        NenModes.trySetMode(z, NenMode.ZETSU);
        helper.assertTrue(NenModes.incomingFactor(z) > 1f, "Zetsu : plus de dégâts subis");
        NenModes.trySetMode(z, NenMode.REN);
        helper.assertTrue(NenModes.outgoingFactor(z) > 1f, "Ren : plus de dégâts infligés");

        // Vérification en vrai : un zombie en Zetsu perd plus de vie qu'un zombie neutre
        Zombie neutral = nenZombie(helper);
        Zombie weak = nenZombie(helper);
        NenModes.trySetMode(weak, NenMode.ZETSU);
        float n0 = neutral.getHealth();
        float w0 = weak.getHealth();
        neutral.hurt(helper.getLevel().damageSources().generic(), 4f);
        weak.hurt(helper.getLevel().damageSources().generic(), 4f);
        helper.assertTrue(w0 - weak.getHealth() > n0 - neutral.getHealth(), "Le Zetsu doit rendre vulnérable");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void divinationRevealsType(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.WATER_DIVINATION.get().defaultBlockState());
        BlockPos abs = helper.absolutePos(pos);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        // Sans Nen : rien ne se passe
        helper.assertTrue(WaterDivinationBlock.divine(helper.getLevel(), abs, player) == null,
                "Sans Nen, la divination ne doit rien révéler");

        // Manipulateur : type révélé et la feuille tourne
        HunterData data = HunterStats.get(player);
        data.setNenType(NenType.MANIPULATOR);
        NenModes.unlock(player);
        Direction before = helper.getLevel().getBlockState(abs).getValue(HorizontalDirectionalBlock.FACING);
        helper.assertTrue(WaterDivinationBlock.divine(helper.getLevel(), abs, player) == NenType.MANIPULATOR,
                "La divination doit révéler Manipulation");
        helper.assertTrue(data.nenRevealed(), "Le type doit être marqué comme révélé");
        Direction after = helper.getLevel().getBlockState(abs).getValue(HorizontalDirectionalBlock.FACING);
        helper.assertTrue(after == before.getClockWise(), "La feuille doit avoir tourné");
        helper.succeed();
    }
}
