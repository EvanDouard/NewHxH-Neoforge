package com.evandouard.hxh.client.geo;

import com.evandouard.hxh.npc.NpcEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Rendu GeckoLib des personnages qui ont un modèle sur mesure (appelé par {@code NpcRenderer}). */
public class NpcGeoRenderer extends GeoEntityRenderer<NpcEntity> {
    public NpcGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new NpcGeoModel());
        this.shadowRadius = 0.45f;
    }
}
