#!/usr/bin/env python3
"""Génère data/hxh/world_places/mafia_hq.json : ébauche du QG de la Communauté mafieuse à Yorknew (Evan repasse dessus)."""
import json
fills, blocks, ents = [], [], []
def F(b, s, m=None):
    d = {"box": b, "block": s}
    if m: d["mode"] = m
    fills.append(d)
def Bk(x, y, z, s): blocks.append({"pos": [x, y, z], "state": s})
BS, PB, BC, GOLD, AIR = "minecraft:polished_blackstone_bricks", "minecraft:polished_blackstone_bricks", "minecraft:black_concrete", "minecraft:gold_block", "minecraft:air"
W, D = 70, 85
F([0, -1, 0, W, -1, D], "minecraft:grass_block")
F([6, -1, 6, 64, -1, 66], "minecraft:smooth_stone"); F([30, -1, 66, 40, -1, 85], "minecraft:gray_concrete")
# enceinte + portail
F([4, 0, 4, 66, 5, 70], BS, "walls"); F([4, 6, 4, 66, 6, 70], "minecraft:blackstone_slab", "walls")
F([31, 0, 70, 39, 4, 70], AIR); F([30, 5, 70, 40, 5, 70], GOLD)
F([30, 0, 69, 30, 7, 71], PB); F([40, 0, 69, 40, 7, 71], PB)
# tours d'angle
for (x, z) in ((1, 1), (63, 1), (1, 67), (63, 67)):
    F([x, 0, z, x + 5, 9, z + 5], PB, "walls"); F([x - 1, 10, z - 1, x + 6, 10, z + 6], "minecraft:blackstone_slab"); Bk(x + 2, 10, z + 2, "minecraft:lantern[hanging=false]")
# bâtiment principal à trois niveaux
F([15, 0, 12, 55, 14, 44], BC, "walls"); F([14, 15, 11, 56, 15, 45], "minecraft:polished_blackstone")
F([16, 4, 13, 54, 4, 43], "minecraft:polished_blackstone"); F([16, 9, 13, 54, 9, 43], "minecraft:polished_blackstone")
for y in (2, 6, 11): F([17, y, 44, 53, y + 1, 44], "minecraft:tinted_glass")
F([33, 0, 44, 37, 3, 44], AIR); F([32, 4, 44, 38, 4, 44], GOLD)
F([34, 0, 30, 36, 0, 43], "minecraft:red_carpet")
F([30, 0, 16, 40, 0, 16], "minecraft:polished_blackstone"); F([33, 1, 16, 37, 1, 16], "minecraft:dark_oak_planks")
# tour sommitale + flèche dorée
F([26, 16, 20, 44, 28, 36], BC, "walls"); F([25, 29, 19, 45, 29, 37], "minecraft:polished_blackstone")
F([35, 30, 28, 35, 36, 28], GOLD)
# voitures noires du parking
for x in (10, 18, 48, 56):
    F([x, 0, 52, x + 3, 0, 53], BC); F([x + 1, 1, 52, x + 2, 1, 53], "minecraft:glass")
# lampadaires de l'allée
for z in range(72, 85, 4):
    for x in (29, 41): F([x, 0, z, x, 1, z], "minecraft:stone_brick_wall"); Bk(x, 2, z, "minecraft:lantern[hanging=false]")
# borne du dirigeable (arrivée à 35, 80)
Bk(42, 0, 80, "hxh:airship_terminal[facing=west]")
def npc(i, x, y, z, yaw=0): ents.append({"type": "hxh:npc", "pos": [x, y, z], "yaw": yaw, "nbt": '{npc:"%s",NoAI:1b,PersistenceRequired:1b,Invulnerable:1b}' % i})
npc("parrain_mafia", 35, 0, 20, 0); npc("garde_mafia", 32, 0, 73, 0); npc("garde_mafia", 38, 0, 73, 0); npc("garde_mafia", 32, 0, 47, 0); npc("garde_mafia", 38, 0, 47, 0)
json.dump({"x": 680, "z": 3790, "clear": [0, 0, 0, W, 40, D], "fills": fills, "blocks": blocks, "entities": ents}, open("src/main/resources/data/hxh/world_places/mafia_hq.json", "w"), indent=1)
print(len(fills), len(blocks))
