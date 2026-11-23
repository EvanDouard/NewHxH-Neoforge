package com.evandouard.hxh.airship;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

/** Une destination du dirigeable : position (x, z) sur la carte du monde HxH (blocs) et prix du billet en Jenny. */
public record AirshipDestination(ResourceLocation id, int x, int z, int price) {
    public static AirshipDestination parse(ResourceLocation id, JsonObject json) {
        return new AirshipDestination(id, json.get("x").getAsInt(), json.get("z").getAsInt(), json.get("price").getAsInt());
    }

    /** Clé de traduction du nom : {@code airship.hxh.dest.<chemin>}. */
    public String nameKey() {
        return "airship.hxh.dest." + id.getPath();
    }
}
