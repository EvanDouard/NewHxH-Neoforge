#!/usr/bin/env python3
"""Génère data/hxh/world_places/padokea_town.json : ébauche de la ville autour de la Tour Céleste (Evan repasse dessus)."""
import json
fills, blocks, ents = [], [], []
def F(b, s, m=None):
    d = {"box": b, "block": s}
    if m: d["mode"] = m
    fills.append(d)
def Bk(x, y, z, s): blocks.append({"pos": [x, y, z], "state": s})
AIR, GOLD = "minecraft:air", "minecraft:gold_block"
W, D = 90, 95
F([0, -1, 0, W, -1, D], "minecraft:grass_block")
F([40, -1, 0, 46, -1, D], "minecraft:gray_concrete"); F([0, -1, 46, W, -1, 50], "minecraft:gray_concrete")
F([43, -1, 0, 43, -1, D], "minecraft:white_concrete"); 
# gare : bâtiment, quai et deux voies
F([4, 0, 6, 31, 5, 15], "minecraft:stone_bricks", "walls"); F([3, 6, 5, 32, 6, 16], "minecraft:stone_brick_slab"); F([14, 0, 15, 20, 3, 15], AIR)
F([4, 0, 16, 31, 0, 19], "minecraft:smooth_stone"); F([4, 0, 20, 31, 0, 20], "minecraft:rail[shape=east_west]"); F([4, 0, 23, 31, 0, 23], "minecraft:rail[shape=east_west]")
for x in (6, 17, 28): F([x, 1, 16, x, 4, 16], "minecraft:oak_fence"); F([x - 2, 5, 16, x + 2, 5, 19], "minecraft:spruce_slab")
# centre commercial
F([52, 0, 6, 81, 6, 23], "minecraft:white_concrete", "walls"); F([51, 7, 5, 82, 7, 24], "minecraft:light_gray_concrete")
F([54, 1, 23, 62, 3, 23], "minecraft:glass"); F([70, 1, 23, 79, 3, 23], "minecraft:glass"); F([64, 0, 23, 68, 3, 23], AIR); F([55, 5, 23, 78, 5, 23], GOLD)
# restaurant
F([54, 0, 30, 63, 3, 37], "minecraft:orange_terracotta", "walls"); F([53, 4, 29, 64, 4, 38], "minecraft:spruce_slab"); F([58, 0, 37, 59, 1, 37], AIR)
F([54, 3, 38, 63, 3, 40], "minecraft:red_wool")
# maisons (quartier résidentiel)
def house(x, z):
    F([x, 0, z, x + 5, 3, z + 5], "minecraft:oak_planks", "walls"); F([x - 1, 4, z - 1, x + 6, 4, z + 6], "minecraft:spruce_slab"); F([x + 2, 0, z, x + 3, 1, z], AIR)
for z in (54, 68):
    for x in (4, 14, 24): house(x, z)
# hôpital avec croix rouge
F([50, 0, 54, 69, 7, 67], "minecraft:white_concrete", "walls"); F([49, 8, 53, 70, 8, 68], "minecraft:light_gray_concrete")
F([57, 5, 67, 61, 5, 67], "minecraft:red_concrete"); F([59, 3, 67, 59, 7, 67], "minecraft:red_concrete"); F([63, 0, 67, 65, 2, 67], AIR)
# commissariat
F([74, 0, 54, 87, 5, 65], "minecraft:blue_terracotta", "walls"); F([73, 6, 53, 88, 6, 66], "minecraft:blue_concrete"); F([79, 0, 65, 81, 2, 65], AIR)
# hôtel de cinq niveaux
F([52, 0, 70, 65, 16, 83], "minecraft:quartz_block", "walls"); F([51, 17, 69, 66, 17, 84], "minecraft:smooth_quartz")
for y in (4, 8, 12): F([53, y, 70, 64, y, 70], GOLD)
F([57, 0, 70, 60, 3, 70], AIR)
# borne du dirigeable (arrivée à 43, 86)
Bk(48, 0, 86, "hxh:airship_terminal[facing=west]")
def npc(i, x, y, z, yaw=0): ents.append({"type": "hxh:npc", "pos": [x, y, z], "yaw": yaw, "nbt": '{npc:"%s",NoAI:1b,PersistenceRequired:1b,Invulnerable:1b}' % i})
npc("chef_de_gare_padokea", 18, 0, 17, 0); npc("infirmiere_padokea", 64, 0, 70, 0); npc("policier_padokea", 80, 0, 68, 0); npc("habitant_padokea", 43, 0, 60, 0)
json.dump({"x": 2610, "z": 1400, "clear": [0, 0, 0, W, 25, D], "fills": fills, "blocks": blocks, "entities": ents}, open("src/main/resources/data/hxh/world_places/padokea_town.json", "w"), indent=1)
print(len(fills), len(blocks))
