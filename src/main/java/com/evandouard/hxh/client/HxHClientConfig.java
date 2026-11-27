package com.evandouard.hxh.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Réglages du joueur, propres à son client (fichier config/hxh-client.toml) : minimap. */
public final class HxHClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue MINIMAP_ENABLED = BUILDER
            .comment("Minimap du monde HxH affichée (touche M pour l'activer ou la cacher)")
            .define("minimap.enabled", false);
    public static final ModConfigSpec.IntValue MINIMAP_ZOOM = BUILDER
            .comment("Niveau de zoom de la minimap (0 = très loin, 7 = très près)")
            .defineInRange("minimap.zoom", 3, 0, 7);
    public static final ModConfigSpec.IntValue MINIMAP_SIZE = BUILDER
            .comment("Côté de la minimap en pixels")
            .defineInRange("minimap.size", 100, 64, 200);
    public static final ModConfigSpec.DoubleValue MINIMAP_X = BUILDER
            .comment("Position horizontale (0 = bord gauche, 1 = bord droit)")
            .defineInRange("minimap.x", 1.0, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue MINIMAP_Y = BUILDER
            .comment("Position verticale (0 = haut, 1 = bas)")
            .defineInRange("minimap.y", 0.0, 0.0, 1.0);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private HxHClientConfig() {}
}
