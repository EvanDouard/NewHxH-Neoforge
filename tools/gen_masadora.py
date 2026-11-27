#!/usr/bin/env python3
"""Génère data/hxh/world_places/masadora.json : ébauche de la ville de Masadora (Greed Island), Evan repasse dessus."""
import json
fills, blocks, ents = [], [], []
def F(b, s, m=None):
    d = {"box": b, "block": s}
    if m: d["mode"] = m
    fills.append(d)
def Bk(x, y, z, s): blocks.append({"pos": [x, y, z], "state": s})
WT, RT, AIR, GOLD = "minecraft:white_terracotta", "minecraft:red_terracotta", "minecraft:air", "minecraft:gold_block"
W, D = 70, 80
F([0, -1, 0, W, -1, D], "minecraft:grass_block")
F([32, -1, 0, 38, -1, D], "minecraft:dirt_path"); F([0, -1, 36, W, -1, 40], "minecraft:dirt_path")
F([24, -1, 28, 46, -1, 48], "minecraft:stone_bricks")
# fontaine centrale
F([32, 0, 34, 38, 0, 40], "minecraft:stone_brick_slab", "walls"); F([33, -1, 35, 37, -1, 39], "minecraft:water"); F([35, 0, 37, 35, 2, 37], "minecraft:stone_brick_wall")
# mairie et tour à flèche dorée
F([22, 0, 4, 48, 12, 18], WT, "walls"); F([21, 13, 3, 49, 13, 19], RT); F([30, 14, 8, 40, 24, 14], WT, "walls"); F([29, 25, 7, 41, 25, 15], RT)
F([35, 26, 11, 35, 31, 11], GOLD); F([34, 0, 18, 36, 3, 18], AIR)
# maisons blanches à toit rouge, portes côté rue
def house(x, z, door_east):
    F([x, 0, z, x + 5, 3, z + 5], WT, "walls"); F([x - 1, 4, z - 1, x + 6, 4, z + 6], "minecraft:brick_slab")
    dx = x + 5 if door_east else x
    F([dx, 0, z + 2, dx, 1, z + 2], AIR)
for z in (22, 44, 56):
    for x in (4, 13): house(x, z, True)
    for x in (52, 61): house(x, z, False)
# étals de marché
def stall(x, z, wool):
    for (a, b) in ((x, z), (x + 3, z), (x, z + 2), (x + 3, z + 2)): F([a, 0, b, a, 1, b], "minecraft:oak_fence")
    F([x, 2, z, x + 3, 2, z + 2], wool); Bk(x + 1, 0, z + 1, "minecraft:barrel[facing=up]"); Bk(x + 2, 0, z + 1, "minecraft:barrel[facing=up]")
stall(25, 29, "minecraft:red_wool"); stall(41, 29, "minecraft:yellow_wool"); stall(25, 43, "minecraft:blue_wool"); stall(41, 43, "minecraft:green_wool")
# portail sud
F([30, 0, 78, 30, 5, 78], "minecraft:stone_bricks"); F([40, 0, 78, 40, 5, 78], "minecraft:stone_bricks"); F([30, 6, 78, 40, 6, 78], "minecraft:stone_bricks")
# borne du dirigeable (arrivée à 35, 72)
Bk(41, 0, 72, "hxh:airship_terminal[facing=west]")
def npc(i, x, y, z, yaw=0): ents.append({"type": "hxh:npc", "pos": [x, y, z], "yaw": yaw, "nbt": '{npc:"%s",NoAI:1b,PersistenceRequired:1b,Invulnerable:1b}' % i})
npc("marchand_cartes_masadora", 27, 0, 32, 0); npc("habitant_masadora", 35, 0, 62, 0)
json.dump({"x": 3440, "z": 4075, "clear": [0, 0, 0, W, 34, D], "fills": fills, "blocks": blocks, "entities": ents}, open("src/main/resources/data/hxh/world_places/masadora.json", "w"), indent=1)
print(len(fills), len(blocks))
