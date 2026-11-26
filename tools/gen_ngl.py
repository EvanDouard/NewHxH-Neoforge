#!/usr/bin/env python3
"""Génère data/hxh/world_places/ngl_village.json : ébauche du village de la République NGL (Neo Green Life), Evan repasse dessus."""
import json
fills, blocks, ents = [], [], []
def F(b, s, m=None):
    d = {"box": b, "block": s}
    if m: d["mode"] = m
    fills.append(d)
def Bk(x, y, z, s): blocks.append({"pos": [x, y, z], "state": s})
PL, SP, LOG, AIR = "minecraft:oak_planks", "minecraft:spruce_slab", "minecraft:oak_log[axis=y]", "minecraft:air"
F([0, -1, 0, 80, -1, 80], "minecraft:grass_block")
# chemins en croix
F([38, -1, 0, 42, -1, 80], "minecraft:dirt_path"); F([0, -1, 38, 80, -1, 42], "minecraft:dirt_path")
# puits central
F([39, 0, 39, 41, 1, 41], "minecraft:cobblestone", "walls"); F([40, -1, 40, 40, -1, 40], "minecraft:water")
F([39, 2, 39, 41, 2, 41], "minecraft:spruce_slab")
for (x, z) in ((39, 39), (41, 39), (39, 41), (41, 41)): F([x, 2, z, x, 3, z], "minecraft:oak_fence") if False else None
# cabanes de bois
def hut(x, z, w=5, d=5):
    F([x, 0, z, x + w - 1, 3, z + d - 1], PL, "walls")
    F([x - 1, 4, z - 1, x + w, 4, z + d], SP)
    F([x + w // 2, 0, z + d - 1, x + w // 2, 1, z + d - 1], AIR)
    Bk(x + w // 2, 2, z + d // 2, "minecraft:lantern[hanging=false]") if False else None
for (x, z) in ((6, 6), (16, 6), (6, 20), (16, 20), (54, 6), (64, 6), (54, 20), (64, 20)): hut(x, z)
# halle commune au toit de paille
F([30, 0, 10, 50, 5, 24], PL, "walls")
for i, (a, b) in enumerate(((29, 25), (31, 23), (33, 21))): pass
F([29, 6, 9, 51, 6, 25], "minecraft:hay_block"); F([31, 7, 11, 49, 7, 23], "minecraft:hay_block"); F([34, 8, 14, 46, 8, 20], "minecraft:hay_block")
F([38, 0, 24, 42, 3, 24], AIR)
for x in (30, 50): F([x, 0, 10, x, 5, 10], LOG); F([x, 0, 24, x, 5, 24], LOG)
# champs : blé à l'ouest, carottes à l'est, un canal d'eau au milieu
F([6, -1, 46, 30, -1, 70], "minecraft:farmland[moisture=7]"); F([6, -1, 58, 30, -1, 58], "minecraft:water")
F([6, 0, 46, 30, 0, 57], "minecraft:wheat[age=7]"); F([6, 0, 59, 30, 0, 70], "minecraft:wheat[age=7]")
F([50, -1, 46, 74, -1, 70], "minecraft:farmland[moisture=7]"); F([50, -1, 58, 74, -1, 58], "minecraft:water")
F([50, 0, 46, 74, 0, 57], "minecraft:carrots[age=7]"); F([50, 0, 59, 74, 0, 70], "minecraft:carrots[age=7]")
# tour de guet en rondins
F([70, 0, 30, 72, 12, 32], LOG, "walls"); F([69, 12, 29, 73, 12, 33], PL); F([69, 13, 29, 73, 13, 33], "minecraft:oak_fence", "walls")
F([71, 0, 30, 71, 1, 30], AIR)
# portail d'entrée
F([36, 0, 76, 36, 6, 76], LOG); F([44, 0, 76, 44, 6, 76], LOG); F([35, 7, 76, 45, 7, 76], "minecraft:spruce_planks")
Bk(40, 6, 76, "minecraft:lantern[hanging=true]")
# borne du dirigeable (arrivée à 40, 66)
Bk(44, 0, 66, "hxh:airship_terminal[facing=west]")
def npc(i, x, y, z, yaw=0): ents.append({"type": "hxh:npc", "pos": [x, y, z], "yaw": yaw, "nbt": '{npc:"%s",NoAI:1b,PersistenceRequired:1b,Invulnerable:1b}' % i})
npc("ancien_ngl", 44, 0, 36, 0); npc("fermier_ngl", 33, 0, 52, 90)
json.dump({"x": 1310, "z": 4850, "clear": [0, 0, 0, 80, 20, 80], "fills": fills, "blocks": blocks, "entities": ents}, open("src/main/resources/data/hxh/world_places/ngl_village.json", "w"), indent=1)
print(len(fills), len(blocks))
