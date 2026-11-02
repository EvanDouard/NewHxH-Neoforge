package com.evandouard.hxh.gametest;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Tests en jeu du squelette. Lancés par ./gradlew runGameTestServer (et en CI).
 */
@GameTestHolder(HxHMod.MODID)
@PrefixGameTestTemplate(false)
public final class SkeletonTests {

    @GameTest(template = "empty")
    public static void hunterCardIsRegistered(GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(HxHMod.id("hunter_card")),
                "L'objet hxh:hunter_card n'est pas enregistré");
        helper.assertTrue(ModItems.HUNTER_CARD.get().getDefaultInstance().getMaxStackSize() == 1,
                "La carte Hunter doit être unique (stack de 1)");
        helper.succeed();
    }
}
