#!/usr/bin/env python3
"""Génère data/hxh/world_places/greed_island.json : ébauche rapide (à retoucher à la main par Evan)."""
import json
fills, blocks, ents = [], [], []
def F(b, s, m=None):
    d = {"box": b, "block": s}
    if m: d["mode"] = m
    fills.append(d)
def Bk(x, y, z, s): blocks.append({"pos": [x, y, z], "state": s})
W, D = 50, 70
F([0, -1, 0, W, -1, D], "minecraft:grass_block")
F([20, -1, 0, 30, -1, D], "minecraft:dirt_path")                 # grand-rue
F([0, -1, 30, W, -1, 40], "minecraft:dirt_path")
F([20, -1, 30, 30, -1, 40], "minecraft:cobblestone")              # place centrale
# maisons (ébauche)
for i, (x, z) in enumerate(((4, 6), (34, 6), (4, 46), (34, 46), (4, 20), (34, 22))):
    F([x, 0, z, x + 9, 5, z + 8], "minecraft:oak_planks", "walls"); F([x, 6, z, x + 9, 6, z + 8], "minecraft:spruce_slab")
    F([x + 4, 0, z + 8 if x < 20 else z, x + 5, 2, z + 8 if x < 20 else z], "minecraft:air")
    Bk(x + 2, 0, z + 2, "minecraft:red_bed[facing=north,part=foot]"); Bk(x + 2, 0, z + 1, "minecraft:red_bed[facing=north,part=head]")
# le Livre (pierre de départ) au centre de la place
F([24, 0, 34, 26, 0, 36], "minecraft:stone_bricks"); Bk(25, 1, 35, "minecraft:lectern[facing=south]"); Bk(25, 0, 35, "minecraft:enchanting_table") if False else None
for (x, z) in ((22, 32), (28, 32), (22, 38), (28, 38)):
    F([x, 0, z, x, 3, z], "minecraft:stone_brick_wall"); Bk(x, 4, z, "minecraft:lantern[hanging=false]")
# borne d'arrivée (la destination est (25, 35) relatif → on pose la borne à côté)
Bk(27, 0, 35, "hxh:airship_terminal[facing=west]")
def npc(i, x, y, z, yaw=0): ents.append({"type": "hxh:npc", "pos": [x, y, z], "yaw": yaw, "nbt": '{npc:"%s",NoAI:1b,PersistenceRequired:1b,Invulnerable:1b}' % i})
npc("guide_greed_island", 25, 0, 33, 0)
json.dump({"x": 3345, "z": 4075, "clear": [0, 0, 0, W, 25, D], "fills": fills, "blocks": blocks, "entities": ents}, open("src/main/resources/data/hxh/world_places/greed_island.json", "w"), indent=1)
print(len(fills), len(blocks))
