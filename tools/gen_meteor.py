#!/usr/bin/env python3
"""Ébauche rapide de Meteor City : montagnes de déchets, bidonville, marché, tour d'ordures (Evan repasse dessus)."""
import json, random
random.seed(7)
fills, blocks, ents = [], [], []
def F(b, s, m=None):
    d = {"box": b, "block": s}
    if m: d["mode"] = m
    fills.append(d)
def Bk(x, y, z, s): blocks.append({"pos": [x, y, z], "state": s})
W = D = 110
JUNK = ["minecraft:gravel", "minecraft:cobblestone", "minecraft:coarse_dirt", "minecraft:andesite", "minecraft:oxidized_copper", "minecraft:black_concrete", "minecraft:gray_concrete", "minecraft:rooted_dirt"]
F([0, -1, 0, W, -1, D], "minecraft:coarse_dirt")
F([40, -1, 70, 70, -1, D], "minecraft:gravel")                       # route d'entrée
# montagnes de déchets (cônes en couches)
PILES = [(14, 14, 11, 17), (96, 16, 12, 20), (12, 62, 9, 13), (98, 64, 10, 15), (20, 98, 8, 10), (92, 98, 8, 11), (30, 40, 7, 9), (80, 40, 7, 10)]
for (cx, cz, r, h) in PILES:
    for k in range(h):
        rr = max(1, round(r * (1 - k / h)))
        F([cx - rr, k, cz - rr, cx + rr, k, cz + rr], JUNK[(k + cx) % len(JUNK)])
# tour d'ordures centrale
F([47, 0, 22, 63, 6, 38], "minecraft:gravel")
for k, (a, h) in enumerate(((7, 14), (5, 28), (4, 40))):
    F([55 - a, 7 + (0 if k == 0 else (14, 28)[k - 1] - 6), 30 - a, 55 + a, h + 6, 30 + a], JUNK[k * 2 % len(JUNK)])
F([55, 47, 30, 55, 52, 30], "minecraft:iron_bars"); Bk(55, 53, 30, "minecraft:lantern[hanging=false]")
F([53, 0, 38, 57, 3, 38], "minecraft:air"); F([53, 0, 38, 57, 0, 38], "minecraft:coarse_dirt")   # entrée de la cachette
# bidonville : cabanes en matériaux de récupération
for i in range(12):
    x, z = 36 + (i % 4) * 11, 44 + (i // 4) * 9
    mat = random.choice(["minecraft:spruce_planks", "minecraft:oak_planks", "minecraft:iron_block", "minecraft:white_terracotta", "minecraft:rusty" if False else "minecraft:stone_bricks"])
    F([x, 0, z, x + 5, 3, z + 5], mat, "walls"); F([x, 4, z, x + 5, 4, z + 5], random.choice(["minecraft:spruce_slab", "minecraft:iron_bars" if False else "minecraft:cobblestone_slab"]))
    F([x + 2, 0, z + 5, x + 3, 1, z + 5], "minecraft:air")
    Bk(x + 1, 0, z + 1, random.choice(["minecraft:barrel[facing=up]", "minecraft:cauldron", "minecraft:crafting_table"]))
# marché de récupération (étals)
for i in range(5):
    x = 36 + i * 9
    F([x, 0, 80, x, 2, 80], "minecraft:oak_fence"); F([x + 4, 0, 80, x + 4, 2, 80], "minecraft:oak_fence")
    F([x, 3, 80, x + 4, 3, 81], random.choice(["minecraft:red_wool", "minecraft:white_wool", "minecraft:brown_wool"]))
    Bk(x + 2, 0, 80, "minecraft:barrel[facing=up]"); Bk(x + 1, 0, 80, "minecraft:cauldron")
# cimetière de véhicules (chariots et enclumes empilés)
for (x, z) in ((70, 76), (76, 80), (84, 74), (88, 84), (66, 86)):
    F([x, 0, z, x + 3, 1, z + 2], "minecraft:iron_block"); Bk(x + 1, 2, z + 1, "minecraft:anvil[facing=north]"); Bk(x + 2, 2, z, "minecraft:chain[axis=y]")
# feux de camp et lampadaires de fortune
for (x, z) in ((50, 66), (60, 66), (45, 90), (65, 90)): Bk(x, 0, z, "minecraft:campfire[lit=true]")
for z in range(74, 108, 6):
    for x in (42, 68): F([x, 0, z, x, 2, z], "minecraft:stone_brick_wall"); Bk(x, 3, z, "minecraft:lantern[hanging=false]")
Bk(57, 0, 98, "hxh:airship_terminal[facing=north]")
def npc(i, x, y, z, yaw=0): ents.append({"type": "hxh:npc", "pos": [x, y, z], "yaw": yaw, "nbt": '{npc:"%s",NoAI:1b,PersistenceRequired:1b,Invulnerable:1b}' % i})
npc("ferrailleur_meteor", 52, 0, 78, 0); npc("habitant_meteor", 48, 0, 68, 90); npc("habitant_meteor", 62, 0, 92, 270)
json.dump({"x": 1645, "z": 3800, "clear": [0, 0, 0, W, 56, D], "fills": fills, "blocks": blocks, "entities": ents}, open("src/main/resources/data/hxh/world_places/meteor_city.json", "w"), indent=1)
print(len(fills), len(blocks))
