package com.evandouard.hxh.client.look;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Apparence d'un personnage, décrite dans {@code assets/<ns>/looks/<id>.json} (générée par tools/skins) :
 * <pre>
 * {
 *   "skin": "hxh:textures/entity/npc/netero.png",   // skin au format joueur 64x64
 *   "slim": false,                                    // bras fins (3 pixels)
 *   "scale": [0.95, 0.93, 0.95],                      // largeur, hauteur, profondeur (autour des pieds)
 *   "extras": {                                       // cubes en plus accrochés aux membres
 *     "texture": "hxh:textures/entity/npc/netero_extra.png", "texture_size": [64, 64],
 *     "cubes": [ { "part": "head", "uv": [0, 0], "uv_size": [4, 3, 2], "from": [-2, 0, -4.4],
 *                  "size": [4, 3, 2], "pivot": [0, 1.5, -3.4], "rotation": [0, 0, 0], "inflate": 0 } ]
 *   }
 * }
 * </pre>
 * Coordonnées comme dans Blockbench / ModelPart : pixels, Y vers le bas, dans le repère du membre ({@code head},
 * {@code body}, {@code right_arm}, {@code left_arm}, {@code right_leg}, {@code left_leg}). « uv_size » donne la
 * taille dépliée dans la texture ; « size » peut être fractionnaire (le cube est alors étiré).
 */
public record CharacterLook(@Nullable ResourceLocation skin, boolean slim, float scaleX, float scaleY, float scaleZ,
                            @Nullable ResourceLocation extrasTexture, Map<String, ModelPart> extras) {

    public static final List<String> PARTS = List.of("head", "body", "right_arm", "left_arm", "right_leg", "left_leg");

    public static CharacterLook parse(JsonObject json) {
        ResourceLocation skin = json.has("skin") ? ResourceLocation.parse(GsonHelper.getAsString(json, "skin")) : null;
        boolean slim = GsonHelper.getAsBoolean(json, "slim", false);
        float[] scale = floats(json, "scale", new float[]{1, 1, 1});
        ResourceLocation texture = null;
        Map<String, ModelPart> extras = Map.of();
        if (json.has("extras")) {
            JsonObject ex = GsonHelper.getAsJsonObject(json, "extras");
            texture = ResourceLocation.parse(GsonHelper.getAsString(ex, "texture"));
            float[] texSize = floats(ex, "texture_size", new float[]{64, 64});
            extras = bake(GsonHelper.getAsJsonArray(ex, "cubes"), (int) texSize[0], (int) texSize[1]);
        }
        return new CharacterLook(skin, slim, scale[0], scale[1], scale[2], texture, extras);
    }

    /** Un modèle par membre, chaque cube étant un enfant posé sur son pivot (rotation puis étirement). */
    private static Map<String, ModelPart> bake(JsonArray cubes, int texW, int texH) {
        Map<String, MeshDefinition> meshes = new LinkedHashMap<>();
        Map<String, Map<String, float[]>> stretch = new HashMap<>();
        int index = 0;
        for (JsonElement el : cubes) {
            JsonObject c = el.getAsJsonObject();
            String part = GsonHelper.getAsString(c, "part");
            if (!PARTS.contains(part)) throw new IllegalArgumentException("membre inconnu : " + part);
            float[] uv = floats(c, "uv", new float[]{0, 0});
            float[] from = floats(c, "from", null);
            float[] size = floats(c, "size", null);
            float[] uvSize = floats(c, "uv_size", new float[]{
                    Math.max(1, Math.round(size[0])), Math.max(1, Math.round(size[1])), Math.max(1, Math.round(size[2]))});
            float[] pivot = floats(c, "pivot", new float[]{from[0] + size[0] / 2, from[1] + size[1] / 2, from[2] + size[2] / 2});
            float[] rot = floats(c, "rotation", new float[]{0, 0, 0});
            float inflate = GsonHelper.getAsFloat(c, "inflate", 0f);
            float sx = size[0] / uvSize[0], sy = size[1] / uvSize[1], sz = size[2] / uvSize[2];
            String name = "c" + index++;
            PartDefinition root = meshes.computeIfAbsent(part, p -> new MeshDefinition()).getRoot();
            root.addOrReplaceChild(name,
                    CubeListBuilder.create().texOffs((int) uv[0], (int) uv[1]).addBox(
                            (from[0] - pivot[0]) / sx, (from[1] - pivot[1]) / sy, (from[2] - pivot[2]) / sz,
                            uvSize[0], uvSize[1], uvSize[2], new CubeDeformation(inflate)),
                    PartPose.offsetAndRotation(pivot[0], pivot[1], pivot[2],
                            rot[0] * Mth.DEG_TO_RAD, rot[1] * Mth.DEG_TO_RAD, rot[2] * Mth.DEG_TO_RAD));
            if (sx != 1 || sy != 1 || sz != 1) stretch.computeIfAbsent(part, p -> new HashMap<>()).put(name, new float[]{sx, sy, sz});
        }
        Map<String, ModelPart> out = new LinkedHashMap<>();
        meshes.forEach((part, mesh) -> {
            ModelPart baked = LayerDefinition.create(mesh, texW, texH).bakeRoot();
            stretch.getOrDefault(part, Map.of()).forEach((name, s) -> {
                ModelPart child = baked.getChild(name);
                child.xScale = s[0];
                child.yScale = s[1];
                child.zScale = s[2];
            });
            out.put(part, baked);
        });
        return out;
    }

    private static float[] floats(JsonObject json, String key, float[] fallback) {
        if (!json.has(key)) {
            if (fallback == null) throw new IllegalArgumentException("champ manquant : " + key);
            return fallback;
        }
        JsonArray arr = GsonHelper.getAsJsonArray(json, key);
        float[] out = new float[arr.size()];
        for (int i = 0; i < out.length; i++) out[i] = arr.get(i).getAsFloat();
        return out;
    }
}
