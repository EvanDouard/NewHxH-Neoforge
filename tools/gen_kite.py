#!/usr/bin/env python3
"""Ébauche rapide du camp de la forêt de Kite (Evan repasse dessus)."""
import json
fills, blocks, ents = [], [], []
def F(b, s, m=None):
    d = {"box": b, "block": s}
    if m: d["mode"] = m
    fills.append(d)
def Bk(x, y, z, s): blocks.append({"pos": [x, y, z], "state": s})
W = D = 40
F([0, -1, 0, W, -1, D], "minecraft:podzol")
F([14, -1, 14, 26, -1, 26], "minecraft:coarse_dirt")
# cabane
F([6, 0, 6, 15, 4, 13], "minecraft:spruce_log[axis=y]", "walls"); F([5, 5, 5, 16, 5, 14], "minecraft:spruce_slab")
F([10, 0, 13, 11, 2, 13], "minecraft:air"); Bk(8, 0, 8, "minecraft:red_bed[facing=north,part=foot]"); Bk(8, 0, 7, "minecraft:red_bed[facing=north,part=head]")
Bk(13, 0, 8, "minecraft:crafting_table"); Bk(13, 0, 10, "minecraft:barrel[facing=up]")
# feu de camp
Bk(20, 0, 20, "minecraft:campfire[lit=true]")
for (x, z) in ((18, 20), (22, 20), (20, 18), (20, 22)): Bk(x, 0, z, "minecraft:spruce_stairs[facing=south]")
# nids de bêtes (monticules de foin et de feuilles)
for (x, z) in ((30, 8), (33, 30), (8, 31)):
    F([x - 2, 0, z - 2, x + 2, 1, z + 2], "minecraft:hay_block[axis=y]"); F([x - 1, 2, z - 1, x + 1, 2, z + 1], "minecraft:oak_leaves[persistent=true]")
# entrée de grotte
F([28, 0, 0, 36, 4, 3], "minecraft:stone", "walls"); F([30, 0, 0, 34, 3, 3], "minecraft:air"); F([29, 4, 0, 35, 4, 3], "minecraft:mossy_cobblestone")
# borne
Bk(21, 0, 24, "hxh:airship_terminal[facing=north]")
for (x, z) in ((12, 17), (28, 22), (16, 30)):
    F([x, 0, z, x, 5, z], "minecraft:spruce_log[axis=y]"); F([x - 2, 6, z - 2, x + 2, 8, z + 2], "minecraft:spruce_leaves[persistent=true]")
def npc(i, x, y, z, yaw=0): ents.append({"type": "hxh:npc", "pos": [x, y, z], "yaw": yaw, "nbt": '{npc:"%s",NoAI:1b,PersistenceRequired:1b,Invulnerable:1b}' % i})
npc("guide_foret_kite", 22, 0, 18, 90)
json.dump({"x": 6400, "z": 2240, "clear": [0, 0, 0, W, 12, D], "fills": fills, "blocks": blocks, "entities": ents}, open("src/main/resources/data/hxh/world_places/kite_forest.json", "w"), indent=1)
print(len(fills), len(blocks))
