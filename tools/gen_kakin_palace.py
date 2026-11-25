#!/usr/bin/env python3
"""Génère data/hxh/world_places/kakin_palace.json : ébauche du Palais royal de Kakin (Evan repasse dessus)."""
import json
fills, blocks, ents = [], [], []
def F(b, s, m=None):
    d = {"box": b, "block": s}
    if m: d["mode"] = m
    fills.append(d)
def Bk(x, y, z, s): blocks.append({"pos": [x, y, z], "state": s})
RED, OR, YE, GOLD = "minecraft:red_concrete", "minecraft:orange_terracotta", "minecraft:yellow_terracotta", "minecraft:gold_block"
STEM = "minecraft:stripped_crimson_stem[axis=y]"
W = D = 100
F([0, -1, 0, W, -1, D], "minecraft:grass_block")
F([8, -1, 8, 92, -1, 96], "minecraft:stone_bricks")
F([46, -1, 73, 54, -1, 96], "minecraft:smooth_stone")
# bassins
F([14, -1, 78, 34, -1, 88], "minecraft:water"); F([66, -1, 78, 86, -1, 88], "minecraft:water")
# mur d'enceinte + porte monumentale
F([4, 0, 4, 96, 6, 96], RED, "walls"); F([4, 7, 4, 96, 7, 96], "minecraft:red_nether_brick_slab", "walls")
F([46, 0, 96, 54, 5, 96], "minecraft:air"); F([45, 6, 96, 55, 6, 96], GOLD)
F([44, 0, 95, 44, 8, 97], STEM); F([56, 0, 95, 56, 8, 97], STEM)
# tours d'angle
for (x, z) in ((2, 2), (90, 2), (2, 90), (90, 90)):
    F([x, 0, z, x + 8, 12, z + 8], RED, "walls"); F([x, 13, z, x + 8, 13, z + 8], RED)
    F([x - 1, 14, z - 1, x + 9, 14, z + 9], OR); F([x + 1, 15, z + 1, x + 7, 15, z + 7], OR); F([x + 3, 16, z + 3, x + 5, 17, z + 5], YE)
# plateforme à trois gradins
F([14, -1, 12, 86, 0, 72], "minecraft:stone_bricks"); F([18, 1, 14, 82, 1, 70], "minecraft:stone_bricks"); F([22, 2, 16, 78, 2, 66], "minecraft:polished_blackstone_bricks")
F([46, 0, 73, 54, 0, 73], "minecraft:stone_brick_stairs[facing=north]")
F([46, 1, 71, 54, 1, 71], "minecraft:stone_brick_stairs[facing=north]")
F([46, 2, 67, 54, 2, 67], "minecraft:stone_brick_stairs[facing=north]")
F([46, 1, 72, 54, 1, 72], "minecraft:stone_bricks") if False else None
# grande salle du trône
F([30, 3, 20, 70, 15, 60], RED, "walls"); F([30, 16, 20, 70, 16, 60], RED)
F([31, 2, 21, 69, 2, 59], "minecraft:polished_blackstone")
F([46, 3, 60, 54, 9, 60], "minecraft:air"); F([45, 10, 60, 55, 10, 60], GOLD)
for x in (34, 40, 46, 54, 60, 66): F([x, 3, 61, x, 14, 61], STEM)
F([32, 15, 61, 68, 15, 62], "minecraft:red_terracotta")
for z in (28, 40, 52):
    for x in (36, 64): F([x, 3, z, x, 15, z], STEM); Bk(x + (1 if x < 50 else -1), 14, z, "minecraft:lantern[hanging=true]")
F([47, 3, 22, 53, 3, 59], "minecraft:red_carpet")
F([42, 3, 22, 58, 3, 28], "minecraft:polished_blackstone"); F([45, 4, 22, 55, 4, 26], "minecraft:polished_blackstone")
Bk(50, 5, 23, GOLD); F([49, 6, 22, 51, 9, 22], GOLD)
# toits à étages
F([28, 17, 18, 72, 18, 62], OR); F([32, 19, 22, 68, 20, 58], OR); F([38, 21, 28, 62, 22, 52], YE); F([46, 23, 36, 54, 24, 44], YE)
F([50, 25, 40, 50, 30, 40], GOLD)
# ailes
for (x0, x1, dx0, dx1) in ((19, 27, 22, 24), (73, 81, 76, 78)):
    F([x0, 2, 24, x1, 9, 56], RED, "walls"); F([x0 - 1, 10, 23, x1 + 1, 10, 57], OR); F([x0 + 1, 11, 26, x1 - 1, 11, 54], OR); F([x0 + 3, 12, 30, x1 - 3, 12, 50], YE)
    F([dx0, 2, 56, dx1, 5, 56], "minecraft:air")
    Bk((x0 + x1) // 2, 8, 40, "minecraft:lantern[hanging=true]")
# lampadaires de l'allée
for z in range(78, 96, 4):
    for x in (44, 56): F([x, 0, z, x, 1, z], "minecraft:stone_brick_wall"); Bk(x, 2, z, "minecraft:lantern[hanging=false]")
# borne (arrivée du dirigeable à 50, 90)
Bk(52, 0, 88, "hxh:airship_terminal[facing=north]")
def npc(i, x, y, z, yaw=0): ents.append({"type": "hxh:npc", "pos": [x, y, z], "yaw": yaw, "nbt": '{npc:"%s",NoAI:1b,PersistenceRequired:1b,Invulnerable:1b}' % i})
npc("chambellan_kakin", 50, 5, 26, 0); npc("garde_royal_kakin", 48, 0, 94, 0); npc("garde_royal_kakin", 52, 0, 94, 0); npc("garde_royal_kakin", 48, 3, 58, 0); npc("garde_royal_kakin", 52, 3, 58, 0)
json.dump({"x": 5950, "z": 2410, "clear": [0, 0, 0, W, 34, D], "fills": fills, "blocks": blocks, "entities": ents}, open("src/main/resources/data/hxh/world_places/kakin_palace.json", "w"), indent=1)
print(len(fills), len(blocks))
